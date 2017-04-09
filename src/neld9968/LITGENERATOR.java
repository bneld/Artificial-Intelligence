package neld9968;

import java.util.Random;

/**
 * This is the chromosome generator that creates new chromosomes.
 * The crossover, mutate, and selection methods determine which alleles are passed to the next generation.
 * Once a new generation is created, the resulting information determines whose genes are passed next.
 * 
 * @author Luis and Brian
 */

public class LITGENERATOR {
	//create like a billion chromosomes
	
	public LITGENERATOR() {
		
	}
	
	//mate chromosomes
	public static LITCHROMOSOME crossover(LITCHROMOSOME parent1, LITCHROMOSOME parent2) {

		int split = LITCHROMOSOME.getRandom(0, 5);
		System.out.println("Split: " + split);
		int[] attribs = new int[7];
		LITCHROMOSOME current = parent1;
		
		for(int i = 0; i < 7; i++){
			attribs[i] = current.getAttributeByIndex(i);
			if(i == split) current = parent2;
		}
		LITCHROMOSOME child = new LITCHROMOSOME(attribs[0]
				,attribs[1],attribs[2],attribs[3],attribs[4],attribs[5],attribs[6]);
		return child;
	}
	
	public static LITCHROMOSOME mutate(LITCHROMOSOME parent) {
		
		Random r = new Random();
		boolean[] mutateAttribute = new boolean[7];
		
		for(int i = 0; i < 7; i++) {
			double probability = r.nextDouble();
			System.out.println("Probability: " + probability);
			
			if(probability <= 0.10) {
				//10% chance to perform mutation 
				mutateAttribute[i] = true;
			} else {
				mutateAttribute[i] = false;
			}
		}

		return mutateHelper(mutateAttribute, parent);
	}
	
	public static LITCHROMOSOME mutateHelper(boolean[] mutateAttribute, LITCHROMOSOME parent) {
		
		int[] attribs = new int[7];
		
		for(int i = 0; i < 7; i++) {
			attribs[i] = parent.getAttributeByIndex(i);
		}
		
		if(mutateAttribute[0]) {
			attribs[0] = LITCHROMOSOME.getRandom(LITCHROMOSOME.MAX_RATE_OF_FIRE, LITCHROMOSOME.MED_RATE_OF_FIRE);
		}
		if(mutateAttribute[1]) {
			attribs[1] = LITCHROMOSOME.getRandom(LITCHROMOSOME.MED_RATE_OF_FIRE, LITCHROMOSOME.MIN_RATE_OF_FIRE);
		}
		if(mutateAttribute[2]) {
			attribs[2] = LITCHROMOSOME.getRandom(LITCHROMOSOME.MIN_DISTANCE_THRESHOLD, LITCHROMOSOME.MED_LOW_DISTANCE_THRESHOLD);
		}
		if(mutateAttribute[3]) {
			attribs[3] = LITCHROMOSOME.getRandom(LITCHROMOSOME.MED_LOW_DISTANCE_THRESHOLD, LITCHROMOSOME.MED_HIGH_DISTANCE_THRESHOLD);
		}
		if(mutateAttribute[4]) {
			attribs[4] = LITCHROMOSOME.getRandom(LITCHROMOSOME.MED_HIGH_DISTANCE_THRESHOLD, LITCHROMOSOME.MAX_DISTANCE_THRESHOLD);
		}
		if(mutateAttribute[5]) {
			attribs[5] = LITCHROMOSOME.getRandom(LITCHROMOSOME.MIN_ASTAR_DISTANCE_THRESHOLD, LITCHROMOSOME.MAX_ASTAR_DISTANCE_THRESHOLD);
		}
		if(mutateAttribute[6]) {
			attribs[6] = LITCHROMOSOME.getRandom(LITCHROMOSOME.MIN_ASTAR_COUNTER, LITCHROMOSOME.MAX_ASTAR_COUNTER);
		}

		LITCHROMOSOME child = new LITCHROMOSOME(attribs[0]
				,attribs[1],attribs[2],attribs[3],attribs[4],attribs[5],attribs[6]);
		return child;
	}
	
	public static void selection() {
		
	}
	public static void main(String[] args){
		LITCHROMOSOME parent1 = new LITCHROMOSOME();
//		LITCHROMOSOME parent2 = new LITCHROMOSOME();
		parent1.print();
//		parent2.print();
//		LITCHROMOSOME punkAssKid = crossover(parent1, parent2);
		LITCHROMOSOME punkAssKid = mutate(parent1);
		punkAssKid.print();
	}
}
