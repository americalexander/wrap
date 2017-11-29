package edu.utexas.wrap;

/**
 * @author rahulpatel
 *
 */
public class Link {

	private float capacity;
	private Node head;
	private Node tail;
	private float length;
	private float fftime;
	private float b;
	private float power;
	private float flow;

	
	
	public Link(Node tail, Node head, float capacity, float length, float fftime, float b, float power) {
		this.tail = tail;
		this.head = head;
		this.capacity = capacity;
		this.length = length;
		this.fftime = fftime;
		this.b = b;
		this.power = power;
		this.flow = 0.0f;
	}

	//B and power are empirical constants in the BPR function
	public float getBValue() {
		return this.b;
	}
	public void setBValue(float bvalue) {
		this.b = bvalue;
	}
	public float getPower() {
		return power;
	}
	public void setPower(float power) {
		this.power = power;
	}
	public float getCapacity() {
		return capacity;
	}
	public void setCapacity(float capacity) {
		this.capacity = capacity;
	}
	public float getFfTime() {
		return fftime;
	}
	public void setFfTime(float fftime) {
		this.fftime = fftime;
	}
	public Node getHead() {
		return head;
	}
	public void setHead(Node head) {
		this.head = head;
	}
	public Node getTail() {
		return tail;
	}
	public void setTail(Node tail) {
		this.tail = tail;
	}
	public float getLength() {
		return length;
	}
	public void setLength(float length) {
		this.length = length;
	}
	public float getFlow() {
		return this.flow;
	}
	public void setFlow(float flow) {
		this.flow = flow;
	}
	//Used to add deltaflow to current link flow
	public void addFlow(float deltaflow) {
		this.flow += deltaflow;
	}

	/**BPR Function
	 * A link performance function using empirical constants (b and power) and 
	 * link characteristics (current flow, capacity, & free flow travel time)
	 * @return travel time for the link at current flow
	 */
	public float getTravelTime() {
		return (float) (getFfTime()*(1.0 + getBValue()*Math.pow(getFlow()/getCapacity(), getPower())));
	}
	
	public String toString() {
		return this.tail.toString() + " -> " + this.head.toString();
	}

	/**Derivative of {@link getTravelTime} formula
	 * Calculate the derivative of the BPR function with respect to the flow
	 * @return t': the derivative of the BPR function
	 */
	public Float tPrime() {
		// Return (a*b*t*(v/c)^a)/v
		return (float) (getPower()*getBValue()*getFfTime()*Math.pow(getFlow()/getCapacity(), getPower())/getFlow());
	}
	
}
