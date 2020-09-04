package edu.ucsf.rbvi.stringApp.internal.utils;

import java.awt.Color;
import java.awt.color.ICC_ColorSpace;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.CyProperty.SavePolicy;
import org.cytoscape.property.SimpleCyProperty;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
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

	// Namespaces
	public static String STRINGDB_NAMESPACE = "stringdb";
	public static String NAMESPACE_SEPARATOR = "::";
	
	// Node information
	public static String CANONICAL = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "canonical name";
	public static String DISPLAY = "display name";
	public static String FULLNAME = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "full name";
	public static String CV_STYLE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "chemViz Passthrough";
	public static String ELABEL_STYLE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "enhancedLabel Passthrough";
	public static String ID = "@id";
	public static String DESCRIPTION = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "description";
	public static String DISEASE_SCORE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "disease score";
	public static String NAMESPACE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "namespace";
	public static String QUERYTERM = "query term";
	public static String SEQUENCE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "sequence";
	public static String SMILES = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "smiles";
	public static String SPECIES = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "species";
	public static String STRINGID = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "database identifier";
	public static String STYLE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "STRING style";
	public static String TYPE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "node type";
	public static String TM_FOREGROUND = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "textmining foreground";
	public static String TM_BACKGROUND = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "textmining background";
	public static String TM_SCORE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "textmining score";

	public static String TISSUE_NAMESPACE = "tissue";
	public static String COMPARTMENT_NAMESPACE = "compartment";
	// public static String TM_LINKOUT = "TextMining Linkout";
	public static List<String> ignoreKeys = new ArrayList<String>(Arrays.asList("image", "canonical", "@id", "description"));
	public static List<String> namespacedNodeAttributes = new ArrayList<String>(Arrays.asList("canonical name", "full name", "chemViz Passthrough", 
			"enhancedLabel Passthrough", "description", "disease score", "namespace", "sequence", "smiles", "species", "database identifier", 
			"STRING style", "node type", "textmining foreground", "textmining background", "textmining score"));
		
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
	public static String SCORE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "score";
	public static String SCORE_NO_NAMESPACE = "score";
	public static String INTERSPECIES = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "interspecies";

	public static List<String> namespacedEdgeAttributes = new ArrayList<String>(Arrays.asList("score", "interspecies", "experiments", "cooccurrence",
			"coexpression", "textmining", "databases", "neighborhood"));
	
	// Network information
	public static String CONFIDENCE = "confidence score";
	public static String DATABASE = "database";
	public static String NET_SPECIES = "species";
	public static String NET_DATAVERSION = "data version";
	public static String NET_URI = "uri";
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

	public static String REQUERY_MSG_USER = 
			"<html>This action cannot be performed on the current network as it <br />"
			+ "appears to be an old STRING network. Would you like to get <br />"
			 + "the latest STRING network for the nodes in your network?</html>";
	public static String REQUERY_TITLE = "Re-query network?";
	
	public static boolean haveQueryTerms(CyNetwork network) {
		if (network == null) return false;
		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).get(QUERYTERM, String.class) != null)
				return true;
		}
		return false;
	}

	public static void selectQueryTerms(CyNetwork network) {
		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).get(QUERYTERM, String.class) != null)
				network.getRow(node).set(CyNetwork.SELECTED, true);
			else
				network.getRow(node).set(CyNetwork.SELECTED, false);
		}
	}

	public static List<String> getCompartmentList(CyNetwork network) {
		List<String> compartments = new ArrayList<>();
		if (network == null) {
			return compartments;
		}
		Collection<CyColumn> columns = network.getDefaultNodeTable().getColumns(COMPARTMENT_NAMESPACE);
		if (columns == null || columns.size() == 0) return compartments;
		for (CyColumn col: columns) {
			compartments.add(col.getNameOnly());
		}
		return compartments;
	}

	public static List<String> getSubScoreList(CyNetwork network) {
		List<String> scores = new ArrayList<>();
		if (network == null) return scores;
		Collection<CyColumn> columns = network.getDefaultEdgeTable().getColumns(STRINGDB_NAMESPACE);
		if (columns == null || columns.size() == 0) return scores;
		for (CyColumn col: columns) {
			if (col.getNameOnly().equals("score") || !col.getType().equals(Double.class))
				continue;
			scores.add(col.getNameOnly());
		}
		return scores;
	}

	public static List<String> getTissueList(CyNetwork network) {
		List<String> tissues = new ArrayList<>();
		if (network == null) {
			// System.out.println("network is null");
			return tissues;
		}
		Collection<CyColumn> columns = network.getDefaultNodeTable().getColumns(TISSUE_NAMESPACE);
		if (columns == null || columns.size() == 0) return tissues;
		for (CyColumn col: columns) {
			tissues.add(col.getNameOnly());
		}
		return tissues;
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
						values.put(NET_PPI_ENRICHMENT, new Double(1e-16).toString());
					} else
						values.put(NET_PPI_ENRICHMENT, ((Double)enr.get("p_value")).toString());
				} else if (enr.get("p_value").getClass().equals(String.class)) {
					if (((String)enr.get("p_value")).equals("0"))
						values.put(NET_PPI_ENRICHMENT, new Double(1e-16).toString());
					else
						values.put(NET_PPI_ENRICHMENT, (String)enr.get("p_value"));
				}
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
			if (stringId == null) 
				continue; // Could be merged from another network
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
		stringNetwork.getManager().showResultsPanel();
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

	public static void setDataVersion(CyNetwork network, String dataVersion) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, NET_DATAVERSION);
		network.getRow(network).set(NET_DATAVERSION, dataVersion);
	}

	public static String getDataVersion(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(NET_DATAVERSION) == null)
			return null;
		return network.getRow(network).get(NET_DATAVERSION, String.class);
	}

	public static void setNetURI(CyNetwork network, String netURI) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, NET_URI);
		network.getRow(network).set(NET_URI, netURI);
	}

	public static String getNetURI(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(NET_URI) == null)
			return null;
		return network.getRow(network).get(NET_URI, String.class);
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

	public static List<String> getEnrichmentNetSpecies(CyNetwork net) {
		List<String> species = new ArrayList<String>();
		for (CyNode node : net.getNodeList()) {
			String nSpecies = net.getRow(node).get(SPECIES, String.class);
			if (nSpecies != null && !nSpecies.equals("") && !species.contains(nSpecies)) {
				Species theSpecies = Species.getSpecies(nSpecies);
				// TODO: This is kind of a hack for now and will be updated once we get the kingdom data from the server 
				if (theSpecies != null && (theSpecies.getType().equals("core") || theSpecies.getType().equals("periphery")))
					species.add(nSpecies);
			}
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
			// String formattedJsonKey = formatForColumnNamespace(jsonKey);
			if (ignoreKeys.contains(jsonKey))
				continue;
			if (listKeys.contains(jsonKey)) {
				createListColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			} else {
				createColumnIfNeeded(table, jsonKeysClass.get(jsonKey), jsonKey);
			}
		}

	}
	
	public static String formatForColumnNamespace(String columnName) {
		String formattedColumnName = columnName;
		if (columnName.contains("::")) {
			if (columnName.startsWith(STRINGDB_NAMESPACE))
				formattedColumnName = columnName.substring(STRINGDB_NAMESPACE.length() + 2);
			else
				formattedColumnName = columnName.replaceFirst("::", " ");
		}
		return formattedColumnName;
	}
	
	public static boolean isMergedStringNetwork(CyNetwork network) {
		CyTable nodeTable = network.getDefaultNodeTable();
		if (nodeTable.getColumn(ID) == null)
			return false;
		// Enough to check for id in the node columns and score in the edge columns
		//if (nodeTable.getColumn(SPECIES) == null)
		//	return false;
		//if (nodeTable.getColumn(CANONICAL) == null)
		//	return false;
		CyTable edgeTable = network.getDefaultEdgeTable();
		if (edgeTable.getColumn(SCORE) == null && edgeTable.getColumn(SCORE_NO_NAMESPACE) == null)
			return false;
		return true;
	}

	public static boolean isStringNetwork(CyNetwork network) {
		// This is a string network only if we have a confidence score in the network table,
		// "@id" column in the node table, and a "score" column in the edge table
		if (network == null || network.getRow(network).get(CONFIDENCE, Double.class) == null)
			return false;
		return isMergedStringNetwork(network);
	}
	
	// This method will tell us if we have the new side panel functionality (i.e. namespaces)
	public static boolean ifHaveStringNS(CyNetwork network) {
		if (network == null) return false;
		CyRow netRow = network.getRow(network);
		Collection<CyColumn> columns = network.getDefaultNodeTable().getColumns(STRINGDB_NAMESPACE);
		if (netRow.isSet(CONFIDENCE) && netRow.isSet(NET_SPECIES) && columns != null && columns.size() > 0)
			return true;
		return false;
	}

	public static boolean isCurrentDataVersion(CyNetwork network) {
		if (network == null || network.getRow(network).get(NET_DATAVERSION, String.class) == null
				|| !network.getRow(network).get(NET_DATAVERSION, String.class)
						.equals(StringManager.DATAVERSION))
			return false;
		return true;
	}

	public static boolean isStitchNetwork(CyNetwork network) {
		if (network == null || network.getDefaultNodeTable().getColumn(SMILES) == null)
			return false;
		return true;
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

	
		String type = (String) nodeObj.get("node type");
		if (type == null)
			type = getType(id);
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
			} else if (key.equals("@id")) {
				// just skip thought this one
			} else if (key.equals("description")) {
				row.set(DESCRIPTION, (String) nodeObj.get("description"));
			} else if (key.equals("canonical")) {
				row.set(CANONICAL, (String) nodeObj.get("canonical"));
			} else if (key.equals(SEQUENCE)) {
				network.getRow(newNode).set(SEQUENCE, (String) nodeObj.get(SEQUENCE));
			} else if (key.equals("image")) {
				row.set(STYLE, "string:" + nodeObj.get("image"));
			} else if (key.equals(SMILES)) {
				if (manager.haveChemViz() || (nodeObj.containsKey("image") && nodeObj.get("image").equals("image:")))
					row.set(CV_STYLE, "chemviz:" + nodeObj.get(SMILES));
				row.set(key, nodeObj.get(SMILES));
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

		String type = net.getRow(node).get(TYPE, String.class);
		return type != null && type.equals("compound");
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
		// NOTE: we should not compute this score but get it from the database!!!
		// double scoreProduct = 1.0;
		for (Object key : scores.keySet()) {
			String score = (String) key;
			// String scoreFormatted = formatForColumnNamespace(score);
			createColumnIfNeeded(network.getDefaultEdgeTable(), Double.class, score);
			Double v = (Double) scores.get(key);
			network.getRow(edge).set(score, v);
			// scoreProduct *= (1 - v);
		}
		// double totalScore = -(scoreProduct - 1.0);
		// network.getRow(edge).set(SCORE, totalScore);
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

	public static void deleteColumnIfExisting(CyTable table, String columnName) {
		if (table.getColumn(columnName) != null)
			table.deleteColumn(columnName);		
	}
	
	public static String getName(CyNetwork network, CyIdentifiable ident) {
		return getString(network, ident, CyNetwork.NAME);
	}

	public static String getDisplayName(CyNetwork network, CyIdentifiable ident) {
		return getString(network, ident, DISPLAY);
	}

	public static String getString(CyNetwork network, CyIdentifiable ident, String column) {
		// System.out.println("network = "+network+", ident = "+ident+" column = "+column);
		if (network.getRow(ident, CyNetwork.DEFAULT_ATTRS) != null)
			return network.getRow(ident, CyNetwork.DEFAULT_ATTRS).get(column, String.class);
		return null;
	}

	public static List<String> getNetworkSpeciesTaxons(CyNetwork network) {
		List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		if (nodes == null || nodes.size() == 0)
			nodes = network.getNodeList();
		List<String> netSpeciesNames = new ArrayList<String>();
		for (CyNode node : nodes) {
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
				if (nodesSUID != null) {
					for (CyNode netNode : net.getNodeList()) {
						if (nodesSUID.contains(netNode.getSUID())) {
							analyzedNodes.add(netNode);
						}
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

	
	public static void deleteEnrichmentTables(CyNetwork network, StringManager manager, boolean publOnly) {
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		Set<CyTable> oldTables = ModelUtils.getEnrichmentTables(manager, network);
		for (CyTable table : oldTables) {
			if (publOnly && !table.getTitle().equals(TermCategory.PMID.getTable())) {
				continue;
			} 
			tableManager.deleteTable(table.getSUID());
			manager.flushEvents();				
		}
	}

	public static void setupEnrichmentTable(CyTable enrichmentTable) {
		if (enrichmentTable.getColumn(EnrichmentTerm.colGenesSUID) == null) {
			enrichmentTable.createListColumn(EnrichmentTerm.colGenesSUID, Long.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colNetworkSUID) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colNetworkSUID, Long.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colName) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colName, String.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colYear) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colYear, Integer.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colIDPubl) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colIDPubl, String.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colDescription) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colDescription, String.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colCategory) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colCategory, String.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colFDR) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colFDR, Double.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colFDRTransf) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colFDRTransf, Double.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colPvalue) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colPvalue, Double.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colGenesBG) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colGenesBG, Integer.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colGenesCount) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colGenesCount, Integer.class, false);
		}
		if (enrichmentTable.getColumn(EnrichmentTerm.colGenes) == null) {
			enrichmentTable.createListColumn(EnrichmentTerm.colGenes, String.class, false);
		}
		// if (table.getColumn(EnrichmentTerm.colShowChart) == null) {
		//	table.createColumn(EnrichmentTerm.colShowChart, Boolean.class, false);
		// }
		if (enrichmentTable.getColumn(EnrichmentTerm.colChartColor) == null) {
			enrichmentTable.createColumn(EnrichmentTerm.colChartColor, String.class, false);
		}
		// table.createColumn(EnrichmentTerm.colPvalue, Double.class, false);
		// table.createColumn(EnrichmentTerm.colBonferroni, Double.class, false);
	}
	
	public static double getMaxFdrLogValue(List<EnrichmentTerm> terms) {
		double maxValue = 0;
		for (EnrichmentTerm term : terms) {
			double termValue = -Math.log10(term.getFDRPValue());
			if (termValue > maxValue)
				maxValue = termValue;
		}
		if (maxValue > 10.0) 
			return 10.0;
		return maxValue;
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
					int genesbg = -1;
					if (eElement.getElementsByTagName("number_of_genes_in_background").getLength() > 0) {
						fdr = Integer.valueOf(
								eElement.getElementsByTagName("number_of_genes_in_background").item(0).getTextContent())
								.intValue();
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
						EnrichmentTerm enrTerm = new EnrichmentTerm(name, 0, descr, enrichmentCategory, pvalue, bonf, fdr, genesbg);
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

	public static void copyRow(CyTable fromTable, CyTable toTable, CyIdentifiable from, CyIdentifiable to, List<String> columnsCreated) {
		for (CyColumn col: fromTable.getColumns()) {
			// TODO: Is it OK to not check for this?
			//if (!columnsCreated.contains(col.getName()))
			//	continue;
			if (col.getName().equals(CyNetwork.SUID)) 
				continue;
			if (from.getClass().equals(CyNode.class) && col.getName().equals(CyNetwork.NAME)) 
				continue;
			if (col.getName().equals(CyNetwork.SELECTED)) 
				continue;
			if (from.getClass().equals(CyNode.class) && col.getName().equals(CyRootNetwork.SHARED_NAME)) 
				continue;
			if (col.getName().equals(ModelUtils.QUERYTERM) || col.getName().equals(ModelUtils.DISPLAY) || col.getName().equals(ModelUtils.ID)) {
				Object v = fromTable.getRow(from.getSUID()).getRaw(col.getName());
				toTable.getRow(to.getSUID()).set(col.getName() + ".copy", v);
				continue;
			}
			// TODO: Is it OK to overwrite interaction type? 
			//if (from.getClass().equals(CyEdge.class) && col.getName().equals(CyRootNetwork.SHARED_INTERACTION)) 
			//	continue;
			//if (from.getClass().equals(CyEdge.class) && col.getName().equals(CyEdge.INTERACTION)) 
			//	continue;
			Object v = fromTable.getRow(from.getSUID()).getRaw(col.getName());
			toTable.getRow(to.getSUID()).set(col.getName(), v);
		}
	}

	public static void copyNodes(CyNetwork fromNetwork, CyNetwork toNetwork, Map<String, CyNode> nodeMap, 
	                             String keyColumn, List<String> toColumns) {
		for (CyNode node: fromNetwork.getNodeList()) {
			String key = fromNetwork.getRow(node).get(keyColumn, String.class);
			// TODO: double-check what happens when key == null
			if (key != null && !nodeMap.containsKey(key)) {
				CyNode newNode = toNetwork.addNode();
				nodeMap.put(key, newNode);
				toNetwork.getRow(newNode).set(CyNetwork.NAME, key);
				for (String col: toColumns) {
					toNetwork.getRow(newNode).set(col, key);
				}
			}
		}
	}

	public static void createNodeMap(CyNetwork network, Map<String, CyNode> nodeMap, String column) {
		// Get all of the nodes in the network
		for (CyNode node: network.getNodeList()) {
			String key = network.getRow(node).get(column, String.class);
			nodeMap.put(key, node);
		}
	}

	public static List<String> copyColumns(CyTable fromTable, CyTable toTable) {
		List<String> columns = new ArrayList<String>();
		for (CyColumn col: fromTable.getColumns()) {
			String fqn = col.getName();
			// Does that column already exist in our target?
			if (toTable.getColumn(fqn) == null) {
				// No, create it.
				if (col.getType().equals(List.class)) {
					// There is no easy way to handle this, unfortunately...
					// toTable.createListColumn(fqn, col.getListElementType(), col.isImmutable(), (List<?>)col.getDefaultValue());
					if (col.getListElementType().equals(String.class))
						toTable.createListColumn(fqn, String.class, col.isImmutable(), 
						                         (List<String>)col.getDefaultValue());
					else if (col.getListElementType().equals(Long.class))
						toTable.createListColumn(fqn, Long.class, col.isImmutable(), 
						                         (List<Long>)col.getDefaultValue());
					else if (col.getListElementType().equals(Double.class))
						toTable.createListColumn(fqn, Double.class, col.isImmutable(), 
						                         (List<Double>)col.getDefaultValue());
					else if (col.getListElementType().equals(Integer.class))
						toTable.createListColumn(fqn, Integer.class, col.isImmutable(), 
						                         (List<Integer>)col.getDefaultValue());
					else if (col.getListElementType().equals(Boolean.class))
						toTable.createListColumn(fqn, Boolean.class, col.isImmutable(), 
						                         (List<Boolean>)col.getDefaultValue());
				} else {
					toTable.createColumn(fqn, col.getType(), col.isImmutable(), col.getDefaultValue());
					columns.add(fqn);
				}
			} else if (fqn.equals(ModelUtils.QUERYTERM) || fqn.equals(ModelUtils.DISPLAY) || fqn.equals(ModelUtils.ID)) {
				toTable.createColumn(fqn + ".copy", col.getType(), col.isImmutable(), col.getDefaultValue());
				columns.add(fqn + ".copy");
			}
		}
		return columns;
	}

	public static void copyNodePositions(StringManager manager, CyNetwork from, CyNetwork to, 
	                                     Map<String, CyNode> nodeMap, String column) {
		CyNetworkView fromView = getNetworkView(manager, from);
		CyNetworkView toView = getNetworkView(manager, to);
		if (fromView == null || toView == null) return;
		for (View<CyNode> nodeView: fromView.getNodeViews()) {
			// Get the to node
			String nodeKey = from.getRow(nodeView.getModel()).get(column, String.class);
			if (!nodeMap.containsKey(nodeKey)) 
				continue;
			View<CyNode> toNodeView = toView.getNodeView(nodeMap.get(nodeKey));
			// Copy over the positions
			Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
			Double z = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION);
			toNodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
			toNodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			if (z != null && z != 0.0)
				toNodeView.setVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION, z);
		}
	}

	public static void copyEdges(CyNetwork fromNetwork, CyNetwork toNetwork, 
	                             Map<String, CyNode> nodeMap, String column) {
		List<String> columnsCreated = copyColumns(fromNetwork.getDefaultEdgeTable(), toNetwork.getDefaultEdgeTable());
		List<CyEdge> edgeList = fromNetwork.getEdgeList();
		for (CyEdge edge: edgeList) {
			CyNode sourceNode = edge.getSource();
			CyNode targetNode = edge.getTarget();
			boolean isDirected = edge.isDirected();

			String source = fromNetwork.getRow(sourceNode).get(column, String.class);
			String target = fromNetwork.getRow(targetNode).get(column, String.class);
			
			if (!nodeMap.containsKey(source) || !nodeMap.containsKey(target))
				continue;

			CyNode newSource = nodeMap.get(source);
			CyNode newTarget = nodeMap.get(target);

			CyEdge newEdge = toNetwork.addEdge(newSource, newTarget, isDirected);
			copyRow(fromNetwork.getDefaultEdgeTable(), toNetwork.getDefaultEdgeTable(), edge, newEdge, columnsCreated);
		}
	}

	public static CyNetworkView getNetworkView(StringManager manager, CyNetwork network) {
		Collection<CyNetworkView> views = 
						manager.getService(CyNetworkViewManager.class).getNetworkViews(network);

		// At some point, figure out a better way to do this
		for (CyNetworkView view: views) {
			return view;
		}
		return null;
	}

	public static void copyNodeAttributes(CyNetwork from, CyNetwork to, 
	                                      Map<String, CyNode> nodeMap, String column) {
		// System.out.println("copyNodeAttributes");
		List<String> columnsCreated = copyColumns(from.getDefaultNodeTable(), to.getDefaultNodeTable());
		for (CyNode node: from.getNodeList()) {
			String nodeKey = from.getRow(node).get(column, String.class);
			if (!nodeMap.containsKey(nodeKey))
				continue;
			CyNode newNode = nodeMap.get(nodeKey);
			copyRow(from.getDefaultNodeTable(), to.getDefaultNodeTable(), node, newNode, columnsCreated);
		}
	}

}
