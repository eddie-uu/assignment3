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

import genius.core.Bid;
import genius.core.Domain;
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

	/*
	 * Return the best bid offerable to the opponent
	 * allBids = the possible bids which we can offer to the opponent
	 */
	@SuppressWarnings("serial")
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {
		if (allBids.size() == 1) { return allBids.get(0); }
		
		/*
		 * highestEvaluation = check what the utility is of the current bids available
		 * bids 			 = all bids grouped by utility
		 * frequency		 = total amount of times a Value is mentioned in the bids with the highest utility
		 */
		double highestEvaluation 							= Double.NEGATIVE_INFINITY;
		HashMap<Double, ArrayList<BidDetails>> bids 		= new HashMap<Double, ArrayList<BidDetails>>();
		HashMap<Integer, HashMap<Value, Integer>> frequency = new HashMap<Integer, HashMap<Value, Integer>>();
		
		// Generate HashMap values for bids and frequency
		for (BidDetails currentBid : allBids) {
			// Utility of the currentBid according to opponent model
			double evaluation = model.getBidEvaluation(currentBid.getBid());

			// Create new ArrayList object if HashMap key is new, or add bid if HashMap key already exists
			if (bids.containsKey(evaluation)) {
				bids.get(evaluation).add(currentBid);
			} else {
				// Update highestEvaluation, reset frequency HashMap
				if (evaluation > highestEvaluation) {
					highestEvaluation = evaluation;
					frequency 		  = new HashMap<Integer, HashMap<Value, Integer>>();
				}
				
				bids.put(evaluation, new ArrayList<BidDetails>(){{ add(currentBid); }});
			}
			
			// Update frequency HashMap if currentBid has the same utility as the highestEvaluation
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
		
		/*
		 * highestBids		 = ArrayList with the highest bids
		 * frequencyIterator = iterator to loop through
		 * highestValues 	 = HashMap with the highest Value for each Issue
		 */
		ArrayList<BidDetails> highestBids 									= bids.get(highestEvaluation);
		Iterator<Entry<Integer, HashMap<Value, Integer>>> frequencyIterator = frequency.entrySet().iterator();
		HashMap<Integer, Value> highestValues 								= new HashMap<Integer, Value>();
		
		// Generate highestValues HashMap by specifying which Value was mentioned the most for each Issue
		while (frequencyIterator.hasNext()) {
			Map.Entry<Integer, HashMap<Value, Integer>> pair = (Map.Entry<Integer, HashMap<Value, Integer>>) frequencyIterator.next();
			int key											 = pair.getKey();
			HashMap<Value, Integer> value 					 = (HashMap<Value, Integer>) pair.getValue();
			
			// Obtain the Value from the HashMap value with the highest utility
			Value highestValue = Collections.max(value.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();
			highestValues.put(key, highestValue);
			frequencyIterator.remove();
		}
		
		// Create new bid with the best correlation of all the best possible bids
		Bid newNextBid 	   = new Bid(highestBids.get(0).getBid().getDomain(), highestValues);
		BidDetails nextBid = new BidDetails(newNextBid,
											negotiationSession.getUtilitySpace().getUtility(newNextBid),
											negotiationSession.getTime());
		
		// Return the best bid
		return nextBid;
	}

	@Override
	public boolean canUpdateOM() { return negotiationSession.getTime() < updateThreshold; }

	@Override
	public Set<BOAparameter> getParameterSpec() { return new HashSet<BOAparameter>(); }

	@Override
	public String getName() { return "Boaninho opponent model strategy"; }
}