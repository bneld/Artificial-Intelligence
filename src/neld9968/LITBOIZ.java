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
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.CircleGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * This agent navigates faster than others using an advanced targeting system.
 * It attacks the closest ship.
 * It will go to an energy beacon when energy is low enough.
 * 
 * @author Luis and Brian
 */
public class LITBOIZ extends TeamClient {
	HashMap <UUID, Ship> asteroidToShipMap;
	HashMap <UUID, Boolean> aimingForBase;
	UUID asteroidCollectorID;
	Ship ourShip;
	Position targetedPosition;

	/**
	 * Assigns ships to be attack or resource ships (currently only 1 attack ship)
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				ourShip = ship;
				
				AbstractAction action = getWeaponShipAction(space, ship);
				
				actions.put(ship.getId(), action);
				
			} else {
				// it is a base.  Heuristically decide when to use the shield (TODO)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		return actions;
	}

	/**
	 * Gets the action for the weapons based ship
	 * @param space the current space environment
	 * @param ship our spacecraft that the simulator passes in
	 * @return
	 */
	private AbstractAction getWeaponShipAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		Mastermind.incFireTimer();
		AbstractAction newAction = null;

		// aim for a beacon if there isn't enough energy
		if (ship.getEnergy() < 600) {
			newAction = getBeaconAction(space, ship);
		}
		
		//if ship is damaged, try evasive action
//		else if(ship.getEnergy() < Mastermind.getOldShipEnergy()){
//			System.out.println("Evade");
//			
//			//if ship is already evading, stay on evade course
//			if(Mastermind.getCurrentAction().equals(Mastermind.ACTION_EVADE)){
//				if(ship.getCurrentAction().isMovementFinished(space)){
//					//stop evading
//					newAction = getChaseAction(space, ship);
//				}
//				else {
//					//continue evasion path
//					newAction = ship.getCurrentAction();
//				}
//			}
//			else {
//				Mastermind.setCurrentAction(Mastermind.ACTION_EVADE);
//				Ship enemy = Mastermind.pickNearestEnemyShip(space, ship);
//				if(space.findShortestDistance(ship.getPosition(), enemy.getPosition()) < 300){
//					Random rand = new Random();
//					Position newPos = space.getRandomFreeLocationInRegion(rand, ship.getRadius(), (int)ship.getPosition().getX(), (int)ship.getPosition().getY(), 100);
//					newAction = new MoveAction(space, ship.getPosition(), newPos);
////					System.out.println("Evade");
//				}
//				else {
//					newAction = getChaseAction(space, ship);
//				}
//			}
//		}
		
		// did we bounce off the base?
		else if (ship.getResources().getTotal() == 0 && ship.getEnergy() > 2000 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
			current = null;
			aimingForBase.put(ship.getId(), false);
			newAction = getChaseAction(space, ship);
		}

		// otherwise aim for the nearest enemy ship
		else if (current == null || current.isMovementFinished(space) || Mastermind.getCurrentAction().equals(Mastermind.ACTION_CHASE_ENEMY)) {
//		if (current == null || current.isMovementFinished(space)) {
			newAction =  getChaseAction(space, ship);
		} 
		
		else {
//			System.out.println("Repeating current action");
			newAction = ship.getCurrentAction();
		}	
		
		Mastermind.setOldShipEnergy(ship.getEnergy());
		return newAction;
	}
	
	public AbstractAction getBeaconAction(Toroidal2DPhysics space, Ship ship){
		AbstractAction newAction = null;
		Position currentPosition = ship.getPosition();
		Beacon beacon = Mastermind.pickNearestBeacon(space, ship);
		if(beacon == null){ //return to base
			Base base = Mastermind.findNearestBase(space, ship);
			newAction = new MoveToObjectAction(space, currentPosition, base);
			Mastermind.setCurrentAction(Mastermind.ACTION_GO_TO_BASE);
		}
		else {
			Mastermind.setCurrentAction(Mastermind.ACTION_FIND_BEACON);
			Position beaconPos = beacon.getPosition();
			double distanceToBeacon = space.findShortestDistance(beaconPos, currentPosition);
	        if(beaconPos.getX() == currentPosition.getX()){ //prevent infinite slope
	            newAction = new MoveToObjectAction(space, currentPosition, beacon);
//				System.out.println("Move Directly To Beacon");
	        }
	        else if(distanceToBeacon < 50){ //slow down and directly target enemy
	            newAction = new MoveToObjectAction(space, currentPosition, beacon);
	            targetedPosition = null;
//				System.out.println("Move Directly To Beacon");
	        }
	        else { //this inflates the objective position
	        	Position inflatedBeaconPosition = Mastermind.inflatePosition(space, currentPosition, beaconPos, 200);
				
				//check for obstacles
				if(!space.isPathClearOfObstructions(currentPosition, inflatedBeaconPosition, space.getAllObjects(), 30)){
					System.out.println("AWWWWW POOP ICEBERG AHEAD");
					Position midpoint = Mastermind.findMidpoint(currentPosition, inflatedBeaconPosition);
					inflatedBeaconPosition = Mastermind.alterPath(currentPosition, midpoint, 0.349066);
					System.out.println("Currently at (" + currentPosition.getX() + ", " + currentPosition.getY() + ")");
					System.out.println("Altering to (" + inflatedBeaconPosition.getX() + ", " + inflatedBeaconPosition.getY() + ")");
				}
				
				newAction = new MoveAction(space, currentPosition, inflatedBeaconPosition);
//				System.out.println("Move To Inflated Beacon Position");
//				targetedPosition = inflatedBeaconPosition;
	        }
			
			aimingForBase.put(ship.getId(), false);
		}			
		return newAction;
	}
	
	public AbstractAction getChaseAction(Toroidal2DPhysics space, Ship ship){
		Position currentPosition = ship.getPosition();
		aimingForBase.put(ship.getId(), false);
		Ship enemy = Mastermind.pickNearestEnemyShip(space, ship);
		Mastermind.setCurrentAction(Mastermind.ACTION_CHASE_ENEMY);

		AbstractAction newAction = null;

		if (enemy == null) {
			// there is no enemy available so collect a beacon
			Beacon beacon = Mastermind.pickNearestBeacon(space, ship);
			// if there is no beacon, then just skip a turn
			if (beacon == null) {
				Base base = Mastermind.findNearestBase(space, ship);
				newAction = new MoveToObjectAction(space, currentPosition, base);
				Mastermind.setCurrentAction(Mastermind.ACTION_GO_TO_BASE);
			} else {
				newAction = new MoveToObjectAction(space, currentPosition, beacon);
				Mastermind.setCurrentAction(Mastermind.ACTION_FIND_BEACON);
//				System.out.println("Move To Beacon");
			}
		} 
		else {
	        Position enemyPos = enemy.getPosition();
	        double distanceToEnemy = space.findShortestDistance(enemyPos, currentPosition);
	        if(Math.abs(enemyPos.getX() - currentPosition.getX()) < 1){ //prevent infinite slope
	            newAction = new MoveToObjectAction(space, currentPosition, enemy);
	//            System.out.println("Move Directly To Enemy");
	        }
	        else if(distanceToEnemy < 100){ //slow down and directly target enemy
	            newAction = new MoveToObjectAction(space, currentPosition, enemy);
	            System.out.println("Move Directly To Enemy " + distanceToEnemy);
	            targetedPosition = null;
	        }
	        else{ //target past enemy to increase velocity
	        	//TODO inflation doesn't handle screen wrap-around
	        	Position target;
	        	if(Mastermind.getOldEnemyPosition() == null){
	        		target = Mastermind.inflatePosition(space, currentPosition, enemyPos, 200);
	        	} else {
	        		Position oldEnemyPos = Mastermind.getOldEnemyPosition();
	        		double distanceEnemyMoved = space.findShortestDistance(oldEnemyPos, enemyPos);
	        		target = Mastermind.predictPath(space, oldEnemyPos, enemyPos, distanceEnemyMoved);
	        		targetedPosition = target;
	        	}
	        	
	        	//check for obstacles
				if(!space.isPathClearOfObstructions(currentPosition, target, Mastermind.getAllObstructions(space), 30)){
					Position midpoint = Mastermind.findMidpoint(currentPosition, enemyPos);
					target = Mastermind.alterPath(currentPosition, midpoint, 0.349066);
	        	}
				Mastermind.setOldEnemyPosition(enemyPos);
	        	newAction = new MoveAction(space, currentPosition, target);	            
	        }
		}
		return newAction;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		ArrayList<Asteroid> finishedAsteroids = new ArrayList<Asteroid>();

		for (UUID asteroidId : asteroidToShipMap.keySet()) {
			Asteroid asteroid = (Asteroid) space.getObjectById(asteroidId);
			if (asteroid != null && !asteroid.isAlive()) {
				finishedAsteroids.add(asteroid);
				//System.out.println("Removing asteroid from map");
			}
		}

		for (Asteroid asteroid : finishedAsteroids) {
			asteroidToShipMap.remove(asteroid);
		}
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		asteroidToShipMap = new HashMap<UUID, Ship>();
		asteroidCollectorID = null;
		aimingForBase = new HashMap<UUID, Boolean>();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	/**
	 * Return any graphics that the team client wants to draw
	 * @return a set of objects that extend the SpacewarGraphics class
	 */
	@Override
	public Set<SpacewarGraphics> getGraphics() {
		if(targetedPosition == null) return null;
		Set<SpacewarGraphics> set = new HashSet<>();
		set.add(new CircleGraphics(20, Color.RED, targetedPosition));
		return set;
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
		double BASE_BUYING_DISTANCE = 200;
		boolean bought_base = false;

		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					Set<Base> bases = space.getBases();

					// how far away is this ship to a base of my team?
					double maxDistance = Double.MIN_VALUE;
					for (Base base : bases) {
						if (base.getTeamName().equalsIgnoreCase(getTeamName())) {
							double distance = space.findShortestDistance(ship.getPosition(), base.getPosition());
							if (distance > maxDistance) {
								maxDistance = distance;
							}
						}
					}

					if (maxDistance > BASE_BUYING_DISTANCE) {
						purchases.put(ship.getId(), PurchaseTypes.BASE);
						bought_base = true;
						//System.out.println("Buying a base!!");
						break;
					}
				}
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
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.FIRE_MISSILE;

			double distanceToEnemy = space.findShortestDistance(enemy.getPosition(), ourShip.getPosition());
			if(actionableObject.isValidPowerup(powerup) && Mastermind.getCurrentAction().equals(Mastermind.ACTION_CHASE_ENEMY)){
				//fire every frame
				if(distanceToEnemy <= 40
						|| (distanceToEnemy <= 100 && Mastermind.getFireTimer() == 8)
						|| (distanceToEnemy <= 200 && Mastermind.getFireTimer() == 15)){
					powerUps.put(actionableObject.getId(), powerup);
					Mastermind.clearFireTimer(); //reset fire rate counter
				}
			}
		}
		
		return powerUps;
	}

}
