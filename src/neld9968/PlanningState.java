package neld9968;

import java.util.HashMap;
import java.util.Set;

import spacesettlers.clients.ImmutableTeamInfo;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Flag;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class PlanningState {
	public Toroidal2DPhysics space;
	public HashMap<Ship, Flag> chasingMap;
	public HashMap<Ship, Flag> haveMap;
	public HashMap<Ship, Double> energyMap;
	public HashMap<String, Integer> flagsMap;
	
	public PlanningState(Toroidal2DPhysics space){
		this.space = space;
		this.chasingMap = new HashMap<>();
		this.haveMap = new HashMap<>();
		this.energyMap = new HashMap<>();
		this.flagsMap = new HashMap<>();
	}
	public PlanningState(PlanningState state){
		this.space = state.space;
		this.chasingMap = (HashMap<Ship, Flag>) state.chasingMap.clone();
		this.haveMap = (HashMap<Ship, Flag>) state.haveMap.clone();
		this.energyMap = (HashMap<Ship, Double>) state.energyMap.clone();
		this.flagsMap = (HashMap<String, Integer>) state.flagsMap.clone();
	}
	public static PlanningState initialState(Toroidal2DPhysics space, 
			Set<AbstractActionableObject> ourShips){
		PlanningState initState = new PlanningState(space);
		for (AbstractObject actionable :  ourShips) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				//put in initial energies
				initState.energyMap.put(ship, ship.getEnergy());
			}
		}
		//
		for(ImmutableTeamInfo teamInfo : space.getTeamInfo()){
			
		}
	}
	public static Set<AbstractObject>
}
