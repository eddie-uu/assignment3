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

public class Group13_BS extends OfferingStrategy{
//	The init method of the offering strategy is automatically called by the BOA framework with
//	four parameters: the negotiation session, the opponent model, the opponent model strategy, and the parameters of the
//	component.
	SortedOutcomeSpace outcomespace;
	
	private double halfTime;
	private double Pmin; // minimum desired utility
	private double Roundsleft =0;
	private double Timeleft = 0;
	private OpponentModel opponentModel;

	public Group13_BS() { }
	
	public Group13_BS(NegotiationSession negotationSession, OpponentModel model, OMStrategy oms) throws Exception{
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

    //Experiment begint hier
	public BidDetails returnBidfromList(List<BidDetails> givenlist){
		Random rand = new Random();
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
		
		// if half of the time has passed
    	if( halftotaltime > currenttime) {
    		return returnBidfromList(UtilityGoalOptions(0.95));
    	} else { // if more time is left
    		double newcurrenttime =  (currenttime - halftotaltime);
    		
    		// je neemt je minimale utility en die gaat omlaag naarmate de tijd vordert tot het maximum dat je wil. 
    		double newutil = 0.95 -( 0.20 * Math.pow(newcurrenttime/(halftotaltime),3));		
    		
    		return omStrategy.getBid(UtilityGoalOptions(newutil));
    	}
		
	}
	// first create list of all bids
	// then find the bids with an utility higher than the target utility
	// adapt the minimum treshold for the target utility to the highest utility attainable up to now 
	
////// Experiment eindigt hier
	
	@Override
	public String getName() { return "2021 - BOAninho"; }
}


