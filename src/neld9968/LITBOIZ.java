package neld9968;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

//import neld9968.LitGraph.Edge;
//import neld9968.LitGraph.Node;
import spacesettlers.actions.*;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.*;
import spacesettlers.objects.resources.*;
import spacesettlers.objects.weapons.EMP;
import spacesettlers.objects.weapons.Missile;
import spacesettlers.clients.TeamClient;
import spacesettlers.clients.ImmutableTeamInfo;
import spacesettlers.clients.Team;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * This agent navigates faster than others using a Roadmap A* targeting system.
 * It attacks the closest ship.
 * It will go to an energy beacon when energy is low enough.
 * 
 * @author Luis and Brian
 */
public class LITBOIZ extends TeamClient {
	boolean isFirst = true;
	HashMap <UUID, Ship> asteroidToShipMap;
	UUID asteroidCollectorID;
	Ship ourShip;
	Flag currentFlag;
	boolean canBuyBase = false;
	boolean TLSpotAvailable = true;
	boolean TRSpotAvailable = true;
	boolean BLSpotAvailable = true;
	boolean BRSpotAvailable = true;
	
	Beacon currentBeacon;
	Beacon oldBeacon;
	Position targetedPosition;
//	public static ArrayList<Edge> edges = new ArrayList<>();
//	public static ArrayList<Node> nodes = new ArrayList<>();
	Toroidal2DPhysics space;
	Map<UUID, Mastermind> shipToMastermindMap;
	Map<UUID, Ship> shipList = new HashMap<UUID, Ship>();
	Mastermind master;
	ArrayList<Position> basePositions = new ArrayList<>();
	
	FollowPathAction followPathAction;
	HashMap <UUID, Graph> graphByShip;
	int currentSteps;
	int REPLAN_STEPS = 20;

	/**
	 * Assigns ships to be attack or resource ships (currently only 1 attack ship)
	 * @param space
	 * @param actionableObjects
	 * return actions for the ship
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		boolean TLSpotAvailable = true;
		boolean TRSpotAvailable = true;
		boolean BLSpotAvailable = true;
		boolean BRSpotAvailable = true;
		
		for(Base b : space.getBases()) {
			if(space.findShortestDistance(b.getPosition(), basePositions.get(0)) < 3) {
				TLSpotAvailable = false;
			} else if (space.findShortestDistance(b.getPosition(), basePositions.get(1)) < 3){
				TRSpotAvailable = false;
			} else if (space.findShortestDistance(b.getPosition(), basePositions.get(2)) < 3) {
				BLSpotAvailable = false;
			} else if (space.findShortestDistance(b.getPosition(), basePositions.get(3)) < 3) {
				BRSpotAvailable = false;
			}	
		}
		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				
				if(isFirst){
					asteroidCollectorID = ship.getId();
					isFirst = false;
				}
				
				//add to ship set if not already there
				if(!shipList.containsKey(ship.getId())){
					shipList.put(ship.getId(), ship);
				} else {
					shipList.replace(ship.getId(), ship);
				}
				
				
				//add to map if not already there
				if(!shipToMastermindMap.containsKey(ship.getId())){
					shipToMastermindMap.put(ship.getId(), new Mastermind());
				}
				master = shipToMastermindMap.get(ship.getId());
				
				ourShip = ship;
				master.ship = ship;
				
				//add actions
				if(asteroidCollectorID.equals(ship.getId())){
					actions.put(ship.getId(), getAsteroidCollectorAction(space, ship));
				}
				else {
					AbstractAction action = getFlagShipAction(space, ship);
					actions.put(ship.getId(), action);
				}			
				
			} else {
				// it is a base.  Heuristically decide when to use the shield (TODO)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		return actions;
	}
	
	/**
	 * Follow an aStar path to the goal
	 * @param space
	 * @param ship
	 * @param goalPosition
	 * @return
	 */
	private AbstractAction getAStarPathToGoal(Toroidal2DPhysics space, Ship ship, Position goalPosition) {
		AbstractAction newAction;
		
		Graph graph = AStarSearch.createGraphToGoalWithBeacons(space, ship, goalPosition, new Random());
		Vertex[] path = graph.findAStarPath(space);
		followPathAction = new FollowPathAction(path);
		newAction = followPathAction.followPath(space, ship);
		//graphByShip.put(ship.getId(), graph);
		return newAction;
	}

	
	/**
	 * Gets the action for the flag runner
	 * @param space the current space environment
	 * @param ship our spacecraft that the simulator passes in
	 * @return newAction 
	 */
	private AbstractAction getFlagShipAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		master.incFireTimer(); //TODO
		AbstractAction newAction = null;
		
		//determine if ship is carrying flag
		Iterator<Flag> iter = space.getFlags().iterator();
		while(iter.hasNext()){
			Flag flag = iter.next();
			if(ship.isCarryingFlag()) {
//				System.out.println("GOT THE FLAG");
				return returnToBaseAction(space, ship);
			}
		}
		
		//if ship is dead, set a new action
		if(!ourShip.isAlive()){
			
			//clear graph and movement stack
//			edges = null;
//			nodes = null;
//			master.stack.clear();
			newAction = getFlagAction(space, ship);
		}
		// aim for a beacon if there isn't enough energy
		else if (ship.getEnergy() < 300) {
			newAction = getBeaconAction(space, ship);
		}

		// otherwise aim for the nearest flag
		else if (!ship.isCarryingFlag() || current == null || current.isMovementFinished(space) || master.getCurrentAction().equals(master.ACTION_FIND_FLAG)) {
			newAction = getFlagAction(space, ship);
		} 
		else {
			newAction = ship.getCurrentAction();
		}	
		
		master.setOldShipEnergy(ship.getEnergy());
		return newAction; 
	}
	
	public AbstractAction returnToBaseAction(Toroidal2DPhysics space, Ship ship){
		AbstractAction newAction = null;
		Position currentPosition = ship.getPosition();
		
		Base base = Mastermind.findNearestBase(space, ship);

		master.currentTarget = base;
		master.setCurrentAction(master.ACTION_GO_TO_BASE);

		Position basePos = base.getPosition();
        if(basePos.getX() == currentPosition.getX()){ //prevent infinite slope
            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, base);
        } else { 
        	newAction = getAStarPathToGoal(space, ship, basePos);
        }

		return newAction;
	}
	
	/**
	 * Gets the action that will deal with the beacon 
	 * @param space the current space environment
	 * @param ship our spacecraft that the simulator passes in
	 * @return newAction 
	 */
	public AbstractAction getBeaconAction(Toroidal2DPhysics space, Ship ship){
		AbstractAction newAction = null;
		Position currentPosition = ship.getPosition();
		//find nearest beacon
		currentBeacon = Mastermind.pickNearestBeacon(space, ship);
		master.currentTarget = currentBeacon;

		if(currentBeacon == null){ //return to base
			Base base = Mastermind.findNearestBase(space, ship);
			newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, base);
			master.setCurrentAction(master.ACTION_GO_TO_BASE);
		}
		else { 
			// find beacon 
			master.setCurrentAction(master.ACTION_FIND_BEACON);
			Position beaconPos = currentBeacon.getPosition();
	        if(beaconPos.getX() == currentPosition.getX()){ //prevent infinite slope
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentBeacon);
	        } else { //directly target beacon
	        	newAction = getAStarPathToGoal(space, ship, beaconPos);	
	        }
		}	
		
		oldBeacon = currentBeacon;
		return newAction;
	}
	
	/**
	 * Gets the action that will deal with the beacon 
	 * @param space the current space environment
	 * @param ship our spacecraft that the simulator passes in
	 * @return newAction 
	 */
	public AbstractAction getChaseAction(Toroidal2DPhysics space, Ship ship){
		Position currentPosition = ship.getPosition();
		Ship currentEnemy = Mastermind.pickNearestFlagShip(space, ship);
		master.currentTarget = currentEnemy;
		master.setCurrentAction(master.ACTION_CHASE_ENEMY);

		AbstractAction newAction = null;

		if (currentEnemy == null) {
			//if no enemy, go to beacon
			newAction = getBeaconAction(space, ship);	
		} 
		else {
	        Position enemyPos = currentEnemy.getPosition();
	        double distanceToEnemy = space.findShortestDistance(enemyPos, currentPosition);
	        if(Math.abs(enemyPos.getX() - currentPosition.getX()) < 1){ //prevent infinite slope
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentEnemy);
	        }
	        else if(distanceToEnemy < Mastermind.enemyDistanceThresholdMedium){ //slow down and directly target enemy
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentEnemy);
	            targetedPosition = null;
	        }
	        else{   	
				
				//Store enemy position
				master.setOldEnemyPosition(enemyPos);
				
				//set action
//	    		targetedPosition = target;
	        	newAction = getAStarPathToGoal(space, ship, enemyPos);	
	        }
		}
		return newAction;
	}
	
	private AbstractAction getAsteroidCollectorAction(Toroidal2DPhysics space,
			Ship ship) {
		Position currentPosition = ship.getPosition();
		Asteroid currentAsteroid = Mastermind.pickHighestValueFreeAsteroid(space, ship);
//		System.out.println(currentFlag);
		master.currentTarget = currentAsteroid;
		master.setCurrentAction(master.ACTION_FIND_RESOURCE);

		//if can afford base, go to base site
		if(canBuyBase){
			if(TLSpotAvailable) {
				return new LITBOIZMOVEACTION(space, ship.getPosition(), basePositions.get(0));
			} else if(TRSpotAvailable) {
				return new LITBOIZMOVEACTION(space, ship.getPosition(), basePositions.get(1));
			} else if(BLSpotAvailable) {
				return new LITBOIZMOVEACTION(space, ship.getPosition(), basePositions.get(2));
			} else if(BRSpotAvailable) {
				return new LITBOIZMOVEACTION(space, ship.getPosition(), basePositions.get(3));
			} 
		}

		if(ship.getEnergy() < 1000){
			return getBeaconAction(space, ship);
		} 

		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 500) {
			return returnToBaseAction(space, ship);
		}

		// otherwise aim for the asteroid
		
		if (currentAsteroid != null) {
			Position asteroidPos = currentAsteroid.getPosition();
	        double distanceToAsteroid = space.findShortestDistance(asteroidPos, currentPosition);
	        if(Math.abs(asteroidPos.getX() - currentPosition.getX()) < 1){ //prevent infinite slope
	            return new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentAsteroid);
	        }
	        else if(distanceToAsteroid < Mastermind.enemyDistanceThresholdMedium){ //slow down and directly target enemy
	            targetedPosition = null;
	        	return new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentAsteroid);
	        }
	        else{ 
	        	return getAStarPathToGoal(space, ship, asteroidPos);	
	        }
			//newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, asteroid);
		} else {
			return ship.getCurrentAction();
		}

	}
	
	public AbstractAction getFlagAction(Toroidal2DPhysics space, Ship ship){
		Position currentPosition = ship.getPosition();
		currentFlag = Mastermind.pickNearestEnemyFlag(space, ship);
//		System.out.println(currentFlag);
		master.currentTarget = currentFlag;
		master.setCurrentAction(master.ACTION_FIND_FLAG);

		AbstractAction newAction = null;

		if (currentFlag == null) {
			// TODO: if no flag, kill everybody
			//if no flag, go to beacon
			if(ship.getEnergy() < 1000){
				newAction = getBeaconAction(space, ship);
			} else {
				newAction = getChaseAction(space, ship);
			}
				
		} 
		else {
	        Position flagPos = currentFlag.getPosition();
	        double distanceToFlag = space.findShortestDistance(flagPos, currentPosition);
	        if(Math.abs(flagPos.getX() - currentPosition.getX()) < 1){ //prevent infinite slope
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentFlag);
	        }
	        else if(distanceToFlag < Mastermind.enemyDistanceThresholdMedium){ //slow down and directly target enemy
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentFlag);
	            targetedPosition = null;
	        }
	        else{ 
	        	newAction = getAStarPathToGoal(space, ship, flagPos);	
	        }
		}
		return newAction;
	}
	
	/**
	 * Gets the action that will deal with the beacon 
	 * @param space the current space environment
	 * @param ship our spacecraft that the simulator passes in
	 * @param currentPosition of ship
	 * @param initialTarget 
	 * @param counter that triggers A*
	 * @return newAction 
	 */
//	public Position getAStarPosition(Toroidal2DPhysics space, Ship ship,
//			Position currentPosition, Position initialTarget, int counter){
//		Position target;
//		
//		//will calculate A* when counter is 10 or stack is empty
//		if(counter == Mastermind.aStarCounter || master.stack.isEmpty()){
//			ArrayList<Position> testPositions = master.getAlternatePoints(space, ship, currentPosition, initialTarget, 0);
////			master.stack =  master.aStar(currentPosition, initialTarget, testPositions, space);
//			
//			target = (!master.stack.isEmpty()) ? master.stack.pop().position : initialTarget;
//    		master.aStarCurrentPosition = target;
//    		
//    		//reset whichever counter aStar is using
//    		if(master.getCurrentAction().equals(master.ACTION_CHASE_ENEMY)){
//    			master.aStarEnemyCounter = 0;
//    		}
//    		else if (master.getCurrentAction().equals(master.ACTION_FIND_BEACON)) {
//    			master.aStarBeaconCounter = 0;
//    		}
//    		else if (master.getCurrentAction().equals(master.ACTION_FIND_FLAG)) {
//    			master.aStarFlagCounter = 0;
//    		}
//		} 
//		//if a Node is on the stack, go to that Position
//		else {
//			//if ship is approaching current target
//    		if(space.findShortestDistance(currentPosition, master.aStarCurrentPosition) < Mastermind.aStarDistanceThreshold){
//    			target = master.stack.pop().position;
//    			master.aStarCurrentPosition = target;
//    		} else { //resume course to Node on top of A* stack
//    			//TODO
//    			//target node might be removed too early
//    			
//    			target = master.aStarCurrentPosition;
//    		}
//		}
//		targetedPosition = target;
//		return target;
//	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		this.space = space;
		shipToMastermindMap = new HashMap<>();
		asteroidCollectorID = null;
		basePositions.add(new Position(150,250));
		basePositions.add(new Position(1450,250));
		basePositions.add(new Position(150,800));
		basePositions.add(new Position(1450,800));
	}

	//called when simulator shuts down
	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// write score & alleles to file 
		double score = 0;
		for (ImmutableTeamInfo team : space.getTeamInfo()){
			if (team.getTeamName().equals("LITBOIZ")){
				score = team.getScore();
			}
		}

        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter("/Users/Luis/Documents/workspace/LITBOIZ/LITCSV.csv", true);
            fileWriter.append(Double.toString(score) + ",");
            fileWriter.append(Integer.toString(Mastermind.rateOfFireFast) + ",");
            fileWriter.append(Integer.toString(Mastermind.rateOfFireSlow) + ",");
            fileWriter.append(Integer.toString(Mastermind.enemyDistanceThresholdClose) + ",");
            fileWriter.append(Integer.toString(Mastermind.enemyDistanceThresholdMedium) + ",");
            fileWriter.append(Integer.toString(Mastermind.enemyDistanceThresholdFar) + ",");
            fileWriter.append(Integer.toString(Mastermind.aStarDistanceThreshold)+ ",");
            fileWriter.append(Integer.toString(Mastermind.aStarCounter) + "\n");

            //System.out.println("CSV file was updated successfully !!!");

        } catch (Exception e) {
            System.out.println("Error when writing to file");
            e.printStackTrace();
        } finally {

            try {
                fileWriter.flush();
                fileWriter.close();

            } catch (IOException e) {

                System.out.println("Error when closing file writer");
                e.printStackTrace();
            }

        }

	}

	/**
	 * Return any graphics that the team client wants to draw
	 * @return a set of objects that extend the SpacewarGraphics class
	 */
	@Override
	public Set<SpacewarGraphics> getGraphics() {
		//if(targetedPosition == null) return null;
		
		HashSet<SpacewarGraphics> set = new HashSet<>();
		
		if (graphByShip != null) {
			for (Graph graph : graphByShip.values()) {
				// uncomment to see the full graph
				set.addAll(graph.getAllGraphics());
				set.addAll(graph.getSolutionPathGraphics());
			}
		}
		
		Set<UUID> keys = shipList.keySet();
		for(UUID id : keys){
			Ship ship = shipList.get(id);
			String currentAction = shipToMastermindMap.get(ship.getId()).getCurrentAction();
			switch(currentAction){
				case "Do nothing":
					set.add(new CircleGraphics(2, Color.WHITE, ship.getPosition()));
					break;
				case "Chase enemy":
					set.add(new CircleGraphics(2, Color.RED, ship.getPosition()));
					break;
				case "Find resource":
					set.add(new CircleGraphics(2, Color.GREEN, ship.getPosition()));
					break;
				case "Find beacon":
					set.add(new CircleGraphics(2, Color.YELLOW, ship.getPosition()));
					break;
				case "Go to base":
					set.add(new CircleGraphics(2, Color.BLUE, ship.getPosition()));
					break;
				case "Find flag":
					set.add(new CircleGraphics(2, Color.CYAN, ship.getPosition()));
					break;
			}
		}
		//set.add(new CircleGraphics(20, Color.RED, targetedPosition));
//		if(!master.stack.isEmpty()){
//			//make a copy
//			Stack<Node> copyStack = (Stack<Node>) master.stack.clone();
//			//pop all off and display
//			for(Node n : copyStack){
//				set.add(new CircleGraphics(10, Color.GREEN, n.position));
//			}
//		}
//		if(edges != null){
//			for(Edge e : edges){
//				set.add(new LineGraphics(e.start.position, 
//						e.end.position, 
//						space.findShortestDistanceVector(e.start.position, e.end.position)));
//			}
//		}
//		
//		if(nodes != null){
//			for(Node n : nodes){
//				//set.add(new CircleGraphics(10, Color.BLUE, n.position));
//			}
//		}
		HashSet<SpacewarGraphics> newSetClone = (HashSet<SpacewarGraphics>) set.clone();
		set.clear();
		return newSetClone;
	}

	@Override
	/**
	 * If there is enough resourcesAvailable, buy a base.  Place it by finding a ship that is sufficiently
	 * far away from the existing bases
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		boolean bought_base = false;
		canBuyBase = purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable);
		if (canBuyBase) {
			Position asteroidCollectorPos = shipList.get(asteroidCollectorID).getPosition();
			System.out.println("trying to buy base");
			System.out.println(asteroidCollectorPos);
			if(space.findShortestDistance(asteroidCollectorPos, basePositions.get(0)) < 3
				|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(1)) < 3
				|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(2)) < 3
				|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(3)) < 3){
//			if(asteroidCollectorPos.equalsLocationOnly(basePositions.get(0))
//					|| asteroidCollectorPos.equalsLocationOnly(basePositions.get(1))
//					|| asteroidCollectorPos.equalsLocationOnly(basePositions.get(2))
//					|| asteroidCollectorPos.equalsLocationOnly(basePositions.get(3))){
				System.out.println("im here");
				purchases.put(asteroidCollectorID, PurchaseTypes.BASE);
				bought_base = true;
				System.out.println("Buying a base!!");
			}		
		}
		
		// see if you can buy EMPs
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_EMP_LAUNCHER, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					
					if (!ship.getId().equals(asteroidCollectorID) && !ship.isValidPowerup(PurchaseTypes.POWERUP_EMP_LAUNCHER.getPowerupMap())) {
						purchases.put(ship.getId(), PurchaseTypes.POWERUP_EMP_LAUNCHER);
					}
				}
			}		
		} 
		

		// can I buy a ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable) && bought_base == false) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}

			}

		}

		return purchases;
	}

	/**
	 * The asteroid collector doesn't use power ups but the weapons one does (at random)
	 * @param space
	 * @param actionableObjects
	 * @return
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();

		for (AbstractActionableObject actionableObject : actionableObjects){
			Ship enemy = Mastermind.pickNearestEnemyShip(space, ourShip);
			
			if(!enemy.isAlive()) { //enemy is dead so stop shooting
				return powerUps;
			}
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.FIRE_MISSILE;

			double distanceToEnemy = space.findShortestDistance(enemy.getPosition(), ourShip.getPosition());
			if(actionableObject.isValidPowerup(powerup) && master.getCurrentAction().equals(master.ACTION_CHASE_ENEMY)){
			    
				//situation where our ship and enemy are moving at same speed
				double xVelocityDiff = Math.abs(enemy.getPosition().getxVelocity() - ourShip.getPosition().getxVelocity());
				double yVelocityDiff = Math.abs(enemy.getPosition().getyVelocity() - ourShip.getPosition().getyVelocity());
				if ( xVelocityDiff <= 10 && yVelocityDiff <= 10) {
			        if(master.getFireTimer() == Mastermind.rateOfFireFast){
				        powerUps.put(actionableObject.getId(), powerup);
				        master.clearFireTimer(); //reset fire rate counter)	
			        }
			    }
				//fire certain rate based on distance
				if(distanceToEnemy <= Mastermind.enemyDistanceThresholdClose
						|| (distanceToEnemy <= Mastermind.enemyDistanceThresholdMedium 
							&& master.getFireTimer() == Mastermind.rateOfFireFast)
						|| (distanceToEnemy <= Mastermind.enemyDistanceThresholdFar 
							&& master.getFireTimer() == Mastermind.rateOfFireSlow)){
					powerUps.put(actionableObject.getId(), powerup);
					master.clearFireTimer(); //reset fire rate counter
				}
			}
		}
		
		return powerUps;
	}
}
