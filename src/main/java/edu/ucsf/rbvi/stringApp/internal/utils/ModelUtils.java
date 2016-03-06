package edu.ucsf.rbvi.stringApp.internal.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import org.cytoscape.view.model.View;

import org.apache.commons.codec.binary.Base64;

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

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
	public static String TM_FOREGROUND = "TextMining Foreground";
	public static String TM_BACKGROUND = "TextMining Background";
	public static String TM_SCORE = "TextMining Score";

	// Edge information
	public static String SCORE = "score";

	// Network information
	public static String CONFIDENCE = "Confidence Score";

	public static List<TextMiningResult> getIdsFromJSON(StringManager manager, int taxon, Object object, String query) {
		if (!(object instanceof JSONArray))
			return null;

		JSONArray tmResults = (JSONArray)object;
		JSONObject nodeDict = (JSONObject)tmResults.get(0);
		Boolean limited = (Boolean)tmResults.get(1);

		List<TextMiningResult> results = new ArrayList<>();

		for (Object stringid: nodeDict.keySet()) {
			JSONObject data = (JSONObject)nodeDict.get(stringid);
			String name = data.get("name").toString();
			int fg = ((Long)data.get("foreground")).intValue();
			int bg = ((Long)data.get("background")).intValue();
			Double score = (Double)data.get("score");
			TextMiningResult tm = new TextMiningResult(taxon+"."+(String)stringid, name, fg, bg, score);
			results.add(tm);
		}
		return results;

	}

	public static void addTextMiningResults (StringManager manager, List<TextMiningResult> tmResults, CyNetwork network) {
		// Create our columns
		createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, TM_FOREGROUND);
		createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, TM_BACKGROUND);
		createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, TM_SCORE);

		// Create a map of our results
		Map<String, TextMiningResult> resultsMap = new HashMap<>();
		for (TextMiningResult tm: tmResults) {
			resultsMap.put(tm.getID(), tm);
		}

		for (CyNode node: network.getNodeList()) {
			CyRow row = network.getRow(node);
			String id = row.get(STRINGID, String.class);
			if (resultsMap.containsKey(id)) {
				TextMiningResult result = resultsMap.get(id);
				row.set(TM_FOREGROUND, result.getForeground());
				row.set(TM_BACKGROUND, result.getBackground());
				row.set(TM_SCORE, result.getScore());
			}
		}
	}

	public static List<CyNode> createTMNetworkFromJSON(StringManager manager, Species species, Object object, String query) {
		if (!(object instanceof JSONArray))
			return null;

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(query);

		List<CyNode> nodes = getJSON(manager, species, newNetwork, (JSONArray)object);
		return nodes;
	}

	public static List<CyNode> augmentNetworkFromJSON(StringManager manager, CyNetwork net,
	                                                  List<CyEdge> newEdges, Object object,
																				            Map<String, String> queryTermMap) {
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

		List<CyNode> nodes = getJSON(manager, species, net, nodeMap, nodeNameMap, queryTermMap, newEdges, json);
		return nodes;
	}

	public static CyNetwork createNetworkFromJSON(StringNetwork stringNetwork, String species, Object object,
	                                              Map<String, String> queryTermMap, String ids) {
		CyNetwork network = createNetworkFromJSON(stringNetwork.getManager(), species, object, queryTermMap, ids);
		stringNetwork.getManager().addStringNetwork(stringNetwork, network);
		return network;
	}

	public static CyNetwork createNetworkFromJSON(StringManager manager, String species, Object object,
	                                              Map<String, String> queryTermMap, String ids) {
		if (!(object instanceof JSONObject))
			return null;

		// Get a network name
		String str = manager.getNetworkName(ids);

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(str);

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

	private static List<CyNode> getJSON(StringManager manager, Species species, CyNetwork network, 
	                                    JSONArray tmResults) {
		List<CyNode> newNodes = new ArrayList<>();
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ID);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, SPECIES);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, STRINGID);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, STYLE);
		createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, TM_FOREGROUND);
		createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, TM_BACKGROUND);
		createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, TM_SCORE);

		JSONObject nodeDict = (JSONObject)tmResults.get(0);
		Boolean limited = (Boolean)tmResults.get(1);

		List<CyNode> nodes = new ArrayList<>();

		for (Object stringid: nodeDict.keySet()) {
			JSONObject data = (JSONObject)nodeDict.get(stringid);
			String name = data.get("name").toString();
			int fg = ((Long)data.get("foreground")).intValue();
			int bg = ((Long)data.get("background")).intValue();
			Double score = (Double)data.get("score");
			CyNode newNode = network.addNode();
			CyRow row = network.getRow(newNode);
			row.set(ID, "stringdb:"+species.getTaxId()+"."+stringid.toString());
			row.set(CyNetwork.NAME, name);
			row.set(SPECIES, species.getName());
			row.set(STRINGID, species.getTaxId()+"."+stringid.toString());
			row.set(STYLE, "string:");
			row.set(TM_FOREGROUND, fg);
			row.set(TM_BACKGROUND, bg);
			row.set(TM_SCORE, score);
			nodes.add(newNode);
		}
		return nodes;
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

		Set<String> columnMap = new HashSet<>();

		// Get the nodes
		JSONArray nodes = (JSONArray)json.get("nodes");
		if (nodes != null && nodes.size() > 0) {
			for (Object nodeObj: nodes) {
				if (nodeObj instanceof JSONObject) {
					JSONObject nodeJSON = (JSONObject)nodeObj;
					CyNode newNode = createNode(network, nodeJSON, species, nodeMap, nodeNameMap, queryTermMap, columnMap);
					if (newNode != null)
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

	public static String getSelected(CyNetwork network, View<CyNode> nodeView) {
		StringBuilder selectedStr = new StringBuilder();
		if (nodeView != null) {
			String stringID = network.getRow(nodeView.getModel()).get(STRINGID, String.class);
			selectedStr.append(stringID+"\n");
		}

		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				String stringID = network.getRow(node).get(STRINGID, String.class);
				if (stringID != null && stringID.length() > 0)
					selectedStr.append(stringID+"\n");
			}
		}
		return selectedStr.toString();
	}

	private static CyNode createNode(CyNetwork network, JSONObject nodeObj, String species,
	                                 Map<String, CyNode> nodeMap,
	                                 Map<String, String> nodeNameMap, Map<String, String> queryTermMap,
																	 Set<String> columnMap) {
		String name = (String)nodeObj.get("name");
		String id = (String)nodeObj.get("@id");
		String namespace = id.substring(0,id.indexOf(":"));
		String stringId = id.substring(id.indexOf(":")+1);
		if (nodeMap.containsKey(stringId))
			return null;
		// System.out.println("Node id = "+id+", stringID = "+stringId+", namespace="+namespace);
		CyNode newNode = network.addNode();
		CyRow row = network.getRow(newNode);

		row.set(CyNetwork.NAME, name);
		row.set(STRINGID, stringId);
		row.set(ID, id);
		if (species != null)
			row.set(SPECIES, species);
		row.set(NAMESPACE, namespace);
		row.set(TYPE, getType(id));
		row.set(STYLE, "string:"); // We may overwrite this, if we get an image

		for (Object objKey: nodeObj.keySet()) {
			String key = (String)objKey;
			// Look for our "special" columns
			if (key.equals("description")) {
				row.set(DESCRIPTION, (String)nodeObj.get("description"));
			} else if (key.equals("canonical")) {
				row.set(CANONICAL, (String)nodeObj.get("canonical"));
			} else if (key.equals("sequence")) {
				network.getRow(newNode).set(SEQUENCE, (String)nodeObj.get("sequence"));
			} else if (key.equals("image")) {
				row.set(STYLE, "string:"+nodeObj.get("image"));
			} else {
				// It's not one of our "standard" attributes, create a column for it (if necessary) and then add it
				Object value = nodeObj.get(key);
				if (value instanceof JSONArray) {
					JSONArray list = (JSONArray)value;
					if (!columnMap.contains(key)) {
						Object element = list.get(0);
						createListColumnIfNeeded(network.getDefaultNodeTable(), element.getClass(), key);
						columnMap.add(key);
					}
					row.set(key, list);
				} else {
					if (!columnMap.contains(key)) {
						createColumnIfNeeded(network.getDefaultNodeTable(), value.getClass(), key);
						columnMap.add(key);
					}
					row.set(key, value);
				}
			}
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
