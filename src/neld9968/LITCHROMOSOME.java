package neld9968;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This is the chromosome representation that allows the agent to learn intelligent behavior.
 * The different alleles and their thresholds (to prevent ridiculous values) are defined. 
 * There are useful methods that interact with the data inside of the chromosomes.
 * 
 * @author Luis and Brian
 */
public class LITCHROMOSOME {
	
	
	//ranges for chromosome values
	public final static int MIN_RATE_OF_FIRE = 100; //min > max because the timer counts up
	public final static int MED_RATE_OF_FIRE = 50;
	public final static int MAX_RATE_OF_FIRE = 0;
	
	public final static int MIN_DISTANCE_THRESHOLD = 0;
	public final static int MED_LOW_DISTANCE_THRESHOLD = 75;
	public final static int MED_HIGH_DISTANCE_THRESHOLD = 200;
	public final static int MAX_DISTANCE_THRESHOLD = 500;
	
	public final static int MIN_ASTAR_DISTANCE_THRESHOLD = 0;
	public final static int MAX_ASTAR_DISTANCE_THRESHOLD = 200;
	
	public final static int MIN_ASTAR_COUNTER = 0; //min > max because the timer counts up
	public final static int MAX_ASTAR_COUNTER = 50;
	
	//alleles
	/** Ship will fire every 5 timesteps */
	public int rateOfFireFast = 5;
	
	/** Ship will fire every 10 timesteps */
	public int rateOfFireSlow = 10;
	
	/** Threshold for ship to fire every timestep */
	public int enemyDistanceThresholdClose = 40;
	
	/** Threshold for ship to fire rateOfFireFast */
	public int enemyDistanceThresholdMedium = 100;
	
	/** Threshold for ship to fire rateOfFireSlow */
	public int enemyDistanceThresholdFar = 200;
	
	/** When agent reaches 40 distance from objective, it switches to next objective on A* stack */ 
	public int aStarDistanceThreshold = 40;
	
	/** Recalculate A* every 10 timesteps */
	public int aStarCounter = 10;
	
	//resulting score
	public double score = 0;

	//constuctor initializing random values to every allele
	public LITCHROMOSOME() {
		rateOfFireFast = getRandom(MAX_RATE_OF_FIRE, MED_RATE_OF_FIRE);
		rateOfFireSlow = getRandom(MED_RATE_OF_FIRE, MIN_RATE_OF_FIRE);
		enemyDistanceThresholdClose = getRandom(MIN_DISTANCE_THRESHOLD, MED_LOW_DISTANCE_THRESHOLD);
		enemyDistanceThresholdMedium = getRandom(MED_LOW_DISTANCE_THRESHOLD, MED_HIGH_DISTANCE_THRESHOLD);
		enemyDistanceThresholdFar = getRandom(MED_HIGH_DISTANCE_THRESHOLD, MAX_DISTANCE_THRESHOLD);
		aStarDistanceThreshold = getRandom(MIN_ASTAR_DISTANCE_THRESHOLD, MAX_ASTAR_DISTANCE_THRESHOLD);
		aStarCounter = getRandom(MIN_ASTAR_COUNTER, MAX_ASTAR_COUNTER);
	}
	
	//constructor that takes values for each allele
	public LITCHROMOSOME(int rateOfFireFast, int rateOfFireSlow, int enemyDistanceThresholdClose, int enemyDistanceThresholdMedium, int enemyDistanceThresholdFar, int aStarDistanceThreshold, int aStarCounter){
		this.rateOfFireFast = rateOfFireFast;
		this.rateOfFireSlow = rateOfFireSlow;
		this.enemyDistanceThresholdClose = enemyDistanceThresholdClose;
		this.enemyDistanceThresholdMedium = enemyDistanceThresholdMedium;
		this.enemyDistanceThresholdFar = enemyDistanceThresholdFar;
		this.aStarDistanceThreshold = aStarDistanceThreshold;
		this.aStarCounter = aStarCounter;
	}
	
	/**
	 *  Reads a chromosome from children.csv
	 * @param lineNumber the csv line number from which to read the chromosome
	 */
	public static LITCHROMOSOME getChromosomeFromCsv(int lineNumber){
		String csvFile = "/Users/Luis/Documents/workspace/LITBOIZ/children.csv";
		BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            //read the lines before 
            for(int i = 0; i < lineNumber; i++){
            	br.readLine();
            }
            line = br.readLine();

            // use comma as separator
            String[] chromosome = line.split(cvsSplitBy);
            
            LITCHROMOSOME chromo = new LITCHROMOSOME(Integer.parseInt(chromosome[0]),
            		Integer.parseInt(chromosome[1]),
            		Integer.parseInt(chromosome[2]),
            		Integer.parseInt(chromosome[3]),
            		Integer.parseInt(chromosome[4]),
            		Integer.parseInt(chromosome[5]),
            		Integer.parseInt(chromosome[6]));   
            System.out.println(chromo);
            return chromo;
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
	}
	
	/**
	 * A way of accessing the alleles by index.
	 * @param index specifies which allele
	 * @return value of the relevant allele
	 */
	public int getAttributeByIndex(int index){
		switch(index){
			case 0:
				return this.rateOfFireFast;
			case 1:
				return this.rateOfFireSlow;
			case 2:
				return this.enemyDistanceThresholdClose;
			case 3:
				return this.enemyDistanceThresholdMedium;
			case 4:
				return this.enemyDistanceThresholdFar;
			case 5:
				return this.aStarDistanceThreshold;
			case 6:
				return this.aStarCounter;
			default:
				return 0;
		}
	}

	@Override
	public String toString(){
 		return this.rateOfFireFast + ", "
 				+ this.rateOfFireSlow + ", "
 				+ this.enemyDistanceThresholdClose + ", "
 				+ this.enemyDistanceThresholdMedium + ", "
 				+ this.enemyDistanceThresholdFar + ", "
 				+ this.aStarDistanceThreshold + ", "
				+ this.aStarCounter;
 	}
	public static int getRandom(int low, int high){
		return low + (int)(Math.random() * ((high - low) + 1));
	}
	
	/**
	 * Score is used as the fitness function.
	 * It is the best determination of how the agent is doing.
	 * A negative or lower score represents an agent that is not achieving goals.
	 * A higher score represents one that performs well with the goals.
	 * 
	 * @return score
	 */
	public double fitnessFunction(){
		return this.score;
	}
}
