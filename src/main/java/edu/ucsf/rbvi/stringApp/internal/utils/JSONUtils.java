package edu.ucsf.rbvi.stringApp.internal.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

import edu.ucsf.rbvi.stringApp.internal.model.EvidenceType;

public class JSONUtils {

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

	public static List<CyNode> createTMNetworkFromJSON(StringManager manager, Species species,
			JSONObject object, String query, String useDATABASE) {
		JSONArray results = getResultsFromJSON(object, JSONArray.class);
		if (results == null)
			return null;

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(query, query);
		ModelUtils.setDatabase(newNetwork, useDATABASE);
		ModelUtils.setNetSpecies(newNetwork, species.getName());

		List<CyNode> nodes = getJSON(manager, species, newNetwork, results);
		return nodes;
	}

	public static String getErrorMessageFromJSON(StringManager manager,
			JSONObject object) {
		JSONObject errorMsg = getResultsFromJSON(object, JSONObject.class);
		if (errorMsg.containsKey("Error")) {
			System.out.println("An error occured while retrieving ppi enrichment: " + errorMsg.get("Error"));
		}
		if (errorMsg.containsKey("ErrorMessage")) {
			return (String) errorMsg.get("ErrorMessage");
		}
		return "";
	}
	
	public static List<EnrichmentTerm> getEnrichmentFromJSON(StringManager manager,
			JSONObject object, Map<String, Long> stringNodesMap,
			CyNetwork network) {
		JSONArray enrichmentArray = getResultsFromJSON(object, JSONArray.class);
		if (enrichmentArray == null) {
			return null;
		}

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
			if (enr.containsKey("p_value"))
				currTerm.setPValue(((Number) enr.get("p_value")).doubleValue());
			if (enr.containsKey("bonferroni"))
				currTerm.setBonfPValue(((Number) enr.get("bonferroni")).doubleValue());
			if (enr.containsKey("fdr"))
				currTerm.setFDRPValue(((Number) enr.get("fdr")).doubleValue());
			if (enr.containsKey("number_of_genes_in_background"))
				currTerm.setGenesBG(((Number) enr.get("number_of_genes_in_background")).intValue());
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
						if (network.getDefaultNodeTable().getColumn(ColumnNames.DISPLAY) != null) {
							enrGeneNodeName = network.getDefaultNodeTable().getRow(nodeSUID)
									.get(ColumnNames.DISPLAY, String.class);
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
			// save only if above cutoff
			// if (currTerm.getFDRPValue() <= enrichmentCutoff)
			// save always since enrichment API returns fdr values < 0.05 
			results.add(currTerm);
		}
		return results;
	}

	public static Map<String, String> getEnrichmentPPIFromJSON(StringManager manager, 
	                                                           JSONObject object,
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
				// Check of the pvalue class needed to keep support for the old perl and new python version of STRING 
				if (enr.get("p_value").getClass().equals(Double.class)) {
					if (((Double)enr.get("p_value")).equals(0.0)) {
						values.put(ColumnNames.NET_PPI_ENRICHMENT, new Double(1e-16).toString());
					} else
						values.put(ColumnNames.NET_PPI_ENRICHMENT, ((Double)enr.get("p_value")).toString());
				} else if (enr.get("p_value").getClass().equals(String.class)) {
					if (((String)enr.get("p_value")).equals("0"))
						values.put(ColumnNames.NET_PPI_ENRICHMENT, new Double(1e-16).toString());
					else
						values.put(ColumnNames.NET_PPI_ENRICHMENT, (String)enr.get("p_value"));
				}
			}
			if (enr.containsKey("expected_number_of_edges")) {
				values.put(ColumnNames.NET_ENRICHMENT_EXPECTED_EDGES, enr.get("expected_number_of_edges").toString());
			}
			if (enr.containsKey("number_of_edges")) {
				values.put(ColumnNames.NET_ENRICHMENT_EDGES, enr.get("number_of_edges").toString());
			}
			if (enr.containsKey("average_node_degree")) {
				values.put(ColumnNames.NET_ENRICHMENT_DEGREE, enr.get("average_node_degree").toString());
			}
			if (enr.containsKey("local_clustering_coefficient")) {
				values.put(ColumnNames.NET_ENRICHMENT_CLSTR, enr.get("local_clustering_coefficient").toString());
			}
			if (enr.containsKey("number_of_nodes")) {
				values.put(ColumnNames.NET_ENRICHMENT_NODES, enr.get("number_of_nodes").toString());
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

	public static List<CyNode> augmentNetworkFromJSON(StringNetwork stringNetwork, CyNetwork net,
			List<CyEdge> newEdges, JSONObject object, Map<String, String> queryTermMap,
			String useDATABASE, String netType) {

		StringManager manager = stringNetwork.getManager();

		Object results = getResultsFromJSON(object, JSONObject.class);
		if (results == null)
			results = getResultsFromJSON(object, JSONArray.class); // See if this is a JSONArray
		else {
			if (((JSONObject)results).containsKey("message")) {
				String msgJSON = (String) ((JSONObject)results).get("message");
				if (msgJSON.length() > 0) {
					throw new RuntimeException(msgJSON);
				}
			}
		}

		if (results == null)
			return null;

		Map<String, CyNode> nodeMap = new HashMap<>();
		Map<String, String> nodeNameMap = new HashMap<>();
		String species = ModelUtils.getNetSpecies(net);
		// TODO: Check if we really don't have to infer the database!
		// String useDATABASE = StringManager.STRINGDB;
		for (CyNode node : net.getNodeList()) {
			if (species == null)
				species = net.getRow(node).get(ColumnNames.SPECIES, String.class);
			String stringId = net.getRow(node).get(ColumnNames.STRINGID, String.class);
			if (stringId == null) 
				continue; // Could be merged from another network
			String name = net.getRow(node).get(CyNetwork.NAME, String.class);
			nodeMap.put(stringId, node);
			nodeNameMap.put(stringId, name);
			// TODO: Change network from string to stitch once we add compounds?
			if (ModelUtils.isCompound(net, node))
				useDATABASE = Databases.STITCH.getAPIName();
		}
		ModelUtils.setDatabase(net, useDATABASE);

		List<CyNode> nodes;
		if (useDATABASE.equals(Databases.STRINGDB.getAPIName())) {
			Map<String, List<Annotation>> annotationsMap = stringNetwork.getAnnotations();
			nodes = getJSONFromStringDb(manager, species, net, nodeMap, nodeNameMap, queryTermMap, null, (JSONArray)results,
					                        useDATABASE, netType, annotationsMap);
		} else {
			nodes = getJSON(manager, species, net, nodeMap, nodeNameMap, queryTermMap, null, (JSONObject)results,
					            useDATABASE, netType);
		}
		
		// if we have "enough" nodes, but not too many, fetch the images, 
		// otherwise allow users to fetch them by setting "has images" to false 
		if (nodes != null && nodes.size() > 0 && nodes.size() <= ModelUtils.MAX_NODES_STRUCTURE_DISPLAY) {
			ModelUtils.fetchImages(net, nodes);
		} else {
			ModelUtils.setNetworkHasImages(net, false);
		}

		return nodes;
	}

	public static CyNetwork createNetworkFromJSON(StringNetwork stringNetwork, String species,
			JSONObject object, Map<String, String> queryTermMap, Map<String, CyNode> nodeMap,
			String ids, String netName,
			String useDATABASE, String netType) {
		stringNetwork.getManager().ignoreAdd();
		CyNetwork network = createNetworkFromJSON(stringNetwork.getManager(), stringNetwork, species, object,
				                                      queryTermMap, nodeMap, ids, netName, useDATABASE, netType);
		if (network == null)
			return null;
		stringNetwork.getManager().addStringNetwork(stringNetwork, network);
		stringNetwork.getManager().listenToAdd();
		stringNetwork.getManager().showResultsPanel();
		return network;
	}

	public static CyNetwork createNetworkFromJSON(StringManager manager, StringNetwork stringNetwork, String species,
			JSONObject object, Map<String, String> queryTermMap, Map<String, CyNode> nodeMap,
			String ids, String netName,
			String useDATABASE, String netType) {

		// STRING API return
		if (object.containsKey("ErrorMessage")) {
			String msgJSON = (String) object.get("ErrorMessage");
			if (msgJSON.length() > 0) {
				throw new RuntimeException(msgJSON);
			}
		}

		
		// Get a network name
		String defaultName;
		String defaultNameRootNet;
		if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
			defaultName = ModelUtils.DEFAULT_NAME_STITCH;
			defaultNameRootNet = ModelUtils.DEFAULT_NAME_STITCH;
		} else {	
			defaultName = ModelUtils.DEFAULT_NAME_STRING;
			defaultNameRootNet = ModelUtils.DEFAULT_NAME_STRING;
		}
		// add physical to name if the network is physical
		if (netType.equals(NetworkType.PHYSICAL.getAPIName()))
			defaultName += " " + ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL;

		// add user suggested name
		if (netName != null && netName != "") {
			defaultName = defaultName + " - " + netName;
			defaultNameRootNet = defaultNameRootNet + " - " + netName;
			//defaultNameRootNet = defaultNameRootNet + 
		} else if (queryTermMap != null && queryTermMap.size() == 1 && queryTermMap.containsKey(ids)) {
			defaultName = defaultName + " - " + queryTermMap.get(ids);
			defaultNameRootNet = defaultNameRootNet + " - " + queryTermMap.get(ids);
		} 
		
		Object results = getResultsFromJSON(object, JSONObject.class);
		if (results == null)
			results = getResultsFromJSON(object, JSONArray.class); // See if this is a JSONArray
		else {
			if (((JSONObject)results).containsKey("message")) {
				String msgJSON = (String) ((JSONObject)results).get("message");
				if (msgJSON.length() > 0) {
					throw new RuntimeException(msgJSON);
				}
			}
		}

		if (results == null)
			return null;

		// Create the network
		CyNetwork newNetwork = manager.createNetwork(defaultName, defaultNameRootNet);
		ModelUtils.setDatabase(newNetwork, useDATABASE);
		ModelUtils.setNetSpecies(newNetwork, species);

		// Create a map to save the node names
		Map<String, String> nodeNameMap = new HashMap<>();

		if (useDATABASE.equals(Databases.STRINGDB.getAPIName())) {
			Map<String, List<Annotation>> annotationsMap = stringNetwork.getAnnotations();
			getJSONFromStringDb(manager, species, newNetwork, nodeMap, nodeNameMap, queryTermMap, null, (JSONArray)results,
					                useDATABASE, netType, annotationsMap);
		} else {
			getJSON(manager, species, newNetwork, nodeMap, nodeNameMap, queryTermMap, null, (JSONObject)results,
					    useDATABASE, netType);
		}

		manager.addNetwork(newNetwork);
		return newNetwork;
	}

	private static List<CyNode> getJSON(StringManager manager, Species species, CyNetwork network,
			JSONArray tmResults) {
		List<CyNode> newNodes = new ArrayList<>();
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.ID);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.SPECIES);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.STRINGID);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.DISPLAY);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.FULLNAME);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.STYLE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, ColumnNames.TM_FOREGROUND);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, ColumnNames.TM_BACKGROUND);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, ColumnNames.TM_SCORE);

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
			row.set(ColumnNames.ID, "stringdb:" + species.getTaxId() + "." + stringid.toString());
			row.set(CyNetwork.NAME, species.getTaxId() + "." + stringid.toString());
			row.set(ColumnNames.DISPLAY, name);
			row.set(ColumnNames.SPECIES, species.getName());
			row.set(ColumnNames.STRINGID, species.getTaxId() + "." + stringid.toString());
			row.set(ColumnNames.STYLE, "string:");
			row.set(ColumnNames.TM_FOREGROUND, fg);
			row.set(ColumnNames.TM_BACKGROUND, bg);
			row.set(ColumnNames.TM_SCORE, score);
			nodes.add(newNode);
		}
		ModelUtils.shortenCompoundNames(network, nodes);
		return nodes;
	}

	// {"pscore":0,"preferredName_B":"OPC11257","preferredName_A":"OPC10681","dscore":0,"tscore":0,"score":0.92,"escore":0.92,"ncbiTaxonId":"110668318",
	// "stringId_B":"110668318.OPC11257","ascore":0,"stringId_A":"110668318.OPC10681","nscore":0,"fscore":0},
	private static List<CyNode> getJSONFromStringDb(StringManager manager, String species, CyNetwork network,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap,
			Map<String, String> queryTermMap, List<CyEdge> newEdges, JSONArray edges,
			String useDATABASE, String netType, Map<String, List<Annotation>> annotationsMap) {
		List<CyNode> newNodes = new ArrayList<>();
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.CANONICAL);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.COLOR);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.DISPLAY);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.FULLNAME);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.STRINGID);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.DESCRIPTION);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.ID);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.NAMESPACE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.TYPE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.QUERYTERM);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.SEQUENCE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.SPECIES);
		ModelUtils.createListColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.STRUCTURES);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.IMAGE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.STYLE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.ELABEL_STYLE);

		ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, ColumnNames.SCORE);
		ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Boolean.class, ColumnNames.INTERSPECIES);

		if (annotationsMap == null) annotationsMap = new HashMap<>();

		if (edges != null && edges.size() > 0) {
			for (Object edgeObj : edges) {
				if (edgeObj instanceof JSONObject)
					JSONUtils.createEdgeFromStringDb(network, (JSONObject) edgeObj, nodeMap, nodeNameMap, 
														                queryTermMap, newNodes, newEdges,
							                              useDATABASE, netType, annotationsMap);
			}
		}

		// TODO: [Custom] add new node to newNodes list if needed
		if (queryTermMap != null && queryTermMap.size() > 0) {
			for (String stringID : queryTermMap.keySet()) {
				if (nodeMap.get(stringID) == null) {
					JSONUtils.createNodeFromStringDb(network, stringID, "", species, nodeMap, nodeNameMap, queryTermMap, annotationsMap.get(queryTermMap.get(stringID)));
				}
			}
		}
		
		return newNodes;
	}

	private static List<CyNode> getJSON(StringManager manager, String species, CyNetwork network,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap,
			Map<String, String> queryTermMap, List<CyEdge> newEdges, JSONObject json,
			String useDATABASE, String netType) {
		
		List<CyNode> newNodes = new ArrayList<>();
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.CANONICAL);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.DISPLAY);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.FULLNAME);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.STRINGID);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.DESCRIPTION);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.ID);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.NAMESPACE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.TYPE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.QUERYTERM);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.SEQUENCE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.SPECIES);
		if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
			ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.SMILES);
			ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.CV_STYLE);
		}
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.IMAGE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.STYLE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, ColumnNames.ELABEL_STYLE);

		ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, ColumnNames.SCORE);
		ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Boolean.class, ColumnNames.INTERSPECIES);

		Set<String> columnMap = new HashSet<>();

		// Get the nodes
		JSONArray nodes = (JSONArray) json.get("nodes");
		if (nodes != null && nodes.size() > 0) {
			createColumnsFromJSON(nodes, network.getDefaultNodeTable());
			for (Object nodeObj : nodes) {
				if (nodeObj instanceof JSONObject) {
					JSONObject nodeJSON = (JSONObject) nodeObj;
					CyNode newNode = JSONUtils.createNode(manager, network, nodeJSON, species, nodeMap, nodeNameMap,
							queryTermMap, columnMap);
					if (newNode != null)
						newNodes.add(newNode);
				}
			}
		}
		ModelUtils.shortenCompoundNames(network, newNodes);
		
		// Get the edges
		JSONArray edges = (JSONArray) json.get("edges");
		if (edges != null && edges.size() > 0) {
			for (Object edgeObj : edges) {
				if (edgeObj instanceof JSONObject)
					JSONUtils.createEdge(network, (JSONObject) edgeObj, nodeMap, nodeNameMap, newEdges,
							useDATABASE, netType);
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
			// String formattedJsonKey = formatForColumnNamespace(jsonKey);
			if (ModelUtils.ignoreKeys.contains(jsonKey))
				continue;
			if (listKeys.contains(jsonKey)) {
				ModelUtils.createListColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			} else {
				ModelUtils.createColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			}
		}

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
		row.set(ColumnNames.DISPLAY, name);
		row.set(ColumnNames.STRINGID, stringId);
		row.set(ColumnNames.ID, id);
		row.set(ColumnNames.NAMESPACE, namespace);
		row.set(ColumnNames.STYLE, "string:"); // We may overwrite this, if we get an image
	
		String type = (String) nodeObj.get("node type");
		if (type == null)
			type = ModelUtils.getType(id);
		row.set(ColumnNames.TYPE, type);

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
			row.set(ColumnNames.SPECIES, species);
		}
		
		for (Object objKey : nodeObj.keySet()) {
			String key = (String) objKey;
			// Look for our "special" columns
			if (key.equals("name")) {
				continue;
			} else if (key.equals("@id")) {
				// just skip thought this one
			} else if (key.equals("description")) {
				row.set(ColumnNames.DESCRIPTION, (String) nodeObj.get("description"));
			} else if (key.equals("canonical")) {
				row.set(ColumnNames.CANONICAL, (String) nodeObj.get("canonical"));
			} else if (key.equals(ColumnNames.SEQUENCE)) {
				row.set(ColumnNames.SEQUENCE, (String) nodeObj.get(ColumnNames.SEQUENCE));
			} else if (key.equals("image")) {
				row.set(ColumnNames.STYLE, "string:" + nodeObj.get("image"));
			} else if (key.equals("imageurl"))  {
				row.set(ColumnNames.IMAGE, (String) nodeObj.get("imageurl"));
			} else if (key.equals(ColumnNames.SMILES)) {
				if (manager.haveChemViz() || (nodeObj.containsKey("image") && nodeObj.get("image").equals("image:")))
					row.set(ColumnNames.CV_STYLE, "chemviz:" + nodeObj.get(ColumnNames.SMILES));
				row.set(key, nodeObj.get(ColumnNames.SMILES));
			} else {
				// It's not one of our "standard" attributes, create a column for it (if necessary)
				// and then add it
				Object value = nodeObj.get(key);
				// String formattedKey = formatForColumnNamespace(key);
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
					enhancedLabel += "labelAlignment=left position=northeast ";
				else
					enhancedLabel += "labelAlignment=middle position=north ";
				enhancedLabel += "outline=true outlineColor=white outlineTransparency=95 outlineWidth=10 ";
				enhancedLabel += "background=false color=black dropShadow=false";
				row.set(ColumnNames.ELABEL_STYLE, enhancedLabel);
			}
		}
		// TODO: Fix hack for saving query term for compounds
		if (queryTermMap != null) {
			if (queryTermMap.containsKey(stringId)) {
				network.getRow(newNode).set(ColumnNames.QUERYTERM, queryTermMap.get(stringId));
			} else if (queryTermMap.containsKey("-1.CID1" + stringId.substring(4))) {
				network.getRow(newNode).set(ColumnNames.QUERYTERM,
						queryTermMap.get("-1.CID1" + stringId.substring(4)));
			} else if (queryTermMap.containsKey("-1.CID0" + stringId.substring(4))) {
				network.getRow(newNode).set(ColumnNames.QUERYTERM,
						queryTermMap.get("-1.CID0" + stringId.substring(4)));
			}
		}
		nodeMap.put(stringId, newNode);
		nodeNameMap.put(stringId, name);
		return newNode;
	}

	private static CyNode createNodeFromStringDb(CyNetwork network, String id, String name,
			String speciesName, Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap, 
			Map<String, String> queryTermMap, List<Annotation> annotations) {

		if (nodeMap.containsKey(id))
			return null;
		// System.out.println("Node id = "+id+", name = "+name);
		CyNode newNode = network.addNode();
		CyRow row = network.getRow(newNode);
		nodeMap.put(id, newNode);

		// System.out.println("Creating node "+id);

		row.set(CyNetwork.NAME, id);
		// row.set(CyRootNetwork.SHARED_NAME, stringId);
		row.set(ColumnNames.DISPLAY, name);
		row.set(ColumnNames.STRINGID, id);
		row.set(ColumnNames.SPECIES, speciesName);
		row.set(ColumnNames.ID, "stringdb:"+id);
		row.set(ColumnNames.NAMESPACE, "stringdb");
		row.set(ColumnNames.STYLE, "string:"); // We may overwrite this, if we get an image
		row.set(ColumnNames.TYPE, "protein");
		{
			// Construct instructions for enhanced graphics label
			String enhancedLabel = "label: attribute=\"display name\" labelsize=12 ";
			enhancedLabel += "labelAlignment=left position=northeast ";
			enhancedLabel += "outline=true outlineColor=white outlineTransparency=95 outlineWidth=10 ";
			enhancedLabel += "background=false color=black dropShadow=false";
			row.set(ColumnNames.ELABEL_STYLE, enhancedLabel);
		}
		// TODO: [Custom] can we just take the first annotation or not?
		if (annotations != null && annotations.size() > 0) {
			Annotation nodeAnnot = annotations.get(0);
			ModelUtils.updateNodeAttributes(row, nodeAnnot, true);
			// Special case depending of whether we create the node from the annotations or from the network json data 
			if (name.equals("") && nodeAnnot.getPreferredName() != null)
				row.set(ColumnNames.DISPLAY, nodeAnnot.getPreferredName());			
		}
		
		if (queryTermMap != null) {
			if (queryTermMap.containsKey(id)) {
				network.getRow(newNode).set(ColumnNames.QUERYTERM, queryTermMap.get(id));
			}
		}
		return newNode;
	}

	public static void addExtraNodeData(StringNetwork stringNet, JSONObject results) {
		JSONArray obj = getResultsFromJSON(results, JSONArray.class);
		CyNetwork network = stringNet.getNetwork();
		Map<String, CyRow> nodeMap = new HashMap<>();
		for (CyNode node: network.getNodeList()) {
			CyRow row = network.getRow(node);
			nodeMap.put(row.get(ColumnNames.ID, String.class), row);
		}
		for (Object nodeData: obj) {
			JSONObject nodeObj = (JSONObject)nodeData;
			String id = (String)nodeObj.get(ColumnNames.ID);
			CyRow row = nodeMap.get(id);
			if (row == null) continue;
			for (Object extraObj: nodeObj.keySet()) {
				String extraName = (String)extraObj;
				// Skip over the data we already got from string-db
				// TODO: pre-create columns beforehand to have them in the right order?
				if (extraName.startsWith(ColumnNames.TARGET_NAMESPACE)) {
					ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class, extraName);
					row.set(extraName, (String)nodeObj.get(extraObj));
				} else if (extraName.startsWith(ColumnNames.TISSUE_NAMESPACE)) {
					ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, extraName);
					row.set(extraName, (Double)nodeObj.get(extraObj));
				} else if (extraName.startsWith(ColumnNames.COMPARTMENT_NAMESPACE)) {
					ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, extraName);
					row.set(extraName, (Double)nodeObj.get(extraObj));
				}
			}
		}
	}

	// {"pscore":0,"preferredName_B":"OPC11257","preferredName_A":"OPC10681","dscore":0,"tscore":0,"score":0.92,"escore":0.92,"ncbiTaxonId":"110668318",
	// "stringId_B":"110668318.OPC11257","ascore":0,"stringId_A":"110668318.OPC10681","nscore":0,"fscore":0},
	private static void createEdgeFromStringDb(CyNetwork network, JSONObject edgeObj,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap, 
			Map<String, String> queryTermMap, List<CyNode> newNodes, List<CyEdge> newEdges,
			String useDATABASE, String netType, Map<String, List<Annotation>> annotationsMap) {

		String source = (String) edgeObj.get("stringId_A");
		String target = (String) edgeObj.get("stringId_B");
		String sourceName = (String) edgeObj.get("preferredName_A");
		String targetName = (String) edgeObj.get("preferredName_B");
		String taxIdName = (String) edgeObj.get("ncbiTaxonId");
		String speciesName = Species.getSpeciesName(taxIdName);
		CyNode sourceNode;
		CyNode targetNode;
		if (nodeMap.get(source) == null) {
			String sourceQuery;
			if (queryTermMap != null)
				sourceQuery = queryTermMap.get(source);
			else
				sourceQuery = source;
			sourceNode = createNodeFromStringDb(network, source, sourceName, speciesName, nodeMap, nodeNameMap, queryTermMap, annotationsMap.get(sourceQuery));
			if (newNodes != null)
				newNodes.add(sourceNode);
		} else {
			sourceNode = nodeMap.get(source);
		}

		if (nodeMap.get(target) == null) {
			String targetQuery;
			if (queryTermMap != null)
				targetQuery = queryTermMap.get(target);
			else
				targetQuery = target;
			targetNode = createNodeFromStringDb(network, target, targetName, speciesName, nodeMap, nodeNameMap, queryTermMap, annotationsMap.get(targetQuery));
			if (newNodes != null)
				newNodes.add(targetNode);
		} else {
			targetNode = nodeMap.get(target);
		}

		String physical = "";
		if (netType.equals(NetworkType.PHYSICAL.getAPIName()))
			physical = "p";

		String interaction = physical+"pp";

		CyEdge edge;
		if (!network.containsEdge(sourceNode, targetNode)) {
			edge = network.addEdge(sourceNode, targetNode, false);
			network.getRow(edge).set(CyNetwork.NAME,
					source + " (" + interaction + ") " + target);
			network.getRow(edge).set(CyEdge.INTERACTION, interaction);

			if (newEdges != null)
				newEdges.add(edge);
		} else {
			List<CyEdge> edges = network.getConnectingEdgeList(sourceNode, targetNode,
					CyEdge.Type.ANY);
			if (edges == null)
				return; // Shouldn't happen!
			edge = edges.get(0);
		}

		// OK, now add the scores.  Here are the scores currently:
		//
		// score  -- score
		// nscore -- neighborhood
		// fscore -- fusion
		// pscore -- coocurrence (?) phylogenetic profile score
		// ascore -- coexpression
		// escore -- experiments
		// dscore -- database
		// tscore -- textmining

		// Update the scores information
		CyRow edgeRow = network.getRow(edge);
		if (edgeObj.containsKey("score"))
			edgeRow.set("stringdb::score", ModelUtils.makeDouble(edgeObj.get("score")));
		if (edgeObj.containsKey("nscore")) {
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, EvidenceType.NEIGHBORHOOD.name());
			edgeRow.set(EvidenceType.NEIGHBORHOOD.name(), ModelUtils.makeDouble(edgeObj.get("nscore")));
		}
		if (edgeObj.containsKey("fscore")) {
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, EvidenceType.GENEFUSIONS.name());
			edgeRow.set(EvidenceType.GENEFUSIONS.name(), ModelUtils.makeDouble(edgeObj.get("fscore")));
		}
		if (edgeObj.containsKey("pscore")) {
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, EvidenceType.COOCCURRENCE.name());
			edgeRow.set(EvidenceType.COOCCURRENCE.name(), ModelUtils.makeDouble(edgeObj.get("pscore")));
		}
		if (edgeObj.containsKey("ascore")) {
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, EvidenceType.COEXPRESSION.name());
			edgeRow.set(EvidenceType.COEXPRESSION.name(), ModelUtils.makeDouble(edgeObj.get("ascore")));
		}
		if (edgeObj.containsKey("escore")) {
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, EvidenceType.EXPERIMENTS.name());
			edgeRow.set(EvidenceType.EXPERIMENTS.name(), ModelUtils.makeDouble(edgeObj.get("escore")));
		}
		if (edgeObj.containsKey("dscore")) {
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, EvidenceType.DATABASES.name());
			edgeRow.set(EvidenceType.DATABASES.name(), ModelUtils.makeDouble(edgeObj.get("dscore")));
		}
		if (edgeObj.containsKey("tscore")) {
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, EvidenceType.TEXTMINING.name());
			edgeRow.set(EvidenceType.TEXTMINING.name(), ModelUtils.makeDouble(edgeObj.get("tscore")));
		}

	}

	private static void createEdge(CyNetwork network, JSONObject edgeObj,
			Map<String, CyNode> nodeMap, Map<String, String> nodeNameMap, List<CyEdge> newEdges,
			String useDATABASE, String netType) {
		String source = (String) edgeObj.get("source");
		String target = (String) edgeObj.get("target");
		CyNode sourceNode = nodeMap.get(source);
		CyNode targetNode = nodeMap.get(target);

		CyEdge edge;
		String physical = "";
		if (netType.equals(NetworkType.PHYSICAL.getAPIName()))
			physical = "p";

		String interaction = physical+"pp";

		// Don't create an edge if we already have one between these nodes
		if (!network.containsEdge(sourceNode, targetNode)) {
			if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
				boolean sourceType = ModelUtils.isCompound(network, sourceNode);
				boolean targetType = ModelUtils.isCompound(network, targetNode);
				if (sourceType == false && targetType == false)
					interaction = physical + "pp";
				else if (sourceType == true && targetType == true)
					interaction = "cc";
				else
					interaction = physical + "pc";
			}

			edge = network.addEdge(sourceNode, targetNode, false);
			network.getRow(edge).set(CyNetwork.NAME,
					source + " (" + interaction + ") " + target);
			network.getRow(edge).set(CyEdge.INTERACTION, interaction);

			String sourceSpecies = ModelUtils.getNodeSpecies(network, sourceNode);
			String targetSpecies = ModelUtils.getNodeSpecies(network, targetNode);
			if (sourceSpecies != null && targetSpecies != null && !sourceSpecies.equals(targetSpecies)) 
				network.getRow(edge).set(ColumnNames.INTERSPECIES, Boolean.TRUE);
			
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
		// NOTE: we should not compute this score but get it from the database!!!
		// double scoreProduct = 1.0;
		for (Object key : scores.keySet()) {
			String score = (String) key;
			// String scoreFormatted = formatForColumnNamespace(score);
			ModelUtils.createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, score);
			Double v = (Double) scores.get(key);
			network.getRow(edge).set(score, v);
			// scoreProduct *= (1 - v);
		}
		// double totalScore = -(scoreProduct - 1.0);
		// network.getRow(edge).set(SCORE, totalScore);
	}

	public static <T> T getResultsFromJSON(JSONObject json, Class<? extends T> clazz) {
		if (json == null || !json.containsKey(StringManager.RESULT))
			return null;
		
		// System.out.println("json: " + json.toJSONString());

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

}
