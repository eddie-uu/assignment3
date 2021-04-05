import java.util.Map;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;


public class AC_BOAninho extends AcceptanceStrategy {
	
	private double minAcceptanceUtility = 1.0;
	private double concessionRate = 0.005;
	private double targetMinUtility = 1.0;
	private double bestBid = 0.0;
	private double opponentConcessionRate = 0.01;
	private double roundsLeft = 60;
	private double totalRounds = 60;
	private double prevBidUtil = 0.5;
	
	
	public AC_BOAninho() {
	}

	/**
	 * BOAconstructor
	 * @params negotiationSession,offeringStrategy
	* 	@return nothing, its a constructor lol
	*/
	public AC_BOAninho(NegotiationSession negotiationSession, OfferingStrategy offeringStrategy)  throws Exception {
		init(negotiationSession, offeringStrategy, null, null);
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;	
		totalRounds = negotiationSession.getTimeline().getTotalTime();
		roundsLeft = totalRounds;
	}

	@Override
	public Actions determineAcceptability() {
	
		BidDetails opponentsLastBid = negotiationSession.getOpponentBidHistory().getLastBidDetails();
					
//		System.out.println("Min Utility: " + minAcceptanceUtility);
//		System.out.println("Utility: " +opponentsLastBid.getMyUndiscountedUtil());
//		
		//If the bid is above the minimum threshold and at least 1/e rounds are passed the bid is accepted
		if (opponentsLastBid != null && (opponentsLastBid.getMyUndiscountedUtil() > minAcceptanceUtility) && roundsLeft/totalRounds<0.63) {
//			System.out.println("ACCEPT");
			return Actions.Accept;
		}
		
		//Safety net: if in the last 10% accept all bids above 0.95% of the best bid
		if (roundsLeft/totalRounds<0.1 && bestBid * 0.95 < opponentsLastBid.getMyUndiscountedUtil()) {
//			System.out.println("ACCEPT");
			return Actions.Accept;
		}
		
		//Calculate the expected opponent concession rate based on the last two bids, used to balance our concession rate
		opponentConcessionRate = opponentsLastBid.getMyUndiscountedUtil() - prevBidUtil;
//		System.out.println("Opponent concession rate: " +opponentConsessionRate);
		targetMinUtility += opponentConcessionRate;
		if(opponentConcessionRate <0) {
			opponentConcessionRate = 0;
		}
		//concede based on the difference between the minimum utility and the expected target, adjusted by rounds
		concessionRate = (minAcceptanceUtility - targetMinUtility)*(totalRounds - roundsLeft)/totalRounds;
		roundsLeft -= 1;
		//concede less if opponent has a high concession rate
		concessionRate = concessionRate-opponentConcessionRate;
		if(concessionRate<0) {
			concessionRate = 0;
		}
		if(minAcceptanceUtility<bestBid) {
			minAcceptanceUtility = bestBid;
		}else {
			minAcceptanceUtility-= concessionRate;
		}
		prevBidUtil= opponentsLastBid.getMyUndiscountedUtil();
		
		//set the minimum to the best bid of the opponent
		targetMinUtility = bestBid;
//		System.out.println("Target min utility: " +targetMinUtility);
//		System.out.println("Rounds left: " +roundsLeft +  " " + roundsLeft/totalRounds);
		bestBid = Math.max(opponentsLastBid.getMyUndiscountedUtil(),bestBid);
		return Actions.Reject;
	}

	@Override
	public String getName() {
		return "2021 - BOAaninho";
	}
}