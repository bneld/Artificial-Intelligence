package neld9968;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import spacesettlers.clients.ImmutableTeamInfo;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Flag;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

public class PlanningState {
	
	//state
	public Toroidal2DPhysics space;
	public HashMap<UUID, UUID> chasingMap;
	public HashMap<UUID, UUID> haveMap;
	public HashMap<UUID, Double> energyMap;
	public HashMap<String, Double> flagsMap;
	public HashMap<UUID, UUID> collectingMap;
	
	public PlanningState(Toroidal2DPhysics space){
		this.space = space;
		this.chasingMap = new HashMap<>();
		this.haveMap = new HashMap<>();
		this.energyMap = new HashMap<>();
		this.flagsMap = new HashMap<>();
		this.collectingMap = new HashMap<>();
	}
	public PlanningState(PlanningState state){
		this.space = state.space;
		this.chasingMap = (HashMap<UUID, UUID>) state.chasingMap.clone();
		this.haveMap = (HashMap<UUID, UUID>) state.haveMap.clone();
		this.energyMap = (HashMap<UUID, Double>) state.energyMap.clone();
		this.flagsMap = (HashMap<String, Double>) state.flagsMap.clone();
		this.collectingMap = (HashMap<UUID, UUID>) state.collectingMap.clone();
	}
	
	public static PlanningState getInitialState(Toroidal2DPhysics space, 
			Set<AbstractActionableObject> ourShips){
		PlanningState initState = new PlanningState(space);
		
		//set energy for each of our ships
		for (AbstractObject actionable :  ourShips) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				//put in initial energies
				initState.energyMap.put(ship.getId(), ship.getEnergy());
			}
		}
		
		//set score for each team
		for(ImmutableTeamInfo teamInfo : space.getTeamInfo()){
			initState.flagsMap.put(teamInfo.getTeamName(), teamInfo.getScore());
		}
		
		return initState;
	}
	//public static Set<AbstractObject>
}
