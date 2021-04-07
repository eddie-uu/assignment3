import java.util.Map;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;

public class Group13_AC extends AcceptanceStrategy {
	private double minAcceptanceUtility = 1.0;
	private double concessionRate = 0.005;
	private double maxConcessionrate = 0.005;
	private double targetMinUtility = 1.0;
	private double bestBid = 0.0;
	private double opponentConcessionRate = 0.01;
	private double roundsLeft = 60;
	private double totalRounds = 60;
	private double prevBidUtil = 0.5;
	private String timeBased = "";
	private double consessionFactor = 0.5;
	
	public Group13_AC() { }

	/**
	 * BOAconstructor
	 * @params negotiationSession,offeringStrategy
	* 	@return nothing, its a constructor lol
	*/
	public Group13_AC(NegotiationSession negotiationSession, OfferingStrategy offeringStrategy)  throws Exception {
		init(negotiationSession, offeringStrategy, null, null);
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel, Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy   = strat;	
		this.totalRounds 	    = negotiationSession.getTimeline().getTotalTime();
		this.roundsLeft 	    = totalRounds;
		this.timeBased 		    = negotiationSession.getTimeline().getType().name();	
		
		// maxConsessionRate gets a minimum utility of 0.4
		maxConcessionrate = 6/totalRounds/10;
	}

	@Override
	public Actions determineAcceptability() {
		BidDetails opponentsLastBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
		
		// If the bid is above the minimum threshold and at least 1/e rounds are passed the bid is accepted
		if (opponentsLastBid != null && (opponentsLastBid.getMyUndiscountedUtil() > minAcceptanceUtility) && roundsLeft/totalRounds < 0.36) {
			return Actions.Accept;
		} 

		// Safety net: last bid or 5% if timebased
		if ((timeBased.equals("Rounds") && roundsLeft == 0 || !timeBased.equals("Rounds") && roundsLeft/totalRounds < 0.05) && bestBid * 0.95 < opponentsLastBid.getMyUndiscountedUtil()) {
			System.out.println("ACCEPT");
			return Actions.Accept;
		}
		
		// Calculate the expected opponent concession rate based on the last two bids, used to balance our concession rate
		opponentConcessionRate = opponentsLastBid.getMyUndiscountedUtil() - prevBidUtil;
		
		targetMinUtility += opponentConcessionRate;
		
		if (opponentConcessionRate < 0) { opponentConcessionRate = 0; }
		
		// concede based on the difference between the minimum utility and the expected target, adjusted by rounds
		concessionRate = (minAcceptanceUtility - targetMinUtility) * (totalRounds - roundsLeft) / totalRounds * consessionFactor;
		roundsLeft 	  -= timeBased.equals("Rounds") ? 1 : negotiationSession.getTime();
		
		//concede less if opponent has a high concession rate
		concessionRate = concessionRate-opponentConcessionRate;
		if (concessionRate < 0) { concessionRate = 0; }
		if (concessionRate > maxConcessionrate) { concessionRate = maxConcessionrate; }
		if (minAcceptanceUtility < bestBid) {
			minAcceptanceUtility = bestBid;
		} else {
			minAcceptanceUtility-= concessionRate;
		}
		
		prevBidUtil = opponentsLastBid.getMyUndiscountedUtil();
		
		//set the minimum to the best bid of the opponent
		targetMinUtility = bestBid;
		bestBid = Math.max(opponentsLastBid.getMyUndiscountedUtil(), bestBid);
		return Actions.Reject;
	}

	@Override
	public String getName() { return "2021 - BOAninho"; }
}
