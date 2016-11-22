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
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import org.cytoscape.view.model.View;

import org.apache.commons.codec.binary.Base64;

import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

public class ModelUtils {

	// Node information
	public static String CANONICAL = "canonical name";
	public static String CV_STYLE = "chemViz Passthrough";
	public static String ID = "@id";
	public static String DESCRIPTION = "description";
	public static String DISEASE_SCORE = "disease score";
	public static String NAMESPACE = "namespace";
	public static String QUERYTERM = "query term";
	public static String SEQUENCE = "sequence";
	public static String SMILES = "smiles";
	public static String SPECIES = "species";
	public static String STRINGID = "database identifier";
	public static String STYLE = "STRING style";
	public static String TYPE = "node type";
	public static String TM_FOREGROUND = "textmining foreground";
	public static String TM_BACKGROUND = "textmining background";
	public static String TM_SCORE = "textmining score";
	// public static String TM_LINKOUT = "TextMining Linkout";

	public static int NDOCUMENTS = 50;
	public static int NEXPERIMENTS = 50;
	public static int NKNOWLEDGE = 50;

	// public static String DISEASEINFO = "http://diseases.jensenlab.org/Entity?type1=9606&type2=-26";

	// Edge information
	public static String SCORE = "score";

	// Network information
	public static String CONFIDENCE = "confidence score";

	public static List<EntityIdentifier> getEntityIdsFromJSON(StringManager manager, Object object) {
		if (!(object instanceof JSONArray))
			return null;

		JSONArray tmResults = (JSONArray)object;
		JSONArray entityArray = (JSONArray)tmResults.get(0);
		Boolean limited = (Boolean)tmResults.get(1);
		List<EntityIdentifier> results = new ArrayList<>();

		for (Object entityDict: entityArray) {
			JSONObject data = (JSONObject)entityDict;
			String matched = data.get("matched").toString();
			String primary = data.get("primary").toString();
			String id = data.get("id").toString();
			Long type = (Long)data.get("type");
			EntityIdentifier ei = new EntityIdentifier(matched, primary, type, id);
			results.add(ei);
		}
		return results;
	}

	public static List<TextMiningResult> getIdsFromJSON(StringManager manager, int taxon, Object object, String query, boolean disease) {
		if (!(object instanceof JSONArray))
			return null;

		JSONArray tmResults = (JSONArray)object;
		JSONObject nodeDict = (JSONObject)tmResults.get(0);
		Boolean limited = (Boolean)tmResults.get(1);

		List<TextMiningResult> results = new ArrayList<>();

		for (Object stringid: nodeDict.keySet()) {
			int fg = -1;
			int bg = -1;
			JSONObject data = (JSONObject)nodeDict.get(stringid);
			String name = data.get("name").toString();
			if (data.containsKey("foreground"))
				fg = ((Long)data.get("foreground")).intValue();
			if (data.containsKey("background"))
				bg = ((Long)data.get("background")).intValue();
			Double score = (Double)data.get("score");
			// String url = getDiseaseURL((String)stringid, query);
			TextMiningResult tm = new TextMiningResult(taxon+"."+(String)stringid, name, fg, bg, score, disease);
			results.add(tm);
		}
		return results;

	}

	public static void addTextMiningResults (StringManager manager, List<TextMiningResult> tmResults, CyNetwork network) {
		boolean haveFBValues = false;
		boolean haveDisease = false;

		// Create a map of our results
		Map<String, TextMiningResult> resultsMap = new HashMap<>();
		for (TextMiningResult tm: tmResults) {
			resultsMap.put(tm.getID(), tm);
			if (tm.getForeground() > 0 || tm.getBackground() > 0)
				haveFBValues = true;
			if (tm.isDisease())
				haveDisease = true;
		}

		// Create our columns
		if (haveFBValues) {
			createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, TM_FOREGROUND);
			createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, TM_BACKGROUND);
		}

		/*
		if (haveLinkout) {
			createColumnIfNeeded(network.getDefaultNodeTable(), String.class, TM_LINKOUT);
		}
		*/

		if (haveDisease)
			createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, DISEASE_SCORE);
		else
			createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, TM_SCORE);

		for (CyNode node: network.getNodeList()) {
			CyRow row = network.getRow(node);
			String id = row.get(STRINGID, String.class);
			if (resultsMap.containsKey(id)) {
				TextMiningResult result = resultsMap.get(id);
				if (result.getForeground() > 0)
					row.set(TM_FOREGROUND, result.getForeground());
				if (result.getBackground() > 0)
					row.set(TM_BACKGROUND, result.getBackground());
				/*
				if (result.getLinkout() != null)
					row.set(TM_LINKOUT, result.getLinkout());
				*/
				if (haveDisease)
					row.set(DISEASE_SCORE, result.getScore());
				else
					row.set(TM_SCORE, result.getScore());
			}
		}
	}

	public static List<CyNode> createTMNetworkFromJSON(StringManager manager, 
	                                                   Species species, Object object, String query) {
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
		boolean useSTITCH = false;
		for (CyNode node: net.getNodeList()) {
			if (species == null)
				species = net.getRow(node).get(SPECIES, String.class);
			String stringId = net.getRow(node).get(STRINGID, String.class);
			String name = net.getRow(node).get(CyNetwork.NAME, String.class);
			nodeMap.put(stringId, node);
			nodeNameMap.put(stringId, name);
			if (isCompound(net, node))
				useSTITCH = true;
		}

		List<CyNode> nodes = getJSON(manager, species, net, nodeMap, nodeNameMap, 
		                             queryTermMap, newEdges, json, useSTITCH);
		return nodes;
	}

	public static CyNetwork createNetworkFromJSON(StringNetwork stringNetwork, String species, Object object,
	                                              Map<String, String> queryTermMap, String ids, String netName,
																								boolean useSTITCH) {
		stringNetwork.getManager().ignoreAdd();
		CyNetwork network = createNetworkFromJSON(stringNetwork.getManager(), species, object, 
		                                          queryTermMap, ids, netName, useSTITCH);
		stringNetwork.getManager().addStringNetwork(stringNetwork, network);
		stringNetwork.getManager().listenToAdd();
		return network;
	}

	public static CyNetwork createNetworkFromJSON(StringManager manager, String species, Object object,
	                                              Map<String, String> queryTermMap, String ids, 
																								String netName, boolean useSTITCH) {
		if (!(object instanceof JSONObject))
			return null;

		// Get a network name
		String defaultName;
	 	if (useSTITCH)
			defaultName	= "STITCH Network";
		else
			defaultName	= "String Network";
		if (netName != null && netName != "") {
			netName = defaultName + " - " + netName;
		}
		else if (queryTermMap.size() == 1 && queryTermMap.containsKey(ids)) {
			netName = defaultName + " - " + queryTermMap.get(ids);
		} else {
			netName = defaultName;
			// netName = manager.getNetworkName(ids);
		}

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(netName);

		// Create a map to save the nodes
		Map<String, CyNode> nodeMap = new HashMap<>();

		// Create a map to save the node names
		Map<String, String> nodeNameMap = new HashMap<>();

		JSONObject json = (JSONObject)object;

		getJSON(manager, species, newNetwork, nodeMap, nodeNameMap, queryTermMap, null, json, useSTITCH);

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
																			JSONObject json, boolean useSTITCH) {

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
		if (useSTITCH) {
			createColumnIfNeeded(network.getDefaultNodeTable(), String.class, CV_STYLE);
			createColumnIfNeeded(network.getDefaultNodeTable(), String.class, SMILES);
		}

		createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, SCORE);

		Set<String> columnMap = new HashSet<>();

		// Get the nodes
		JSONArray nodes = (JSONArray)json.get("nodes");
		if (nodes != null && nodes.size() > 0) {
			for (Object nodeObj: nodes) {
				if (nodeObj instanceof JSONObject) {
					JSONObject nodeJSON = (JSONObject)nodeObj;
					CyNode newNode = 
									createNode(network, nodeJSON, species, nodeMap, nodeNameMap, queryTermMap, columnMap);
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

	public static boolean isMergedStringNetwork(CyNetwork network) {
		CyTable nodeTable = network.getDefaultNodeTable();
		if (nodeTable.getColumn(ID) == null) return false;
		if (nodeTable.getColumn(SPECIES) == null) return false;
		if (nodeTable.getColumn(CANONICAL) == null) return false;
		if (nodeTable.getColumn(SPECIES) == null) return false;
		CyTable edgeTable = network.getDefaultEdgeTable();
		if (edgeTable.getColumn(SCORE) == null) return false;
		return true;
	}

	public static boolean isStringNetwork(CyNetwork network) {
		// This is a string network only if we have a confidence score in the network table,
		// "@id", "species", "canonical name", and "sequence" columns in the node table, and 
		// a "score" column in the edge table
		if (network == null || network.getRow(network).get(CONFIDENCE, Double.class) == null) return false;
		return isMergedStringNetwork(network);
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
		row.set(STYLE, "string:"); // We may overwrite this, if we get an image

		String type = getType(id);
		row.set(TYPE, type);

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
			} else if (key.equals("smiles")) {
				row.set(CV_STYLE, "chemviz:"+nodeObj.get("smiles"));
				row.set(key, nodeObj.get("smiles"));
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

	public static boolean isCompound(CyNetwork net, CyNode node) {
		if (net == null || node == null)
			return false;

		String ns = net.getRow(node).get(ID, String.class);
		return getType(ns).equals("compound");
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

	public static String getName(CyNetwork network, CyIdentifiable ident) {
		return getString(network, ident, CyNetwork.NAME);
	}

	public static String getString(CyNetwork network, CyIdentifiable ident, String column) {
		if (network.getRow(ident) != null) 
			return network.getRow(ident).get(column, String.class);
		return null;
	}

}
