package edu.ucsf.rbvi.stringApp.internal.utils;

import java.awt.Color;
import java.awt.color.ICC_ColorSpace;
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
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

public class ModelUtils {

	// Node information
	public static String CANONICAL = "canonical name";
	public static String DISPLAY = "display name";
	public static String FULLNAME = "full name";
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
	
	public static int MAX_SHORT_NAME_LENGTH = 15; // 15 characters, or 14 characters plus the dot
	public static int SECOND_SEGMENT_LENGTH = 3;
	public static int FIRST_SEGMENT_LENGTH = MAX_SHORT_NAME_LENGTH - SECOND_SEGMENT_LENGTH - 2;
	
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
	public static String NET_ANALYZED_NODES = "analyzedNodes.SUID";
	public static String NET_PPI_ENRICHMENT = "ppiEnrichment";
	public static String NET_ENRICHMENT_NODES = "enrichmentNodes";
	public static String NET_ENRICHMENT_EXPECTED_EDGES = "enrichmentExpectedEdges";
	public static String NET_ENRICHMENT_EDGES = "enrichmentEdges";
	public static String NET_ENRICHMENT_CLSTR = "enrichmentClusteringCoeff";
	public static String NET_ENRICHMENT_DEGREE = "enrichmentAvgDegree";
	public static String NET_ENRICHMENT_SETTINGS = "enrichmentSettings";

	public static String NET_ENRICHMENT_VISTEMRS = "visualizedTerms";
	public static String NET_ENRICHMENT_VISCOLORS = "visualizedTermsColors";
	
	// Session information
	public static String showStructureImagesFlag = "showStructureImages";
	public static String showEnhancedLabelsFlag = "showEnhancedLabels";
	public static String showGlassBallEffectFlag = "showGlassBallEffect";

	// Create network view size threshold
	// See https://github.com/cytoscape/cytoscape-impl/blob/develop/core-task-impl/
	// src/main/java/org/cytoscape/task/internal/loadnetwork/AbstractLoadNetworkTask.java
	public static int DEF_VIEW_THRESHOLD = 3000;
	public static String VIEW_THRESHOLD = "viewThreshold";
	
	// Other stuff
	public static String COMPOUND = "STITCH compounds";
	public static String EMPTYLINE = "--------";

	public static boolean ifString(CyNetwork network) {
		CyRow netRow = network.getRow(network);
		if (netRow.isSet(CONFIDENCE) && netRow.isSet(NET_SPECIES))
			return true;
		return false;
	}
	
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

	public static List<EnrichmentTerm> getEnrichmentFromJSON(StringManager manager,
			JSONObject object, double enrichmentCutoff, Map<String, Long> stringNodesMap,
			CyNetwork network) {
		JSONArray enrichmentArray = getResultsFromJSON(object, JSONArray.class);
		if (enrichmentArray == null)
			return null;

		List<EnrichmentTerm> results = new ArrayList<>();
		// {"p_value":0.01,"number_of_genes":"3","description":"single organism signaling","ncbiTaxonId":"9606",
		// "term":"GO:0044700","inputGenes":"SMO,CDK2,TP53","fdr":0.877,"bonferroni":1,"category":"Process",
		// "preferredNames":"SMO,CDK2,TP53"}
		for (Object enrObject : enrichmentArray) {
			JSONObject enr = (JSONObject) enrObject;
			EnrichmentTerm currTerm = new EnrichmentTerm();
			if (enr.containsKey("term"))
				currTerm.setName((String) enr.get("term"));
			if (enr.containsKey("category"))
				if (TermCategory.containsKey((String) enr.get("category"))) {
					currTerm.setCategory(TermCategory.getName((String) enr.get("category")));
				} else {
					currTerm.setCategory((String) enr.get("category"));
				}
			if (enr.containsKey("description"))
				currTerm.setDescription((String) enr.get("description"));
			if (enr.containsKey("pvalue"))
				currTerm.setPValue(((Number) enr.get("pvalue")).doubleValue());
			if (enr.containsKey("bonferroni"))
				currTerm.setBonfPValue(((Number) enr.get("bonferroni")).doubleValue());
			if (enr.containsKey("fdr"))
				currTerm.setFDRPValue(((Number) enr.get("fdr")).doubleValue());
			if (enr.containsKey("inputGenes")) {
				List<String> currGeneList = new ArrayList<String>();
				List<Long> currNodeList = new ArrayList<Long>();
				JSONArray genes = (JSONArray)enr.get("inputGenes");
				for (int i = 0; i < genes.size(); i++) {
					String enrGeneEnsemblID = (String)genes.get(i);
					String enrGeneNodeName = enrGeneEnsemblID;
					if (stringNodesMap.containsKey(enrGeneEnsemblID)) {
						final Long nodeSUID = stringNodesMap.get(enrGeneEnsemblID);
						currNodeList.add(nodeSUID);
						if (network.getDefaultNodeTable().getColumn(DISPLAY) != null) {
							enrGeneNodeName = network.getDefaultNodeTable().getRow(nodeSUID)
									.get(DISPLAY, String.class);
						} else if (network.getDefaultNodeTable().getColumn(CyNetwork.NAME) != null) {
							enrGeneNodeName = network.getDefaultNodeTable().getRow(nodeSUID)
									.get(CyNetwork.NAME, String.class);
						}
					}
					currGeneList.add(enrGeneNodeName);
				}
				currTerm.setGenes(currGeneList);
				currTerm.setNodesSUID(currNodeList);
			}
			if (enr.containsKey("error")) {
				System.out.println("error");
				return null;
			}
			if (enr.containsKey("Error")) {
				System.out.println("An error occured while retrieving ppi enrichment.");
			}
			if (enr.containsKey("ErrorMessage")) {
				System.out.println(enr.get("ErrorMessage"));
				return null;
			}
			// save only if above cutoff
			if (currTerm.getFDRPValue() <= enrichmentCutoff)
				results.add(currTerm);
		}
		return results;
	}

	public static Map<String, String> getEnrichmentPPIFromJSON(StringManager manager, 
	                                                           JSONObject object,
	                                                           double enrichmentCutoff, 
	                                                           Map<String, Long> stringNodesMap, 
	                                                           CyNetwork network) {
		Map<String, String> values = new HashMap<>();
		JSONArray ppienrichmentArray = getResultsFromJSON(object, JSONArray.class);
		if (ppienrichmentArray == null)
			return null;

		// {"p_value":"1.03e-10","average_node_degree":13,"expected_number_of_edges":43,"number_of_edges":91,
		// "local_clustering_coefficient":1,"number_of_nodes":14}
		for (Object enrObject : ppienrichmentArray) {
			JSONObject enr = (JSONObject) enrObject;

			if (enr.containsKey("p_value")) {
				if (((String)enr.get("p_value")).equals("0"))
					values.put(NET_PPI_ENRICHMENT, new Double(1e-16).toString());
				else
					values.put(NET_PPI_ENRICHMENT, (String)enr.get("p_value"));
			}
			if (enr.containsKey("expected_number_of_edges")) {
				values.put(NET_ENRICHMENT_EXPECTED_EDGES, enr.get("expected_number_of_edges").toString());
			}
			if (enr.containsKey("number_of_edges")) {
				values.put(NET_ENRICHMENT_EDGES, enr.get("number_of_edges").toString());
			}
			if (enr.containsKey("average_node_degree")) {
				values.put(NET_ENRICHMENT_DEGREE, enr.get("average_node_degree").toString());
			}
			if (enr.containsKey("local_clustering_coefficient")) {
				values.put(NET_ENRICHMENT_CLSTR, enr.get("local_clustering_coefficient").toString());
			}
			if (enr.containsKey("number_of_nodes")) {
				values.put(NET_ENRICHMENT_NODES, enr.get("number_of_nodes").toString());
			}
			if (enr.containsKey("Error")) {
				System.out.println("An error occured while retrieving ppi enrichment.");
			}
			if (enr.containsKey("ErrorMessage")) {
				values.put("ErrorMessage", (String) enr.get("ErrorMessage"));
				return values;
			}
		}
		return values;
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
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, DISPLAY);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, FULLNAME);
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
			row.set(CyNetwork.NAME, species.getTaxId() + "." + stringid.toString());
			row.set(DISPLAY, name);
			row.set(SPECIES, species.getName());
			row.set(STRINGID, species.getTaxId() + "." + stringid.toString());
			row.set(STYLE, "string:");
			row.set(TM_FOREGROUND, fg);
			row.set(TM_BACKGROUND, bg);
			row.set(TM_SCORE, score);
			nodes.add(newNode);
		}
		shortenCompoundNames(network, nodes);
		return nodes;
	}

	private static List<CyNode> getJSON(StringManager manager, String species, CyNetwork network,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap,
			Map<String, String> queryTermMap, List<CyEdge> newEdges, JSONObject json,
			String useDATABASE) {
		
		List<CyNode> newNodes = new ArrayList<>();
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, CANONICAL);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, DISPLAY);
		createColumnIfNeeded(network.getDefaultNodeTable(), String.class, FULLNAME);
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
		shortenCompoundNames(network, newNodes);
		
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

		row.set(CyNetwork.NAME, stringId);
		// row.set(CyRootNetwork.SHARED_NAME, stringId);
		row.set(DISPLAY, name);
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
			if (key.equals("name")) {
				continue;
			} else if (key.equals("description")) {
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
				String enhancedLabel = "label: attribute=\"display name\" labelsize=12 ";
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

	public static void replaceColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null) 
			table.deleteColumn(columnName);
		
		table.createColumn(columnName, clazz, false);
	}

	public static void createListColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null)
			return;

		table.createListColumn(columnName, clazz, false);
	}

	public static void replaceListColumnIfNeeded(CyTable table, Class<?> clazz, String columnName) {
		if (table.getColumn(columnName) != null)
			table.deleteColumn(columnName);

		table.createListColumn(columnName, clazz, false);
	}

	public static String getName(CyNetwork network, CyIdentifiable ident) {
		return getString(network, ident, CyNetwork.NAME);
	}

	public static String getDisplayName(CyNetwork network, CyIdentifiable ident) {
		return getString(network, ident, DISPLAY);
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
		if (netSp != null && !species.contains(netSp)) {
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

	public static List<CyNode> getEnrichmentNodes(CyNetwork net) {
		List<CyNode> analyzedNodes = new ArrayList<CyNode>();
		if (net != null) {
			CyTable netTable = net.getDefaultNetworkTable();
			if (netTable.getColumn(ModelUtils.NET_ANALYZED_NODES) != null) {
				List<Long> nodesSUID = (List<Long>) netTable.getRow(net.getSUID())
						.get(ModelUtils.NET_ANALYZED_NODES, List.class);
				for (CyNode netNode : net.getNodeList()) {
					if (nodesSUID.contains(netNode.getSUID())) {
						analyzedNodes.add(netNode);
					}
				}
			}
		}
		return analyzedNodes;
	}
	
	public static Double getPPIEnrichment(CyNetwork net) {
		if (net != null) {
			CyTable netTable = net.getDefaultNetworkTable();
			if (netTable.getColumn(ModelUtils.NET_PPI_ENRICHMENT) != null) {
				return (Double) netTable.getRow(net.getSUID()).get(ModelUtils.NET_PPI_ENRICHMENT,
						Double.class);
			}
		}
		return null;
	}
	
	public static List<String> getVisualizedEnrichmentTerms(CyNetwork net) {
		if (net != null) {
			CyTable netTable = net.getDefaultNetworkTable();
			if (netTable.getColumn(ModelUtils.NET_ENRICHMENT_VISTEMRS) != null) {
				return netTable.getRow(net.getSUID()).getList(ModelUtils.NET_ENRICHMENT_VISTEMRS,
						String.class);
			}
		}
		return null;
		
	}
	
	public static void shortenCompoundNames(CyNetwork network, List<CyNode> nodes) {
		HashMap<String, List<CyNode>> shortNames = new HashMap<String, List<CyNode>>();
		for (CyNode node : nodes) {
			// get a dictionary of short names to nodes
			if (isCompound(network, node)) {
				String name = getDisplayName(network, node);
				if (name == null || name.length() <= MAX_SHORT_NAME_LENGTH) {
					continue;
				}
				String shortName = name.substring(0, MAX_SHORT_NAME_LENGTH);
				List<CyNode> nameNodes = new ArrayList<CyNode>();
				if (shortNames.containsKey(shortName)) {
					nameNodes = shortNames.get(shortName);
				}
				nameNodes.add(node);
				shortNames.put(shortName, nameNodes);
			}
		}
		// System.out.println(shortNames);
		for (String nameKey : shortNames.keySet()) {
			List<CyNode> nodesWithName = shortNames.get(nameKey);
			// if only one node with this short name, just use it
			if (nodesWithName.size() == 1) {
				String shortName = nameKey.substring(0,  MAX_SHORT_NAME_LENGTH - 1) + ".";
				CyRow row = network.getRow(nodesWithName.get(0));
				row.set(FULLNAME, row.get(DISPLAY, String.class));
				row.set(DISPLAY, shortName);
			} else {
			// else try to find another combination 
			// TODO: work in progress
				HashMap<String, CyNode> fullNames = new HashMap<String, CyNode>();
				for (CyNode node : nodesWithName) {
					fullNames.put(getDisplayName(network, node), node);
				}
				int i = MAX_SHORT_NAME_LENGTH - 1; // 14
				HashMap<String, CyNode> letters = new HashMap<String, CyNode>();
				boolean found = false;
				for (String fullName : fullNames.keySet()) {
					if (i + SECOND_SEGMENT_LENGTH < fullName.length()) {
						String letter = fullName.substring(i, i + SECOND_SEGMENT_LENGTH);
						if (letters.containsKey(letter)) {
							letters.put(letter, null);
						} else {
							letters.put(letter, fullNames.get(fullName));
						}
					} else {
						// # We have run out of letters for this name. Remove this name, and start
						// collecting letters again.
						// # Heuristic: Hopefully, there is only one name that is non-unique until
						// its end
						String shortName = fullName.substring(0, MAX_SHORT_NAME_LENGTH);
						if (fullName.length() > MAX_SHORT_NAME_LENGTH) {
							shortName = shortName.substring(0, shortName.length() - 2) + ".";
						}
						// store_short_name(short_names, cid, short_name)
						CyRow row = network.getRow(fullNames.get(fullName));
						row.set(FULLNAME, row.get(DISPLAY, String.class));
						row.set(DISPLAY, shortName);
						// del full_names[cid]
						found = true;
						break;
					}
				}
				
			}
		}
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
		Set<String> tableNames = new HashSet<String>(TermCategory.getTables());
		Set<CyTable> currTables = tableManager.getAllTables(true);
		for (CyTable current : currTables) {
			if (tableNames.contains(current.getTitle())
					&& current.getColumn(EnrichmentTerm.colNetworkSUID) != null
					&& current.getAllRows().size() > 0) {
				CyRow tempRow = current.getAllRows().get(0);
				if (tempRow.get(EnrichmentTerm.colNetworkSUID, Long.class) != null && tempRow
						.get(EnrichmentTerm.colNetworkSUID, Long.class).equals(network.getSUID())) {
					netTables.add(current);
				}
			}
		}
		return netTables;
	}

	public static CyTable getEnrichmentTable(StringManager manager, CyNetwork network, String name) {
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		Set<CyTable> currTables = tableManager.getAllTables(true);
		for (CyTable current : currTables) {
			if (name.equals(current.getTitle())
					&& current.getColumn(EnrichmentTerm.colNetworkSUID) != null
					&& current.getAllRows().size() > 0) {
				CyRow tempRow = current.getAllRows().get(0);
				if (tempRow.get(EnrichmentTerm.colNetworkSUID, Long.class) != null && tempRow
						.get(EnrichmentTerm.colNetworkSUID, Long.class).equals(network.getSUID())) {
					return current;
				}
			}
		}
		return null;
	}

	public static List<EnrichmentTerm> parseXMLDOM(Object results, double cutoff, String enrichmentCategory, CyNetwork network,
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
						EnrichmentTerm enrTerm = new EnrichmentTerm(name, descr, enrichmentCategory, pvalue, bonf, fdr);
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


	public static void setStringProperty(CyProperty<Properties> properties, 
	                                     String propertyKey, Object propertyValue) {
		Properties p = properties.getProperties();
		p.setProperty(propertyKey, propertyValue.toString());
	}

	public static boolean hasProperty(CyProperty<Properties> properties, String propertyKey) {
		Properties p = properties.getProperties();
		if (p.getProperty(propertyKey) != null) 
			return true;
		return false;
	}
	public static String getStringProperty(CyProperty<Properties> properties, String propertyKey) {
		Properties p = properties.getProperties();
		if (p.getProperty(propertyKey) != null) 
			return p.getProperty(propertyKey);
		return null;
	}

	public static Double getDoubleProperty(CyProperty<Properties> properties, String propertyKey) {
		String value = ModelUtils.getStringProperty(properties, propertyKey);
		if (value == null) return null;
		return Double.valueOf(value);
	}

	public static Integer getIntegerProperty(CyProperty<Properties> properties, String propertyKey) {
		String value = ModelUtils.getStringProperty(properties, propertyKey);
		if (value == null) return null;
		return Integer.valueOf(value);
	}

	public static Boolean getBooleanProperty(CyProperty<Properties> properties, String propertyKey) {
		String value = ModelUtils.getStringProperty(properties, propertyKey);
		if (value == null) return null;
		return Boolean.valueOf(value);
	}

	public static String listToString(List<?> list) {
		String str = "";
		if (list == null || list.size() == 0) return str;
		for (int i = 0; i < list.size()-1; i++) {
			str += list.get(i)+",";
		}
		return str + list.get(list.size()-1).toString();
	}

	public static List<String> stringToList(String string) {
		if (string == null || string.length() == 0) return new ArrayList<String>();
		String [] arr = string.split(",");
		return Arrays.asList(arr);
	}

	public static void updateEnrichmentSettings(CyNetwork network, Map<String, String> settings) {
		String setting = "";
		int index = 0;
		for (String key: settings.keySet()) {
			if (index > 0) {
				setting += ";";
			}
			setting += key+"="+settings.get(key);
			index ++;
		}
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, NET_ENRICHMENT_SETTINGS);
		network.getRow(network).set(NET_ENRICHMENT_SETTINGS, setting);
	}

	public static Map<String, String> getEnrichmentSettings(CyNetwork network) {
		Map<String, String> settings = new HashMap<String, String>();
		String setting = network.getRow(network).get(NET_ENRICHMENT_SETTINGS, String.class);
		if (setting == null || setting.length() == 0)
			return settings;

		String[] settingArray = setting.split(";");
		for (String s: settingArray) {
			String[] pair = s.split("=");
			if (pair.length == 2) {
				settings.put(pair[0], pair[1]);
			}
		}
		return settings;
	}

	public static CyProperty<Properties> getPropertyService(StringManager manager,
			SavePolicy policy) {
			String name = "stringApp";
			if (policy.equals(SavePolicy.SESSION_FILE)) {
				CyProperty<Properties> service = manager.getService(CyProperty.class, "(cyPropertyName="+name+")");
				// Do we already have a session with our properties
				if (service.getSavePolicy().equals(SavePolicy.SESSION_FILE))
					return service;

				// Either we have a null session or our properties aren't in this session
				Properties props = new Properties();
				service = new SimpleCyProperty(name, props, Properties.class, SavePolicy.SESSION_FILE);
				Properties serviceProps = new Properties();
				serviceProps.setProperty("cyPropertyName", service.getName());
				manager.registerAllServices(service, serviceProps);
				return service;
			} else if (policy.equals(SavePolicy.CONFIG_DIR) || policy.equals(SavePolicy.SESSION_FILE_AND_CONFIG_DIR)) {
				CyProperty<Properties> service = new ConfigPropsReader(policy, name);
				Properties serviceProps = new Properties();
				serviceProps.setProperty("cyPropertyName", service.getName());
				manager.registerAllServices(service, serviceProps);
				return service;
		}
		return null;
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
