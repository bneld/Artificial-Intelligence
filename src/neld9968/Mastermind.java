package neld9968;

import spacesettlers.objects.*;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.*;
import spacesettlers.objects.weapons.*;
import java.util.Iterator;
import java.util.Set;


/**
 * Knowledge Representation Class that contains knowledge of the environment and ship.
 * This class can also calculate the closest ships, beacons, and bases.
 * 
 * @author Luis & Brian
 */
public class Mastermind {

	//flags for different actions
	public final String ACTION_DO_NOTHING = "Do nothing";
	public final String ACTION_CHASE_ENEMY = "Chase enemy";
	public final String ACTION_FIND_RESOURCE = "Find resource";
	public final String ACTION_FIND_BEACON = "Find beacon";
	public final String ACTION_FIND_FLAG = "Find flag";
	public final String ACTION_EVADE = "Evade";
	public final String ACTION_GO_TO_BASE = "Go to base";
	
	// degrees in radian form of which to turn
	public static final double DEGREES_15 = 0.261799;
	public static final double DEGREES_25 = 0.436332;
	public static final double[] DEGREES_45_TO_85_BY_FIVE = {0.785398, 0.872665, 0.959931, 1.0472, 1.13446
		                                                         , 1.22173, 1.309, 1.39626, 1.48353};
	private String currentAction = "";
	// saves energy of ship at previous state in time
	// counter that controls fire rate of agent
	private int fireTimer;
	
	public Ship ship;
	public int timeout = 0;
	public Position aStarCurrentPosition;
	
	public int aStarCounter = 30;
	public AbstractObject currentTarget;

	public static int rateOfFireFast = 17;
	public static int rateOfFireSlow = 66;
	public static int enemyDistanceThresholdClose = 43;
	public static int enemyDistanceThresholdMedium = 185;
	public static int enemyDistanceThresholdFar = 317;
	public static int aStarDistanceThreshold = 136;
	public int aStarCounterReplan = 15;
	
	public Mastermind(){
	}
	
	/**
	 * Gets the action for the ship
	 * @return currentAction
	 */
	public String getCurrentAction() {
		return currentAction;
	}
	
	/**
	 * Sets the action for the ship
	 * @param action
	 */
	public void setCurrentAction(String action) {
		currentAction = action;
	}
	
	/**
	 * Gets fire timer to regulate fire rate.
	 * @return fireTimer 
	 */
	public int getFireTimer() {
		return fireTimer;
	}
	
	/**Sets the fire timer
	 * @param time counter for fire rate
	 */
	public void setFireTimer(int time) {
		fireTimer = time;
	}
	
	/**Increments the fire timer */
	public void incFireTimer() {
		fireTimer++;
	}
	
	/**Clears the fire timer*/
	public void clearFireTimer() {
		fireTimer = 0;
	}
	
	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return closestBeacon
	 */
	public static Beacon pickNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		// loop through all beacons to find closest
		for (Beacon beacon : beacons) { 
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
	}
	
	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space
	 * @param ship
	 * @return nearestBase
	 */
	public static Base findNearestBase(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.MAX_VALUE;
		Base nearestBase = null;

		// loop through all bases to find closest
		for (Base base : space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName())) {
				double dist = space.findShortestDistance(ship.getPosition(), base.getPosition());
				if (dist < minDistance) {
					minDistance = dist;
					nearestBase = base;
				}
			}
		}
		return nearestBase;
	}
	
	/**
	 * Find the nearest ship on another team and aim for it
	 * @param space
	 * @param ship
	 * @return nearestShip
	 */
	public static Ship pickNearestEnemyShip(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		
		// loop through all ships
		for (Ship otherShip : space.getShips()) {
			
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			// find shortest distance of ships
			double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return nearestShip;
	}
	
	
	/**
	 * Find the nearest ship on another team and aim for it
	 * @param space
	 * @param ship
	 * @return nearestShip
	 */
	public static Ship pickNearestFlagShip(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		
		// loop through all ships
		for (Ship otherShip : space.getShips()) {
			
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName()) || !otherShip.isCarryingFlag()) {
				continue;
			}
			
			// find shortest distance of ships
			double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return nearestShip;
	}
	
	/**
	 * Find the nearest flag on another team and aim for it
	 * @param space
	 * @param ship
	 * @return flag
	 */
	public static Flag pickNearestEnemyFlag(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Flag nearestFlag = null;
		
		// loop through all ships
		for (Flag flag : space.getFlags()) {
			
			// don't aim for our own team (or ourself)
			//also only aim for flags that are not taken yet
			if (flag.getTeamName().equals(ship.getTeamName())
					|| flag.isBeingCarried()) {
				continue;
			}
			// find shortest distance of ships
			double distance = space.findShortestDistance(ship.getPosition(), flag.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestFlag = flag;
			}
		}
		
		return nearestFlag;
	}
	
	/**
	 * Find the nearest flag on another team and aim for it
	 * @param space
	 * @param ship
	 * @return flag
	 */
	public static Flag getCarriedFlag(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Flag nearestFlag = null;
		
		// loop through all flags
		for (Flag flag : space.getFlags()) {
			
			// don't aim for our own team (or ourself)
			//also only aim for flags that are not taken yet
			if (flag.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			// find shortest distance of ships
			double distance = space.findShortestDistance(ship.getPosition(), flag.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestFlag = flag;
			}
		}
		
		return nearestFlag;
	}
	
	/**
	 * Finds closest enemy flags in the given set
	 * @param space
	 * @param ship
	 * @param set
	 * return closest flag
	 */
	public static Flag findClosestEnemyFlagInSet(Toroidal2DPhysics space, Ship ship, Set<Flag> set){
		double minDistance = Double.POSITIVE_INFINITY;
		Flag nearestFlag = null;
		
		// loop through all ships
		for (Flag flag : set) {
			
			// don't aim for our own team (or ourself)
			//also only aim for flags that are not taken yet
			if (flag.getTeamName().equals(ship.getTeamName())
					|| flag.isBeingCarried()) {
				continue;
			}
			// find shortest distance of flags
			double distance = space.findShortestDistance(ship.getPosition(), flag.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestFlag = flag;
			}
		}
		
		return nearestFlag;
	}
	
	/**
	 * Finds closest asteroid in the given set
	 * @param space
	 * @param ship
	 * @param set
	 * return closest asteroid
	 */
	public static Asteroid findClosestAsteroidInSet(Toroidal2DPhysics space, Ship ship, Set<Asteroid> set){
		double minDistance = Double.POSITIVE_INFINITY;
		Asteroid nearestAsteroid = null;
		
		// loop through all ships
		for (Asteroid asteroid: set) {
			// find shortest distance of ships
			double distance = space.findShortestDistance(ship.getPosition(), asteroid.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestAsteroid = asteroid;
			}
		}
		
		return nearestAsteroid;
	}
    
	/**
	 * Find the midpoint of two objects
	 * @param start
	 * @param objective
	 * @return position in middle
	 */
    public static Position findMidpoint(Position start, Position objective){
    	double x = (objective.getX() + start.getX()) / 2.0;
    	double y = (objective.getY() + start.getY()) / 2.0;
    	return new Position(x, y);
    }
    
    /** 
     * Method that returns a rotated position to veer around obstacles
     * @param space - the simulator environment
     * @param start - the position of your ship
     * @param objective - position of destination
     * @param angle - angle of alternate path in radians
     * @return altered position
     */
    public static Position alterPath(Position start, Position objective, double angle){
    	
    	//translate point to origin 
    	double originX = objective.getX() - start.getX();
    	double originY = objective.getY() - start.getY();
    	
    	//rotate about origin
    	double rotatedOriginX = originX * Math.cos(angle) - originY * Math.sin(angle);
    	double rotatedOriginY = originY * Math.cos(angle) + originX * Math.sin(angle);
    	
    	//translate from origin back to start
    	double rotatedFinalX = rotatedOriginX + start.getX();
    	double rotatedFinalY = rotatedOriginY + start.getY();
    	
    	return new Position(rotatedFinalX, rotatedFinalY);
    }
    
    /** 
     * Method that returns a point along the midpoint axis correspond to specified angle
     * @param space - the simulator environment
     * @param start - the position of your ship
     * @param objective - position of destination
     * @param angle - angle of alternate path in radians
     * @return altered position
     */
    public static Position alterPathAlongMidpointAxis(Toroidal2DPhysics space, Position start, Position objective, double angle){
    	
    	double dist = space.findShortestDistance(start, objective);
    	
    	double deltaX = dist / Math.tan(angle);
    	
    	double rotatedFinalX = deltaX + start.getX();
    	double rotatedFinalY = dist + start.getY();
    	
    	return new Position(rotatedFinalX, rotatedFinalY);
    }
    
	/**
	 * returns set of all obstructions
	 * @param space
	 * @param ship
	 * @return set
	 */
    public static Set<AbstractObject> getAllObstructions(Toroidal2DPhysics space, Ship ship){
    	Set<AbstractObject> set = space.getAllObjects();
        Iterator<AbstractObject> iterator = set.iterator();
        
        // iterate through all objects and remove objects we don't consider an obstruction
        while(iterator.hasNext()) {
        	AbstractObject obj = iterator.next();
        	if(obj instanceof Beacon
        		|| (obj instanceof Asteroid && ((Asteroid)obj).isMineable()) // remove mineable asteroids
        		|| (obj instanceof Ship && obj.getId().compareTo(ship.getId()) == 0) // remove our ship
        		|| (obj instanceof Missile)
        		|| (obj instanceof EMP)){
    			iterator.remove();
    		}
        }
        
    	return set;
    }
    
	/**
	 * returns set of all obstructions between two objects
	 * @param space
	 * @param excludeObject
	 * @return set
	 */
    public Set<AbstractObject> getAllObstructionsBetweenAbstractObjects(Toroidal2DPhysics space, AbstractObject excludeObject){
    	Set<AbstractObject> set = space.getAllObjects();

        // iterate through all objects and remove objects we don't consider an obstruction
        Iterator<AbstractObject> iterator = set.iterator();
        while(iterator.hasNext()) {
        	AbstractObject obj = iterator.next();
        	if(obj instanceof Beacon
        		|| (obj instanceof Asteroid && ((Asteroid)obj).isMineable()) // remove mineable asteroids
        		|| (obj instanceof Missile)
        		|| (obj instanceof EMP)
        		|| (obj.getId().compareTo(excludeObject.getId()) == 0) // remove target
        		|| (obj.getId().compareTo(this.ship.getId()) == 0)){ // remove our ship
    			iterator.remove();
    		}
        }
        
    	return set;
    }
    
	/**
	 * uses straight line interpolation to predict path of any position
	 * @param space
	 * @param old
	 * @param offset
	 * @return predicted position
	 */
    public static Position predictPath(Toroidal2DPhysics space, Position old, Position current, double offset){
        double distanceToObjective = space.findShortestDistance(old, current); // find distance to object
        
        //checking for infinite slope
        if(distanceToObjective < .001) {
        	return current;
        }
        double offsetX = Math.min(distanceToObjective, offset); // make offset
        double slope = (current.getY() - old.getY()) / (current.getX() - old.getX()); // calculate slope
        double newX = (current.getX() - old.getX() > 0 ) ? current.getX() + offsetX : current.getX() - offsetX; // create new x coordinate
        double newY = slope*(newX - current.getX()) + current.getY(); // create new y coordinate
        return new Position(newX, newY); // create new position        
    }
	
	/**
	 * Check to see if following a straight line path between two given locations would result in a collision with a provided set of obstructions
	 * Edited to narrow focus of obstruction analysis
	 * 
	 * @author Andrew and Thibault
	 * 
	 * @param  startPosition the starting location of the straight line path
	 * @param  goalPosition the ending location of the straight line path
	 * @param  obstructions an Set of AbstractObject obstructions (i.e., if you don't wish to consider mineable asteroids or beacons obstructions)
	 * @param  freeRadius used to determine free space buffer size 
	 * @param  space the simulator representation
	 * @return Whether or not a straight line path between two positions contains obstructions from a given set
	 */
	
	public static boolean isPathClearOfObstructions(Position startPosition, Position goalPosition, Set<AbstractObject> obstructions, int freeRadius, Toroidal2DPhysics space) {
		final double checkAngle = 0.349; // 20 degrees
		Vector2D pathToGoal = space.findShortestDistanceVector(startPosition,  goalPosition); 	// Shortest straight line path from startPosition to goalPosition
		double distanceToGoal = pathToGoal.getMagnitude();										// Distance of straight line path

		boolean pathIsClear = true; // Boolean showing whether or not the path is clear
		
		// Calculate distance between obstruction center and path (including buffer for ship movement)
		// Uses hypotenuse * sin(theta) = opposite (on a right hand triangle)
		Vector2D pathToObstruction; // Vector from start position to obstruction
		double angleBetween; 		// Angle between vector from start position to obstruction
		
		// Loop through obstructions
		for (AbstractObject obstruction: obstructions) {
			// If the distance to the obstruction is greater than the distance to the end goal, ignore the obstruction
			pathToObstruction = space.findShortestDistanceVector(startPosition, obstruction.getPosition());
		    if (pathToObstruction.getMagnitude() > distanceToGoal) {
				continue;
			}
		    
			// Ignore angles greater than arbitrary angle
			angleBetween = Math.abs(pathToObstruction.angleBetween(pathToGoal));
			if (angleBetween > checkAngle) {
				continue;
			}

			// Compare distance between obstruction and path with buffer distance
			if (pathToObstruction.getMagnitude() * Math.sin(angleBetween) < obstruction.getRadius() + freeRadius*1.5) {
				pathIsClear = false;
				break;
			}
		}
		return pathIsClear;
		
	}
	
	/**
	 * Returns the asteroid of highest value that isn't already being chased by this team
	 * 
	 * @return
	 */
	public static Asteroid pickHighestValueFreeAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		int bestMoney = Integer.MIN_VALUE;
		Asteroid bestAsteroid = null;

		for (Asteroid asteroid : asteroids) {

			if (asteroid.isMineable() && asteroid.getResources().getTotal() > bestMoney) {
				bestMoney = asteroid.getResources().getTotal();
				bestAsteroid = asteroid;
			}
			
		}
		//System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}
	
}
