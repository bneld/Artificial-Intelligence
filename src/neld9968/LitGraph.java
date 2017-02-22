package neld9968;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class LitGraph {
		
	public ArrayList<Node> nodes;
	public ArrayList<Edge> edges;
	Map<Position, Node> map;
	Node startNode;
	Node targetNode;
	
	public LitGraph(Position start, Position target, List<Position> points, Toroidal2DPhysics space){
		nodes = new ArrayList<>();
		edges = new ArrayList<>();
		map = new HashMap<>();
		
		//add all nodes
		for(Position pos : points){
			Node node = new Node(pos, space.findShortestDistance(pos, target));
			nodes.add(node);
			map.put(pos, node);
		}
		
		//find edges
		for(Position current : points){
			for(Position other : points){
				if(!other.equals(current)){ //don't add edge to itself
					if(space.isPathClearOfObstructions(current, other, Mastermind.getAllObstructionsBetweenAbstractObjects(space, Mastermind.currentTarget), Ship.SHIP_RADIUS)){
//						System.out.println("\n\n\n\n\n rofl \n\n\n\n");
						addEdge(map.get(current), map.get(other), space.findShortestDistance(current, other));
					}
				}
				
			}
		}
		startNode = map.get(start);
		targetNode = map.get(target);
		LITBOIZ.edges = edges;
		LITBOIZ.nodes = nodes;
	}
	
	public void addEdge(Node x, Node y, double weight){
		Edge edgeX = new Edge(x, y, weight);
		x.edges.add(edgeX);
		Edge edgeY = new Edge(y, x, weight);
		y.edges.add(edgeY);
		edges.add(edgeX);
		edges.add(edgeY);
	}
	
	static class Node {
		public Position position;
		public List<Edge> edges;
		public double f = Double.MAX_VALUE;
		public double h;
		public double g;
		
		public Node(Position position, double h){
			this.position = position;
			edges = new ArrayList<>();
			this.h= h;
		}
		
		public String toString(){
			return "(" + position.getX() + ", " + position.getY() + ")";
		}
	}
	
	class Edge {
		public Node start;
		public Node end;
		public double weight;
		
		public Edge(Node start, Node end, double weight){
			this.start = start;
			this.end = end;
			this.weight = weight;
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(Node n : nodes){
			sb.append(n.toString() + " ");
		}
		return sb.toString();
	}
}

