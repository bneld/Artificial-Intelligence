package neld9968;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
//import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import spacesettlers.objects.weapons.*;

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
	public final static String ACTION_DO_NOTHING = "Do nothing";
	public final static String ACTION_CHASE_ENEMY = "Chase enemy";
	public final static String ACTION_FIND_RESOURCE = "Find resource";
	public final static String ACTION_FIND_BEACON = "Find beacon";
	public final static String ACTION_EVADE = "Evade";
	public final static String ACTION_GO_TO_BASE = "Go to base";
	private static String currentAction = "";
	private static double oldShipEnergy = Double.MIN_VALUE;
	private static int fireTimer;
	private static Position oldEnemyPosition;
	public static Ship ship;
	public static int TIMEOUT = 0;
	public static Stack<Node> stack;
	public static Position aStarCurrentPosition;
	public static final double DEGREES_15 = 0.261799;
	public static int aStarEnemyCounter = 0;
	public static int aStarBeaconCounter = 0;
	
	/**
	 * Gets the action for the ship
	 * @return currentAction
	 */
	public static String getCurrentAction() {
		return currentAction;
	}
	
	/**
	 * Sets the action for the ship
	 * @param action
	 */
	public static void setCurrentAction(String action) {
		currentAction = action;
	}
	
	/**
	 * Gets the energy of the ship at the last time interval.
	 * @return oldShipEnergy 
	 */
	public static double getOldShipEnergy() {
		return oldShipEnergy;
	}
	
	/**Sets the energy of the ship at the last time interval.*/ 
	public static void setOldShipEnergy(double energy) {
		oldShipEnergy = energy;
	}
	
	/**
	 * Gets fire timer to regulate fire rate.
	 * @return fireTimer 
	 */
	public static int getFireTimer() {
		return fireTimer;
	}
	
	/**Sets the fire timer
	 * @param time counter for fire rate
	 */
	public static void setFireTimer(int time) {
		fireTimer = time;
	}
	
	/**Increments the fire timer */
	public static void incFireTimer() {
		fireTimer++;
	}
	
	/**Clears the fire timer*/
	public static void clearFireTimer() {
		fireTimer = 0;
	}
	
	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	public static Beacon pickNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) { // loop through all beacons to find closest
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
	 * @return
	 */
	public static Base findNearestBase(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.MAX_VALUE;
		Base nearestBase = null;

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
	 * @return
	 */
	public static Ship pickNearestEnemyShip(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			
			//TODO
			//experimenting with only targeting Do Nothing Client below
//			if(!(otherShip.getTeamName().equals("DoNothingTeam"))){
//				continue;
//			}
			//experimenting with only targeting Do Nothing Client above
			
			double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return nearestShip;
	}
    
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
    
    public static Set<AbstractObject> getAllObstructions(Toroidal2DPhysics space, Ship ship){
    	Set<AbstractObject> set = space.getAllObjects();
        Iterator<AbstractObject> iterator = set.iterator();
        while(iterator.hasNext()) {
        	AbstractObject obj = iterator.next();
        	if(obj instanceof Beacon
        		|| (obj instanceof Asteroid && ((Asteroid)obj).isMineable())
        		|| (obj instanceof Ship && obj.getId().compareTo(ship.getId()) == 0)
        		|| (obj instanceof Missile)
        		|| (obj instanceof EMP)){
    			iterator.remove();
//    			System.out.println("Removed " + obj.toString());
    		}
        }
    	return set;
    }
    
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

	public static Position getOldEnemyPosition() {
		return oldEnemyPosition;
	}

	public static void setOldEnemyPosition(Position oldEnemyPosition) {
		Mastermind.oldEnemyPosition = oldEnemyPosition;
	}
	
	public static Stack<Node> aStar(Position start, Position target, ArrayList<Position> points, Toroidal2DPhysics space){
		
		Stack<Node> parents = new Stack<>();
		Node lastVisited;
		
		points.add(start);
		if(!points.contains(target)) {
			System.out.println("i hate my life");
			points.add(target);
		}

		LitGraph graph = new LitGraph(start, target, points, space);
//		List<Node> nodes = graph.nodes;
		
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
//			System.out.println("\n\n\n ADDING TO DA FRINGE IN ASTAR \n\n\n");
			fringe.add(child);	
		}
		
		//add start to stack for saving optimal path
		parents.add(graph.startNode);
		lastVisited = graph.startNode;
		
		//start timer
		long startTime = System.nanoTime();
		
		//loop
		while(true) {
			//increment timeout
			TIMEOUT++;
			
			//check for timeout
			if (TIMEOUT > 1000) { //TODO
				System.out.println("TIMEOUT > 1000");
				TIMEOUT = 0;
				return checkStartNode(reverseStack(parents), start);
			}
			
			//target was not found
			if(fringe.isEmpty()){
				System.out.println("FRINGE IS EMPTY");
				return checkStartNode(reverseStack(parents), start);
			}
			
			//find node with next best f(n)
			Node next = fringe.peek();
			
			//h(n)=0 means found target
			if(next.h == 0) {
				parents.add(next);
				System.out.println("FOUND TARGET");
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
//						System.out.println("adding child to the fringe");
						 fringe.add(child);	
					}
				}
			}
		}
	}
	
	public static Stack<Node> checkStartNode(Stack<Node> stack, Position startPosition) {
		if(stack.peek().position.getX() == startPosition.getX() && stack.peek().position.getY() == startPosition.getY()){
			stack.pop();
			System.out.println("gotta remove this here top node");
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
//		System.out.println(goalPosition.toString());
		final double checkAngle = 0.005;
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
	
//	public static Position predictPathConstantAcceleration(Toroidal2DPhysics space, Ship ship, double velocity){
//		AbstractAction action = ship.getCurrentAction();
//		Movement mv = action.getMovement(space, ship);
//		Vector2D accel = mv.getTranslationalAcceleration();
//		//get velocity from Position object
//	}
	
	public static ArrayList<Position> getAlternatePoints(Toroidal2DPhysics space, Ship ship, Position start, Position end, int counter){
		
		//check if points are equal
		if(start.getX() == end.getX() && start.getY() == end.getY()){
			System.out.println("hey its null u suk \n\n");
			return null;
		} 
		//recursion is limited to 5 times
		if (counter > 4) { 
			return null;
		}
		
		ArrayList<Position> result = new ArrayList<>();
		if(isPathClearOfObstructions(start, end, getAllObstructions(space, ship), ship.getRadius(), space)){
			result.add(end);
//			System.out.println("ITS CLEAR!!!!");
		} else {
//			System.out.println("NOT CLEAR YO");
			
			Position toTheRight = alterPath(start, findMidpoint(start, end), DEGREES_15);
			Position toTheLeft = alterPath(start, findMidpoint(start, end), DEGREES_15);
			result.add(toTheLeft);
			result.add(toTheRight);
			if(!isPathClearOfObstructions(start, toTheRight, getAllObstructions(space, ship), ship.getRadius(), space)){
				result.addAll(getAlternatePoints(space, ship, start, toTheRight, ++counter));
			}
			if(!isPathClearOfObstructions(start, toTheLeft, getAllObstructions(space, ship), ship.getRadius(), space)){
				result.addAll(getAlternatePoints(space, ship, start, toTheLeft, ++counter));
			}
		}
		return result;
	}
	
	public static Stack<Node> reverseStack(Stack<Node> stack){
		
		Stack<Node> reversedStack = new Stack<>();
		while(!stack.isEmpty()){
			reversedStack.push(stack.pop());
		}
		return reversedStack;
	}
}
