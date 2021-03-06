package edu.utexas.wrap.net;


import edu.utexas.wrap.modechoice.Mode;
import edu.utexas.wrap.net.Link;
import edu.utexas.wrap.net.Node;
import edu.utexas.wrap.net.TolledBPRLink;
import edu.utexas.wrap.net.TolledEnhancedLink;

public class LinkTimeTest {
	public static void main(String... args) {
		Node na = new Node(1,0,null);
		Node nb = new Node(2,1,null);
		Link a = new TolledBPRLink(na, nb, 100F, 100F, 100F, 0.15F, 4F, 0F,0);
		Link b = new TolledEnhancedLink(na,nb, 100F, 100F, 0.15F, 0.15F, 0.15F, 0.15F, 100F, 0.15F, 0.15F, 0.15F, 0.15F, 0.15F, 0.15F, 0.15F, new float[Mode.values().length], new boolean[Mode.values().length],1);
		a.changeFlow(10.0);
		b.changeFlow(10.0);
		double att=0.0,btt=0.0;
		long bstart = System.currentTimeMillis();
		for (int i = 0; i < 100_000_000; i++) {
			btt = b.getTravelTime();
			b.changeFlow(1.0);
		}
		long bend = System.currentTimeMillis();
		long astart = System.currentTimeMillis();
		for (int i = 0; i < 100000000; i++) {
			att = a.getTravelTime();
			a.changeFlow(1.0);
		}
		long aend = System.currentTimeMillis();

		
		System.out.println("BPR:\t"+(aend-astart)+"\t"+att);
		System.out.println("Conic:\t"+(bend-bstart)+"\t"+btt);
	}
}
