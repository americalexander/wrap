/*
    wrap - free and open-source urban transportation modeling software
    Copyright (C) 2017 the wrap project, The University of Texas at Austin

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.utexas.wrap.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import edu.utexas.wrap.assignment.Path;
import edu.utexas.wrap.net.Graph;
import edu.utexas.wrap.net.Link;
import edu.utexas.wrap.net.Node;


public class SPAlgorithms {
	public static Path dijkstra(Graph g, Node origin, Node destination) {
		
		Map<Node, Link> back = new HashMap<Node, Link>();
		FibonacciHeap Q = new FibonacciHeap();
		
		for (Node n : g.getNodes()) {
			if (!n.equals(origin)) {
				Q.add(n, Double.MAX_VALUE);
			}
		}
		Q.add(origin, 0.0);
		
		while (!Q.isEmpty()) {
			FibonacciLeaf u = Q.poll();
			for (Link uv : u.node.forwardStar()) {
				FibonacciLeaf v = Q.getLeaf(uv.getHead());
				Double alt = uv.getTravelTime() + u.key;
				if (alt < v.key) {
					Q.decreaseKey(v, alt);
					back.put(v.node, uv);
				}
			}
		}
		
		Path path = new Path();
		Node i = destination;
		while (i != origin) {
			Link backLink = back.get(i);
			if (backLink==null) return null;
			path.addFirst(backLink);
			i = backLink.getTail();
		}
		return path;
	}
	
	public static List<Path> kShortestPaths(Graph g, Node origin, Node destination, Integer K) throws Exception{
		List<Path> A = new LinkedList<Path>();
		PriorityQueue<Path> B = new PriorityQueue<Path>();
		A.add(SPAlgorithms.dijkstra(g, origin,destination));
		if (A.get(0) == null) throw new Exception();
		
		for (Integer k = 1; k < K; k++) {
		
			for (Integer i = 0; i < A.get(k-1).size(); i++) {
				Graph gprime = new Graph(g);
				Node spurNode = A.get(k-1).node(i);
				Path rootPath = A.get(k-1).subPath(0,i);
				
				for (Path p : A) {
					if (rootPath.equals(p.subPath(0,i))){
						gprime.remove(p.get(i));
					}
				}
				
				for (Node rootPathNode : rootPath.nodes()) {
					if (!rootPathNode.equals(spurNode)){
						gprime.remove(rootPathNode);
					}
				}
				
				Path spurPath = SPAlgorithms.dijkstra(gprime, spurNode, destination);
				if (spurPath != null) {
					rootPath.append(spurPath);
					B.add(rootPath);	
				}
			}
			
			if (B.isEmpty()) break;
			
			A.add(B.poll());
			
		}
		
		return A;
	}
}

