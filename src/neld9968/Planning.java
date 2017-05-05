package neld9968;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import spacesettlers.objects.*;
import spacesettlers.simulator.Toroidal2DPhysics;


/**
 * Planning class that determines what moves to make
 * 
 * 
 * @author Luis & Brian
 */
public class Planning {
	final String CHASE = "chaseFlag";
	final String OBTAIN = "obtainFlag";
	final String RETURN = "returnFlag";
	final int MAX_TREE_DEPTH = 8;
	Stack<StateNode> stack;
	
	Toroidal2DPhysics space;
	Set<AbstractActionableObject> actionableObjects;
	PlanningState initialState;
	PlanningState currentState;
	
	// constructor
	public Planning(){
		stack = new Stack<>();
	}
	
	//constructor with copy of space
	public Planning(Toroidal2DPhysics space){
		this.space = space;
		stack = new Stack<>();
	}
	
	/**
	 * Initialize variables
	 * @param space
	 * @param set
	 * @param actionableObjects
	 */
	public void init(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects){
		this.space = space;
		this.actionableObjects = actionableObjects;
		initialState = PlanningState.getInitialState(space, actionableObjects);
		currentState = new PlanningState(initialState);
	}
	
	/**
	 * Update the objects
	 * @param actionableObjects
	 */
	public void update(Set<AbstractActionableObject> actionableObjects){
		this.actionableObjects = actionableObjects;
	}

	/**
	 * chase flag action
	 * @param space
	 * @param flag
	 * @return boolean if action should be taken
	 */
	public boolean chaseFlag(Ship s, Flag f){
		if (f == null) return false;
		//preconditions
		if(!someoneElseInteractingWithObject(s.getId(), f.getId(), currentState.chasingMap) 
				&& s.getEnergy() > 1000 
				&& !someoneElseInteractingWithObject(s.getId(), f.getId(), currentState.haveMap)) {
			return true;
		} else {
			return false;	
		}
	}
	
	/**
	 * obtain flag action
	 * @param space
	 * @param flag
	 * @return boolean if action should be taken
	 */
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
	/**
	 * return flag action
	 * @param space
	 * @param flag
	 * @return boolean if action should be taken
	 */
	public boolean returnFlag(Ship s, Flag f){
		//preconditions
		if(interactingWithObject(s.getId(), f.getId(), currentState.haveMap) ) {
			return true;
		} else {
			System.out.println("Not return flag");
			return false;
		}
	}
	/**
	 * get resource action
	 * @param space
	 * @param asteroid
	 * @return boolean if action should be taken
	 */
	public boolean getResource(Ship s, Asteroid a){
		//preconditions
		if(s.getEnergy() > 2000 
				&& !someoneElseInteractingWithObject(s.getId(), a.getId(), currentState.collectingMap)) {
			return true;
		} else {
			return false;	
		}
	}
	
	/**
	 * return resource action
	 * @param space
	 * @return boolean if action should be taken
	 */
	public boolean returnResource(Ship s){
		//preconditions
		if(s.getEnergy() > 2000 
				&& s.getResources().getTotal() > 500) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * build base action
	 * @param checkBase
	 * @return boolean if action should be taken
	 */
	public boolean buildBase(boolean checkBase) {
		return canBuyBase(checkBase);
	}
	
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
	
	/**
	 * check if something is already interacting with passed object
	 * @param space
	 * @param asteroid
	 * @param map
	 * @return true if interacting, false if not
	 */
	public boolean interactingWithObject(UUID s, UUID a, HashMap<UUID,UUID> map) {
		return a.equals(map.get(s));
	}
	
	
	/**
	 * determine if base can be bought
	 * @param canBuy
	 * @return boolean if action should be taken
	 */
	public boolean canBuyBase(boolean canBuy) {
		return canBuy;
	}
	
	/**
	 * populate planning/action tree reccursively
	 * @param node
	 * @param counter
	 * @return nodes
	 */
	public StateNode populateSearchTree(StateNode node, int counter){
		if(counter > MAX_TREE_DEPTH) return null;
		System.out.println(counter);
		
//		check legal actions
		if(isChasingLegal(node.state)){
			StateNode childNode = new StateNode(new PredictedState(node.state));
			childNode.state.isChasingFlag = true;
			node.children.add(childNode);
			childNode.actionLeadingToState = CHASE;
			System.out.println("ADding chasing node");
		}
		if(isObtainingLegal(node.state)){
			StateNode childNode = new StateNode(new PredictedState(node.state));
			childNode.state.isChasingFlag = false;
			childNode.state.hasFlag = true;
			node.children.add(childNode);
			childNode.actionLeadingToState = OBTAIN;
			System.out.println("ADding obtian node");
		}
		if(isReturningLegal(node.state)){
			StateNode childNode = new StateNode(new PredictedState(node.state));
			childNode.state.hasFlag = false;
			childNode.state.flags++;
			node.children.add(childNode);
			childNode.actionLeadingToState = RETURN;
			System.out.println("ADding return node");
		}
		//iterate through and reccursively populate children of new nodes
		for(StateNode childNode : node.children){
			System.out.println("  recursion: " + childNode.state.hashCode());
			populateSearchTree(childNode, ++counter);
		}
		return node;
		
	}
	
	/**
	 * check if ndoe is the goal state
	 * @param node
	 * @return true if goal
	 */
	public boolean isGoalState(StateNode node){
		return node.state.flags == 1;
	}
	
	/**
	 * build the search tree
	 * @param node
	 * @param stack
	 * @return stack of tree
	 */
	public Stack<StateNode> searchTree(StateNode node, Stack<StateNode> stack){
		if(stack != null) stack.add(node); // add node
		if(isGoalState(node)){ // return if goal
			return stack;
		}
		for(StateNode childNode : node.children){ // recursive call
			Stack<StateNode> resultStack = searchTree(childNode, stack);
			if(resultStack != null) return resultStack;
		}
		stack.pop(); //pop
		return null;
	}
	
	/**
	 * check if chasing is legal
	 * @param state
	 * @return true if legal, false if not
	 */
	public boolean isChasingLegal(PredictedState state){
		return (!state.hasFlag && !state.isChasingFlag);
	}
	
	/**
	 * check if obtaining is legal
	 * @param state
	 * @return true if legal, false if not
	 */
	public boolean isObtainingLegal(PredictedState state){
		return (!state.hasFlag && state.isChasingFlag);
	}
	
	/**
	 * check if returning is legal
	 * @param state
	 * @return true if legal, false if not
	 */
	public boolean isReturningLegal(PredictedState state){
		return (state.hasFlag);
	}
	
	/**
	 * Build state node class, holds states of node
	 * 
	 * @author Luis & Brian
	 */
	public class StateNode {
		
		PredictedState state; // prediction
		String actionLeadingToState; // action
		ArrayList<StateNode> children; // children of node
		
		// populate variables
		StateNode(PredictedState state){
			children = new ArrayList<>();
			actionLeadingToState = "";
			this.state = state;
		}
		
		@Override
		public String toString(){
			return state.toString() + "\n" + actionLeadingToState + ", " + children.size() + " children";
		}
	}
}
