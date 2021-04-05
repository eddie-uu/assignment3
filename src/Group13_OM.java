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
	private HashMap<String, HashMap<String, Integer>> frequency;
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

	/*
	 * Generates the default of the predicted opponent model
	 */
	private void generateModel() {
		double defaultWeight 	 = 1D / amountOfIssues;
		previousBiddingOfferTime = 0.0;
		frequency				 = new HashMap<String, HashMap<String, Integer>>();
		minModifier			 	 = 0.01;
		maxModifier			 	 = 0.1;
		modifierScaling		 	 = 0.001;

		for (Entry<Objective, Evaluator> evaluator : opponentUtilitySpace.getEvaluators()) {
			opponentUtilitySpace.unlock(evaluator.getKey());
			evaluator.getValue().setWeight(defaultWeight);

			for (ValueDiscrete valueDiscrete : ((IssueDiscrete) evaluator.getKey()).getValues()) {
				((EvaluatorDiscrete) evaluator.getValue()).setEvaluation(valueDiscrete, 1);
				
				HashMap<String, Integer> newValue = new HashMap<String, Integer>();
				newValue.put(valueDiscrete.getValue(), 0);
				if (frequency.containsKey(evaluator.getKey().toString())) {
					frequency.get(evaluator.getKey().toString()).put(valueDiscrete.getValue(), 0);
				} else {
					frequency.put(evaluator.getKey().toString(), newValue);
				}
			}
		}
	}

	/*
	 * Updates the opponent model based on frequency an Value has been offered
	 * Each time a Value has been offered, its evaluation will increase by on
	 * 
	 * Each time a Value is similar to the previous Value of an Issue, the weights
	 * of the issues will increase or decrease
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void updateModel(Bid opponentBid, double time) {
		
		// Return if only one bid has been offered by the opponent in total
		if (negotiationSession.getOpponentBidHistory().size() < 2) { return; }
		
		/*
		 * previousOpponentBid 		 = previous bidDetails the opponent offered
		 * previousOpponentBidValues = the bid within the bidDetails
		 * currentNegotiationTime 	 = total time which has past between current and previous bid
		 * currentModifier			 = modifier by which the weights will be influenced
		 */
		BidDetails previousOpponentBid = negotiationSession.getOpponentBidHistory()
										 	.getHistory()
											.get(negotiationSession.getOpponentBidHistory().size() - 2);
		Bid previousOpponentBidValues = previousOpponentBid.getBid();
		double currentNegotiationTime = time - previousBiddingOfferTime;
		double currentModifier 		  = minModifier;
		previousBiddingOfferTime 	  = time;
		
		// Set default modifier to decrease all weights with in total if round based
		// dynamic modifier if the negotiation is time based
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
		HashMap<String, Integer> highestIssues 				= new HashMap<String, Integer>();
		int sameBid 										= 0;
		
		// Evaluate each Value whether it was in the previous bid as well, increase the 
		// frequency a issue has been offered
		while (opponentBidIterator.hasNext()) {
			Map.Entry pair 	 	 = (Map.Entry) opponentBidIterator.next();
			Value comparison 	 = previousOpponentBidValues.getValue((int) pair.getKey());
			Issue name 			 = opponentBidIssues.get((int) pair.getKey() - 1);
			int currentFrequency = frequency.get(name.getName()).get(comparison.toString()) + 1;

			if (comparison == pair.getValue()) {
				frequency.get(name.getName()).put(comparison.toString(), currentFrequency);
				sameBid++;
			}
			
			bidValues.add(pair.getValue().toString());
			sameValues.put(name.getName(), comparison == pair.getValue());
			opponentBidIterator.remove();
		}
		
		// Generate highestIssues HashMap by specifying which Value was mentioned the most for each Issue
		frequency.entrySet().forEach(entry -> {
			String key = entry.getKey();
			if (sameValues.get(key)) {
				HashMap<String, Integer> value 	= (HashMap<String, Integer>) entry.getValue();
				int highestValue 				= Collections.max(value.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getValue();
				
				highestIssues.put(key, highestValue);
			}
		});

		
		// Generate the rankings of which each issues' weight receives more value or less value
		HashMap<String, Integer> orderedHighestIssues = sortByValue(highestIssues);
		HashMap<String, Integer> worth 				  = new HashMap<String, Integer>();
		Object[] keys 								  = orderedHighestIssues.keySet().toArray();
		int budget 				 					  = 0;
		double best40Percent 	 					  = sameBid * 0.6;
		
		for (int i = 0; i < sameBid; i++) {
			int value = i > best40Percent ? i : 1;
			Object key = keys[i];
			worth.put(key.toString(), value);
			
			budget+= value;
		}
		
		/*
		 * budgetToSame  = the base value with which each weight will be increased with without the ranking modifier
		 * reducedWeight = the base value with which each weight will be decreased with before handing out the increase
		 */
		double budgetToSame  = amountOfIssues != sameBid ? currentModifier / budget : 0;
		double reducedWeight = amountOfIssues != sameBid ? currentModifier / amountOfIssues : 0;

		// Update the opponent model evaluators
		for (Entry<Objective, Evaluator> evaluator : opponentUtilitySpace.getEvaluators()) {
			double currentWeight = evaluator.getValue().getWeight();
			
			// Reduce the weight with of the evaluator with the reducedWeight variable
			evaluator.getValue().setWeight(currentWeight - reducedWeight);
			
			// Increase the weight with the budgetToSame * multiplier by ranking if the Value of
			// current bid issue was the same as the previous bid issue
			if (sameValues.get(evaluator.getKey().toString())) {
				currentWeight = evaluator.getValue().getWeight();
				evaluator.getValue().setWeight(currentWeight + (budgetToSame * worth.get(evaluator.getKey().toString())));
			}
			
			// Increase the evaluation of each Value by one which was present in the current bid
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
	public String getName() { return "2021 - BOAaninho"; }

	@Override
	public Set<BOAparameter> getParameterSpec() { return new HashSet<BOAparameter>(); }
	
	/*
	 * Sort HashMap from highest to lowest
	 */
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