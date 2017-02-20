package neld9968;

import spacesettlers.objects.AbstractObject;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.actions.*;


/**
 * Calls MoveAction for the actual movements but allows you to aim for a spacewar object
 * and to stop when the object dies (e.g. someone (maybe you) reached it)
 * 
 * @author amy
 */
public class LITBOIZMOVETOOBJECTACTION extends LITBOIZMOVEACTION {
	AbstractObject goalObject;
	Position originalGoalLocation;
	
	/**
	 * Initialize with your location and the goal object 
	 * 
	 * @param space
	 * @param currentLocation
	 * @param goalObject
	 */
	public LITBOIZMOVETOOBJECTACTION(Toroidal2DPhysics space, Position currentLocation, AbstractObject goalObject) {
		super(space, currentLocation, goalObject.getPosition());
		this.goalObject = goalObject;
		this.originalGoalLocation = goalObject.getPosition().deepCopy();
	}
	
	/**
	 * Return the goal object (and remember it is a clone so use its UUID!)
	 * @return
	 */
	public AbstractObject getGoalObject() {
		return goalObject;
	}




	/**
	 * Returns true if the movement finished or the goal object died or moved
	 * 
	 */
	public boolean isMovementFinished(Toroidal2DPhysics space) {
		if (super.isMovementFinished(space)) {
			return true;
		}
		
		AbstractObject newGoalObj = space.getObjectById(goalObject.getId());
		
		// goal object disappeared
		if (newGoalObj == null) {
			return true;
		}
		
		// goal object died
		if (!newGoalObj.isAlive()) {
			return true;
		} 

		// goal object moved
		if (!newGoalObj.getPosition().equalsLocationOnly(originalGoalLocation)) {
			return true;
		}
		
		return false;
	}
	

}