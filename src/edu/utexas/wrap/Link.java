package edu.utexas.wrap;

import java.util.HashMap;
import java.util.Map;
import java.sql.*;
//import java.sql.DriverManager;
/**
 * @author rahulpatel
 *
 */
public abstract class Link implements Priced {

	private final Float capacity;
	private final Node head;
	private final Node tail;

	private Map<AssignmentContainer,Double> flow;

	private BigDecimal cachedFlow = null;
	protected BigDecimal cachedTT = null;
	protected BigDecimal cachedPrice = null;

	public Link(Node tail, Node head, Float capacity, Float length, Float fftime) {
		this.tail = tail;
		this.head = head;
		this.capacity = capacity;
		this.length = length;
		this.fftime = fftime;

		this.flow = new HashMap<Bush,BigDecimal>();
		try {
			Class.forName("org.postgresql.Driver");
			databaseCon = DriverManager.getConnection("jdbc:postgresql://localhost:5432/network");
			System.out.println("Able to connect to database");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not find database/table to connect to");
			System.exit(3);
		}
	}


	private void createTable(Connection con) {
		Statement stm = null;
		String query = "CREATE TABLE " + hashCode() + "(" +
				"bush_origin_id integer" +
				"vot real" +
				"vehicle_class integer" +
                "flow decimal(" + Optimizer.defMC.getPrecision() + ")" +
				")";
		try {
		    stm = con.createStatement();
		    stm.execute(query);
        } catch (SQLException e) {

        }
	}

	public Float getCapacity() {
		return capacity;
	}

	public Float freeFlowTime() {
		return fftime;
	}

	public Node getHead() {
		return head;
	}

	public Node getTail() {
		return tail;
	}


	public abstract Boolean allowsClass(VehicleClass c);

	/** Modifies the flow on a link which comes from a specified bush. 
	 * <b> THIS METHOD SHOULD ONLY BE CALLED BY THE {@link edu.utexas.wrap.Bush}'s {@link edu.utexas.wrap.Bush.changeFlow} METHOD </b>
	 * @param delta amount by how much the flow should be altered
	 * @param bush the origin Bush of this flow
	 * @return whether the flow from this bush on the link is non-zero
	 */
	public synchronized Boolean alterBushFlow(Double delta, Bush bush) {
		if (delta != 0) {
			cachedTT = null;
			cachedPrice = null;
			cachedFlow = null;
		}
		Double newFlow = flow.getOrDefault(bush,0.0)+ delta.doubleValue();
		if (newFlow < 0) throw new NegativeFlowException("invalid alter request");
		else if (newFlow > 0) flow.put(bush, newFlow);
		else {
			flow.remove(bush);
			return false;
		}
		return true;

	}

	public Float freeFlowTime() {
		return fftime;
	}

	public Double getBushFlow(Bush bush) {
		return flow.getOrDefault(bush, 0.0);
	}

	public Float getCapacity() {
		return capacity;
	}

	public Double getFlow() {
		if (cachedFlow != null) return cachedFlow;
		Double f = flow.values().stream().mapToDouble(Double::doubleValue).sum();
		if (f < 0) throw new NegativeFlowException("Negative link flow");
		cachedFlow = f;
		return f;
	}

	public Node getHead() {
		return head;
	}

	public Float getLength() {
		return length;
	}

	public abstract Double getPrice(Float vot, VehicleClass c);

	public Node getTail() {
		return tail;
	}

	public abstract Double getTravelTime();

	public Boolean hasFlow(Bush bush) {
		return flow.get(bush) != null;
	}

	public abstract Double pricePrime(Float float1);

	public abstract Double tIntegral();

	public String toString() {
		return tail.toString() + "\t" + head.toString();
	}

	public abstract Boolean allowsClass(VehicleClass c);
}