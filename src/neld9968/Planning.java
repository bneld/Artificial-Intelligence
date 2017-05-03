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
	
	public Planning(){
		
	}
	public Planning(Toroidal2DPhysics space){
		this.space = space;
		initialState = PlanningState.getInitialState(space, actionableObjects);
	}
	
	public void update(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects, PlanningState currentState){
		this.space = space;
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
	public boolean chaseFlag(Ship s, Flag f){
		//figure out if Flag f is already being chased
		if(currentState.chasingMap.containsValue(f)){
			Flag shipsFlag = currentState.chasingMap.get(s);
			if( shipsFlag != null){
				if(!shipsFlag.getId().equals(f.getId())){
					//Flag is already being chased
				
				}
			} else {
				//Ship is not in Map but Flag is
				
			}
		} else {
			currentState.chasingMap.put(s, f);
		}
		
	}
	public void obtainFlag(Ship s, Flag f){
		currentState.
	}
	public void returnFlag(Ship s, Flag f){
		currentState.
	}
	public void getResource(Ship s, Asteroid a){
		currentState.
	}
	public void returnResource(Ship s, Asteroid a){
		currentState.
	}
	public void returnToBase(Ship s, Base b){
		
	}
}
