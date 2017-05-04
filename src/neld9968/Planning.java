package neld9968;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
	
	public void update(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects){
		this.space = space;
		this.actionableObjects = actionableObjects;
		initialState = PlanningState.getInitialState(space, actionableObjects);
		currentState = new PlanningState(initialState);
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
		if(!someoneElseInteractingWithFlag(s, f, currentState.chasingMap) 
				&& s.getEnergy() > 1000 
				&& !someoneElseInteractingWithFlag(s, f, currentState.haveMap)) {
			return true;
		} else {
			return false;	
		}
	}
	
	public boolean obtainFlag(Ship s, Flag f){
		//currentState
		//check if s is chasing f
		if(interactingWithFlag(s, f, currentState.chasingMap) 
				&& !someoneElseInteractingWithFlag(s, f, currentState.haveMap)
				&& !interactingWithFlag(s, f, currentState.haveMap) ) {
			// preconditions met
			System.out.println("Obtaining flag");
			return true;
		} else {
			System.out.println("Not obtaining flag wut wut");
			return false;
		}
	}
	public boolean returnFlag(Ship s, Flag f){
		if(interactingWithFlag(s, f, currentState.haveMap) ) {
			System.out.println("Returning flag!");
			return true;
		} else {
			System.out.println("Not return flag");
			return false;
		}
	}
	public boolean getResource(Ship s, Asteroid a){
		if(s.getEnergy() > 1000 
				&& !someoneElseInteractingWithAsteroid(s, a, currentState.collectingMap)) {
			return true;
		} else {
			return false;	
		}
	}
	public boolean returnResource(Ship s, Asteroid a){
		if(s.getEnergy() > 1000 
				&& s.getResources().getTotal() > 500 
				&& !interactingWithAsteroid(s, a, currentState.collectingMap)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean buildBase(boolean checkBase) {
		return canBuyBase(checkBase);
	}

	public boolean someoneElseInteractingWithFlag(Ship s, Flag f, HashMap<Ship,Flag> map) {
		//figure out if Flag f is already being chased
		if(map.containsValue(f)){ 
			Flag shipsFlag = map.get(s);
			if( shipsFlag != null){ // if ship is in map
				// if not flag passing in
				if(!shipsFlag.getId().equals(f.getId())){
					//Flag is already being chased
					return true;
				} else {
					return false;
				}
			} else {
				//Ship is not in Map but Flag is
				return true;
			}
		} else {
			return false;
		}
	}
	
	public boolean interactingWithFlag(Ship s, Flag f, HashMap<Ship,Flag> map) {
		if(map.get(s).getId().equals(f.getId())){ 
			return true;
		} else {
			return false;
		}
	}
	
	public boolean someoneElseInteractingWithAsteroid(Ship s, Asteroid a, HashMap<Ship,Asteroid> map) {
		//figure out if asteroid a is already being chased
		if(map.containsValue(a)){ 
			Asteroid shipsAsteroid = map.get(s);
			if( shipsAsteroid != null){ // if ship is in map
				// if not asteroid passing in
				if(!shipsAsteroid.getId().equals(a.getId())){
					//Asteroid is already being chased
					return true;
				} else {
					return false;
				}
			} else {
				//Ship is not in Map but asteroid is
				return true;
			}
		} else {
			return false;
		}
	}
	
	public boolean interactingWithAsteroid(Ship s, Asteroid a, HashMap<Ship,Asteroid> map) {
		if(map.get(s).getId().equals(a.getId())){ 
			return true;
		} else {
			return false;
		}
	}
	
	public boolean canBuyBase(boolean canBuy) {
		return canBuy;
	}
}
