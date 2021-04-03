import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

public class Group13_OM extends OpponentModel {
	private HashMap<String, Integer> frequency;
	private int amountOfIssues;
	private double totalNegotiationTime;
	private double previousBiddingOfferTime;
	private double minModifier;
	private double maxModifier;
	private double modifierScaling;
	
	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		this.negotiationSession   = negotiationSession;
		this.opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
		this.amountOfIssues       = opponentUtilitySpace.getDomain().getIssues().size();
		this.totalNegotiationTime = negotiationSession.getTimeline().getTotalTime() * 0.05;

		generateModel();
	}

	private void generateModel() {
		double defaultWeight 	 = 1D / amountOfIssues;
		previousBiddingOfferTime = 0.0;
		frequency 			 	 = new HashMap<String, Integer>();
		minModifier			 	 = 0.01;
		maxModifier			 	 = 0.1;
		modifierScaling		 	 = 0.001;

		for (Entry<Objective, Evaluator> evaluator : opponentUtilitySpace.getEvaluators()) {
			opponentUtilitySpace.unlock(evaluator.getKey());
			evaluator.getValue().setWeight(defaultWeight);
			frequency.put(evaluator.getKey().toString(), 0);

			for (ValueDiscrete valueDiscrete : ((IssueDiscrete) evaluator.getKey()).getValues()) {
				((EvaluatorDiscrete) evaluator.getValue()).setEvaluation(valueDiscrete, 1);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) { return; }
		
		BidDetails previousOpponentBid = negotiationSession.getOpponentBidHistory()
										 	.getHistory()
											.get(negotiationSession.getOpponentBidHistory().size() - 2);
		Bid previousOpponentBidValues = previousOpponentBid.getBid();
		
		double currentNegotiationTime = time - previousBiddingOfferTime;
		double currentModifier 		  = minModifier;
		previousBiddingOfferTime 	  = time;
		
		if (negotiationSession.getTimeline().getType().name() == "Rounds") {
			currentModifier = 1.5;
		} else {
			int multiplier = (int) (currentNegotiationTime / totalNegotiationTime);
			currentModifier += (modifierScaling * multiplier);
			
			if (currentModifier > 2) { currentModifier = maxModifier; }
		}
		
		// Current opponent's bid values it offered
		Iterator<Entry<Integer, Value>> opponentBidIterator = opponentBid.getValues().entrySet().iterator();
		ArrayList<String> bidValues 					    = new ArrayList<String>();
		List<Issue> opponentBidIssues 						= opponentBid.getIssues();
		HashMap<String, Boolean> sameValues 				= new HashMap<String, Boolean>();
		int sameBid 										= 0;
		
		while (opponentBidIterator.hasNext()) {
			Map.Entry pair 	 	 = (Map.Entry) opponentBidIterator.next();
			Value comparison 	 = previousOpponentBidValues.getValue((int) pair.getKey());
			Issue name 			 = opponentBidIssues.get((int) pair.getKey() - 1);
			int currentFrequency = frequency.get(name.getName()) + 1;

			if (comparison == pair.getValue()) {
				frequency.put(name.getName(), currentFrequency);
				sameBid++;
			}
			
			bidValues.add(pair.getValue().toString());
			sameValues.put(name.getName(), comparison == pair.getValue());
			opponentBidIterator.remove();
		}

		ArrayList<Integer> worth = new ArrayList<Integer>();
		int budget 				 = 0;
		double best40Percent 	 = sameBid * 0.6;
		
		for (int i = sameBid; i > 0; i--) {
			int value = i > best40Percent ? i : 1;

			worth.add(value);
			budget += value;
		}
		
		double budgetToSame  = amountOfIssues != sameBid ? currentModifier / budget : 0;
		double reducedWeight = amountOfIssues != sameBid ? currentModifier / amountOfIssues : 0;
		frequency 			 = sortByValue(frequency);
		int ranking 		 = 0;
		
		// For each evaluator
		for (Entry<Objective, Evaluator> evaluator : opponentUtilitySpace.getEvaluators()) {
			double currentWeight = evaluator.getValue().getWeight();
			
			evaluator.getValue().setWeight(currentWeight - reducedWeight);
			
			if (sameValues.get(evaluator.getKey().toString())) {
				currentWeight = evaluator.getValue().getWeight();
				evaluator.getValue().setWeight(currentWeight + (budgetToSame * worth.get(ranking)));
				ranking++;
			}
			
			// For each value in evaluator
			for (ValueDiscrete valueDiscrete : ((IssueDiscrete) evaluator.getKey()).getValues()) {
				double currentValue = 1;
				try {
					currentValue = ((EvaluatorDiscrete) evaluator.getValue()).getEvaluation(valueDiscrete);
				} catch (Exception e) { }
				
				// Increase value if offered in bid
				if (bidValues.contains(valueDiscrete.getValue().toString())) { currentValue += 1; }
				
				((EvaluatorDiscrete) evaluator.getValue()).setEvaluation(valueDiscrete, (int) currentValue);
			}
		}
	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() { return "Boaninho opponent model"; }

	@Override
	public Set<BOAparameter> getParameterSpec() { return new HashSet<BOAparameter>(); }
	
	private HashMap<String, Integer> sortByValue(HashMap<String, Integer> hashMap) {
		List<Map.Entry<String, Integer>> linkedList = new LinkedList<Map.Entry<String, Integer>>(hashMap.entrySet());
        HashMap<String, Integer> tempList 			= new LinkedHashMap<String, Integer>();

		Collections.sort(linkedList, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> object1, Map.Entry<String, Integer> object2) {
                return (object1.getValue()).compareTo(object2.getValue());
            }
        });

        for (Map.Entry<String, Integer> temporary : linkedList) { tempList.put(temporary.getKey(), temporary.getValue()); }
        
        return tempList;
	}
}