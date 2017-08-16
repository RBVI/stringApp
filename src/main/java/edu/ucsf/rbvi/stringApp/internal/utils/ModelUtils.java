package edu.ucsf.rbvi.stringApp.internal.utils;

import java.awt.Color;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.CyProperty.SavePolicy;
import org.cytoscape.property.SimpleCyProperty;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.view.model.View;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

public class ModelUtils {

	// Node information
	public static String CANONICAL = "canonical name";
	public static String CV_STYLE = "chemViz Passthrough";
	public static String ELABEL_STYLE = "enhancedLabel Passthrough";
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

	//public static Pattern cidmPattern = Pattern.compile("\\(CIDm\\)0*");
	public static Pattern cidmPattern = Pattern.compile("CIDm0*");
	// public static String DISEASEINFO =
	// "http://diseases.jensenlab.org/Entity?type1=9606&type2=-26";

	// Edge information
	public static String SCORE = "score";
	public static String INTERSPECIES = "interspecies";

	// Network information
	public static String CONFIDENCE = "confidence score";
	public static String DATABASE = "database";
	public static String NET_SPECIES = "species";

	// Session information
	public static String showStructureImagesFlag = "showStructureImages";
	public static String showEnhancedLabelsFlag = "showEnhancedLabels";

	// Create network view size threshold
	// See https://github.com/cytoscape/cytoscape-impl/blob/develop/core-task-impl/
	// src/main/java/org/cytoscape/task/internal/loadnetwork/AbstractLoadNetworkTask.java
	public static int DEF_VIEW_THRESHOLD = 3000;
	public static String VIEW_THRESHOLD = "viewThreshold";
	
	// Other stuff
	public static String COMPOUND = "STITCH compounds";
	public static String EMPTYLINE = "--------";
	
	
	public static List<EntityIdentifier> getEntityIdsFromJSON(StringManager manager,
			JSONObject object) {
		JSONArray tmResults = getResultsFromJSON(object, JSONArray.class);
		if (tmResults == null)
			return null;
		JSONArray entityArray = (JSONArray) tmResults.get(0);
		Boolean limited = (Boolean) tmResults.get(1);
		List<EntityIdentifier> results = new ArrayList<>();

		for (Object entityDict : entityArray) {
			JSONObject data = (JSONObject) entityDict;
			String matched = data.get("matched").toString();
			String primary = data.get("primary").toString();
			String id = data.get("id").toString();
			Long type = (Long) data.get("type");
			EntityIdentifier ei = new EntityIdentifier(matched, primary, type, id);
			results.add(ei);
		}
		return results;
	}

	public static List<TextMiningResult> getIdsFromJSON(StringManager manager, int taxon,
			JSONObject object, String query, boolean disease) {
		JSONArray tmResults = getResultsFromJSON(object, JSONArray.class);
		if (tmResults == null)
			return null;
		JSONObject nodeDict = (JSONObject) tmResults.get(0);
		Boolean limited = (Boolean) tmResults.get(1);

		List<TextMiningResult> results = new ArrayList<>();

		for (Object stringid : nodeDict.keySet()) {
			int fg = -1;
			int bg = -1;
			JSONObject data = (JSONObject) nodeDict.get(stringid);
			String name = data.get("name").toString();
			if (data.containsKey("foreground"))
				fg = ((Long) data.get("foreground")).intValue();
			if (data.containsKey("background"))
				bg = ((Long) data.get("background")).intValue();
			Double score = (Double) data.get("score");
			// String url = getDiseaseURL((String)stringid, query);
			TextMiningResult tm = new TextMiningResult(taxon + "." + (String) stringid, name, fg,
					bg, score, disease);
			results.add(tm);
		}
		return results;

	}

	public static void addTextMiningResults(StringManager manager, List<TextMiningResult> tmResults,
			CyNetwork network) {
		boolean haveFBValues = false;
		boolean haveDisease = false;

		// Create a map of our results
		Map<String, TextMiningResult> resultsMap = new HashMap<>();
		for (TextMiningResult tm : tmResults) {
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
		 * if (haveLinkout) { createColumnIfNeeded(network.getDefaultNodeTable(), String.class,
		 * TM_LINKOUT); }
		 */

		if (haveDisease)
			createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, DISEASE_SCORE);
		else
			createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, TM_SCORE);

		for (CyNode node : network.getNodeList()) {
			CyRow row = network.getRow(node);
			String id = row.get(STRINGID, String.class);
			if (resultsMap.containsKey(id)) {
				TextMiningResult result = resultsMap.get(id);
				if (result.getForeground() > 0)
					row.set(TM_FOREGROUND, result.getForeground());
				if (result.getBackground() > 0)
					row.set(TM_BACKGROUND, result.getBackground());
				/*
				 * if (result.getLinkout() != null) row.set(TM_LINKOUT, result.getLinkout());
				 */
				if (haveDisease)
					row.set(DISEASE_SCORE, result.getScore());
				else
					row.set(TM_SCORE, result.getScore());
			}
		}
	}

	public static List<CyNode> createTMNetworkFromJSON(StringManager manager, Species species,
			JSONObject object, String query, String useDATABASE) {
		JSONArray results = getResultsFromJSON(object, JSONArray.class);
		if (results == null)
			return null;

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(query);
		setDatabase(newNetwork, useDATABASE);
		setNetSpecies(newNetwork, species.getName());

		List<CyNode> nodes = getJSON(manager, species, newNetwork, results);
		return nodes;
	}

	public static List<CyNode> augmentNetworkFromJSON(StringManager manager, CyNetwork net,
			List<CyEdge> newEdges, JSONObject object, Map<String, String> queryTermMap,
			String useDATABASE) {
		JSONObject results = getResultsFromJSON(object, JSONObject.class);
		if (results == null)
			return null;

		Map<String, CyNode> nodeMap = new HashMap<>();
		Map<String, String> nodeNameMap = new HashMap<>();
		String species = ModelUtils.getNetSpecies(net);
		// TODO: Check if we really don't have to infer the database!
		// String useDATABASE = StringManager.STRINGDB;
		for (CyNode node : net.getNodeList()) {
			if (species == null)
				species = net.getRow(node).get(SPECIES, String.class);
			String stringId = net.getRow(node).get(STRINGID, String.class);
			String name = net.getRow(node).get(CyNetwork.NAME, String.class);
			nodeMap.put(stringId, node);
			nodeNameMap.put(stringId, name);
			// TODO: Change network from string to stitch once we add compounds?
			if (isCompound(net, node))
				useDATABASE = Databases.STITCH.getAPIName();
		}
		setDatabase(net, useDATABASE);
		
		List<CyNode> nodes = getJSON(manager, species, net, nodeMap, nodeNameMap, queryTermMap,
				newEdges, results, useDATABASE);
		return nodes;
	}

	public static CyNetwork createNetworkFromJSON(StringNetwork stringNetwork, String species,
			JSONObject object, Map<String, String> queryTermMap, String ids, String netName,
			String useDATABASE) {
		stringNetwork.getManager().ignoreAdd();
		CyNetwork network = createNetworkFromJSON(stringNetwork.getManager(), species, object,
				queryTermMap, ids, netName, useDATABASE);
		stringNetwork.getManager().addStringNetwork(stringNetwork, network);
		stringNetwork.getManager().listenToAdd();
		return network;
	}

	public static CyNetwork createNetworkFromJSON(StringManager manager, String species,
			JSONObject object, Map<String, String> queryTermMap, String ids, String netName,
			String useDATABASE) {
		JSONObject results = getResultsFromJSON(object, JSONObject.class);
		if (results == null)
			return null;

		// Get a network name
		String defaultName;
		if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			defaultName = "STITCH Network";
		else
			defaultName = "String Network";
		if (netName != null && netName != "") {
			netName = defaultName + " - " + netName;
		} else if (queryTermMap != null && queryTermMap.size() == 1 && queryTermMap.containsKey(ids)) {
			netName = defaultName + " - " + queryTermMap.get(ids);
		} else {
			netName = defaultName;
			// netName = manager.getNetworkName(ids);
		}

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(netName);
		setDatabase(newNetwork, useDATABASE);
		setNetSpecies(newNetwork, species);

		// Create a map to save the nodes
		Map<String, CyNode> nodeMap = new HashMap<>();

		// Create a map to save the node names
		Map<String, String> nodeNameMap = new HashMap<>();

		getJSON(manager, species, newNetwork, nodeMap, nodeNameMap, queryTermMap, null, results,
				useDATABASE);

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

	public static void setDatabase(CyNetwork network, String database) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, DATABASE);
		network.getRow(network).set(DATABASE, database);
	}

	public static String getDatabase(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(DATABASE) == null)
			return null;
		return network.getRow(network).get(DATABASE, String.class);
	}

	public static void setNetSpecies(CyNetwork network, String species) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, NET_SPECIES);
		network.getRow(network).set(NET_SPECIES, species);
	}

	public static String getNetSpecies(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(NET_SPECIES) == null)
			return null;
		return network.getRow(network).get(NET_SPECIES, String.class);
	}

	public static String getNodeSpecies(CyNetwork network, CyNode node) {
		if (network.getDefaultNodeTable().getColumn(SPECIES) == null)
			return null;
		return network.getRow(node).get(SPECIES, String.class);
	}

	public static String getMostCommonNetSpecies(CyNetwork net) {
		Map<String, Integer> species = new HashMap<String, Integer>();
		for (CyNode node : net.getNodeList()) {
			String nSpecies = net.getRow(node).get(SPECIES, String.class);
			if (nSpecies == null || nSpecies.equals(""))
				continue;
			if (!species.containsKey(nSpecies)) {
				species.put(nSpecies, new Integer(1));
			} else {
				int count = species.get(nSpecies).intValue() + 1;
				species.put(nSpecies, new Integer(count));
			}
		}
		String netSpecies = "";
		int maxCount = 0;
		for (Map.Entry<String, Integer> tempSp : species.entrySet()) {
			if (netSpecies == "" || tempSp.getValue() > maxCount) {
				netSpecies = tempSp.getKey();
				maxCount = tempSp.getValue();
			}
		}
		return netSpecies;
	}

	public static List<String> getAllNetSpecies(CyNetwork net) {
		List<String> species = new ArrayList<String>();
		for (CyNode node : net.getNodeList()) {
			String nSpecies = net.getRow(node).get(SPECIES, String.class);
			if (nSpecies != null && !nSpecies.equals("") && !species.contains(nSpecies))
				species.add(nSpecies);
		}
		return species;
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

		JSONObject nodeDict = (JSONObject) tmResults.get(0);
		Boolean limited = (Boolean) tmResults.get(1);

		List<CyNode> nodes = new ArrayList<>();

		for (Object stringid : nodeDict.keySet()) {
			JSONObject data = (JSONObject) nodeDict.get(stringid);
			String name = data.get("name").toString();
			int fg = ((Long) data.get("foreground")).intValue();
			int bg = ((Long) data.get("background")).intValue();
			Double score = (Double) data.get("score");
			CyNode newNode = network.addNode();
			CyRow row = network.getRow(newNode);
			row.set(ID, "stringdb:" + species.getTaxId() + "." + stringid.toString());
			row.set(CyNetwork.NAME, name);
			row.set(SPECIES, species.getName());
			row.set(STRINGID, species.getTaxId() + "." + stringid.toString());
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
			Map<String, String> queryTermMap, List<CyEdge> newEdges, JSONObject json,
			String useDATABASE) {
		
		List<CyNode> newNodes = new ArrayList<>();
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, CANONICAL);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, STRINGID);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, DESCRIPTION);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ID);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, NAMESPACE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, TYPE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, QUERYTERM);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, SEQUENCE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, SPECIES);
		if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
			createColumnIfNeeded(network.getDefaultNodeTable(), String.class, SMILES);
			createColumnIfNeeded(network.getDefaultNodeTable(), String.class, CV_STYLE);
		}
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, STYLE);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ELABEL_STYLE);

		createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, SCORE);
		createColumnIfNeeded(network.getDefaultEdgeTable(), Boolean.class, INTERSPECIES);

		Set<String> columnMap = new HashSet<>();

		// Get the nodes
		JSONArray nodes = (JSONArray) json.get("nodes");
		if (nodes != null && nodes.size() > 0) {
			createColumnsFromJSON(nodes, network.getDefaultNodeTable());
			for (Object nodeObj : nodes) {
				if (nodeObj instanceof JSONObject) {
					JSONObject nodeJSON = (JSONObject) nodeObj;
					CyNode newNode = createNode(manager, network, nodeJSON, species, nodeMap, nodeNameMap,
							queryTermMap, columnMap);
					if (newNode != null)
						newNodes.add(newNode);
				}
			}
		}

		// Get the edges
		JSONArray edges = (JSONArray) json.get("edges");
		if (edges != null && edges.size() > 0) {
			for (Object edgeObj : edges) {
				if (edgeObj instanceof JSONObject)
					createEdge(network, (JSONObject) edgeObj, nodeMap, nodeNameMap, newEdges,
							useDATABASE);
			}
		}
		return newNodes;
	}

	public static void createColumnsFromJSON(JSONArray nodes, CyTable table) {
		Map<String, Class<?>> jsonKeysClass = new HashMap<String, Class<?>>();
		Set<String> listKeys = new HashSet<>();
		for (Object nodeObj : nodes) {
			if (nodeObj instanceof JSONObject) {
				JSONObject nodeJSON = (JSONObject) nodeObj;
				for (Object objKey : nodeJSON.keySet()) {
					String key = (String) objKey;
					if (jsonKeysClass.containsKey(key)) {
						continue;
					}
					Object value = nodeJSON.get(key);
					if (value instanceof JSONArray) {
						JSONArray list = (JSONArray) value;
						Object element = list.get(0);
						jsonKeysClass.put(key, element.getClass());
						listKeys.add(key);
					} else {
						jsonKeysClass.put(key, value.getClass());
					}
				}
			}
		}
		List<String> jsonKeysSorted = new ArrayList<String>(jsonKeysClass.keySet());
		Collections.sort(jsonKeysSorted);
		for (String jsonKey : jsonKeysSorted) {
			if (listKeys.contains(jsonKey)) {
				createListColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			} else {
				createColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			}
		}

	}
	
	public static boolean isMergedStringNetwork(CyNetwork network) {
		CyTable nodeTable = network.getDefaultNodeTable();
		if (nodeTable.getColumn(ID) == null)
			return false;
		if (nodeTable.getColumn(SPECIES) == null)
			return false;
		if (nodeTable.getColumn(CANONICAL) == null)
			return false;
		if (nodeTable.getColumn(SPECIES) == null)
			return false;
		CyTable edgeTable = network.getDefaultEdgeTable();
		if (edgeTable.getColumn(SCORE) == null)
			return false;
		return true;
	}

	public static boolean isStringNetwork(CyNetwork network) {
		// This is a string network only if we have a confidence score in the network table,
		// "@id", "species", "canonical name", and "sequence" columns in the node table, and
		// a "score" column in the edge table
		if (network == null || network.getRow(network).get(CONFIDENCE, Double.class) == null)
			return false;
		return isMergedStringNetwork(network);
	}

	public static String getExisting(CyNetwork network) {
		StringBuilder str = new StringBuilder();
		for (CyNode node : network.getNodeList()) {
			String stringID = network.getRow(node).get(STRINGID, String.class);
			if (stringID != null && stringID.length() > 0)
				str.append(stringID + "\n");
		}
		return str.toString();
	}

	public static String getSelected(CyNetwork network, View<CyNode> nodeView) {
		StringBuilder selectedStr = new StringBuilder();
		if (nodeView != null) {
			String stringID = network.getRow(nodeView.getModel()).get(STRINGID, String.class);
			selectedStr.append(stringID + "\n");
		}

		for (CyNode node : network.getNodeList()) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				String stringID = network.getRow(node).get(STRINGID, String.class);
				if (stringID != null && stringID.length() > 0)
					selectedStr.append(stringID + "\n");
			}
		}
		return selectedStr.toString();
	}

	private static CyNode createNode(StringManager manager, CyNetwork network, JSONObject nodeObj, 
	                                 String species,
	                                 Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap,
	                                 Map<String, String> queryTermMap, Set<String> columnMap) {

		String name = (String) nodeObj.get("name");
		String id = (String) nodeObj.get("@id");
		String namespace = id.substring(0, id.indexOf(":"));
		String stringId = id.substring(id.indexOf(":") + 1);

		if (nodeMap.containsKey(stringId))
			return null;
		// System.out.println("Node id = "+id+", stringID = "+stringId+", namespace="+namespace);
		CyNode newNode = network.addNode();
		CyRow row = network.getRow(newNode);

		row.set(CyNetwork.NAME, name);
		row.set(CyRootNetwork.SHARED_NAME, stringId);
		row.set(STRINGID, stringId);
		row.set(ID, id);
		row.set(NAMESPACE, namespace);
		row.set(STYLE, "string:"); // We may overwrite this, if we get an image

		String type = getType(id);
		row.set(TYPE, type);

		// TODO: Check if this is ok for handling multiple species as well as stitch molecules
		if (type.equals("protein") && stringId.contains(".")) {				
			String taxID = stringId.substring(0,stringId.indexOf("."));
			String nodeSpecies = Species.getSpeciesName(taxID);
			if (!"".equals(nodeSpecies) && !species.equals(nodeSpecies)) {
				species = nodeSpecies;
			}			
		}
		// TODO: Should compounds have a species? 
		if (species != null && !type.equals("compound")) {
			row.set(SPECIES, species);
		}
		
		for (Object objKey : nodeObj.keySet()) {
			String key = (String) objKey;
			// Look for our "special" columns
			if (key.equals("description")) {
				row.set(DESCRIPTION, (String) nodeObj.get("description"));
			} else if (key.equals("canonical")) {
				row.set(CANONICAL, (String) nodeObj.get("canonical"));
			} else if (key.equals("sequence")) {
				network.getRow(newNode).set(SEQUENCE, (String) nodeObj.get("sequence"));
			} else if (key.equals("image")) {
				row.set(STYLE, "string:" + nodeObj.get("image"));
			} else if (key.equals("smiles")) {
				if (manager.haveChemViz() || nodeObj.get("image").equals("image:"))
					row.set(CV_STYLE, "chemviz:" + nodeObj.get("smiles"));
				row.set(key, nodeObj.get("smiles"));
			} else {
				// It's not one of our "standard" attributes, create a column for it (if necessary)
				// and then add it
				Object value = nodeObj.get(key);
				row.set(key, value);
				// if (value instanceof JSONArray) {
				// JSONArray list = (JSONArray) value;
				// if (!columnMap.contains(key)) {
				// Object element = list.get(0);
				// createListColumnIfNeeded(network.getDefaultNodeTable(), element.getClass(),
				// key);
				// columnMap.add(key);
				// }
				// row.set(key, list);
				// } else {
				// if (!columnMap.contains(key)) {
				// createColumnIfNeeded(network.getDefaultNodeTable(), value.getClass(), key);
				// columnMap.add(key);
				// }
				// row.set(key, value);
				// }
			}
			{
				// Construct instructions for enhanced graphics label
				String enhancedLabel = "label: attribute=name labelsize=12 ";
				if (type.equals("protein"))
					enhancedLabel += "labelAlignment=left ";
				else
					enhancedLabel += "labelAlignment=center ";
				enhancedLabel += "outline=true outlineColor=white outlineTransparency=95 outlineWidth=10 ";
				enhancedLabel += "background=false color=black dropShadow=false";
				row.set(ELABEL_STYLE, enhancedLabel);
			}
		}
		// TODO: Fix hack for saving query term for compounds
		if (queryTermMap != null) {
			if (queryTermMap.containsKey(stringId)) {
				network.getRow(newNode).set(QUERYTERM, queryTermMap.get(stringId));
			} else if (queryTermMap.containsKey("-1.CID1" + stringId.substring(4))) {
				network.getRow(newNode).set(QUERYTERM,
						queryTermMap.get("-1.CID1" + stringId.substring(4)));
			} else if (queryTermMap.containsKey("-1.CID0" + stringId.substring(4))) {
				network.getRow(newNode).set(QUERYTERM,
						queryTermMap.get("-1.CID0" + stringId.substring(4)));
			}
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

	private static void createEdge(CyNetwork network, JSONObject edgeObj,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap, List<CyEdge> newEdges,
			String useDATABASE) {
		String source = (String) edgeObj.get("source");
		String target = (String) edgeObj.get("target");
		CyNode sourceNode = nodeMap.get(source);
		CyNode targetNode = nodeMap.get(target);

		CyEdge edge;
		String interaction = "pp";

		// Don't create an edge if we already have one between these nodes
		if (!network.containsEdge(sourceNode, targetNode)) {
			if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
				boolean sourceType = isCompound(network, sourceNode);
				boolean targetType = isCompound(network, targetNode);
				if (sourceType == false && targetType == false)
					interaction = "pp";
				else if (sourceType == true && targetType == true)
					interaction = "cc";
				else
					interaction = "pc";
			}

			edge = network.addEdge(sourceNode, targetNode, false);
			network.getRow(edge).set(CyNetwork.NAME,
					nodeNameMap.get(source) + " (" + interaction + ") " + nodeNameMap.get(target));
			network.getRow(edge).set(CyEdge.INTERACTION, interaction);

			String sourceSpecies = getNodeSpecies(network, sourceNode);
			String targetSpecies = getNodeSpecies(network, targetNode);
			if (sourceSpecies != null && targetSpecies != null && !sourceSpecies.equals(targetSpecies)) 
				network.getRow(edge).set(INTERSPECIES, Boolean.TRUE);
			
			if (newEdges != null)
				newEdges.add(edge);
		} else {
			List<CyEdge> edges = network.getConnectingEdgeList(sourceNode, targetNode,
					CyEdge.Type.ANY);
			if (edges == null)
				return; // Shouldn't happen!
			edge = edges.get(0);
		}

		// Update the score information
		JSONObject scores = (JSONObject) edgeObj.get("scores");
		double scoreProduct = 1.0;
		for (Object key : scores.keySet()) {
			String score = (String) key;
			createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, score);
			Double v = (Double) scores.get(key);
			network.getRow(edge).set(score, v);
			scoreProduct *= (1 - v);
		}
		double totalScore = -(scoreProduct - 1.0);
		network.getRow(edge).set(SCORE, totalScore);
	}

	public static void createColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null)
			return;

		table.createColumn(columnName, clazz, false);
	}

	public static void createListColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null)
			return;

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

	public static List<String> getNetworkSpeciesTaxons(CyNetwork network) {
		List<String> netSpeciesNames = new ArrayList<String>();
		for (CyNode node : network.getNodeList()) {
			final String species = network.getRow(node).get(SPECIES, String.class);
			if (species != null && !netSpeciesNames.contains(species)) {
				netSpeciesNames.add(species);
			}
		}
		List<String> netSpeciesTaxons = new ArrayList<String>();
		for (Species sp : Species.getSpecies()) {
			for (String netSp : netSpeciesNames) {
				if (netSp.equals(sp.getName())) {
					netSpeciesTaxons.add(String.valueOf(sp.getTaxId()));
				}
			}
		}
		return netSpeciesTaxons;
	}

	public static List<String> getAvailableInteractionPartners(CyNetwork network) {
		List<String> availableTypes = new ArrayList<String>();
		List<String> species = ModelUtils.getAllNetSpecies(network);
		Collections.sort(species);
		String netSp = getNetSpecies(network);
		if (!species.contains(netSp)) {
			availableTypes.add(netSp);
		}
		availableTypes.addAll(species);
		availableTypes.add(COMPOUND);
		List<String> spPartners = new ArrayList<String>();
		for (String sp : species) {
			List<String> partners = Species.getSpeciesPartners(sp);
			for (String spPartner : partners) {
				if (!species.contains(spPartner)) 
					spPartners.add(spPartner);
			}
		}
		Collections.sort(spPartners);
		if (spPartners.size() > 0) {
			availableTypes.add(EMPTYLINE);
			availableTypes.addAll(spPartners);
		}
		return availableTypes;
	}	

	
	public static <T> T getResultsFromJSON(JSONObject json, Class<? extends T> clazz) {
		if (json == null || !json.containsKey(StringManager.RESULT))
			return null;

		Object result = json.get(StringManager.RESULT);
		if (!clazz.isAssignableFrom(result.getClass()))
			return null;

		return (T) result;
	}

	public static Integer getVersionFromJSON(JSONObject json) {
		if (json == null || !json.containsKey(StringManager.APIVERSION))
			return null;
		return (Integer) json.get(StringManager.APIVERSION);
	}

	public static Set<CyTable> getEnrichmentTables(StringManager manager, CyNetwork network) {
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		Set<CyTable> netTables = new HashSet<CyTable>();
		Set<String> tableNames = new HashSet<String>(Arrays.asList(EnrichmentTerm.termTables));
		Set<CyTable> currTables = tableManager.getAllTables(true);
		for (CyTable current : currTables) {
			if (tableNames.contains(current.getTitle())
					&& current.getColumn(EnrichmentTerm.colNetworkSUID) != null
					&& current.getAllRows().size() > 0) {
				CyRow tempRow = current.getAllRows().get(0);
				if (tempRow.get(EnrichmentTerm.colNetworkSUID, Long.class)
						.equals(network.getSUID())) {
					netTables.add(current);
				}
			}
		}
		return netTables;
	}

	public static List<EnrichmentTerm> parseXMLDOM(Object results, double cutoff, CyNetwork network,
			Map<String, Long> stringNodesMap, StringManager manager) {
		if (!(results instanceof Document)) {
			return null;
		}
		List<EnrichmentTerm> enrichmentTerms = new ArrayList<EnrichmentTerm>();
		try {
			Element root = ((Document) results).getDocumentElement();
			root.normalize();
			NodeList nList = ((Document) results).getElementsByTagName("status");
			for (int i = 0; i < nList.getLength(); i++) {
				final Node nNode = nList.item(i);
				if (nNode instanceof Element) {
					if (((Element) nNode).getElementsByTagName("code").getLength() > 0) {
						String status = ((Element) nNode).getElementsByTagName("code").item(0)
								.getTextContent();
						if (!status.equals("ok")) {
							String message = "";
							if (((Element) nNode).getElementsByTagName("message").getLength() > 0) {
								message = ((Element) nNode).getElementsByTagName("message").item(0)
										.getTextContent();
							}
							System.out.println("Error from ernichment server: " + message);
							manager.error("Error from ernichment server: " + message);
							return null;
						}
					}
					if (((Element) nNode).getElementsByTagName("warning").getLength() > 0) {
						String warning = ((Element) nNode).getElementsByTagName("warning").item(0)
								.getTextContent();
						System.out.println("Warning from enrichment server: " + warning);
						manager.info("Warning from enrichment server: " + warning);
					}
				}
			}
			nList = ((Document) results).getElementsByTagName("term");
			for (int i = 0; i < nList.getLength(); i++) {
				final Node nNode = nList.item(i);
				// <term>
				// <name>GO:0008585</name>
				// <description>female gonad development</description>
				// <numberOfGenes>1</numberOfGenes>
				// <pvalue>1E0</pvalue>
				// <bonferroni>1E0</bonferroni>
				// <fdr>1E0</fdr>
				// <genes><gene>9606.ENSP00000269260</gene></genes>
				// </term>
				if (nNode instanceof Element) {
					Element eElement = (Element) nNode;
					double pvalue = -1;
					if (eElement.getElementsByTagName("pvalue").getLength() > 0) {
						pvalue = Double.valueOf(
								eElement.getElementsByTagName("pvalue").item(0).getTextContent())
								.doubleValue();
					}
					double bonf = -1;
					if (eElement.getElementsByTagName("bonferroni").getLength() > 0) {
						bonf = Double.valueOf(eElement.getElementsByTagName("bonferroni").item(0)
								.getTextContent()).doubleValue();
					}
					double fdr = -1;
					if (eElement.getElementsByTagName("fdr").getLength() > 0) {
						fdr = Double.valueOf(
								eElement.getElementsByTagName("fdr").item(0).getTextContent())
								.doubleValue();
					}
					String name = "";
					if (eElement.getElementsByTagName("name").getLength() > 0) {
						name = eElement.getElementsByTagName("name").item(0).getTextContent();

					}
					NodeList genesList = eElement.getElementsByTagName("gene");
					List<String> enrGenes = new ArrayList<String>();
					List<Long> enrNodes = new ArrayList<Long>();
					for (int j = 0; j < genesList.getLength(); j++) {
						final Node geneNode = genesList.item(j);
						if (geneNode instanceof Element) {
							String enrGeneEnsemblID = ((Element) geneNode).getTextContent();
							if (enrGeneEnsemblID != null) {
								String enrGeneNodeName = enrGeneEnsemblID;
								if (stringNodesMap.containsKey(enrGeneEnsemblID)) {
									final Long nodeSUID = stringNodesMap.get(enrGeneEnsemblID);
									enrNodes.add(nodeSUID);
									if (network.getDefaultNodeTable()
											.getColumn(CyNetwork.NAME) != null) {
										enrGeneNodeName = network.getDefaultNodeTable().getRow(nodeSUID)
												.get(CyNetwork.NAME, String.class);
									}
								}
								enrGenes.add(enrGeneNodeName);
							}
						}
					}
					String descr = "";
					if (eElement.getElementsByTagName("description").getLength() > 0) {
						descr = eElement.getElementsByTagName("description").item(0)
								.getTextContent();
					}
					// else {
					// System.out.println("Term without description: " + name);
					// System.out.println(enrGenes);
					// }
					if (!name.equals("") && fdr > -1 && fdr <= cutoff) {
						EnrichmentTerm enrTerm = new EnrichmentTerm(name, descr, pvalue, bonf, fdr);
						enrTerm.setGenes(enrGenes);
						enrTerm.setNodesSUID(enrNodes);
						enrichmentTerms.add(enrTerm);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		// monitor.setStatusMessage("Number of terms: " + enrichmentTerms.size());
		return enrichmentTerms;
	}


	public static void setStringProperty(StringManager manager,
			String propertyKey, Object propertyValue, SavePolicy savePolicy) {
		CyProperty<Properties> sessionProperties = getPropertyService(manager, savePolicy);
		Properties p = sessionProperties.getProperties();
		p.setProperty(propertyKey, propertyValue.toString());
	}

	public static String getStringProperty(StringManager manager,
			String propertyKey, SavePolicy savePolicy) {
		CyProperty<Properties> sessionProperties = getPropertyService(manager,
				savePolicy);
		Properties p = sessionProperties.getProperties();
		if (p.getProperty(propertyKey) != null) 
			return p.getProperty(propertyKey);
		return null;
	}

	private static CyProperty<Properties> getPropertyService(StringManager manager,
			SavePolicy policy) {
			String name = "stringApp";
			// Do we already have a session with our properties
			CySessionManager sessionManager = manager.getService(CySessionManager.class);
			CySession session = sessionManager.getCurrentSession();
			if (session != null) {
				Set<CyProperty<?>> sessionProperties = session.getProperties();
				for (CyProperty<?> cyProp : sessionProperties) {
					if (cyProp.getName() != null && cyProp.getName().equals(name)) {
						return (CyProperty<Properties>) cyProp;
					}
				}
			}
			// Either we have a null session or our properties aren't in this session
			Properties props = new Properties();
			CyProperty<Properties> service = new SimpleCyProperty(name, props, Properties.class,
					SavePolicy.SESSION_FILE);
			Properties serviceProps = new Properties();
			serviceProps.setProperty("cyPropertyName", service.getName());
			manager.registerAllServices(service, serviceProps);
			return service;
	}

	public static class ConfigPropsReader extends AbstractConfigDirPropsReader {
		ConfigPropsReader(SavePolicy policy, String name) {
			super(name, "stringApp.props", policy);
		}
	}

	public static int getViewThreshold(StringManager manager) {
		final Properties props = (Properties) manager
				.getService(CyProperty.class, "(cyPropertyName=cytoscape3.props)").getProperties();
		final String vts = props.getProperty(VIEW_THRESHOLD);
		int threshold;

		try {
			threshold = Integer.parseInt(vts);
		} catch (Exception e) {
			threshold = DEF_VIEW_THRESHOLD;
		}

		return threshold;
	}

	// Method to convert terms entered in search text to
	// appropriate newline-separated string to send to server
	public static String convertTerms(String terms, boolean splitComma, boolean splitSpaces) {
		String regexSp = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		String regexComma = "[,]+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		if (splitSpaces) {
			// Substitute newlines for space
			terms = terms.replaceAll(regexSp, "\n");
		}

		if (splitComma) {
			// Substitute newlines for commas
			terms = terms.replaceAll(regexComma, "\n");
		}

		// Strip off any blank lines
		terms = terms.replaceAll("(?m)^\\s*", "");
		return terms;
	}

}
