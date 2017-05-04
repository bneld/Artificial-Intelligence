package neld9968;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.objects.*;
import spacesettlers.simulator.Toroidal2DPhysics;

public class Planning {
	Toroidal2DPhysics space;
	Set<AbstractActionableObject> actionableObjects;
	PlanningState initialState;
	PlanningState currentState;
	
	public Planning(Toroidal2DPhysics space){
		this.space = space;
	}
	
	public void init(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects){
		this.space = space;
		this.actionableObjects = actionableObjects;
		initialState = PlanningState.getInitialState(space, actionableObjects);
		currentState = new PlanningState(initialState);
	}
	public void update(Set<AbstractActionableObject> actionableObjects){
		this.actionableObjects = actionableObjects;
	}
	
	public void getPossibleActions(PlanningState state){
		//actions
		//chaseFlag
		
		//obtainFlag
		//returnFlag
		//getResource
		//returnToBase
	}
	//TODO returns true if ship should chase flag, else try another ship
	public boolean chaseFlag(Ship s, Flag f){
		if (f == null) return false;
		if(!someoneElseInteractingWithObject(s.getId(), f.getId(), currentState.chasingMap) 
				&& s.getEnergy() > 1000 
				&& !someoneElseInteractingWithObject(s.getId(), f.getId(), currentState.haveMap)) {
			return true;
		} else {
			return false;	
		}
	}
	
	public boolean obtainFlag(Ship s, Flag f){
		//currentState
		//check if s is chasing f
		if(interactingWithObject(s.getId(), f.getId(), currentState.chasingMap) 
				&& !someoneElseInteractingWithObject(s.getId(), f.getId(), currentState.haveMap)
				&& !interactingWithObject(s.getId(), f.getId(), currentState.haveMap) ) {
			// preconditions met
			System.out.println("Obtaining flag");
			return true;
		} else {
			System.out.println("Not obtaining flag wut wut");
			return false;
		}
	}
	public boolean returnFlag(Ship s, Flag f){
		if(interactingWithObject(s.getId(), f.getId(), currentState.haveMap) ) {
			System.out.println("Returning flag!");
			return true;
		} else {
			System.out.println("Not return flag");
			return false;
		}
	}
	public boolean getResource(Ship s, Asteroid a){
		if(s.getEnergy() > 1000 
				&& !someoneElseInteractingWithObject(s.getId(), a.getId(), currentState.collectingMap)) {
			return true;
		} else {
			return false;	
		}
	}
	public boolean returnResource(Ship s, Asteroid a){
		if(s.getEnergy() > 1000 
				&& s.getResources().getTotal() > 500 
				&& !interactingWithObject(s.getId(), a.getId(), currentState.collectingMap)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean buildBase(boolean checkBase) {
		return canBuyBase(checkBase);
	}

//	public boolean someoneElseInteractingWithFlag(Ship s, Flag f, HashMap<Ship,Flag> map) {
//		//figure out if Flag f is already being chased
//		if(map.containsValue(f)){ 
//			Flag shipsFlag = map.get(s);
//			if( shipsFlag != null){ // if ship is in map
//				// if not flag passing in
//				if(!shipsFlag.getId().equals(f.getId())){
//					//Flag is already being chased
//					return true;
//				} else {
//					return false;
//				}
//			} else {
//				//Ship is not in Map but Flag is
//				return true;
//			}
//		} else {
//			return false;
//		}
//	}
	
//	public boolean interactingWithFlag(UUID shipsId, UUID flagId, HashMap<UUID,UUID> map) {
//		if(map.get(shipsId) != null && map.get(shipsId).equals(flagId)){ 
//			return true;
//		} else {
//			return false;
//		}
//	}
	
	/** 
	 * Figure out if asteroid a is already being chased
	 */
	public boolean someoneElseInteractingWithObject(UUID s, UUID a, HashMap<UUID,UUID> map) {
		if(map.containsValue(a)){ 
			//asteroid exists in map
			
			if(map.containsKey(s) && !a.equals(map.get(s))){
				//Ship s in in map
				return true;
			} 
		}
		return false;
	}
	
	public boolean interactingWithObject(UUID s, UUID a, HashMap<UUID,UUID> map) {
		return a.equals(map.get(s));
	}
	
	public boolean canBuyBase(boolean canBuy) {
		return canBuy;
	}
}
