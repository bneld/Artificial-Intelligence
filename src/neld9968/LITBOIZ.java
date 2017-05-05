package neld9968;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import neld9968.Planning.StateNode;
import spacesettlers.actions.*;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.*;
import spacesettlers.objects.resources.*;
import spacesettlers.clients.*;
import spacesettlers.graphics.*;
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
	public static int ENERGY_THRESHOLD = 600;
	boolean isFirst = true;
	UUID asteroidCollectorID;
	UUID asteroidCollectorID2;
	Ship ourShip;
	boolean canBuyBase = false;
	int numShips = 3;
	int numBases = 1;
	int MAX_NUM_SHIPS = 4;
	int HEALTH_THRESHOLD = 1000;
	boolean TLSpotAvailable = true;
	boolean TRSpotAvailable = true;
	boolean BLSpotAvailable = true;
	boolean BRSpotAvailable = true;
	
	//Planning
	Planning planning;
	PlanningState currentState;
	
	//agent data
	Toroidal2DPhysics space;
	Map<UUID, Mastermind> shipToMastermindMap;
	Map<UUID, Ship> shipList = new HashMap<UUID, Ship>();
	Mastermind master;
	ArrayList<Position> basePositions = new ArrayList<>();
	
	// A* data
	FollowPathAction followPathAction;
	HashMap <UUID, Graph> graphByShip;
	
	/**
	 * Assigns ships to be attack or resource ships
	 * @param space
	 * @param actionableObjects
	 * return actions for the ship
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		
		//Planning
		planning.update(actionableObjects);
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		TLSpotAvailable = TRSpotAvailable = BLSpotAvailable = BRSpotAvailable = true;
		
		// determine which bases are available
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
					
					//make initial state
					planning.init(space, actionableObjects);
					
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
				master.aStarCounter++;
				
			} else {
				// it is a base.  Heuristically decide when to use the shield
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		return actions;
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
		master.incFireTimer();
		
		//search for a goal state
		PredictedState state = new PredictedState();
		Planning.StateNode treeNode = planning.populateSearchTree(planning.new StateNode(state), 0);
		Stack<Planning.StateNode> planStack = planning.searchTree(treeNode, new Stack<Planning.StateNode>());
		while(!planStack.isEmpty()){
			StateNode node = planStack.pop();
			System.out.println(node.actionLeadingToState);
		}
		
		// begin planning
		if(ship.isCarryingFlag()){
			planning.currentState.haveMap.put(ship.getId(), ship.getFlag().getId());
			planning.currentState.chasingMap.put(ship.getId(), null);
			boolean isReturning = planning.returnFlag(ship, ship.getFlag());
			if(isReturning){
				return returnToBaseAction(space, ship);
			}
		} else { // ship not carrying flag
			if(planning.currentState.haveMap.get(ship.getId()) != null){
				//we lost or deposited the flag
				planning.currentState.haveMap.put(ship.getId(), null);
				return getFlagAction(space, ship);
			}
		}
		
		//if ship is dead, set a new action
		if(!ourShip.isAlive()){
			planning.currentState.haveMap.put(ship.getId(), null);
			planning.currentState.chasingMap.put(ship.getId(), null);
			return new DoNothingAction();
		}
		//no previous action
		if(current == null
			|| current.isMovementFinished(space)
			|| master.getCurrentAction().equals(master.ACTION_FIND_FLAG)){
			return getFlagAction(space, ship);	
		} 
		else if(ship.getEnergy() < HEALTH_THRESHOLD) {
			return getHealthAction(space, ship);
		}
		else {
			return getFlagAction(space, ship);	
		}
	}
	
	/**
	 * returns ship to base
	 * @param space
	 * @param ship
	 * return actions for the ship
	 */
	public AbstractAction returnToBaseAction(Toroidal2DPhysics space, Ship ship){		
		Base base = Mastermind.findNearestBase(space, ship);

		// set master values
		master.currentTarget = base;
		master.setCurrentAction(master.ACTION_GO_TO_BASE);

        return getMoveToObjectAction(space, ship, base);
	}
	
	/**
	 * Gets the action that will deal with the beacon 
	 * @param space the current space environment
	 * @param ship our spacecraft that the simulator passes in
	 * @return newAction 
	 */
	public AbstractAction getBeaconAction(Toroidal2DPhysics space, Ship ship){
		//find nearest beacon
		Beacon currentBeacon = Mastermind.pickNearestBeacon(space, ship);
		master.currentTarget = currentBeacon;

		if(currentBeacon == null){ //return to base
			master.setCurrentAction(master.ACTION_GO_TO_BASE);
			Base base = Mastermind.findNearestBase(space, ship);
			return getMoveToObjectAction(space, ship, base);
		}
		else { 
			// find beacon 
			master.setCurrentAction(master.ACTION_FIND_BEACON);
			return getMoveToObjectAction(space, ship, currentBeacon);
		}	
	}
	
	/**
	 * Gets the action that will chase an enemy
	 * @param space the current space environment
	 * @param ship our spacecraft that the simulator passes in
	 * @return newAction 
	 */
	public AbstractAction getChaseAction(Toroidal2DPhysics space, Ship ship){
		Ship currentEnemy = Mastermind.pickNearestFlagShip(space, ship);
		master.currentTarget = currentEnemy;
		master.setCurrentAction(master.ACTION_CHASE_ENEMY);

		if (currentEnemy == null) {
			Ship enemy = master.pickNearestEnemyShip(space, ship);
			return getMoveToObjectAction(space, ship, enemy);
			//if no enemy, go to beacon
//			System.out.println("THERE'S NO ENEMY, GET HELLA HEALTH");
//			return getHealthAction(space, ship);
		} 
		else {
			return getMoveToObjectAction(space, ship, currentEnemy);
		}
	}
	
	/**
	 * Assigns asteroid collectors actions
	 * @param space
	 * @param ship
	 * return actions for the ship
	 */
	private AbstractAction getAsteroidCollectorAction(Toroidal2DPhysics space,
			Ship ship) {
		//set A* counter higher for resource collectors
		Asteroid currentAsteroid = Mastermind.pickHighestValueFreeAsteroid(space, ship);
		master.currentTarget = currentAsteroid;
		master.setCurrentAction(master.ACTION_FIND_RESOURCE);
		
		if(!ship.isAlive()){
			//ship is dead so reset everything
			planning.currentState.collectingMap.put(ship.getId(), null);
			return new DoNothingAction();
		}

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
		
		//begin planning
		boolean canReturnToBase = planning.returnResource(ship);
		if(canReturnToBase){
			return returnToBaseAction(space, ship);
		}
		
		//find mineable asteroids
		Set<Asteroid> allAsteroids = space.getAsteroids();
		Set<Asteroid> asteroids = new HashSet<>();
		for(Asteroid asteroid: allAsteroids) {
			if(asteroid.isMineable()) {
				asteroids.add(asteroid);
			}
		}
		//find closest asteroid
		for(int i = 0; i < asteroids.size(); i++){
			Asteroid closestAsteroid = Mastermind.findClosestAsteroidInSet(space, ship, asteroids);
			if (closestAsteroid == null) break;
			
			boolean canChase = planning.getResource(ship, closestAsteroid);
			if(canChase){
				//add to planning state
				planning.currentState.collectingMap.put(ship.getId(), closestAsteroid.getId());
				master.currentTarget = closestAsteroid;
		        return getMoveToObjectAction(space, ship, closestAsteroid);
			} else {
				asteroids.remove(closestAsteroid);
			}
		}
		
		planning.currentState.collectingMap.put(ship.getId(), null);
		
		// health check
		if(ship.getEnergy() < 2000) {
			return getHealthAction(space, ship);
		} else {
	        return getMoveToObjectAction(space, ship, Mastermind.pickHighestValueFreeAsteroid(space, ship));
		}
	}
	
	/**
	 * Moves ship
	 * @param space
	 * @param ship
	 * @param goal
	 * return move actions for the ship
	 */
	public AbstractAction getMoveToObjectAction(Toroidal2DPhysics space, Ship ship, AbstractObject goal){
		
		//if path is clear, go crazy fast to object
		if(space.isPathClearOfObstructions(ship.getPosition(), goal.getPosition(), 
				master.getAllObstructionsBetweenAbstractObjects(space, goal), ship.getRadius())){
			return new LITBOIZMOVEACTION(space, ship.getPosition(), goal.getPosition());
		}
		else if(master.aStarCounter >= master.aStarCounterReplan){ // use astar
    		master.aStarCounter = 0;
    		return getAStarPathToGoal(space, ship, goal.getPosition());	
    	} else { // continue moving
    		return ship.getCurrentAction();
    	}
	}
	
	/**
	 * Assigns flag ships actions
	 * @param space
	 * @param ship
	 * return actions for the ship
	 */

	public AbstractAction getFlagAction(Toroidal2DPhysics space, Ship ship){
		
		//set mastermind action
		master.setCurrentAction(master.ACTION_FIND_FLAG);
		
		//get all flags
		Set<Flag> flags = space.getFlags();
		//find closest free flag
		for(int i = 0; i < flags.size(); i++){
			Flag closestEnemyFlag = Mastermind.findClosestEnemyFlagInSet(space, ship, flags);
			if (closestEnemyFlag == null) break;
			
			boolean canChase = planning.chaseFlag(ship, closestEnemyFlag);
			if(canChase){
				//add to planning state
				planning.currentState.chasingMap.put(ship.getId(), closestEnemyFlag.getId());
				master.currentTarget = closestEnemyFlag;
		        return getMoveToObjectAction(space, ship, closestEnemyFlag);
			} else {
				flags.remove(closestEnemyFlag);
			}
		}
		
		//if no free enemy flag is found
		if(ship.getEnergy() < HEALTH_THRESHOLD) {
			return getHealthAction(space, ship);
		} else {
			// kill everybody
			return getChaseAction(space, ship);
		}
	}
	
	/**
	 * Gets health
	 * @param space
	 * @param ship
	 * @return
	 */
	public AbstractAction getHealthAction(Toroidal2DPhysics space, Ship ship){

		return getBeaconAction(space, ship);
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
		graphByShip.put(ship.getId(), graph);
		return newAction;
	}
	

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	//initialize varables 
	@Override
	public void initialize(Toroidal2DPhysics space) {
		this.space = space;
		shipToMastermindMap = new HashMap<>();
		graphByShip = new HashMap<>();
		asteroidCollectorID = null;
		basePositions.add(new Position(150,250));
		basePositions.add(new Position(1450,250));
		basePositions.add(new Position(150,800));
		basePositions.add(new Position(1450,800));
		
		planning = new Planning(space);
	}

	//called when simulator shuts down
	@Override
	public void shutDown(Toroidal2DPhysics space) {
	}

	/**
	 * Return any graphics that the team client wants to draw
	 * @return a set of objects that extend the SpacewarGraphics class
	 */
	@Override
	public Set<SpacewarGraphics> getGraphics() {
		
		HashSet<SpacewarGraphics> set = new HashSet<>();
		
		if (graphByShip != null) {
			for (Graph graph : graphByShip.values()) {
				// uncomment to see the full graph
				//set.addAll(graph.getAllGraphics());
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

		// can I buy a ship?
		//cost of every item doubles every purchase
		//limit us to 5 ships and then buy bases after that
		if ( numBases > 3
				|| (numShips < MAX_NUM_SHIPS && purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable))) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					numShips++;
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}
			}

		}
		//after we have MAX_NUM_SHIPS ships, focus on buying bases
		if(numShips >= MAX_NUM_SHIPS){
			canBuyBase = purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable);
			if (canBuyBase) {
				
				//find asteroid collector ship
				Position asteroidCollectorPos = null;
				for(AbstractActionableObject obj : actionableObjects){
					if(obj.getId().equals(asteroidCollectorID)){
						asteroidCollectorPos = obj.getPosition();
					}
				}
				if(asteroidCollectorPos == null){
					//gets the position of the outdated Ship object
					asteroidCollectorPos = shipList.get(asteroidCollectorID).getPosition();
				}
				
				if(space.findShortestDistance(asteroidCollectorPos, basePositions.get(0)) < 3
					|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(1)) < 3
					|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(2)) < 3
					|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(3)) < 3){
					purchases.put(asteroidCollectorID, PurchaseTypes.BASE);
					numBases++;
					System.out.println("Buying a base!!");
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
