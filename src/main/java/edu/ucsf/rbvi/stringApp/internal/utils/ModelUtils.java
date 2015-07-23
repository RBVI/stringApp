package edu.ucsf.rbvi.stringApp.internal.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;

import org.apache.commons.codec.binary.Base64;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

public class ModelUtils {

	// Node information
	public static String CANONICAL = "Canonical Name";
	public static String ID = "@id";
	public static String DESCRIPTION = "Description";
	public static String NAMESPACE = "Namespace";
	public static String QUERYTERM = "Query Term";
	public static String SEQUENCE = "Sequence";
	public static String SPECIES = "species";
	public static String STRINGID = "Database Identifier";
	public static String STYLE = "String Style";
	public static String TYPE = "Node Type";

	// Edge information
	public static String SCORE = "score";

	// Network information
	public static String CONFIDENCE = "Confidence Score";

	public static List<CyNode> augmentNetworkFromJSON(StringManager manager, CyNetwork net, 
	                                                  List<CyEdge> newEdges, Object object) {
		if (!(object instanceof JSONObject))
			return null;

		JSONObject json = (JSONObject)object;
		
		Map<String, CyNode> nodeMap = new HashMap<>();
		Map<String, String> nodeNameMap = new HashMap<>();
		String species = null;
		for (CyNode node: net.getNodeList()) {
			if (species == null)
				species = net.getRow(node).get(SPECIES, String.class);
			String stringId = net.getRow(node).get(STRINGID, String.class);
			String name = net.getRow(node).get(CyNetwork.NAME, String.class);
			nodeMap.put(stringId, node);
			nodeNameMap.put(stringId, name);
		}

		List<CyNode> nodes = getJSON(manager, species, net, nodeMap, nodeNameMap, null, newEdges, json);
		return nodes;
	}

	public static CyNetwork createNetworkFromJSON(StringNetwork stringNetwork, String species, Object object,
	                                              Map<String, String> queryTermMap) {
		CyNetwork network = createNetworkFromJSON(stringNetwork.getManager(), species, object, queryTermMap);
		stringNetwork.getManager().addStringNetwork(stringNetwork, network);
		return network;
	}

	public static CyNetwork createNetworkFromJSON(StringManager manager, String species, Object object,
	                                              Map<String, String> queryTermMap) {
		if (!(object instanceof JSONObject))
			return null;

		// Create the network
		CyNetwork newNetwork = manager.createNetwork("String network");

		// Create a map to save the nodes
		Map<String, CyNode> nodeMap = new HashMap<>();

		// Create a map to save the node names
		Map<String, String> nodeNameMap = new HashMap<>();

		JSONObject json = (JSONObject)object;

		getJSON(manager, species, newNetwork, nodeMap, nodeNameMap, queryTermMap, null, json);

		manager.addNetwork(newNetwork);
		return newNetwork;
	}

	public static void setConfidence(CyNetwork network, double confidence) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), Double.class, CONFIDENCE);
		network.getRow(network).set(CONFIDENCE, confidence);
	}

	public static Double getConfidence(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(CONFIDENCE) == null)
			return null;
		return network.getRow(network).get(CONFIDENCE, Double.class);
	}

	private static List<CyNode> getJSON(StringManager manager, String species, CyNetwork network, 
	                                    Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap,
																			Map<String, String> queryTermMap,
	                                    List<CyEdge> newEdges,
																			JSONObject json) {

		List<CyNode> newNodes = new ArrayList<>();
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, CANONICAL);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, DESCRIPTION);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ID);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, NAMESPACE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, QUERYTERM);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, SEQUENCE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, SPECIES);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, STRINGID);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, STYLE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, TYPE);

		createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, SCORE);

		// Get the nodes
		JSONArray nodes = (JSONArray)json.get("nodes");
		if (nodes != null && nodes.size() > 0) {
			for (Object nodeObj: nodes) {
				if (nodeObj instanceof JSONObject) {
					JSONObject nodeJSON = (JSONObject)nodeObj;
					CyNode newNode = createNode(network, nodeJSON, species, nodeMap, nodeNameMap, queryTermMap);
					newNodes.add(newNode);
				}
			}
		}

		// Get the edges
		JSONArray edges = (JSONArray)json.get("edges");
		if (edges != null && edges.size() > 0) {
			for (Object edgeObj: edges) {
				if (edgeObj instanceof JSONObject)
					createEdge(network, (JSONObject)edgeObj, nodeMap, nodeNameMap, newEdges);
			}
		}
		return newNodes;
	}

	public static boolean isStringNetwork(CyNetwork network) {
		if (network != null && network.getDefaultNodeTable().getColumn(STRINGID) != null)
			return true;
		return false;
	}

	public static String getExisting(CyNetwork network) {
		StringBuilder str = new StringBuilder();
		for (CyNode node: network.getNodeList()) {
			String stringID = network.getRow(node).get(STRINGID, String.class);
			if (stringID != null && stringID.length() > 0)
				str.append(stringID+"\n");
		}
		return str.toString();
	}

	public static String getSelected(CyNetwork network) {
		StringBuilder selectedStr = new StringBuilder();
		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				String stringID = network.getRow(node).get(STRINGID, String.class);
				if (stringID != null && stringID.length() > 0)
					selectedStr.append(stringID+"\n");
			}
		}
		return selectedStr.toString();
	}

	private static CyNode createNode(CyNetwork network, JSONObject nodeObj, String species, Map<String, CyNode> nodeMap,
	                                 Map<String, String> nodeNameMap, Map<String, String> queryTermMap) {
		String name = (String)nodeObj.get("name");
		String id = (String)nodeObj.get("@id");
		String namespace = id.substring(0,id.indexOf(":"));
		String stringId = id.substring(id.indexOf(":")+1);
		// System.out.println("Node id = "+id+", stringID = "+stringId+", namespace="+namespace);
		CyNode newNode = network.addNode();
		if (nodeObj.containsKey("description")) {
			network.getRow(newNode).set(DESCRIPTION, (String)nodeObj.get("description"));
		}
		if (nodeObj.containsKey("canonical")) {
			network.getRow(newNode).set(CANONICAL, (String)nodeObj.get("canonical"));
		}
		if (nodeObj.containsKey("sequence")) {
			network.getRow(newNode).set(SEQUENCE, (String)nodeObj.get("sequence"));
		}
		network.getRow(newNode).set(CyNetwork.NAME, name);
		network.getRow(newNode).set(STRINGID, stringId);
		network.getRow(newNode).set(ID, id);
		if (species != null)
			network.getRow(newNode).set(SPECIES, species);
		network.getRow(newNode).set(NAMESPACE, namespace);
		network.getRow(newNode).set(TYPE, getType(id));
		if (nodeObj.containsKey("image")) {
			network.getRow(newNode).set(STYLE, "string:"+nodeObj.get("image"));
		} else {
			network.getRow(newNode).set(STYLE, "string:");
		}
		if (queryTermMap != null && queryTermMap.containsKey(stringId)) {
			network.getRow(newNode).set(QUERYTERM, queryTermMap.get(stringId));
		}
		nodeMap.put(stringId, newNode);
		nodeNameMap.put(stringId, name);
		return newNode;
	}

	private static String getType(String id) {
		// Get the namespace
		String namespace = id.substring(0, id.indexOf(":"));
		if (namespace.equals("stringdb"))
			return "protein";
		if (namespace.equals("stitchdb"))
			return "compound";
		return "unknown";
	}

	private static void createEdge(CyNetwork network, JSONObject edgeObj, Map<String, CyNode> nodeMap,
	                               Map<String, String> nodeNameMap, List<CyEdge> newEdges) {
		String source = (String)edgeObj.get("source");
		String target = (String)edgeObj.get("target");
		CyNode sourceNode = nodeMap.get(source);
		CyNode targetNode = nodeMap.get(target);

		CyEdge edge;

		// Don't create an edge if we already have one between these nodes
		if (!network.containsEdge(sourceNode, targetNode)) {
			edge = network.addEdge(sourceNode, targetNode, false);
			network.getRow(edge).set(CyNetwork.NAME, nodeNameMap.get(source)+" (pp) "+nodeNameMap.get(target));
			network.getRow(edge).set(CyEdge.INTERACTION, "pp");
			if (newEdges != null) newEdges.add(edge);
		} else {
			List<CyEdge> edges = network.getConnectingEdgeList(sourceNode, targetNode, CyEdge.Type.ANY);
			if (edges == null)
				return; // Shouldn't happen!
			edge = edges.get(0);
		}

		// Update the score information
		JSONObject scores = (JSONObject)edgeObj.get("scores");
		double scoreProduct = 1.0;
		for (Object key: scores.keySet()) {
			String score = (String) key;
			createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, score);
			Double v = (Double)scores.get(key);
			network.getRow(edge).set(score, v);
			scoreProduct *= (1-v);
		}
		double totalScore = -(scoreProduct-1.0);
		network.getRow(edge).set(SCORE, totalScore);
	}

	public static void createColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null) return;

		table.createColumn(columnName, clazz, false);
	}

	public static void createListColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null) return;

		table.createListColumn(columnName, clazz, false);
	}
}
