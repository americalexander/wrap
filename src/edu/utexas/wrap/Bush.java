package edu.utexas.wrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class Bush {

	// Bush structure
	private final Origin origin;
	//This needs to have a topological order
	private final LinkedList<Node> bushNodes; 
	private Set<Link> activeLinks;
	private Set<Link> inactiveLinks;
	
	// Labels (for solving)
	private Map<Integer, Double> 	nodeL;
	private Map<Integer, Double>	nodeU;
	private Map<Integer, Link> 		qShort;
	private Map<Integer, Link>		qLong;
	private Map<Link, Double> 		flow;

	
	public Bush(
			Origin origin,  
			Set<Link> activeLinks,
			Set<Link> inactiveLinks) 
	{
		super();
		this.origin 		= origin;
		this.activeLinks 	= activeLinks;
		this.inactiveLinks	= inactiveLinks;
		this.bushNodes 		= getTopologicalOrder();

	}

	
	/** Calculate a topological order using Kahn's algorithm
	 * 
	 * Evaluate the set of bush links, starting from the origin
	 * and determine a topological order for the nodes that they
	 * attach
	 * @return a topological ordering of this bush's nodes
	 */
	private LinkedList<Node> getTopologicalOrder() {
		// Start with a set of all bush edges
		Set<Link> links = new HashSet<Link>(activeLinks);
		LinkedList<Node> to = new LinkedList<Node>();
		LinkedList<Node> S = new LinkedList<Node>();
		// "start nodes"
		S.add(origin);
		Node n;
		
		while (!S.isEmpty()) {
			n = S.pop();// remove node from S
			to.add(n); 	// append node to L
			
			// for each active edge out of this node
			for (Link l : n.getOutgoingLinks()) {
				if (links.contains(l)) {
					
					// remove the links from the set
					links.remove(l);
					// the node on the other end
					Node m = l.getHead();
					
					// see if this node has no other incoming active links
					boolean mHasIncoming = false;
					for (Link e : m.getIncomingLinks()) {
						if (links.contains(e)) {
							mHasIncoming = true;
							break;
						}
					}
					// if not, add to the list of start nodes
					if (!mHasIncoming) S.add(m);
				}
			}
		}
		if (!links.isEmpty()) {
			//throw new Exception();
			return null;
		}
		else return to;
	}


	public void setqShort(Map<Integer, Link> qShort) {
		this.qShort = qShort;
	}


	public void setNodeL(Map<Integer, Double> nodeL) {
		this.nodeL = nodeL;
	}
	
	public void runDijkstras(Boolean longest) {
		// Initialize every nodeL to infinity except this, the origin
				// Initialize the empty map of finalized nodes, the map of 
				// eligible nodes to contain this origin only, and the 
				// back-link mapping to be empty
				Set<Integer> finalized = new HashSet<Integer>();
				Set<Integer> eligible  = new HashSet<Integer>();

				nodeL.put(origin.getID(), new Double(0.0));
				eligible.add(origin.getID());
				
				// While not all nodes have been reached
				while (true) {
					// Find eligible node of minimal nodeL
					Node i = null;
					for (Integer nodeID : eligible) {
						Node node = bushNodes.get(nodeID);
						if ( i == null || nodeL.get(node.getID()) < nodeL.get(i.getID()) ) {
							i = node;
						}
					}
					
					//DEBUG CODE BELOW
					if (i == null) break;
					//DEBUG CODE ABOVE
					
					// Finalize node by adding to finalized
					finalized.add(i.getID());
					// And remove from eligible
					eligible.remove(i.getID());
					
					// If all nodes finalized, terminate
					if (finalized.size() >= bushNodes.size()) break;
					
					// Update labels and backnodes for links leaving node i
					for (Link link : i.getOutgoingLinks()) {
						Node j = link.getHead();
						
						// nodeL(j) = min( nodeL(j), nodeL(i)+c(ij) )
						Double Lj    = nodeL.get(j.getID());
						Double Licij = nodeL.get(i.getID())+link.getTravelTime();
						if (Lj == null || Licij < Lj) {
							nodeL.put(j.getID(), Licij);
							qShort.put(j.getID(), link);
						}
						if (!finalized.contains(j.getID())) eligible.add(j.getID());
					}
				}
	}
	
	
}
