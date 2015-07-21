package edu.ucsf.rbvi.stringApp.internal.utils;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;

import org.apache.commons.codec.binary.Base64;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ModelUtils {

	// Node information
	public static String CANONICAL = "Canonical Name";
	public static String ID = "@id";
	public static String DESCRIPTION = "Description";
	public static String NAMESPACE = "Namespace";
	public static String SEQUENCE = "Sequence";
	public static String SPECIES = "species";
	public static String STRINGID = "Database Identifier";
	public static String STYLE = "String Style";
	public static String TYPE = "Node Type";

	public static String SCORE = "score";

	public static CyNetwork createNetworkFromJSON(StringManager manager, String species, Object object) {
		if (!(object instanceof JSONObject))
			return null;

		JSONObject json = (JSONObject)object;

		// Create the network
		CyNetwork newNetwork = manager.createNetwork("String network");

		// Create a map to save the nodes
		Map<String, CyNode> nodeMap = new HashMap<>();

		// Create a map to save the node names
		Map<String, String> nodeNameMap = new HashMap<>();

		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, CANONICAL);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, DESCRIPTION);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, ID);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, NAMESPACE);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, SEQUENCE);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, SPECIES);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, STRINGID);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, STYLE);
		createColumnIfNeeded(newNetwork.getDefaultNodeTable(), String.class, TYPE);

		createColumnIfNeeded(newNetwork.getDefaultEdgeTable(), Double.class, SCORE);

		// Get the nodes
		JSONArray nodes = (JSONArray)json.get("nodes");
		if (nodes != null && nodes.size() > 0) {
			for (Object nodeObj: nodes) {
				if (nodeObj instanceof JSONObject) {
					JSONObject nodeJSON = (JSONObject)nodeObj;
					CyNode newNode = createNode(newNetwork, nodeJSON, species, nodeMap, nodeNameMap);
				}
			}
		}

		// Get the edges
		JSONArray edges = (JSONArray)json.get("edges");
		if (edges != null && edges.size() > 0) {
			for (Object edgeObj: edges) {
				if (edgeObj instanceof JSONObject)
					createEdge(newNetwork, (JSONObject)edgeObj, nodeMap, nodeNameMap);
			}
		}

		manager.addNetwork(newNetwork);
		return newNetwork;
	}

	private static CyNode createNode(CyNetwork network, JSONObject nodeObj, String species, Map<String, CyNode> nodeMap,
	                                 Map<String, String> nodeNameMap) {
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
		network.getRow(newNode).set(SPECIES, species);
		network.getRow(newNode).set(NAMESPACE, namespace);
		network.getRow(newNode).set(TYPE, getType(id));
		if (nodeObj.containsKey("image")) {
			network.getRow(newNode).set(STYLE, "string:"+nodeObj.get("image"));
		} else {
			network.getRow(newNode).set(STYLE, "string:");
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
	                               Map<String, String> nodeNameMap) {
		String source = (String)edgeObj.get("source");
		String target = (String)edgeObj.get("target");
		CyNode sourceNode = nodeMap.get(source);
		CyNode targetNode = nodeMap.get(target);
		CyEdge newEdge = network.addEdge(sourceNode, targetNode, false);
		network.getRow(newEdge).set(CyNetwork.NAME, nodeNameMap.get(source)+" (pp) "+nodeNameMap.get(target));
		network.getRow(newEdge).set(CyEdge.INTERACTION, "pp");

		JSONObject scores = (JSONObject)edgeObj.get("scores");
		double scoreProduct = 1.0;
		for (Object key: scores.keySet()) {
			String score = (String) key;
			createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, score);
			Double v = (Double)scores.get(key);
			network.getRow(newEdge).set(score, v);
			scoreProduct *= (1-v);
		}
		double totalScore = -(scoreProduct-1.0);
		network.getRow(newEdge).set(SCORE, totalScore);
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
