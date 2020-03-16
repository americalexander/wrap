package edu.utexas.wrap.assignment.bush.algoB;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.utexas.wrap.assignment.AssignmentOptimizer;
import edu.utexas.wrap.assignment.AssignmentProvider;
import edu.utexas.wrap.assignment.AssignmentConsumer;
import edu.utexas.wrap.assignment.bush.Bush;
import edu.utexas.wrap.assignment.bush.BushEvaluator;
import edu.utexas.wrap.net.Graph;

public class AlgorithmBOptimizer implements AssignmentOptimizer<Bush> {
	private final AssignmentProvider<Bush> provider;
	private final AssignmentConsumer<Bush> consumer;
	
	private final AlgorithmBUpdater updater;
	private final AlgorithmBEquilibrator equilibrator;

	private Collection<Bush> queue;
	private final Graph graph;
	private final BushEvaluator evaluator;
	private double threshold;
	private int maxIterations = 10;
	
	public AlgorithmBOptimizer(
			AssignmentProvider<Bush> provider, 
			AssignmentConsumer<Bush> consumer, 
			BushEvaluator evaluator,
			double threshold,
			Graph graph) {
		
		this.provider = provider;
		this.consumer = consumer;
		this.evaluator = evaluator;
		this.threshold = threshold;
		this.graph = graph;
		
		updater = new AlgorithmBUpdater();
		equilibrator = new AlgorithmBEquilibrator();
	}

	@Override
	public void optimize(Stream<Bush> bushStream) {
		queue = bushStream.map( bush -> {
			try{
				provider.getStructure(bush);
			} catch (IOException e) {
				System.err.println("WARN: Could not find source for "+bush+". Ignoring");
				return null;
			}
			
			updater.update(bush);
			
			try {
				consumer.consumeStructure(bush);
			} catch (IOException e) {
				System.err.println("WARN: Could not write structure for "+bush+". Source may be corrupted");
			}
			
			return bush;
		}).collect(Collectors.toSet());

		int numIterations = 0;
		
		while (!queue.isEmpty() && numIterations < maxIterations) {
			numIterations++;
			queue = queue
					.stream()
					.filter(bush -> {
						boolean isEquilibrated = false;
						try{
							provider.getStructure(bush);
						} catch (IOException e) {
							System.err.println("WARN: Could not find source for "+bush+". Ignoring");
							return false;
						}

						synchronized (this) {
							equilibrator.equilibrate(bush);
							isEquilibrated = (evaluator.getValue(bush) < threshold);

						}


						try {
							consumer.consumeStructure(bush);
						} catch (IOException e) {
							System.err.println("WARN: Could not write structure for "+bush+". Source may be corrupted");
						}
						return !isEquilibrated;
					})
					.collect(Collectors.toSet());
		}
	}
	
	
}
