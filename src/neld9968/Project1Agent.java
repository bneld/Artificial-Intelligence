package neld9968;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * Collects nearby asteroids and brings them to the base, picks up beacons as needed for energy.
 * 
 * If there is more than one ship, only one ship is dedicated to picking up asteroids and
 * the other is dedicated to using weapons
 * 
 * @author amy
 */
public class Project1Agent extends TeamClient {
	HashMap <UUID, Ship> asteroidToShipMap;
	HashMap <UUID, Boolean> aimingForBase;
	UUID asteroidCollectorID;
	double weaponsProbability = 1;
	Ship ourShip;
	Position oldEnemyPosition;
	double oldShipEnergy = Double.MIN_VALUE;
	final String ACTION_CHASE_ENEMY = "Chase enemy";
	final String ACTION_FIND_RESOURCE = "Find resource";
	final String ACTION_FIND_BEACON = "Find beacon";
	final String ACTION_EVADE = "Evade";
	final String ACTION_GO_TO_BASE = "Go to base";
	final String ACTION_DO_NOTHING = "Do nothing";
	String currentAction = "";

	/**
	 * Assigns ships to asteroids and beacons, as described above
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();
	

		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				ourShip = ship;

				// the first time we initialize, decide which ship is the asteroid collector
//				if (asteroidCollectorID == null) {
//					asteroidCollectorID = ship.getId();
//				}
				
				AbstractAction action;
				action = getWeaponShipAction(space, ship);
//				if (ship.getId().equals(asteroidCollectorID)) {
//					// get the asteroids
//					action = getAsteroidCollectorAction(space, ship);
//				} else {
//					// this ship will try to shoot other ships so its movements take it towards the nearest other ship not on our team
//					action = getWeaponShipAction(space, ship);
//				}
				
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
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getWeaponShipAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		Position currentPosition = ship.getPosition();

		// aim for a beacon if there isn't enough energy
		if (ship.getEnergy() < 2000) {
			AbstractAction newAction = null;
			Beacon beacon = pickNearestBeacon(space, ship);
			Base base = findNearestBase(space, ship);
			if(beacon == null || space.findShortestDistance(currentPosition, base.getPosition()) 
					< space.findShortestDistance(currentPosition, beacon.getPosition())){
				//base is closer
				newAction = new MoveToObjectAction(space, currentPosition, base);
				currentAction = ACTION_GO_TO_BASE;
				System.out.println("Go To Base");
				aimingForBase.put(ship.getId(), true);
			}
			else {
				//beacon is closer
				newAction = new MoveToObjectAction(space, currentPosition, beacon);
				currentAction = ACTION_FIND_BEACON;
				System.out.println("Move To Beacon");
				aimingForBase.put(ship.getId(), false);
			}
			return newAction;
		}
		
		//if ship is damaged, try evasive action
		if(ship.getEnergy() < oldShipEnergy){
			AbstractAction newAction = null;
			
			//if ship is already evading, stay on evade course
			if(currentAction.equals(ACTION_EVADE)){
				newAction = ship.getCurrentAction();
			}
			else {
				currentAction = ACTION_EVADE;
				Ship enemy = pickNearestEnemyShip(space, ship);
				if(space.findShortestDistance(ship.getPosition(), enemy.getPosition()) < 300){
					Random rand = new Random();
					Position newPos = space.getRandomFreeLocationInRegion(rand, ship.getRadius(), (int)ship.getPosition().getX(), (int)ship.getPosition().getY(), 100);
					newAction = new MoveAction(space, ship.getPosition(), newPos);
					System.out.println("Evade");
				}
			}
			
			oldShipEnergy = ship.getEnergy();
			return newAction;
		}

		// did we bounce off the base?
		if (ship.getResources().getTotal() == 0 && ship.getEnergy() > 2000 && aimingForBase.containsKey(ship.getId()) && aimingForBase.get(ship.getId())) {
			current = null;
			aimingForBase.put(ship.getId(), false);
		}

		// otherwise aim for the nearest enemy ship
		if (current == null || current.isMovementFinished(space) || currentAction.equals(ACTION_CHASE_ENEMY)) {
			return getChaseAction(space, ship);
		} 
		
		else {
			return ship.getCurrentAction();
		}
	}
	
	public AbstractAction getChaseAction(Toroidal2DPhysics space, Ship ship){
		Position currentPosition = ship.getPosition();
		aimingForBase.put(ship.getId(), false);
		Ship enemy = pickNearestEnemyShip(space, ship);
		currentAction = ACTION_CHASE_ENEMY;

		AbstractAction newAction = null;

		if (enemy == null) {
			// there is no enemy available so collect a beacon
			Beacon beacon = pickNearestBeacon(space, ship);
			// if there is no beacon, then just skip a turn
			if (beacon == null) {
				newAction = new DoNothingAction();
				currentAction = ACTION_DO_NOTHING;
				System.out.println("Do Nothing");
			} else {
				newAction = new MoveToObjectAction(space, currentPosition, beacon);
				currentAction = ACTION_FIND_BEACON;
				System.out.println("Move To Beacon");
			}
		} 
		//straight line prediction
//			if(oldEnemyPosition == null){
//				oldEnemyPosition = enemy.getPosition();
//				newAction = new MoveToObjectAction(space, currentPosition, enemy);
//			} else {
//				Position currEnemyPosition = enemy.getPosition();
//				//check if enemy has not moved
//				if(currEnemyPosition.equalsLocationOnly(oldEnemyPosition)){
//					newAction = new MoveToObjectAction(space, currentPosition, enemy);
//				} else {
//					double slope = (oldEnemyPosition.getY() - currEnemyPosition.getY()) / (oldEnemyPosition.getX() - currEnemyPosition.getX());
//					double newX = currEnemyPosition.getX() + (currEnemyPosition.getX() - oldEnemyPosition.getX());
//					double newY = slope*(newX - currEnemyPosition.getX()) + currEnemyPosition.getY();
//					System.out.println("Enemy: " + oldEnemyPosition.toString() + " -> " + currEnemyPosition.toString());
//					System.out.println("Slope: " + slope);
//					Position predictedPosition = new Position(newX, newY);
//					System.out.println("Predicted: " + predictedPosition);
//					newAction = new MoveAction(space, ship.getPosition(), predictedPosition);
//					oldEnemyPosition = enemy.getPosition();
//				}
//			}
		
		//distance inflation
		//TODO inflation doesn't handle screen wrap-around
		Position enemyPos = enemy.getPosition();
		double distanceToEnemy = space.findShortestDistance(enemyPos, currentPosition);
		if(enemyPos.getX() == currentPosition.getX()){ //prevent infinite slope
			newAction = new MoveToObjectAction(space, currentPosition, enemy);
		}
		else if(distanceToEnemy < 200){ //slow down and directly target enemy
			newAction = new MoveToObjectAction(space, currentPosition, enemy);
			System.out.println("Move Directly To Enemy " + distanceToEnemy);
		}
		else{ //target past enemy to increase velocity
			double slope = (enemyPos.getY() - currentPosition.getY()) / (enemyPos.getX() - currentPosition.getX());
			double newX = (enemyPos.getX() - currentPosition.getX() > 0 ) ? enemyPos.getX() + 300 : enemyPos.getX() - 300; // 300 arbitrary
			double newY = slope*(newX - enemyPos.getX()) + enemyPos.getY();
			Position inflatedPosition = new Position(newX, newY);
			newAction = new MoveAction(space, currentPosition, inflatedPosition);
			System.out.println("Move Past Enemy " + distanceToEnemy);
		}
		
		return newAction;
	}


	/**
	 * Find the nearest ship on another team and aim for it
	 * @param space
	 * @param ship
	 * @return
	 */
	private Ship pickNearestEnemyShip(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			
			double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return nearestShip;
	}

	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Base findNearestBase(Toroidal2DPhysics space, Ship ship) {
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
	 * Returns the asteroid of highest value that isn't already being chased by this team
	 * 
	 * @return
	 */
	private Asteroid pickHighestValueFreeAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		int bestMoney = Integer.MIN_VALUE;
		Asteroid bestAsteroid = null;

		for (Asteroid asteroid : asteroids) {
			if (!asteroidToShipMap.containsKey(asteroid)) {
				if (asteroid.isMineable() && asteroid.getResources().getTotal() > bestMoney) {
					bestMoney = asteroid.getResources().getTotal();
					bestAsteroid = asteroid;
				}
			}
		}
		//System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}

	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private Beacon pickNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
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

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		// TODO Auto-generated method stub
		return null;
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

		Random random = new Random();
		for (AbstractActionableObject actionableObject : actionableObjects){
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random.nextInt(SpaceSettlersPowerupEnum.values().length)];
			Ship enemy = pickNearestEnemyShip(space, ourShip);
			if(space.findShortestDistance(enemy.getPosition(), ourShip.getPosition()) < 200 
					&& actionableObject.isValidPowerup(powerup)
					&& currentAction.equals(ACTION_CHASE_ENEMY)){
				powerUps.put(actionableObject.getId(), powerup);
			}
		}
		
		//Set<Ship> allShips = space.getShips();
		
		
		return powerUps;
	}

}
