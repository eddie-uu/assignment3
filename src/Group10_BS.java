import java.util.List;
import java.util.Random;
import java.util.Map;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.misc.Range;

public class Group10_BS extends OfferingStrategy{
	private SortedOutcomeSpace outcomespace;
	
	public Group10_BS() { }
	
	public Group10_BS(NegotiationSession negotationSession, OpponentModel model, OMStrategy oms) throws Exception {
		init(negotiationSession, model,oms, null);
	}
	
	@Override
	public void init(NegotiationSession negoSession, OpponentModel om, OMStrategy oms, Map<String,Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.opponentModel 		= om;
		this.omStrategy 		= oms;
		this.outcomespace 		= new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negoSession.setOutcomeSpace(outcomespace);
		
	}

	@Override
	public BidDetails determineOpeningBid() { return returnthisBid(); }
	
	// Method which determines the first bid to be offered to the component.
	@Override
	public BidDetails determineNextBid() { return returnthisBid(); }
    
    public double getTotalTime() { return negotiationSession.getTimeline().getTotalTime(); }

	public double getCurrentTime() { return negotiationSession.getTimeline().getCurrentTime(); }

	public BidDetails returnBidfromList(List<BidDetails> givenlist){
		Random rand 			   = new Random();
	    BidDetails randombiddetail = givenlist.get(rand.nextInt(givenlist.size()));
	    
	    return randombiddetail;
	}
	
	public List<BidDetails> UtilityGoalOptions(double UtilUpBound){
		Range newrange = new Range(UtilUpBound, 1.00);
    	return outcomespace.getBidsinRange(newrange);
	}

	//	Method which determines the bids offered to the opponent after the first bid.
	public BidDetails returnthisBid() {
		double totaltime 	 = getTotalTime();
		double currenttime 	 = getCurrentTime();
		double halftotaltime = totaltime/2;
		
		// If half of the time has passed
    	if( halftotaltime > currenttime) {
    		return returnBidfromList(UtilityGoalOptions(0.95));
    	} else { // If more time is left
    		// Take the minimal utility and decrease it based on the time passed until a certain limit
    		double newcurrenttime =  (currenttime - halftotaltime);
    		double newUtil 		  = 0.95 - ( 0.20 * Math.pow(newcurrenttime/(halftotaltime),3));	
    		
    		return omStrategy.getBid(UtilityGoalOptions(newUtil));
    	}	
	}
	
	@Override
	public String getName() { return "2021 - BOAninho"; }
}


