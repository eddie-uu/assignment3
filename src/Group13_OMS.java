import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.Value;

public class Group13_OMS extends OMStrategy {

	double updateThreshold = 1.1;

	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		if (parameters.get("t") != null) {
			updateThreshold = parameters.get("t").doubleValue();
		} else {
			System.out.println("OMStrategy assumed t = 1.1");
		}
	}

	@SuppressWarnings("serial")
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {
		if (allBids.size() == 1) { return allBids.get(0); }
		
		double highestEvaluation 							= Double.NEGATIVE_INFINITY;
		HashMap<Double, ArrayList<BidDetails>> bids 		= new HashMap<Double, ArrayList<BidDetails>>();
		HashMap<Integer, HashMap<Value, Integer>> frequency = new HashMap<Integer, HashMap<Value, Integer>>();
		
		for (BidDetails currentBid : allBids) {
			double evaluation = model.getBidEvaluation(currentBid.getBid());

			if (bids.containsKey(evaluation)) {
				bids.get(evaluation).add(currentBid);
			} else {
				if (evaluation > highestEvaluation) {
					highestEvaluation = evaluation;
					frequency 		  = new HashMap<Integer, HashMap<Value, Integer>>();
				}
				
				bids.put(evaluation, new ArrayList<BidDetails>(){{ add(currentBid); }});
			}
			
			if (evaluation == highestEvaluation) {
				for (Issue issue : currentBid.getBid().getIssues()) {
					int issueId = issue.getNumber();
					
					Value issueValue = currentBid.getBid().getValue(issueId);

					if (!frequency.containsKey(issueId)) { frequency.put(issueId, new HashMap<Value, Integer>()); }
					
					HashMap<Value, Integer> frequencyIssue = frequency.get(issueId);

					if (!frequencyIssue.containsKey(issueValue)) { frequencyIssue.put(issueValue, 0); }

					frequencyIssue.put(issueValue, frequencyIssue.get(issueValue) + 1);
				}
			}
		}
		
		ArrayList<BidDetails> highestBids 									= bids.get(highestEvaluation);
		Iterator<Entry<Integer, HashMap<Value, Integer>>> frequencyIterator = frequency.entrySet().iterator();
		HashMap<Integer, Value> highestValues 								= new HashMap<Integer, Value>();
		
		while (frequencyIterator.hasNext()) {
			Map.Entry<Integer, HashMap<Value, Integer>> pair = (Map.Entry<Integer, HashMap<Value, Integer>>) frequencyIterator.next();
			int key											 = pair.getKey();
			HashMap<Value, Integer> value 					 = (HashMap<Value, Integer>) pair.getValue();
			
			Value highestValue = Collections.max(value.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();
			highestValues.put(key, highestValue);
			frequencyIterator.remove();
		}
		
		Random r 		   = new Random();
		BidDetails bestBid = highestBids.get(r.nextInt(highestBids.size()));
		int mostSimilarities = -1;
		
		for (BidDetails currentBid : highestBids) {
			int similarity = 0;
			for (Issue issue : currentBid.getBid().getIssues()) {
				int issueId = issue.getNumber();				
				Value issueValue = currentBid.getBid().getValue(issueId);
				
				Value highestValue = highestValues.get(issueId);

				if (highestValue == issueValue) { similarity++; }
			}
			
			if (similarity > mostSimilarities) {
				mostSimilarities = similarity;
				bestBid = currentBid;
				System.out.println("Current best bid: " + bestBid.getBid());
			}
		}
		
		
		System.out.println("Highest bid: " + bestBid.getBid());

		BidDetails nextBid = new BidDetails(bestBid.getBid(),
											negotiationSession.getUtilitySpace().getUtility(bestBid.getBid()),
											negotiationSession.getTime());
		
		return nextBid;
	}

	@Override
	public boolean canUpdateOM() { return negotiationSession.getTime() < updateThreshold; }

	@Override
	public Set<BOAparameter> getParameterSpec() { return new HashSet<BOAparameter>(); }

	@Override
	public String getName() { return "Boaninho opponent model strategy"; }
}