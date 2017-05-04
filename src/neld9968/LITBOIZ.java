package neld9968;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.*;
import spacesettlers.objects.*;
import spacesettlers.objects.powerups.*;
import spacesettlers.objects.resources.*;
import spacesettlers.objects.weapons.*;
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
		AbstractAction newAction = null;
		
		if(ship.isCarryingFlag()){
			planning.currentState.haveMap.put(ship.getId(), Mastermind.getCarriedFlag(space, ship).getId());
			planning.currentState.chasingMap.put(ship.getId(), null);
			boolean isReturning = planning.returnFlag(ship, Mastermind.getCarriedFlag(space, ship));
			if(isReturning){
				return returnToBaseAction(space, ship);
			}
		} else {
			if(planning.currentState.haveMap.get(ship.getId()) != null){
				//we lost or deposited the flag
				planning.currentState.haveMap.put(ship.getId(), null);
			}
		}
		
		//determine if ship is carrying flag
//		if(ship.isCarryingFlag()) {
////			System.out.println("GOT THE FLAG");
//			return returnToBaseAction(space, ship);
//		}
		
		//if ship is dead, set a new action
		//TODO testing planning
		if(!ourShip.isAlive()
			|| current == null
			|| current.isMovementFinished(space)
			|| master.getCurrentAction().equals(master.ACTION_FIND_FLAG)){
			newAction = getFlagAction(space, ship);	
		} 
		else if(ship.getEnergy() < ENERGY_THRESHOLD){
			newAction = getBeaconAction(space, ship);
		}
		else {
			newAction = ship.getCurrentAction();
		}
		//TODO testing planning
		
		
//		if(!ourShip.isAlive()){		
//			//clear graph and movement stack
//			newAction = getFlagAction(space, ship);
//		}
//		// aim for a beacon if there isn't enough energy
//		else if (ship.getEnergy() < ENERGY_THRESHOLD) {
//			newAction = getBeaconAction(space, ship);
//		}
//
//		// otherwise aim for the nearest flag
//		else if (!ship.isCarryingFlag() || current == null || current.isMovementFinished(space) || master.getCurrentAction().equals(master.ACTION_FIND_FLAG)) {
//			newAction = getFlagAction(space, ship);
//		} 
//		else {
//			newAction = ship.getCurrentAction();
//		}	
		
//		master.setOldShipEnergy(ship.getEnergy());
		return newAction; 
	}
	
	public AbstractAction returnToBaseAction(Toroidal2DPhysics space, Ship ship){		
		Base base = Mastermind.findNearestBase(space, ship);

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
			//if no enemy, go to beacon
			return getBeaconAction(space, ship);	
		} 
		else {
			return getMoveToObjectAction(space, ship, currentEnemy);
		}
	}
	
	private AbstractAction getAsteroidCollectorAction(Toroidal2DPhysics space,
			Ship ship) {
		//set A* counter higher for resource collectors
		master.aStarCounterReplan = 30;
		Position currentPosition = ship.getPosition();
		Asteroid currentAsteroid = Mastermind.pickHighestValueFreeAsteroid(space, ship);
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

			//if(ship.getEnergy() < 1000) {
//				Base closestBase = Mastermind.findNearestBase(space, ship);
//				Beacon closestBeacon = Mastermind.pickNearestBeacon(space, ship);
	
				// find shortest healing object
//				if(space.findShortestDistance(ship.getPosition(), closestBeacon.getPosition()) 
//						< space.findShortestDistance(ship.getPosition(), closestBase.getPosition())
//						|| closestBase.getEnergy() < 50) { 
//					return getBeaconAction(space, ship);
//				} else {
//					return returnToBaseAction(space, ship);
//				}
			//}
		//planning.


		// if the ship has enough resourcesAvailable, take it back to base
//		if (ship.getResources().getTotal() > 500) {
//			return returnToBaseAction(space, ship);
//		}

		// otherwise aim for the asteroid
		
		if (currentAsteroid != null) {
			Position asteroidPos = currentAsteroid.getPosition();
	        double distanceToAsteroid = space.findShortestDistance(asteroidPos, currentPosition);
	        if(Math.abs(asteroidPos.getX() - currentPosition.getX()) < 1){ //prevent infinite slope
	            return new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentAsteroid);
	        }
	        else if(distanceToAsteroid < Mastermind.enemyDistanceThresholdMedium){ //slow down and directly target enemy
	        	return new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentAsteroid);
	        }
	        else{ 
	        	return getMoveToObjectAction(space, ship, currentAsteroid);
	        }
		} else {
			return ship.getCurrentAction();// TODO this is the default 
		}

	}
	
	public AbstractAction getMoveToObjectAction(Toroidal2DPhysics space, Ship ship, AbstractObject goal){
		if(space.isPathClearOfObstructions(ship.getPosition(), goal.getPosition(), 
				master.getAllObstructionsBetweenAbstractObjects(space, goal), ship.getRadius())){
			return new LITBOIZMOVEACTION(space, ship.getPosition(), goal.getPosition());
		}
		else if(master.aStarCounter >= master.aStarCounterReplan){
    		master.aStarCounter = 0;
    		return getAStarPathToGoal(space, ship, goal.getPosition());	
    	} else {
    		return ship.getCurrentAction();
    	}
	}
	
	public AbstractAction getFlagAction(Toroidal2DPhysics space, Ship ship){
		Position currentPosition = ship.getPosition();
		Flag currentFlag = Mastermind.pickNearestEnemyFlag(space, ship);
		master.currentTarget = currentFlag;
		master.setCurrentAction(master.ACTION_FIND_FLAG);
		
		
		//TODO testing planning
		//get all flags
		Set<Flag> flags = space.getFlags();
		//find closest flag
		for(Flag f : flags){
			Flag closestEnemyFlag = Mastermind.findClosestEnemyFlagInSet(space, ship, flags);
			if (closestEnemyFlag == null) break;
			
			boolean canChase = planning.chaseFlag(ship, closestEnemyFlag);
			if(canChase){
				//add to planning state
				planning.currentState.chasingMap.put(ship.getId(), currentFlag.getId());
		        return getMoveToObjectAction(space, ship, currentFlag);
			} else {
				flags.remove(closestEnemyFlag);
			}
		}
		
		
		
		//if flag already being chased find a new one
		//if(flag == null) break;
		//do this 
		
		//TODO testing planning
		boolean canChase = planning.chaseFlag(ship, currentFlag);
		if(canChase){
			//add to planning state
			planning.currentState.chasingMap.put(ship.getId(), currentFlag.getId());

	        return getMoveToObjectAction(space, ship, currentFlag);
		} 
		else {
			//TODO since currentFlag is taken, find a new flag to chase
			if(ship.getEnergy() < 1000) {
				Base closestBase = Mastermind.findNearestBase(space, ship);
				Beacon closestBeacon = Mastermind.pickNearestBeacon(space, ship);

				// find shortest healing object
				if(space.findShortestDistance(ship.getPosition(), closestBeacon.getPosition()) 
						< space.findShortestDistance(ship.getPosition(), closestBase.getPosition())
						|| closestBase.getEnergy() < 50) { 
					return getBeaconAction(space, ship);
				} else {
					return returnToBaseAction(space, ship);
				}
			} else {
				// kill everybody
				return getChaseAction(space, ship);
			}
		}

//		if (currentFlag == null) {
//			//if no flag, go to beacon
//			if(ship.getEnergy() < 1000) {
//				
//				Base closestBase = Mastermind.findNearestBase(space, ship);
//				Beacon closestBeacon = Mastermind.pickNearestBeacon(space, ship);
//
//				// find shortest healing object
//				if(space.findShortestDistance(ship.getPosition(), closestBeacon.getPosition()) 
//						< space.findShortestDistance(ship.getPosition(), closestBase.getPosition())
//						|| closestBase.getEnergy() < 50) { 
//					return getBeaconAction(space, ship);
//				} else {
//					return returnToBaseAction(space, ship);
//				}
//			} else {
//				// kill everybody
//				return getChaseAction(space, ship);
//			}
//		} 
//		else {
//	        Position flagPos = currentFlag.getPosition();
//	        double distanceToFlag = space.findShortestDistance(flagPos, currentPosition);
//	        if(Math.abs(flagPos.getX() - currentPosition.getX()) < 1){ //prevent infinite slope
//	            return new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentFlag);
//	        }
//	        else if(distanceToFlag < Mastermind.enemyDistanceThresholdMedium){ //slow down and directly target enemy
//	            targetedPosition = null;
//	            return new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentFlag);   
//	        }
//	        else{ 
//	        	return getMoveToObjectAction(space, ship, currentFlag);
//	        }
//		}
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
//		for(Position x : basePositions){
//			set.add(new CircleGraphics(4, Color.YELLOW, x));
//			//set.add(new CircleGraphics(2, Color.RED, new Position(x.getX(), x.getY() + 3)));
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

		// can I buy a ship?
		//cost of every item doubles every purchase
		//limit us to 5 ships and then buy bases after that
		if (numShips < 5 && purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					numShips++;
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}
			}

		}
		//after we have 5 ships, focus on buying bases
		if(numShips >= 5){
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
				
	//			System.out.println("trying to buy base");
	//			System.out.println(asteroidCollectorPos);
				if(space.findShortestDistance(asteroidCollectorPos, basePositions.get(0)) < 3
					|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(1)) < 3
					|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(2)) < 3
					|| space.findShortestDistance(asteroidCollectorPos, basePositions.get(3)) < 3){
					purchases.put(asteroidCollectorID, PurchaseTypes.BASE);
					System.out.println("Buying a base!!");
				}
			}
		}
		
		// see if you can buy EMPs
//		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_EMP_LAUNCHER, resourcesAvailable)) {
//			for (AbstractActionableObject actionableObject : actionableObjects) {
//				if (actionableObject instanceof Ship) {
//					Ship ship = (Ship) actionableObject;
//					
//					if (!ship.getId().equals(asteroidCollectorID) && !ship.isValidPowerup(PurchaseTypes.POWERUP_EMP_LAUNCHER.getPowerupMap())) {
//						purchases.put(ship.getId(), PurchaseTypes.POWERUP_EMP_LAUNCHER);
//					}
//				}
//			}		
//		} 
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
