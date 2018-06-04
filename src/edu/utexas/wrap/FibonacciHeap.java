package edu.utexas.wrap;

import java.util.AbstractQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class FibonacciHeap<E> extends AbstractQueue<Leaf<E>>{
	private Integer n;
	private Leaf<E> min;
	private List<Leaf<E>> rootList;
	private Map<E,Leaf<E>> map;
	
	public FibonacciHeap(){
		n = 0;
		min = null;
		rootList = new LinkedList<Leaf<E>>();
		map = new HashMap<E,Leaf<E>>();
	}
	
	public boolean add(E node, Double d) throws Exception{
		if (map.get(node) != null) throw new Exception();
		Leaf<E> e = new Leaf<E>(node,d);
		e.degree = 0;
		e.parent = null;
		e.child = new LinkedList<Leaf<E>>();
		e.mark = false;
		map.put(node, e);
		
		return offer(e);

	}

	@Override
	public boolean offer(Leaf<E> e) {
		rootList.add(e);
		if (min == null || e.key < min.key) min = e;
		
		n++;
		return true;
	}


	@Override
	public Leaf<E> peek() {
		return min;
	}

	public void delete(E n) throws Exception {
		Leaf<E> x = map.get(n);
		decreaseKey(x,-Double.MAX_VALUE);
		poll();
	}

	@Override
	public Leaf<E> poll() {
		Leaf<E> z = min;
		if (z != null) {
			for (Leaf<E> x : z.child) {
				rootList.add(x);
				x.parent = null;
			}
			rootList.remove(z);
			if (rootList.isEmpty()) {
				min = null;
			}
			else {
				min = rootList.get(0);
				consolidate();
			}
			n--;
		}
		return z;
	}

	private void consolidate() {
		HashMap<Integer, Leaf<E>> A = new HashMap<Integer, Leaf<E>>();
		Set<Leaf<E>> ignore = new HashSet<Leaf<E>>();
		for (Leaf<E> w : rootList) {
			if (ignore.contains(w)) continue;
			Leaf<E> x = w;
			Integer d = x.degree;
			while (A.get(d) != null) {
				Leaf<E> y = A.get(d);
				if (x.key > y.key) {
					Leaf<E> temp = x;
					x = y;
					y = temp;
				}
				link(x,y, ignore);
				A.remove(d);
				d++;
			}
			A.put(d, x);
		}
		for (Leaf<E> w : ignore) rootList.remove(w);
		min = null;
		for (Integer i : new PriorityQueue<Integer>(A.keySet())) {
			Leaf<E> ai = A.get(i);
			if (ai != null) {
				if (min == null) {
					rootList = new LinkedList<Leaf<E>>();
					rootList.add(ai);
					min = ai;
				}
				else {
					rootList.add(ai);
					if (ai.key < min.key) {
						min = ai;
					}
				}
			}
		}
	}

	private void link(Leaf<E> x, Leaf<E> y, Set<Leaf<E>> ignore) {
		ignore.add(y);
		x.child.add(y);
		x.degree++;
		y.parent = x;
		y.mark = false;
	}

	public void decreaseKey(Leaf<E> x, Double k) throws Exception {
		if (k > x.key) return; //throw new Exception();
		x.key = k;
		Leaf<E> y = x.parent;
		if (y != null && x.key < y.key) {
			cut(x,y);
			cascadingCut(y);
		}
		if (x.key < min.key) min = x; 
	}

	private void cut(Leaf<E> x, Leaf<E> y) {
		y.child.remove(x);
		y.degree--;
		rootList.add(x);
		x.parent = null;
		x.mark = false;
	}

	private void cascadingCut(Leaf<E> y) {
		Leaf<E> z = y.parent;
		if (z != null) {
			if (!y.mark) y.mark = true;
			else {
				cut(y,z);
				cascadingCut(z);
			}
		}
	}

	@Override
	public int size() {
		return n;
	}
	
	@Override
	public boolean isEmpty() {
		return n == 0;
	}

	//TODO: Discuss usage of this method re: duplicating leaves before release
//	public FibonacciHeap<E> merge(FibonacciHeap<E> h2) {
//		FibonacciHeap<E> h = new FibonacciHeap<E>();
//		h.min = min;
//		h.rootList = rootList;
//		h.rootList.addAll(h2.rootList);
//		
//		if (min==null || (h2.min!=null && h2.min.key < min.key)) {
//			h.min = h2.min;
//		}
//		h.n = n+h2.n;
//		return h;
//	}

	@Override
	public Iterator<Leaf<E>> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	public Leaf<E> getLeaf(E head) {
		return map.get(head);
	}
	
	public String toString() {
		return "Heap size="+size()+"\tmin="+min.toString();
	}
}

class Leaf<E>{
	public E n;
	public Double key;
	public Integer degree;
	public Leaf<E> parent;
	public List<Leaf<E>> child;
	public Boolean mark;
	
	public Leaf(E n, Double d) {
		this.n = n;
		this.key = d;
	}

	public String toString() {
		return "Leaf\t"+n.toString()+"\t"+key.toString();
	}

}