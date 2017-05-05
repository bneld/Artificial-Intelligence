package neld9968;

import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import com.sun.corba.se.spi.orbutil.fsm.State;
import com.sun.org.apache.xerces.internal.dom.ChildNode;
import com.sun.org.apache.xpath.internal.axes.NodeSequence;
import com.sun.xml.internal.ws.api.model.wsdl.editable.EditableWSDLBoundFault;

import jdk.nashorn.internal.runtime.regexp.joni.constants.NodeStatus;
import spacesettlers.objects.*;
import spacesettlers.simulator.Toroidal2DPhysics;

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
	
	public Planning(){
		stack = new Stack<>();
	}
	public Planning(Toroidal2DPhysics space){
		this.space = space;
		stack = new Stack<>();
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
//			System.out.println("Returning flag!");
			return true;
		} else {
			System.out.println("Not return flag");
			return false;
		}
	}
	public boolean getResource(Ship s, Asteroid a){
		if(s.getEnergy() > 2000 
				&& !someoneElseInteractingWithObject(s.getId(), a.getId(), currentState.collectingMap)) {
			return true;
		} else {
			return false;	
		}
	}
	public boolean returnResource(Ship s){
		if(s.getEnergy() > 2000 
				&& s.getResources().getTotal() > 500) {
			return true;
		} else {
			return false;
		}
	}
	
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
	
	public boolean interactingWithObject(UUID s, UUID a, HashMap<UUID,UUID> map) {
		return a.equals(map.get(s));
	}
	
	public boolean canBuyBase(boolean canBuy) {
		return canBuy;
	}
	
	public StateNode populateSearchTree(StateNode node, int counter){
		if(counter > MAX_TREE_DEPTH) return null;
		//StateNode node = new StateNode(state);
		System.out.println(counter);
		
//		System.out.println("\n");
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
		//System.out.println("parent: " + state);
		for(StateNode childNode : node.children){
			System.out.println("  recursion: " + childNode.state.hashCode());
			populateSearchTree(childNode, ++counter);
		}
		//System.out.println(state);
		return node;
		
	}
	public boolean isGoalState(StateNode node){
		return node.state.flags == 1;
	}
	public Stack<StateNode> searchTree(StateNode node, Stack<StateNode> stack){
		System.out.println(node);
		if(stack != null) stack.add(node);
		if(isGoalState(node)){
			return stack;
		}
		for(StateNode childNode : node.children){
			Stack<StateNode> resultStack = searchTree(childNode, stack);
			if(resultStack != null) return resultStack;
		}
		stack.pop();
		return null;
	}
	public boolean isChasingLegal(PredictedState state){
		return (!state.hasFlag && !state.isChasingFlag);
	}
	public boolean isObtainingLegal(PredictedState state){
		return (!state.hasFlag && state.isChasingFlag);
	}
	public boolean isReturningLegal(PredictedState state){
		return (state.hasFlag);
	}
	
	public class StateNode {
		
		PredictedState state;
		String actionLeadingToState;
		ArrayList<StateNode> children;
		
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
	
	public static void main(String[] args){
		PredictedState state = new PredictedState();
		Planning planning = new Planning();
		StateNode node = planning.new StateNode(state);

		planning.populateSearchTree(node, 0);
		Stack<StateNode> stack = planning.searchTree(node, new Stack<StateNode>());
		System.out.println(node);
		System.out.println(stack);
		while(!stack.isEmpty()){
			StateNode node2 = stack.pop();
			System.out.println(node2.actionLeadingToState);
		}
	}
}
