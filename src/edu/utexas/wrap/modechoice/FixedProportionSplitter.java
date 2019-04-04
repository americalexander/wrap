package edu.utexas.wrap.modechoice;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import edu.utexas.wrap.net.Node;

import edu.utexas.wrap.demand.containers.ModalHashMatrix;
import it.unimi.dsi.fastutil.Hash;
import javafx.util.Pair;
import edu.utexas.wrap.demand.AggregatePAMatrix;
import edu.utexas.wrap.demand.ModalPAMatrix;

/**The purpose of this class is to return
 * a set of modal PA matrices that contain
 * the proportion of trips that are taken for
 * a particular mode.
 *
 * @author Karthik & Rishabh
 *
 */
public class FixedProportionSplitter extends TripInterchangeSplitter {

	private Map<Mode, Map<Pair<Node, Node>, Float>> map;

	public FixedProportionSplitter(Map<Mode, Map<Pair<Node, Node>, Float>> map) {
		this.map = map;
	}

	/**
	 *	This method returns a set of Modal PA matrices
	 *
	 *  This method	essentially iterates through all
	 *  (i,j) pairs for each mode type and then adds the
	 *  respective proportional value to the a new matrix
	 *  which is added to a set.
	 */
	@Override
	public Set<ModalPAMatrix> split(AggregatePAMatrix aggregate) {
		Set<ModalPAMatrix> fixedProp = new HashSet<ModalPAMatrix>();
		for (Mode m : map.keySet()) {
			Map<Pair<Node,Node>,Float> pctMap = map.get(m);

			// Everything between here and the line marked !!!!! can (and should) be parallelized, I think. -Wm
			ModalPAMatrix pa = new ModalHashMatrix(aggregate.getGraph(), m);
			
			// TODO Consider replacing with Strassen's algorithm
			for (Pair<Node,Node> p : pctMap.keySet()) {
				float proportion = pctMap.get(p);
				if (pctMap.get(p) != 0.0) {
					float demand = aggregate.getDemand(p.getKey(), p.getValue());
					pa.put(p.getKey(), p.getValue(), demand*proportion);
				}
			}
			// !!!!!
			
			fixedProp.add(pa);
		}
		return fixedProp;
	}

}