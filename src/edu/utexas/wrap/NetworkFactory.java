package edu.utexas.wrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkFactory {
	private Graph g;
	private Set<Origin> origins;
	private Integer numZones;

	public NetworkFactory() {}

	public Network getNetwork() {
		if (g == null) 				throw new RuntimeException("Graph has not been constructed. Use a Graph-reading method");
		else if (origins == null)	throw new RuntimeException("OD Matrix has not been constructed. Use an OD Matrix-reading method");
		return new Network(origins,g);
	}

	public void readTNTPUniformVOTtrips(File VOTfile, File odMatrix) throws FileNotFoundException {
		if (g == null) throw new RuntimeException("Graph must be constrcuted before reading OD matrix");
		try {
			readTNTPOriginSpecificProportionalVOTDemand(odMatrix,readUniformVOTDistrib(VOTfile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Used when each origin has a specific VOT proportion breakdown with a single class
	 * 
	 * @param odMatrix
	 * @param VOTs
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void readTNTPOriginSpecificProportionalVOTDemand(File odMatrix, Map<Node, List<Float[]>> VOTs)
			throws FileNotFoundException, IOException {
		/////////////////////////////////////
		// Read OD Matrix and assign flows
		/////////////////////////////////////
		BufferedReader of = new BufferedReader(new FileReader(odMatrix));
		String line;

		HashMap<Integer, Float> bushDests;
		origins = new HashSet<Origin>();

		do { // Move past headers in the file
			line = of.readLine();
		} while (!line.startsWith("Origin"));

		while (true) { //While more Origins to read
			while (line != null && !line.startsWith("O")) line = of.readLine(); // Read in the origin header
			if (line == null || line.trim().equals("")) break; // If the origin header is empty, we've reached the end of the list

			Integer origID = Integer.parseInt(line.trim().split("\\s+")[1]);
			Node root = g.getNode(origID);	// Retrieve the existing node with that ID

			HashMap<Integer, Float> dests = readDestinationDemand(of);
			Origin o = new Origin(root); 	// Construct an origin to replace it

			for (Float[] entry : VOTs.get(root)) {
				bushDests = new HashMap<Integer, Float>();
				for (Integer temp : dests.keySet()) {
					bushDests.put(temp, entry[1] * dests.get(temp));
				}
				o.buildBush(g, entry[0], bushDests, null);
			}
			origins.add(o);
			line = of.readLine();

		}
		of.close();
	}


	/**
	 * @param of
	 * @return
	 * @throws IOException
	 */
	private HashMap<Integer, Float> readDestinationDemand(BufferedReader of) throws IOException {
		String[] cols;
		Integer destID;
		Float demand;
		HashMap<Integer, Float> dests = new HashMap<Integer, Float>();


		while (true) {
			String line = of.readLine();
			if (line == null || line.trim().startsWith("O") || line.trim().equals("")) break; // If we've reached the gap, move to the next origin
			String[] entries = line.trim().split(";");

			for (String entry : entries) {	// For each entry on this line
				cols = entry.split(":");	// Get its values
				destID = Integer.parseInt(cols[0].trim());
				demand = Float.parseFloat(cols[1].trim());
				if (demand > 0.0) dests.put(destID, demand);
			}
		}
		return dests;
	}

	/**
	 * @param linkFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void readTNTPGraph(File linkFile) throws FileNotFoundException, IOException {
		String line;
		g = new Graph();
		BufferedReader lf = new BufferedReader(new FileReader(linkFile));
		Map<Integer,Node> nodes = new HashMap<Integer, Node>();
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
			Float capacity	= parse(cols[2]);
			Float length 	= parse(cols[3]);
			Float fftime 	= parse(cols[4]);
			Float B 		= parse(cols[5]);
			Float power 	= parse(cols[6]);
			Float toll		= parse(cols[8]);

			//Create new node(s) if new, then add to map
			if (!nodes.containsKey(tail)) nodes.put(tail,new Node(tail, false));
			
			if (!nodes.containsKey(head)) nodes.put(head,new Node(head, false));
			

			//Construct new link and add to the list
			Link link = new TolledBPRLink(nodes.get(tail), nodes.get(head), capacity, length, fftime, B, power, toll);
			g.addLink(link);

			nodes.get(tail).addOutgoing(link);
			nodes.get(head).addIncoming(link);
		}
		lf.close();
	}

	/**
	 * @param VOTfile
	 * @param g 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Map<Node, List<Float[]>> readUniformVOTDistrib(File VOTfile) throws FileNotFoundException, IOException {

		BufferedReader vf = new BufferedReader(new FileReader(VOTfile));
		LinkedList<Float[]> VOTs = new LinkedList<Float[]>();


		String line;
		vf.readLine(); //Ignore header line
		do {
			line = vf.readLine();
			if (line == null) break;
			String[] args = line.split("\t");
			Float vot = parse(args[0]);
			Float vProp = parse(args[1]);
			Float[] entry = {vot, vProp};
			VOTs.add(entry);
		} while (!line.equals(""));
		vf.close();

		Map<Node, List<Float[]>> votMap = new HashMap<Node, List<Float[]>>();
		for (Node n : g.getNodes()) {
			votMap.put(n, VOTs);
		}
		return votMap;
	}

	public void readEnhancedGraph(File f, Integer thruNode) throws FileNotFoundException, IOException {
		String line;
		g = new Graph();
		numZones = 0;
		BufferedReader lf = new BufferedReader(new FileReader(f));
		Map<Integer,Node> nodes = new HashMap<Integer, Node>();
		lf.readLine();	//skip header
		
		while (true) {
			line = lf.readLine();
			if (line == null || line.equals("")) break;
			
			String[] args = line.split(",");

			Integer nodeA = Integer.parseInt(args[1]);
			Integer nodeB = Integer.parseInt(args[2]);

			if (!nodes.containsKey(nodeA)) {
				if (nodeA < thruNode) {
					nodes.put(nodeA, new Node(nodeA, true));	
					numZones++;
				}
				else nodes.put(nodeA, new Node(nodeA, false));
			}
			
			if (!nodes.containsKey(nodeB)) {
				if (nodeB < thruNode){
					nodes.put(nodeB, new Node(nodeB, true));	
					numZones++;
				}
				else nodes.put(nodeB, new Node(nodeB, false));

			}
		
			
			Float aCap		= parse(args[3]);
			Float bCap		= parse(args[4]);
			Float length	= parse(args[5]);
			Float ffTimeA	= parse(args[6]);
			Float ffTimeB	= parse(args[7]);
			
			BigDecimal alpha	= BigDecimal.valueOf(parse(args[8]));
			BigDecimal epsilon	= BigDecimal.valueOf(parse(args[9]));
			BigDecimal sParA	= BigDecimal.valueOf(parse(args[10]));
			BigDecimal sParB	= BigDecimal.valueOf(parse(args[11]));
			BigDecimal satFlowA	= BigDecimal.valueOf(parse(args[12]));
			BigDecimal satFlowB	= BigDecimal.valueOf(parse(args[13]));
			
			BigDecimal caA = BigDecimal.valueOf(parse(args[14]));
			BigDecimal cbA = BigDecimal.valueOf(parse(args[15]));
			BigDecimal ccA = BigDecimal.valueOf(parse(args[16]));
			BigDecimal cdA = BigDecimal.valueOf(parse(args[17]));
			
			BigDecimal caB = BigDecimal.valueOf(parse(args[18]));
			BigDecimal cbB = BigDecimal.valueOf(parse(args[19]));
			BigDecimal ccB = BigDecimal.valueOf(parse(args[20]));
			BigDecimal cdB = BigDecimal.valueOf(parse(args[21]));
			
			BigDecimal minDel	= BigDecimal.valueOf(parse(args[22]));
			BigDecimal uParA	= BigDecimal.valueOf(parse(args[23]));
			BigDecimal uParB	= BigDecimal.valueOf(parse(args[24]));
			BigDecimal opCostA	= BigDecimal.valueOf(parse(args[25]));
			BigDecimal opCostB	= BigDecimal.valueOf(parse(args[26]));
			
			Map<VehicleClass, BigDecimal> tollsA = new HashMap<VehicleClass, BigDecimal>();
			Map<VehicleClass, BigDecimal> tollsB = new HashMap<VehicleClass, BigDecimal>();
			
			tollsA.put(VehicleClass.SINGLE_OCC, BigDecimal.valueOf(parse(args[27])));
			tollsB.put(VehicleClass.SINGLE_OCC, BigDecimal.valueOf(parse(args[28])));
			tollsA.put(VehicleClass.HIGH_OCC, BigDecimal.valueOf(parse(args[29])));
			tollsB.put(VehicleClass.HIGH_OCC, BigDecimal.valueOf(parse(args[30])));
			tollsA.put(VehicleClass.MED_TRUCK, BigDecimal.valueOf(parse(args[31])));
			tollsB.put(VehicleClass.MED_TRUCK, BigDecimal.valueOf(parse(args[32])));
			tollsA.put(VehicleClass.HVY_TRUCK, BigDecimal.valueOf(parse(args[33])));
			tollsB.put(VehicleClass.HVY_TRUCK, BigDecimal.valueOf(parse(args[34])));
			
			Map<VehicleClass, Boolean> allowed = new HashMap<VehicleClass, Boolean>();
			
			allowed.put(VehicleClass.SINGLE_OCC, !Boolean.parseBoolean(args[35].trim()));
			
			// TODO: find out which links are not symmetric
			// TODO: figure out what I meant by ^^^ that ^^^ comment
			if (aCap > 0.0) { 
				Link AB = null;
				if(satFlowA.compareTo(BigDecimal.ZERO) > 0) {
					AB = new TolledEnhancedLink(nodes.get(nodeA), nodes.get(nodeB), aCap, length, ffTimeA, alpha, epsilon, sParA, uParA, satFlowA, minDel, opCostA, caA, cbA, ccA, cdA, allowed, tollsA);
				} else if (aCap > 0.0) {
					AB = new CentroidConnector(nodes.get(nodeA), nodes.get(nodeB), aCap, length, ffTimeA, opCostA.floatValue());
				}
				g.addLink(AB);
				nodes.get(nodeA).addOutgoing(AB);
				nodes.get(nodeB).addIncoming(AB);

			}
		

			if (bCap > 0.0) { 
				Link BA = null;
				if (satFlowB.compareTo(BigDecimal.ZERO) > 0) {
					BA = new TolledEnhancedLink(nodes.get(nodeB), nodes.get(nodeA), bCap, length, ffTimeB, alpha, epsilon, sParB, uParB, satFlowB, minDel, opCostB, caB, cbB, ccB, cdB, allowed, tollsB);
				} else {
					BA = new CentroidConnector(nodes.get(nodeB),nodes.get(nodeA), bCap, length, ffTimeB, opCostB.floatValue());
				}
				g.addLink(BA);
				nodes.get(nodeB).addOutgoing(BA);
				nodes.get(nodeA).addIncoming(BA);

			}
		}
		lf.close();
	}
	
	private Float parse(String s) {
		try {
			return Float.parseFloat(s);
		} catch (NumberFormatException e) {
			return 0.0F;
		}
	}

	public void readEnhancedTrips(File odMatrix) throws FileNotFoundException, IOException {
//		Map<Integer, 				// Origin
//			Map<VehicleClass, 		// VehicleClass
//				Map<Double, 		// VOT
//					Map<Integer,	// Destination
//						Double>>>> origMap = new HashMap<Integer, Map<VehicleClass, Map<Double, Map<Integer, Double>>>>();
		BufferedReader of = new BufferedReader(new FileReader(odMatrix));
		String line;
		Integer curOrig = null;
		origins = new HashSet<Origin>();
		Map<Integer, Float> 
				solo17 = null,
				solo35 = null,
				solo45 = null,
				solo90 = null,
				
				hov17 = null,
				hov35 = null,
				hov45 = null,
				hov90 = null,
				
				medTrucks = null,
				hvyTrucks = null;
		
		// read each line and map to correct bush identity
		while (true) {
			line = of.readLine();
			if (line == null || line.trim().equals("")) {
				if (curOrig != null) {
					//build previous bushes
					
					Origin o = new Origin(g.getNode(curOrig));
					o.buildBush(g, 0.17F, solo17, VehicleClass.SINGLE_OCC);
					o.buildBush(g, 0.35F, solo35, VehicleClass.SINGLE_OCC);
					o.buildBush(g, 0.45F, solo45, VehicleClass.SINGLE_OCC);
					o.buildBush(g, 0.90F, solo90, VehicleClass.SINGLE_OCC);
					
					o.buildBush(g, 0.17F, hov17, VehicleClass.HIGH_OCC);
					o.buildBush(g, 0.35F, hov35, VehicleClass.HIGH_OCC);
					o.buildBush(g, 0.45F, hov45, VehicleClass.HIGH_OCC);
					o.buildBush(g, 0.90F, hov90, VehicleClass.HIGH_OCC);
					
					o.buildBush(g, 1.0F, medTrucks, VehicleClass.MED_TRUCK);
					o.buildBush(g, 1.0F, hvyTrucks, VehicleClass.HVY_TRUCK);
					
					origins.add(o);
				}
				break;
			}
			String[] args = line.split(",");
			
			Integer orig = Integer.parseInt(args[0]);
			Integer dest = Integer.parseInt(args[1]);
			
			Float da35 = parse(args[2]);
			Float da90 = parse(args[3]);
			Float sr35 = parse(args[4]);
			Float sr90 = parse(args[5]);
			Float da17 = parse(args[6]);
			Float da45 = parse(args[7]);
			Float sr17 = parse(args[8]);
			Float sr45 = parse(args[9]);
			Float mdtk = parse(args[10]);
			Float hvtk = parse(args[11]);
			
			if (curOrig == null || !orig.equals(curOrig)) {
				//Moving on to next origin
				if (curOrig != null) {
					//build previous bushes
					
					Origin o = new Origin(g.getNode(curOrig));
					o.buildBush(g, 0.17F, solo17, VehicleClass.SINGLE_OCC);
					o.buildBush(g, 0.35F, solo35, VehicleClass.SINGLE_OCC);
					o.buildBush(g, 0.45F, solo45, VehicleClass.SINGLE_OCC);
					o.buildBush(g, 0.90F, solo90, VehicleClass.SINGLE_OCC);
					
					o.buildBush(g, 0.17F, hov17, VehicleClass.HIGH_OCC);
					o.buildBush(g, 0.35F, hov35, VehicleClass.HIGH_OCC);
					o.buildBush(g, 0.45F, hov45, VehicleClass.HIGH_OCC);
					o.buildBush(g, 0.90F, hov90, VehicleClass.HIGH_OCC);
					
					o.buildBush(g, 1.0F, medTrucks, VehicleClass.MED_TRUCK);
					o.buildBush(g, 1.0F, hvyTrucks, VehicleClass.HVY_TRUCK);
					
					origins.add(o);
				}
				
				//Reset maps
				System.out.println("Building bushes for origin "+orig);
				curOrig = orig;
				solo17 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				solo35 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				solo45 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				solo90 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				
				hov17 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				hov35 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				hov45 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				hov90 = new HashMap<Integer, Float>(numZones/2, 1.0f);
				
				medTrucks = new HashMap<Integer, Float>(numZones/2, 1.0f);
				hvyTrucks = new HashMap<Integer, Float>(numZones/2, 1.0f);
			}
			
			if (da17 > 0.0F) solo17.put(dest, da17);
			if (da35 > 0.0F) solo35.put(dest, da35);
			if (da45 > 0.0F) solo45.put(dest, da45);
			if (da90 > 0.0F) solo90.put(dest, da90);
			
			if (sr17 > 0.0F) hov17.put(dest, sr17);
			if (sr35 > 0.0F) hov35.put(dest, sr35);
			if (sr45 > 0.0F) hov45.put(dest, sr45);
			if (sr90 > 0.0F) hov90.put(dest, sr90);
			
			if (mdtk > 0.0F) medTrucks.put(dest, mdtk);
			if (hvtk > 0.0F) hvyTrucks.put(dest, hvtk);
		}
	}
}
