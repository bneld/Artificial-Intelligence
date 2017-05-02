package neld9968;

import java.util.HashMap;

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
}
