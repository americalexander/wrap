package edu.utexas.wrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class Network {

	//private Map<Integer, Node> nodes;
//	protected Set<Link> links;
	protected Set<Origin> origins;
	protected Graph graph;
	
	public Network(Set<Origin> origins, Graph g) {
		this.origins = origins;
		graph = g;
	}
	
	public Graph getGraph() {
		return new Graph(graph);
	}
	
	public static Network fromFiles(File linkFile, File odMatrix, File VOTfile) throws Exception {

		LinkedList<Double[]> VOTs = readVOTs(VOTfile);
		
		//////////////////////////////////////////////
		// Read links and build corresponding nodes
		//////////////////////////////////////////////
		
		//TODO externalize this graph constructor phase
		String line;
		Graph g = new Graph();
		BufferedReader lf = new BufferedReader(new FileReader(linkFile));
		Map<Integer,Node> nodes = new HashMap<Integer, Node>();
		Set<Link> links = new HashSet<Link>();
		do { //Move past headers in the file
			line = lf.readLine();
		} while (!line.startsWith("~"));
		
		while (true) { //Iterate through each link (row)
			line = lf.readLine();
			if (line == null) break;	// End of link list reached
			if (line.startsWith("~") || line.trim().equals("")) continue;
			line = line.trim();
			String[] cols 	= line.split("\\s+");
			Integer tail 	= Integer.parseInt(cols[0]);
			Integer head 	= Integer.parseInt(cols[1]);
			Double capacity = Double.parseDouble(cols[2]);
			Double length 	= Double.parseDouble(cols[3]);
			Double fftime 	= Double.parseDouble(cols[4]);
			Double B 		= Double.parseDouble(cols[5]);
			Double power 	= Double.parseDouble(cols[6]);
			Double toll		= Double.parseDouble(cols[8]);
			
			//Create new node(s) if new, then add to map
			if (!nodes.containsKey(tail)) {
				nodes.put(tail,new Node(tail));
			}
			if (!nodes.containsKey(head)) {
				nodes.put(head,new Node(head));
			}
			
			//Construct new link and add to the list
			Link link = new Link(nodes.get(tail), nodes.get(head), capacity, length, fftime, B, power, toll);
			g.addLink(link);
			
			nodes.get(tail).addOutgoing(link);
			nodes.get(head).addIncoming(link);
			links.add(link);
		}
		lf.close();
		
		/////////////////////////////////////
		// Read OD Matrix and assign flows
		/////////////////////////////////////
		//TODO externalize this Origin builder
		BufferedReader of = new BufferedReader(new FileReader(odMatrix));
		Integer origID;
		Node old;
		HashMap<Integer, Double> dests;
		String[] entries;
		Origin o;
		String[] cols;
		Integer destID;
		Double demand;
		HashMap<Integer, Double> bushDests;
		Set<Origin> origins = new HashSet<Origin>();
		
		do { // Move past headers in the file
			line = of.readLine();
		} while (!line.startsWith("Origin"));
		
		while (true) { //While more Origins to read
			origID = Integer.parseInt(line.trim().split("\\s+")[1]);
			old = nodes.get(origID);	// Retrieve the existing node with that ID
			dests = new HashMap<Integer, Double>();
			
			
			while (true) {
				line = of.readLine();
				if (line.trim().startsWith("O") || line.trim().equals("")) break; // If we've reached the gap, move to the next origin
				entries = line.trim().split(";");
				
				for (String entry : entries) {	// For each entry on this line
					cols = entry.split(":");	// Get its values
					destID = Integer.parseInt(cols[0].trim());
					demand = Double.parseDouble(cols[1].trim());
					if (demand > 0.0) dests.put(destID, demand);
				}
			}
			o = new Origin(old); 	// Construct an origin to replace it
			
			for (Double[] entry : VOTs) {
				bushDests = new HashMap<Integer, Double>();
				for (Integer temp : dests.keySet()) {
					bushDests.put(temp, entry[1] * dests.get(temp));
				}
				o.buildBush(links, nodes, entry[0], bushDests);
			}
			origins.add(o);
			
			while (line != null && !line.startsWith("O")) line = of.readLine(); // Read in the origin header
			if (line == null || line.trim().equals("")) break; // If the origin header is empty, we've reached the end of the list
		}
		of.close();
		
		
		return new Network(origins, g);
	}

	/**
	 * @param VOTfile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static LinkedList<Double[]> readVOTs(File VOTfile) throws FileNotFoundException, IOException {
		// TODO handle this better so that breakdowns can be different per origin
		BufferedReader vf = new BufferedReader(new FileReader(VOTfile));
		LinkedList<Double[]> VOTs = new LinkedList<Double[]>();


		String line;
		vf.readLine(); //Ignore header line
		do {
			line = vf.readLine();
			if (line == null) break;
			String[] args = line.split("\t");
			Double vot = Double.parseDouble(args[0]);
			Double vProp = Double.parseDouble(args[1]);
			Double[] entry = {vot, vProp};
			VOTs.add(entry);
		} while (!line.equals(""));
		vf.close();
		return VOTs;
	}
	
	public Set<Link> getLinks() {
		return graph.getLinks();
	}

	public Set<Origin> getOrigins() {
		return origins;
	}
	
	public Double tstt() {
		Double tstt = 0.0;
		
		for(Link l: getLinks()){
			tstt += l.getFlow() * l.getTravelTime();
		}
		return tstt;
	}
	
	public Double relativeGap() {
		Double numerator = 0.0;
		Double denominator = 0.0;
		
		for (Link l : getLinks()) {
			for (Origin o : origins) {
				for (Bush b : o.getBushes()) {
					numerator += l.getBushFlow(b) * l.getPrice(b.getVOT());
				}
			}
		}
		
		for (Origin o : origins) {
			for (Bush b : o.getBushes()) {
				b.topoSearch(false);
				for (Node d : b.getNodes()) {
					
					Double demand = b.getDemand(d.getID());
					try {
						denominator += b.getL(d) * demand;
					} catch (UnreachableException e) {
						if (e.demand > 0.0)	e.printStackTrace();
					}
				}
			}
		}
		
		return (numerator/denominator) - 1.0;
	}
	
	public Double AEC() throws Exception {
		//TODO: Modify for generalized cost
		throw new Exception();
//		Double numerator = tstt();
//		Double denominator = 0.0;
//		
//		for (Origin o : origins) {
//			for (Bush b : o.getBushes()) {
//				b.topoSearch(false);
//				for (Node d : b.getNodes()) {
//					Double demand = o.getDemand(d.getID());
//					if (demand > 0.0) numerator -= b.getL(d) * demand;
//					denominator += demand;
//				}
//			}
//		}
//		
//		return numerator/denominator;
	}
	
	public Double Beckmann() {
		Double b = 0.0;
		for (Link l : getLinks()) {
			b += l.tIntegral();
		}
		return b;
	}
	
	public String toString() {
		String out = "";
		try {
			out += String.format("%6.10E", AEC()) + "\t";
		} catch (Exception e) {
			out += "Error           \t";
		}
		out += String.format("%6.10E",tstt()) + "\t";
		out += String.format("%6.10E",Beckmann()) + "\t";
		out += String.format("%6.10E",relativeGap());
	
		return out;
	}

	public void printFlows(PrintStream out) {
		System.out.println("\r\n\r\nLink\tflow");
		for (Link l : getLinks()) {
			Double sum = 0.0;
			for (Origin o : origins) {
				for (Bush b : o.getBushes()) {
						sum += l.getBushFlow(b);	
				}
			}
			out.println(l+"\t"+sum);
		}
	}

}
