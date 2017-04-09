package neld9968;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import neld9968.LitGraph.Edge;
import neld9968.LitGraph.Node;
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
 * This agent navigates faster than others using an Roadmap A* targeting system.
 * It attacks the closest ship.
 * It will go to an energy beacon when energy is low enough.
 * 
 * @author Luis and Brian
 */
public class LITBOIZ extends TeamClient {
	HashMap <UUID, Ship> asteroidToShipMap;
	UUID asteroidCollectorID;
	Ship ourShip;
	Ship currentEnemy;
	Ship oldEnemy;
	Beacon currentBeacon;
	Beacon oldBeacon;
	Position targetedPosition;
	ArrayList<Position> testPositions = new ArrayList<>();
	public static ArrayList<Edge> edges = new ArrayList<>();
	public static ArrayList<Node> nodes = new ArrayList<>();
	Toroidal2DPhysics space;
//	public static Set<AbstractObject> testSet;
//	public static ArrayList<Position> testPositions = new ArrayList<>();
	

	/**
	 * Assigns ships to be attack or resource ships (currently only 1 attack ship)
	 * @param space
	 * @param actionableObjects
	 * return actions for the ship
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				ourShip = ship;
				if(Mastermind.ship == null) Mastermind.ship = ship;
				
				//add actions
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
	 * @return newAction 
	 */
	private AbstractAction getWeaponShipAction(Toroidal2DPhysics space,
			Ship ship) {
		AbstractAction current = ship.getCurrentAction();
		Mastermind.incFireTimer();
		AbstractAction newAction = null;
		
		//if ship is dead or idle, set a new action
		if(!ourShip.isAlive() || (ship.getEnergy() > 1000 && Mastermind.getCurrentAction().equals(Mastermind.ACTION_FIND_BEACON))){
			
			//clear graph and movement stack
			edges = null;
			nodes = null;
			Mastermind.stack.clear();
			newAction = getChaseAction(space, ship);
		}
		// aim for a beacon if there isn't enough energy
		else if (ship.getEnergy() < 1000) {
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
//					newAction = new LITBOIZMOVEACTION(space, ship.getPosition(), newPos);
////					System.out.println("Evade");
//				}
//				else { 
//					newAction = getChaseAction(space, ship);
//				}
//			}
//		}
		
		// did we bounce off the base?
//		else if (ship.getResources().getTotal() == 0 && ship.getEnergy() > 2000 ) {
//			current = null;
//			newAction = getChaseAction(space, ship);
//		}

		// otherwise aim for the nearest enemy ship
		else if (current == null || current.isMovementFinished(space) || Mastermind.getCurrentAction().equals(Mastermind.ACTION_CHASE_ENEMY)) {
			newAction = getChaseAction(space, ship);
		} 
		else {
			newAction = ship.getCurrentAction();
		}	
		
		Mastermind.setOldShipEnergy(ship.getEnergy());
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
		Mastermind.currentTarget = currentBeacon;

		if(currentBeacon == null){ //return to base
			Base base = Mastermind.findNearestBase(space, ship);
			newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, base);
			Mastermind.setCurrentAction(Mastermind.ACTION_GO_TO_BASE);
		}
		else { 
			// find beacon 
			Mastermind.setCurrentAction(Mastermind.ACTION_FIND_BEACON);
			Position beaconPos = currentBeacon.getPosition();
	        if(beaconPos.getX() == currentPosition.getX()){ //prevent infinite slope
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentBeacon);
	        } else { //directly target beacon
	        	Position target = getAStarPosition(space, ship, currentPosition, beaconPos, ++Mastermind.aStarBeaconCounter);	        	
	        	newAction = new LITBOIZMOVEACTION(space, currentPosition, target);	
//	            targetedPosition = null;
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
		currentEnemy = Mastermind.pickNearestEnemyShip(space, ship);
		Mastermind.currentTarget = currentEnemy;
		Mastermind.setCurrentAction(Mastermind.ACTION_CHASE_ENEMY);

		AbstractAction newAction = null;

		if (currentEnemy == null) {
			//if no enemy, go to beacon
			newAction = getBeaconAction(space, ship);	
		} 
		else {
	        Position enemyPos = currentEnemy.getPosition();
	        double distanceToEnemy = space.findShortestDistance(enemyPos, currentPosition);
//	        System.out.println(distanceToEnemy);
	        if(Math.abs(enemyPos.getX() - currentPosition.getX()) < 1){ //prevent infinite slope
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentEnemy);
	        }
	        else if(distanceToEnemy < Mastermind.enemyDistanceThresholdMedium){ //slow down and directly target enemy
	            newAction = new LITBOIZMOVETOOBJECTACTION(space, currentPosition, currentEnemy);
	            targetedPosition = null;
	        }
	        else{ 
//	        	Position initialTarget;
	        	
	        	//find first target point
//	        	if(Mastermind.getOldEnemyPosition() == null){
//	        		//no previous enemy position to use
//	        		initialTarget = enemyPos; //? 
//	        	} else {
//	        		Position oldEnemyPos = Mastermind.getOldEnemyPosition();
//	        		double distanceEnemyMoved = space.findShortestDistance(oldEnemyPos, enemyPos);
//	        		//TODO change prediction method
//	        		initialTarget = Mastermind.predictPath(space, oldEnemyPos, enemyPos, distanceEnemyMoved);
//	        		targetedPosition = initialTarget;
//	        	}
	        	
	        	//will recalculate A* if switching enemy ships
//	        	boolean shouldForceRecalc = (oldEnemy == null) ? false : !currentEnemy.getId().equals(oldEnemy.getId());
	        	
	        	Position target = getAStarPosition(space, ship, currentPosition, enemyPos, ++Mastermind.aStarEnemyCounter);
				
				//Store enemy position
				Mastermind.setOldEnemyPosition(enemyPos);
				
//				if(Mastermind.stack.peek().position.getX() == currentPosition.getX() && Mastermind.stack.peek().position.getY() == currentPosition.getY()){
//					Mastermind.stack.pop();
//					target = Mastermind.stack.peek().position;
//				}
				
				//set action
//	    		targetedPosition = target;
	        	newAction = new LITBOIZMOVEACTION(space, currentPosition, target);	
	        }
		}
		oldEnemy = currentEnemy; //record the enemy ship
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
	public Position getAStarPosition(Toroidal2DPhysics space, Ship ship, Position currentPosition, Position initialTarget, int counter){
		Position target;
		
		//will calculate A* when counter is 10 or stack is empty
		if(counter == Mastermind.aStarCounter || Mastermind.stack.isEmpty()){
			testPositions = Mastermind.getAlternatePoints(space, ship, currentPosition, initialTarget, 0);
			Mastermind.stack =  Mastermind.aStar(currentPosition, initialTarget, testPositions, space);
			
			target = (!Mastermind.stack.isEmpty()) ? Mastermind.stack.pop().position : initialTarget;
    		Mastermind.aStarCurrentPosition = target;
    		
    		//reset whichever counter aStar is using
    		if(Mastermind.getCurrentAction().equals(Mastermind.ACTION_CHASE_ENEMY)){
    			Mastermind.aStarEnemyCounter = 0;
    		}
    		else if (Mastermind.getCurrentAction().equals(Mastermind.ACTION_FIND_BEACON)) {
    			Mastermind.aStarBeaconCounter = 0;
    		}
		} 
		//if a Node is on the stack, go to that Position
		else {
			//if ship is approaching current target
    		if(space.findShortestDistance(currentPosition, Mastermind.aStarCurrentPosition) < Mastermind.aStarDistanceThreshold){
    			target = Mastermind.stack.pop().position;
    			Mastermind.aStarCurrentPosition = target;
    		} else { //resume course to Node on top of A* stack
    			//TODO
    			//target node might be removed too early
    			
    			//target = Mastermind.stack.peek().position;
    			target = Mastermind.aStarCurrentPosition;
    		}
		}
		targetedPosition = target;
		return target;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		this.space = space;
		Mastermind.stack = new Stack<>();
		asteroidCollectorID = null;
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// write score & alleles to file 
		double score = 0;
		System.out.println("yo its lit i think we won");
		System.out.println(Mastermind.rateOfFireFast);
		for (ImmutableTeamInfo team : space.getTeamInfo()){
			if (team.getTeamName().equals("LITBOIZ")){
				score = team.getScore();
			}
		}
		System.out.println(score);

        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter("LITCSV.csv", true);
            fileWriter.append("\n" + Double.toString(score) + ",");
            fileWriter.append(Integer.toString(Mastermind.rateOfFireFast) + ",");
            fileWriter.append(Integer.toString(Mastermind.rateOfFireSlow) + ",");
            fileWriter.append(Integer.toString(Mastermind.enemyDistanceThresholdClose) + ",");
            fileWriter.append(Integer.toString(Mastermind.enemyDistanceThresholdMedium) + ",");
            fileWriter.append(Integer.toString(Mastermind.enemyDistanceThresholdFar) + ",");
            fileWriter.append(Integer.toString(Mastermind.aStarDistanceThreshold)+ ",");
            fileWriter.append(Integer.toString(Mastermind.aStarCounter));

            System.out.println("CSV file was updated successfully !!!");

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
		if(targetedPosition == null) return null;
		Set<SpacewarGraphics> set = new HashSet<>();
		//set.add(new CircleGraphics(20, Color.RED, targetedPosition));
		if(!Mastermind.stack.isEmpty()){
			//make a copy
			Stack<Node> copyStack = (Stack<Node>) Mastermind.stack.clone();
			//pop all off and display
			for(Node n : copyStack){
				set.add(new CircleGraphics(10, Color.GREEN, n.position));
			}
		}
		if(edges != null){
			for(Edge e : edges){
				set.add(new LineGraphics(e.start.position, 
						e.end.position, 
						space.findShortestDistanceVector(e.start.position, e.end.position)));
			}
		}
		
		if(nodes != null){
			for(Node n : nodes){
				//set.add(new CircleGraphics(10, Color.BLUE, n.position));
			}
		}
		
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
			
			if(!enemy.isAlive()) { //enemy is dead so stop shooting
				return powerUps;
			}
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.FIRE_MISSILE;

			double distanceToEnemy = space.findShortestDistance(enemy.getPosition(), ourShip.getPosition());
			if(actionableObject.isValidPowerup(powerup) && Mastermind.getCurrentAction().equals(Mastermind.ACTION_CHASE_ENEMY)){
			    
				//situation where our ship and enemy are moving at same speed
				double xVelocityDiff = Math.abs(enemy.getPosition().getxVelocity() - ourShip.getPosition().getxVelocity());
				double yVelocityDiff = Math.abs(enemy.getPosition().getyVelocity() - ourShip.getPosition().getyVelocity());
				if ( xVelocityDiff <= 10 && yVelocityDiff <= 10) {
			        if(Mastermind.getFireTimer() == Mastermind.rateOfFireFast){
				        powerUps.put(actionableObject.getId(), powerup);
				        Mastermind.clearFireTimer(); //reset fire rate counter)	
			        }
			    }
				//fire certain rate based on distance
				if(distanceToEnemy <= Mastermind.enemyDistanceThresholdClose
						|| (distanceToEnemy <= Mastermind.enemyDistanceThresholdMedium 
							&& Mastermind.getFireTimer() == Mastermind.rateOfFireFast)
						|| (distanceToEnemy <= Mastermind.enemyDistanceThresholdFar 
							&& Mastermind.getFireTimer() == Mastermind.rateOfFireSlow)){
					powerUps.put(actionableObject.getId(), powerup);
					Mastermind.clearFireTimer(); //reset fire rate counter
				}
			}
		}
		
		return powerUps;
	}

}
