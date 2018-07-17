package edu.utexas.wrap;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Bush {

	// Bush structure
	private final Origin origin;
	private final Double vot;
	private final Map<Integer, Double>	destDemand;
	private final Map<Integer, Node> 	nodes; 
	private Set<Link> activeLinks; // Set of active links

	// Labels (for solving)
	private Map<Integer, Link> 		qShort;
	private Map<Integer, Link>		qLong;

	private LinkedList<Node>		topoOrder;


	public Bush(Origin o, Map<Integer,Node> nodes, Set<Link> links, Double vot, Map<Integer, Double> destDemand) {
		origin = o;
		this.vot = vot;
		this.destDemand = destDemand;
		//Initialize flow and status maps
		activeLinks = new HashSet<Link>(links.size(),1.0f);
		//flow	= new HashMap<Link, Double>(links.size(),1.0f);
		this.nodes	= nodes;
		qShort	= new HashMap<Integer, Link>(nodes.size(),1.0f);
		qLong	= new HashMap<Integer, Link>(nodes.size(),1.0f);
		
		try {runDijkstras();} catch (Exception e) { throw new RuntimeException();}
		dumpFlow();
	}

	public Bush(Origin o, Graph g, Double vot, Map<Integer, Double> destDemand) {
		this(o, g.getNodeMap(), g.getLinks(), vot, destDemand);
	}

	public void changeFlow(Link l, BigDecimal delta) {
		l.alterBushFlow(delta, this);
	}

	/**Initialize demand flow on shortest paths
	 * Add each destination's demand to the shortest path to that destination
	 * */
	private void dumpFlow() {
		for (Integer node : nodes.keySet()) {

			if (nodes.get(node).getIncomingLinks().isEmpty()) continue;

			Double x = getDemand(node);
			while (!node.equals(origin.getID())) {
				Link back = qShort.get(node);
				changeFlow(back, BigDecimal.valueOf(x));
				markActive(back);
				node = back.getTail().getID();
			} 
		}

	}

	/** Calculate a topological order using Kahn's algorithm
	 * 
	 * Evaluate the set of bush links, starting from the origin
	 * and determine a topological order for the nodes that they
	 * attach
	 * @return a topological ordering of this bush's nodes
	 */
	public LinkedList<Node> getTopologicalOrder() {
		return (topoOrder != null) ?  topoOrder :  generateTopoOrder();
	}

	/**
	 * @return Nodes in topological order
	 */
	private LinkedList<Node> generateTopoOrder() {
		// Start with a set of all bush edges
		Set<Link> currentLinks = new HashSet<Link>();
		for (Link l : activeLinks) if (isActive(l)) currentLinks.add(l);

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
				if (currentLinks.contains(l)) {

					// remove the links from the set
					currentLinks.remove(l);
					// the node on the other end
					Node m = l.getHead();

					// see if this node has no other incoming active links
					boolean mHasIncoming = false;
					for (Link e : m.getIncomingLinks()) {
						if (currentLinks.contains(e)) {
							mHasIncoming = true;
							break;
						}
					}
					// if not, add to the list of start nodes
					if (!mHasIncoming) S.add(m);
				}
			}
		}
		if (!currentLinks.isEmpty()) {
			throw new RuntimeException("Cyclic graph error");
		}
		else return to;
	}


	/**Generate an initial bush (dag) by solving Dijkstra's Shortest Paths
	 * 
	 * To be called on initialization. Overwrites nodeL and qShort.
	 * @throws Exception if link travel times are unavailable
	 */
	private void runDijkstras() throws Exception {
		// Initialize every nodeL to infinity except this, the origin
		// Initialize the empty map of finalized nodes, the map of 
		// eligible nodes to contain this origin only, and the 
		// back-link mapping to be empty
		Map<Integer, Link> back = new HashMap<Integer, Link>(nodes.size(),1.0f);
		FibonacciHeap<Integer> Q = new FibonacciHeap<Integer>(nodes.size(),1.0f);
		for (Node n : nodes.values()) {
			if (!n.equals(origin)) {
				Q.add(n.getID(), Double.MAX_VALUE);
			}
		}
		Q.add(origin.getID(), 0.0);

		while (!Q.isEmpty()) {
			Leaf<Integer> u = Q.poll();
			//			nodeL.put(u.n, u.key);
			for (Link uv : nodes.get(u.n).getOutgoingLinks()) {
				Leaf<Integer> v = Q.getLeaf(uv.getHead().getID());
				BigDecimal alt = uv.getPrice(vot).add(BigDecimal.valueOf(u.key));
				if (alt.compareTo(BigDecimal.valueOf(v.key)) < 0) {
					Q.decreaseKey(v, alt.doubleValue());
					back.put(v.n, uv);
				}
			}
		}
		qShort = back;
	}


	/**Calculate shortest or longest paths in bush (DAG) using topological search
	 * 
	 * Leverage the presence of a topological order to decrease search time for 
	 * shortest/longest paths calculation
	 * 
	 * @param longest switch for Longest/Shortest
	 * @param to a topological ordering of the nodes
	 */
	public void topoSearch(Boolean longest)  {
		// Initialize all nodeU values as 0 and all nodes as not visited
		List<Node> to = getTopologicalOrder();
		Map<Node, BigDecimal> cache = new HashMap<Node, BigDecimal>(nodes.size());
		//SHORTEST PATHS
		if(!longest) {
			//Initialize infinity-filled nodeL and empty qShort
			qShort = new HashMap<Integer, Link>(nodes.size(),1.0f);
			for (Node d : to) {

				for (Link l : d.getOutgoingLinks()) {
					if (isActive(l)) {
						try {
							BigDecimal Licij = l.getPrice(vot).add(getCachedL(d,cache));

							Node head = l.getHead();
							Integer id = l.getHead().getID();
							if (qShort.get(id) == null || Licij.compareTo(getCachedL(head,cache)) < 0) {
								qShort.put(id, l);
								cache.put(head, Licij);
							}
						} catch (UnreachableException e) {
							if (getDemand(d.getID()) > 0.0) {
								throw new RuntimeException();
							}
						}
					}
				}
			}
		}

		//LONGEST PATHS
		else  {
			qLong = new HashMap<Integer, Link>(nodes.size(),1.0f);
			for (Node d : to) {
				for (Link l : d.getOutgoingLinks()) {
					if (isActive(l)) {
						try {
							BigDecimal Uicij = l.getPrice(vot).add(getCachedU(d,cache));
							Node head = l.getHead();
							Integer id = l.getHead().getID();
							if (qLong.get(id) == null || Uicij.compareTo(getCachedU(head,cache)) > 0) {
								qLong.put(id, l);
								cache.put(head, Uicij);
							}
						} catch (UnreachableException e) {
							if (getDemand(d.getID()) > 0.0) {
								throw new RuntimeException();
							}
						}
					}
				}
			}
		}
	}

	public Link getqShort(Node n) {
		return qShort.get(n.getID());
	}

	public Link getqLong(Node n) {
		return qLong.get(n.getID());
	}

	public Path getShortestPath(Node n) {
		Path p = new Path();
		Link curLink = getqShort(n);
		while (curLink != null) {
			p.addFirst(curLink);
			curLink = getqShort(curLink.getTail());
		}
		return p;
	}

	public Path getLongestPath(Node n) {
		Path p = new Path();
		Link curLink = getqLong(n);
		while (curLink != null) {
			p.addFirst(curLink);
			curLink = getqLong(curLink.getTail());
		}
		return p;
	}

	public BigDecimal getU(Node n) throws UnreachableException {

		Link back = qLong.get(n.getID());
		if (n.equals(origin)) return BigDecimal.ZERO;
		else if (back == null) throw new UnreachableException(n,this);
		else return getU(back.getTail()).add(back.getPrice(vot));

	}

	public BigDecimal getL(Node n) throws UnreachableException {

		Link back = qShort.get(n.getID());
		if (n.equals(origin)) return BigDecimal.ZERO;
		else if (back == null) throw new UnreachableException(n,this);
		else return getL(back.getTail()).add(back.getPrice(vot));
	}
	
	public BigDecimal getCachedU(Node n, Map<Node, BigDecimal> cache) throws UnreachableException {
		Link back = qLong.get(n.getID());
		if (n.equals(origin)) return BigDecimal.ZERO;
		else if (back == null) throw new UnreachableException(n, this);
		else if (cache.containsKey(n)) return cache.get(n);
		else {
			BigDecimal newU = getCachedU(back.getTail(),cache).add(back.getPrice(vot));
			cache.put(n, newU);
			return newU;
		}
	}
	
	public BigDecimal getCachedL(Node n, Map<Node, BigDecimal> cache) throws UnreachableException {
		Link back = qShort.get(n.getID());
		if (n.equals(origin)) return BigDecimal.ZERO;
		else if (back == null) throw new UnreachableException(n,this);
		else if (cache.containsKey(n)) return cache.get(n);
		else {
			BigDecimal newL = getCachedL(back.getTail(), cache).add(back.getPrice(vot));
			cache.put(n,newL);
			return newL;
		}
	}


	public Node getOrigin() {
		return origin;
	}

	public Set<Link> getLinks(){
		return activeLinks;
	}

	public Collection<Node> getNodes() {
		return nodes.values();
	}

	public Double getVOT() {
		return vot;
	}

	public Double getDemand(Integer n) {
		return destDemand.getOrDefault(n, 0.0);
	}

	public String toString() {
		return "ORIG="+origin.getID()+"VOT="+vot;
	}

	private void markActive(Link l) {
		if (activeLinks.add(l)) topoOrder = null;
	}

	private boolean isActive(Link l) {
		return activeLinks.contains(l);
	}

	public void setActive(Set<Link> m) {
		activeLinks = m;
		topoOrder = null;
	}
}
