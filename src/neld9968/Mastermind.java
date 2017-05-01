package neld9968;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
//import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Flag;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import spacesettlers.objects.weapons.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import neld9968.LitGraph;
import neld9968.LitGraph.Edge;
import neld9968.LitGraph.Node;


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
	// saves enery of ship at previous state in time
	private double oldShipEnergy = Double.MIN_VALUE;
	// counter that controls fire rate of agent
	private int fireTimer;
	
	private Position oldEnemyPosition;
	public Ship ship;
	public int timeout = 0;
	// stack that holds each node in graph
	public Stack<Node> stack;
	public Position aStarCurrentPosition;
	
	public int aStarEnemyCounter = 0;
	public int aStarBeaconCounter = 0;
	public int aStarBaseCounter = 0;
	public int aStarFlagCounter = 0;
	public AbstractObject currentTarget;
	
	//chromosome parameters
	//static LITCHROMOSOME currentChromosome = initChromosome();
	
	//use genetic algorithm best parameters
	static LITCHROMOSOME currentChromosome = new LITCHROMOSOME(17, 66, 43, 185, 317, 136, 10);

	public static int rateOfFireFast = currentChromosome.rateOfFireFast;
	public static int rateOfFireSlow = currentChromosome.rateOfFireSlow;
	public static int enemyDistanceThresholdClose = currentChromosome.enemyDistanceThresholdClose;
	public static int enemyDistanceThresholdMedium = currentChromosome.enemyDistanceThresholdMedium;
	public static int enemyDistanceThresholdFar = currentChromosome.enemyDistanceThresholdFar;
	public static int aStarDistanceThreshold = currentChromosome.aStarDistanceThreshold;
	public static int aStarCounter = currentChromosome.aStarCounter;
	
	public Mastermind(){
		stack = new Stack<>();
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
	 * Gets the energy of the ship at the last time interval.
	 * @return oldShipEnergy 
	 */
	public double getOldShipEnergy() {
		return oldShipEnergy;
	}
	
	/**Sets the energy of the ship at the last time interval.*/ 
	public void setOldShipEnergy(double energy) {
		oldShipEnergy = energy;
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
	 * returns old enemy position
	 * @return oldEnemyPosition 
	 */
	public Position getOldEnemyPosition() {
		return oldEnemyPosition;
	}

	/**
	 * sets old enemy position
	 * @param oldEnemyPosition
	 */
	public void setOldEnemyPosition(Position oldEnemyPosition) {
		this.oldEnemyPosition = oldEnemyPosition;
	}
	
	/**
	 * a* search algorithm. returns stack of all nodes in graph
	 * @param start position
	 * @param target position
	 * @param points is a list of points besides the start and end nodes
	 * @param space simulator object
	 * @return oldEnemyPosition 
	 */
	public Stack<Node> aStar(Position start, Position target, ArrayList<Position> points, Toroidal2DPhysics space){
		
		// create stack and flags for first initialization 
		Stack<Node> parents = new Stack<>();
		Node lastVisited;
		boolean startExists = false;
		boolean targetExists = false;
		
		// check if start and target are already included in points
		for(Position p : points) {
			if(p.getX() == start.getX() && p.getY() == start.getY()){
				startExists = true;
			}
			if(p.getX() == target.getX() && p.getY() == target.getY()){
				targetExists = true;
			}
		}
		
		// add start or target to points if not already there
		if(!startExists) {
			points.add(start);
		}
		if(!targetExists) {
			points.add(target);
		}

		// create new graph
		LitGraph graph = new LitGraph(this, start, target, points, space);
		
		//set of visited nodes
		Set<Node> closed = new HashSet<>();
		
		//empty priority queue for unexplored nodes
		PriorityQueue<Node> fringe = new PriorityQueue<>(10, new Comparator<Node>(){
			@Override
			public int compare(Node arg0, Node arg1) {
				if(arg0.f == arg1.f) { return 0; }
				//comparator is reversed to put smallest value on top of queue
				else { return (arg0.f < arg1.f) ? 1 : -1; }
			}
		});
		
		graph.startNode.g = 0;
		
		//add all children of initial node to fringe
		for(Edge e : graph.startNode.edges){
			Node child = e.end;
			child.g = e.weight;
			child.f = child.g + child.h; //with priority f(n) = g(n) + h(n)
			fringe.add(child);	
		}
		
		//add start to stack for saving optimal path
		parents.add(graph.startNode);
		lastVisited = graph.startNode;

		//loop
		while(true) {
			//increment timeout
			timeout++;
			
			//check for timeout
			if (timeout > 1000) { 
				timeout = 0;
				return checkStartNode(reverseStack(parents), start);
			}
			
			//target was not found
			if(fringe.isEmpty()){
				return checkStartNode(reverseStack(parents), start);
			}
			
			//find node with next best f(n)
			Node next = fringe.poll();
			
			//h(n)=0 means found target
			if(next.h == 0) {
				parents.add(next);
				return checkStartNode(reverseStack(parents), start);
			}
			
			if(!closed.contains(next)){
				//visit that node
				closed.add(next);
				
				if(!lastVisited.equals(parents.peek())){
					parents.pop();
				}
				
				//add to parent stack
				parents.push(next);
				
				//adding children to fringe
				for(Edge e : next.edges){
					Node child = e.end;
					child.g = e.weight + next.g;
					if(child.f > child.g + child.h) child.f = child.g + child.h;
						
					if(!fringe.contains(child) && !closed.contains(child)){
						 fringe.add(child);	
					}
				}
			}
		}
	}
	
	/**
	 * checks if start node is at top of stack
	 * @return stack of nodes from graph 
	 */
	public static Stack<Node> checkStartNode(Stack<Node> stack, Position startPosition) {
		if(stack.peek().position.getX() == startPosition.getX() && stack.peek().position.getY() == startPosition.getY()){
			stack.pop();
		}
		return stack;
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
	
	/**
	 * returns alternate points when there is an obstruction in the original path (using limited recursion)
	 * @param space simulator object 
	 * @param ship our ship 
	 * @param start position 
	 * @param end position 
	 * @param counter for exit condition 
	 * @return result containing alternate points 
	 */
	public ArrayList<Position> getAlternatePoints(Toroidal2DPhysics space, Ship ship, Position start, Position end, int counter){
		
		//check if points are equal
		if(start.getX() == end.getX() && start.getY() == end.getY()){
			return null;
		} 
	
		//recursion is limited to 5 times
		//if no path is found after 5 recursions, increase angle of search
		if (counter > 4) {
			//TODO
			ArrayList<Position> extraPoints = new ArrayList<>();
			//for(double i = 400; i < 700; i+= 100){
				//Position[] pos = getPerpendicularPositions(start, end, i);
				//extraPoints.add(pos[0]);
				//extraPoints.add(pos[1]);
			//}
			for(double angle : DEGREES_45_TO_85_BY_FIVE){
				Position right = alterPath(start, findMidpoint(start, end), angle); // position to the right 
				Position left = alterPath(start, findMidpoint(start, end), -angle);  // position to the left
				extraPoints.add(right);
				extraPoints.add(left);
			}
			
			return extraPoints;
		}
		
		ArrayList<Position> result = new ArrayList<>();
		// check if path is clear 
		if(isPathClearOfObstructions(start, end, getAllObstructionsBetweenAbstractObjects(space, this.currentTarget), ship.getRadius(), space)){
			//do nothing
		} else {
			
			//TODO 
			//if(distance to obstruction is < 20) DEGREES_45
			// else if (distance to obstruction is < 40) DEGREES_25
			// else { DEGREES_15
			Position toTheRight = alterPath(start, findMidpoint(start, end), DEGREES_25); // position to the right 
			Position toTheLeft = alterPath(start, findMidpoint(start, end), -DEGREES_25);  // position to the left
			
			// add to result 
			result.add(toTheLeft);
			result.add(toTheRight);
			
			// if path to right is clear
			if(!isPathClearOfObstructions(start, toTheRight, getAllObstructions(space, ship), ship.getRadius(), space)){
				ArrayList<Position> alternatePoints = getAlternatePoints(space, ship, start, toTheRight, ++counter);
				if(alternatePoints != null){
					result.addAll(alternatePoints);
				}
			}
			
			//  if path to left is clear
			if(!isPathClearOfObstructions(start, toTheLeft, getAllObstructions(space, ship), ship.getRadius(), space)){
				ArrayList<Position> alternatePoints = getAlternatePoints(space, ship, start, toTheLeft, ++counter);
				if(alternatePoints != null){
					result.addAll(alternatePoints);
				}
			}
		}
		return result;
	}
	
	public static Position[] getPerpendicularPositions(Position start, Position end, double distanceFromTarget){
		double vectorX = end.getX() - start.getX();
		double vectorY = end.getY() - start.getY();
		
		//find perpendicular vector
		double perpX = 1;
		double perpY = (0 - perpX*vectorX) / vectorY;
		double magnitude = Math.sqrt(Math.pow(perpX, 2) + Math.pow(perpY, 2));
		//normalize 
		perpX /= magnitude;
		perpY /= magnitude;
		//scale up
		Position p1 = new Position(end.getX() + distanceFromTarget*perpX, end.getY() + distanceFromTarget*perpY);
		Position p2 = new Position(end.getX() - distanceFromTarget*perpX, end.getY() - distanceFromTarget*perpY);
		return new Position[]{p1, p2};
	}
	
	/**
	 * reverses stack generated from a*
	 * @param stack of nodes in graph
	 * @return reversedStack
	 */
	public static Stack<Node> reverseStack(Stack<Node> stack){
		
		Stack<Node> reversedStack = new Stack<>();
		while(!stack.isEmpty()){ 
			reversedStack.push(stack.pop());
		}
		return reversedStack;
	}
	
	/** Used for genetic algorithm to initialize the chromosome's parameters for the agent.
	 * Knows if it should use random values for first gen or take values from parent generation.
	 * 
	 */
	public static LITCHROMOSOME initChromosome() {
		File numberFile = new File("/Users/Luis/Documents/workspace/LITBOIZ/LITNUMBER.txt");
		boolean randomize = !(new File("/Users/Luis/Documents/workspace/LITBOIZ/children.csv").exists());
		int numberToWrite = 0;
		if(randomize){
			return new LITCHROMOSOME();	
		}
		else {
			try {
				
				if(!numberFile.exists()){
					numberToWrite = 0;
				}
				else {
					//read number line from LITNUMBER.txt
					FileInputStream fileInStream = new FileInputStream(numberFile);
					BufferedReader reader = new BufferedReader(new InputStreamReader(fileInStream));
					String line = reader.readLine();
					
					if(line != null && !line.isEmpty()){
						numberToWrite = Integer.parseInt(line);
					} else {
						numberToWrite = 0;
					}
					reader.close();
				}
				FileWriter f2 = new FileWriter("/Users/Luis/Documents/workspace/LITBOIZ/LITNUMBER.txt", false);
				f2.write(Integer.toString(numberToWrite + 1));		    
			    f2.close();
			    return LITCHROMOSOME.getChromosomeFromCsv(numberToWrite);
			} catch (Exception e) {
	            e.printStackTrace();
	        }
			return null;
		}
	}
}
