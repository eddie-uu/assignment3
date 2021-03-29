import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

/**
 * BOA framework implementation of the HardHeaded Frequecy Model.
 * 
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 * 
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 */
public class Group13_OM extends OpponentModel {

	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	
	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			learnCoef = parameters.get("l");
		} else {
			learnCoef = 0.2;
		}
		learnValueAddition = 1;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;

		generateModel();
	}

	/**
	 * Init to flat weight (and flat evaluation???) distribution
	 */
	private void generateModel() {
		double defaultWeight = 1D / amountOfIssues;

		for (Entry<Objective, Evaluator> evaluator : opponentUtilitySpace.getEvaluators()) {
			opponentUtilitySpace.unlock(evaluator.getKey());
			evaluator.getValue().setWeight(defaultWeight);

			for (ValueDiscrete valueDiscrete : ((IssueDiscrete) evaluator.getKey()).getValues()) {
				((EvaluatorDiscrete) evaluator.getValue()).setEvaluation(valueDiscrete, 1);
			}
		}
	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		// Return if heuristic is uncheckable
		if (negotiationSession.getOpponentBidHistory().size() < 2) { return; }
		
		List<Issue> opponentBidIssues = opponentBid.getIssues();
		BidDetails previousOpponentBid = negotiationSession.getOpponentBidHistory()
										 	.getHistory()
											.get(negotiationSession.getOpponentBidHistory().size() - 2);
		
		// Influence of heuristic
		double heuristic = opponentBid.getDistance(previousOpponentBid.getBid());
		// Influence of time
		double totalTime = time;

		Iterator<Entry<Integer, Value>> iterator = opponentBid.getValues().entrySet().iterator();
		ArrayList<String> bidValues = new ArrayList<String>();
		
		while (iterator.hasNext()) {
			Map.Entry pair = (Map.Entry) iterator.next();
			bidValues.add(pair.getValue().toString());
			iterator.remove();
		}

		for (Entry<Objective, Evaluator> evaluator : opponentUtilitySpace.getEvaluators()) {
			for (ValueDiscrete valueDiscrete : ((IssueDiscrete) evaluator.getKey()).getValues()) {
				double currentValue = 1;
				try {
					currentValue = ((EvaluatorDiscrete) evaluator.getValue()).getEvaluation(valueDiscrete);
				} catch (Exception e) { }
				
				if (bidValues.contains(valueDiscrete.getValue().toString())) {
					currentValue += 1;
				} else if (currentValue > 1) {
					currentValue -= 1;
				}
				
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
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2,"The learning coefficient determines how quickly the issue weights are learned"));
		return set;
	}
}