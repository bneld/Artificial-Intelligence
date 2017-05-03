package neld9968;

import java.util.HashMap;
import java.util.Map;

import spacesettlers.objects.*;
import spacesettlers.simulator.Toroidal2DPhysics;

public class Planning {
	Toroidal2DPhysics space;
	PlanningState initialState;
	
	public Planning(Toroidal2DPhysics space){
		this.space = space;
		initialState = new PlanningState(space);
		
		
	}
	
}
