import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.BoaParty;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.Bid;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

/**
 * Info voor nu:
 * "BOA components can be made into an independent negotiation party and
 *  which can handle preference uncertainty.
 * 
 * Note that this is equivalent to adding a BOA party via the GUI by selecting
 * the components and parameters. However, this method gives more control over
 * the implementation, as the agent designer can choose to override behavior
 * (such as handling preference uncertainty)."
 *
 */

@SuppressWarnings("serial")
public class Boaparty_boaninho extends BoaParty {
	@Override
	public void init(NegotiationInfo info) {
		// The choice for each component is made here
		AcceptanceStrategy ac = new Group13_AS();
		OfferingStrategy os   = new Group13_BS();
		OpponentModel om 	  = new Group13_OM();
		OMStrategy oms 		  = new Group13_OMS();

		// All component parameters can be set below.
		Map<String, Double> noparams = Collections.emptyMap();
		Map<String, Double> osParams = new HashMap<String, Double>();
		// Set the concession parameter "e" for the offering strategy to yield
		// Boulware-like behavior
		osParams.put("e", 0.2);

		// Initialize all the components of this party to the choices defined above
		configure(ac, noparams, os, osParams, om, noparams, oms, noparams);
		super.init(info);
	}

	@Override
	public AbstractUtilitySpace estimateUtilitySpace() {
		AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(getDomain());
		List<IssueDiscrete> issues = additiveUtilitySpaceFactory.getIssues();
		HashMap<Value, Integer> idHash = new HashMap<Value, Integer>();
		// Looks at every issue
		int id = 0;
		for (IssueDiscrete i : issues) {
			additiveUtilitySpaceFactory.setWeight(i, 1.0 / issues.size());

			HashMap<Integer, int[]> counter = new HashMap<Integer, int[]>();
			// Looks at every value 
			for (ValueDiscrete v : i.getValues()) {
				int a = 0;
				int b = 0;
				idHash.put(v, id);
				
				List<Bid> newbidlist = userModel.getBidRanking().getBidOrder();
				double size 		 = newbidlist.size();
				boolean existing 	 = false;
				
				for (int j = 0; j < newbidlist.size(); j++) {
					if (newbidlist.get(j).containsValue(i, v)) {
						existing = true;
						a += j - b;
						b += 1;
					}
				}

				if (existing) {
					counter.put(id, new int[] { a, b }); // First value is the Value, second value is the frequency
				} else {
					counter.put(id, new int[] { (int) size, 0 });
				}
				id++;
			}
			
			List<Integer> stdlist = new ArrayList<Integer>();

			// Looks at every value 
			for (ValueDiscrete v : i.getValues()) {
				int w = idHash.get(v);
				stdlist.add(counter.get(w)[0] * counter.get(w)[1]); // add value to list to calculate weights with of this issue
				additiveUtilitySpaceFactory.setUtility(i, v, counter.get(w)[0]); // issue, value, valuescore
			}
			
			// set weight of issue
			double sum  = stdlist.stream().mapToInt(Integer::intValue).sum();
			double mean = sum / stdlist.size();
			double std 	= 0;
			double size2 = stdlist.size();

			for(double k : stdlist) { std += Math.pow(mean-k,2); }
			
			std = Math.sqrt((std)/(size2-1));

			additiveUtilitySpaceFactory.setWeight(i, std);
		}
		
		// Normalize the attribute functions, since we gave them integer scores
		additiveUtilitySpaceFactory.scaleAllValuesFrom0To1();
		additiveUtilitySpaceFactory.normalizeWeights();
		
		// The factory is done with setting all parameters, now return the estimated utility space
		return additiveUtilitySpaceFactory.getUtilitySpace();
	}

	@Override
	public String getDescription() { return "2021 - BOAninho BOA party"; }
}