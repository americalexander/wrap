package edu.utexas.wrap.net;


import edu.utexas.wrap.assignment.bush.BackVector;
import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.util.NegativeFlowException;

/**
 * @author rahulpatel
 *
 */
public abstract class Link implements Priced, BackVector {

	private final Float capacity, length, fftime;
	private final Node head;
	private final Node tail;
	
	private double flo;

//	private Map<AssignmentContainer,Double> flow;

//	private Double cachedFlow = null;
	protected Double cachedTT = null;

	public Link(Node tail, Node head, Float capacity, Float length, Float fftime) {
		this.tail = tail;
		this.head = head;
		this.capacity = capacity;
		this.length = length;
		this.fftime = fftime;
		
//		flow = new Reference2DoubleOpenHashMap<AssignmentContainer>();
	}
 

	public abstract Boolean allowsClass(Mode c);

	/** Modifies the flow on a link which comes from a specified bush. 
	 * @param delta amount by how much the flow should be altered
	 * @return whether the flow from this bush on the link is non-zero
	 */
	public synchronized Boolean changeFlow(Double delta) {
		if (delta < 0.0 && flo+delta < 0.0 - Math.max(Math.ulp(flo), Math.ulp(delta)))
			throw new RuntimeException("Too much flow removed");
		else if (delta < 0.0 && flo + delta <0.0) flo = 0.0;
		else flo += delta;
		return flo > 0.0;
//
	}

	public Float freeFlowTime() {
		return fftime;
	}


	public Float getCapacity() {
		return capacity;
	}

	public Double getFlow() {
//		if (cachedFlow != null) return cachedFlow;
//		Double f = flow.values().stream().mapToDouble(Double::doubleValue).sum();
		if (flo < 0) throw new NegativeFlowException("Negative flow on link "+this.toString());
//		cachedFlow = f;
		return flo;
	}

	public Node getHead() {
		return head;
	}

	public Float getLength() {
		return length;
	}
	
	public abstract Double getPrice(Float vot, Mode c);

	public Node getTail() {
		return tail;
	}

	public abstract Double getTravelTime();

	public int hashCode() {
		int c = 76537;	//	UT 76-5-37 TAMC \m/
		int b = 1831;	//	Founding of Univ. of Alabama
		int a = 2017;	//	Year of inception for this project
		return (((head.getID()*a + tail.getID())*b + capacity.intValue())*c + fftime.intValue());
	}
	
	public abstract Double pricePrime(Float float1);

	public abstract Double tIntegral();

	public String toString() {
		return tail.toString() + "\t" + head.toString();
	}
	
	public abstract Double tPrime();
}