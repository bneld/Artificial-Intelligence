package neld9968;

/**
 * This is the chromosome representation that allows the agent to learn intelligent behavior.
 * The different alleles and their thresholds (to prevent ridiculous values) are defined. 
 * There are useful methods that interact with the data inside of the chromosomes.
 * 
 * @author Luis and Brian
 */
public class LITCHROMOSOME {
	
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

	
	public int rateOfFireFast = 5;
	public int rateOfFireSlow = 10;
	public int enemyDistanceThresholdClose = 40;
	public int enemyDistanceThresholdMedium = 100;
	public int enemyDistanceThresholdFar = 200;
	public int aStarDistanceThreshold = 40;
	public int aStarCounter = 10;
	
	public LITCHROMOSOME() {
		rateOfFireFast = getRandom(MAX_RATE_OF_FIRE, MED_RATE_OF_FIRE);
		rateOfFireSlow = getRandom(MED_RATE_OF_FIRE, MIN_RATE_OF_FIRE);
		enemyDistanceThresholdClose = getRandom(MIN_DISTANCE_THRESHOLD, MED_LOW_DISTANCE_THRESHOLD);
		enemyDistanceThresholdMedium = getRandom(MED_LOW_DISTANCE_THRESHOLD, MED_HIGH_DISTANCE_THRESHOLD);
		enemyDistanceThresholdFar = getRandom(MED_HIGH_DISTANCE_THRESHOLD, MAX_DISTANCE_THRESHOLD);
		aStarDistanceThreshold = getRandom(MIN_ASTAR_DISTANCE_THRESHOLD, MAX_ASTAR_DISTANCE_THRESHOLD);
		aStarCounter = getRandom(MIN_ASTAR_COUNTER, MAX_ASTAR_COUNTER);
	}
	public LITCHROMOSOME(int rateOfFireFast, int rateOfFireSlow, int enemyDistanceThresholdClose, int enemyDistanceThresholdMedium, int enemyDistanceThresholdFar, int aStarDistanceThreshold, int aStarCounter){
		this.rateOfFireFast = rateOfFireFast;
		this.rateOfFireSlow = rateOfFireSlow;
		this.enemyDistanceThresholdClose = enemyDistanceThresholdClose;
		this.enemyDistanceThresholdMedium = enemyDistanceThresholdMedium;
		this.enemyDistanceThresholdFar = enemyDistanceThresholdFar;
		this.aStarDistanceThreshold = aStarDistanceThreshold;
		this.aStarCounter = aStarCounter;
	}
	
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
	public void print(){
		System.out.print(this.rateOfFireFast + ", ");
		System.out.print(this.rateOfFireSlow + ", ");
		System.out.print(this.enemyDistanceThresholdClose + ", ");
		System.out.print(this.enemyDistanceThresholdMedium + ", ");
		System.out.print(this.enemyDistanceThresholdFar + ", ");
		System.out.print(this.aStarDistanceThreshold + ", ");
		System.out.print(this.aStarCounter + "\n");
	}
	
	public static int getRandom(int low, int high){
		return low + (int)(Math.random() * ((high - low) + 1));
	}
}
