package neld9968;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
//import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.AbstractAction;
import spacesettlers.utilities.Movement;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.Vector;

import neld9968.Mastermind.Graph;
import neld9968.Mastermind.Graph.Edge;
import neld9968.Mastermind.Graph.Node;


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
	 * Gets the beacon action
	 * @param space
	 * @param ship
	 * @return
	 */
	public static AbstractAction getBeaconAction(Toroidal2DPhysics space, Ship ship){
		AbstractAction newAction = null;
		Position currentPosition = ship.getPosition();
		Beacon beacon = pickNearestBeacon(space, ship);
//		Base base = findNearestBase(space, ship);
//				if(beacon == null || space.findShortestDistance(currentPosition, base.getPosition()) 
//						< space.findShortestDistance(currentPosition, beacon.getPosition())){
//					//base is closer
//					newAction = new MoveToObjectAction(space, currentPosition, base);
//					currentAction = ACTION_GO_TO_BASE;
//					System.out.println("Go To Base");
//					aimingForBase.put(ship.getId(), true);
//				}
		if(beacon == null){ //return to base
			Base base = findNearestBase(space, ship);
			newAction = new MoveToObjectAction(space, currentPosition, base);
			currentAction = ACTION_GO_TO_BASE;
		}
		else {  //locate beacon and move to it
			Position beaconPos = beacon.getPosition();
			double distanceToBeacon = space.findShortestDistance(beaconPos, currentPosition);
	        if(beaconPos.getX() == currentPosition.getX()){ //prevent infinite slope
	            newAction = new MoveToObjectAction(space, currentPosition, beacon);
//				System.out.println("Move Directly To Beacon");

	        }
	        else if(distanceToBeacon < 50){ //slow down and directly target enemy
	            newAction = new MoveToObjectAction(space, currentPosition, beacon);
//				System.out.println("Move Directly To Beacon");
	        }
	        else { //this inflates the objective position
	        	Position inflatedBeaconPosition = inflatePosition(space, currentPosition, beaconPos, 200);
				
				//check for obstacles
//				if(!space.isPathClearOfObstructions(currentPosition, inflatedBeaconPosition, space.getAllObjects(), 30)){
//					System.out.println("AWWWWW POOP ICEBERG AHEAD");
//				}
				
				newAction = new MoveAction(space, currentPosition, inflatedBeaconPosition);
//				System.out.println("Move To Inflated Beacon Position");

	        }
			currentAction = ACTION_FIND_BEACON;
//			aimingForBase.put(ship.getId(), false);
		}			
		return newAction;
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
	
	/**
	 * Targets past objective to move quicker
	 * @param space
	 * @param start
	 * @param objective
	 * @param offset
	 * @return
	 */
    public static Position inflatePosition(Toroidal2DPhysics space, Position start, Position objective, double offset){
        double distanceToObjective = space.findShortestDistance(start, objective); // find distance to object
        double offsetX = Math.min(distanceToObjective, offset); // make offset
        double slope = (objective.getY() - start.getY()) / (objective.getX() - start.getX()); // calculate slope
        double newX = (objective.getX() - start.getX() > 0 ) ? objective.getX() + offsetX : objective.getX() - offsetX; // create new x coordinate
        double newY = slope*(newX - objective.getX()) + objective.getY(); // create new y coordinate
        Position inflatedPosition = new Position(newX, newY); // create new position
        return inflatedPosition;
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
    
    public static Set<AbstractObject> getAllObstructions(Toroidal2DPhysics space){
    	Set<AbstractObject> set = space.getAllObjects();
        Iterator<AbstractObject> iterator = set.iterator();
        while(iterator.hasNext()) {
        	AbstractObject obj = iterator.next();
        	if(obj instanceof Beacon
        		|| (obj instanceof Asteroid && ((Asteroid)obj).isMineable())){
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
		points.add(target);
		Graph graph = new Graph(start, target, points, space);
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
			fringe.add(child);	
		}
		
		//add start to stack for saving optimal path
		parents.add(graph.startNode);
		lastVisited = graph.startNode;
		
		//start timer
		long startTime = System.nanoTime();
		
		//loop
		while(true) {
			
			//check for timeout
			long currentTime = System.nanoTime();
			System.out.println((currentTime - startTime) / 1000000);
			if ((currentTime - startTime) / 1000000 > 300) { //TODO
				return parents;
			}
			
			//target was not found
			if(fringe.isEmpty()){
				return null;
			}
			
			//find node with next best f(n)
			Node next = fringe.peek();
			
			//h(n)=0 means found target
			if(next.h == 0) {
				parents.add(next);
				return parents;
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
	
	class PositionNode{
		Position position;
		double f;
		
		public PositionNode(Position position, double f){
			this.position = position;
			this.f = f;
		}
	}
	
	static class Graph {
		
		public List<Node> nodes;
		public List<Edge> edges;
		Map<Position, Node> map;
		Node startNode;
		Node targetNode;
		
		public Graph(Position start, Position target, List<Position> points, Toroidal2DPhysics space){
			nodes = new ArrayList<>();
			edges = new ArrayList<>();
			map = new HashMap<>();
			
			//add all nodes
			for(Position pos : points){
				Node node = new Node(pos, space.findShortestDistance(pos, target));
				nodes.add(node);
				map.put(pos, node);
			}
			
			//find edges
			for(Position current : points){
				for(Position other : points){
					if(space.isPathClearOfObstructions(current, other, getAllObstructions(space), Ship.SHIP_RADIUS)){
						addEdge(map.get(current), map.get(other), space.findShortestDistance(current, other));
					}
				}
			}
			startNode = map.get(start);
			targetNode = map.get(target);
		}
		
		public void addEdge(Node x, Node y, double weight){
			Edge edgeX = new Edge(x, y, weight);
			x.edges.add(edgeX);
			Edge edgeY = new Edge(y, x, weight);
			y.edges.add(edgeY);
		}
		
		class Node {
			public Position position;
			public List<Edge> edges;
			public double f = Double.MAX_VALUE;
			public double h;
			public double g;
			
			public Node(Position position, double h){
				this.position = position;
				edges = new ArrayList<>();
				this.h= h;
			}
			
			public String toString(){
				return "(" + position.getX() + ", " + position.getY() + ")";
			}
		}
		
		class Edge {
			public Node start;
			public Node end;
			public double weight;
			
			public Edge(Node start, Node end, double weight){
				this.start = start;
				this.end = end;
				this.weight = weight;
			}
		}
		
		public String toString(){
			StringBuilder sb = new StringBuilder();
			for(Node n : nodes){
				sb.append(n.toString() + " ");
			}
			return sb.toString();
		}
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
		final double checkAngle = 0.15;
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
//		
//	}
}
