package neld9968;

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
	
	public static void mutate() {
		
	}
	
	public static void selection() {
		
	}
	public static void main(String[] args){
		LITCHROMOSOME parent1 = new LITCHROMOSOME();
		LITCHROMOSOME parent2 = new LITCHROMOSOME();
		parent1.print();
		parent2.print();
		LITCHROMOSOME punkAssKid = crossover(parent1, parent2);
		punkAssKid.print();
	}
}
