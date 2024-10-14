package edu.ucsf.rbvi.stringApp.internal.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.CyProperty.SavePolicy;
import org.cytoscape.property.SimpleCyProperty;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

public class ModelUtils {

	// Network names
	public static String DEFAULT_NAME_STRING = "STRING network";
	public static String DEFAULT_NAME_STITCH = "STITCH network";
	public static String DEFAULT_NAME_ADDON_PHYSICAL = "(physical)";
	public static String DEFAULT_NAME_ADDON_PHYSICAL_REGEXP = " \\(physical\\)";
	
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

	public static List<String> namespacedEdgeAttributes = new ArrayList<String>(Arrays.asList("score", "interspecies", "experiments", "cooccurrence",
			"coexpression", "textmining", "databases", "neighborhood"));
	
	// Session information
	public static String showStructureImagesFlag = "showStructureImages";
	public static String showEnhancedLabelsFlag = "showEnhancedLabels";
	public static String showGlassBallEffectFlag = "showGlassBallEffect";
	public static String showFlatNodeDesignFlag = "showFlatNodeDesign";

	// Create network view size threshold
	// See https://github.com/cytoscape/cytoscape-impl/blob/develop/core-task-impl/
	// src/main/java/org/cytoscape/task/internal/loadnetwork/AbstractLoadNetworkTask.java
	public static int DEF_VIEW_THRESHOLD = 3000;
	public static String VIEW_THRESHOLD = "viewThreshold";
	
	public static int MAX_NODES_STRUCTURE_DISPLAY = 300;
	public static int MAX_NODE_PANELS = 25;
	public static int MAX_EDGE_PANELS = 25;
	
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
			if (network.getRow(node).get(ColumnNames.QUERYTERM, String.class) != null)
				return true;
		}
		return false;
	}

	public static void selectQueryTerms(CyNetwork network) {
		for (CyNode node: network.getNodeList()) {
			if (network.getRow(node).get(ColumnNames.QUERYTERM, String.class) != null)
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
		Collection<CyColumn> columns = network.getDefaultNodeTable().getColumns(ColumnNames.COMPARTMENT_NAMESPACE);
		if (columns == null || columns.size() == 0) return compartments;
		for (CyColumn col: columns) {
			compartments.add(col.getNameOnly());
		}
		return compartments;
	}

	public static List<String> getSubScoreList(CyNetwork network) {
		List<String> scores = new ArrayList<>();
		if (network == null) return scores;
		Collection<CyColumn> columns = network.getDefaultEdgeTable().getColumns(ColumnNames.STRINGDB_NAMESPACE);
		if (columns == null || columns.size() == 0) return scores;
		for (CyColumn col: columns) {
			if (col.getNameOnly().equals("score") || !col.getType().equals(Double.class))
				continue;
			scores.add(col.getNameOnly());
		}
		Collections.sort(scores);
		return scores;
	}

	public static List<String> getTissueList(CyNetwork network) {
		List<String> tissues = new ArrayList<>();
		if (network == null) {
			// System.out.println("network is null");
			return tissues;
		}
		Collection<CyColumn> columns = network.getDefaultNodeTable().getColumns(ColumnNames.TISSUE_NAMESPACE);
		if (columns == null || columns.size() == 0) return tissues;
		for (CyColumn col: columns) {
			tissues.add(col.getNameOnly());
		}
		return tissues;
	}
	
	public static Map<String, Double> getSubScores(CyNetwork network, CyEdge edge) {
		Map<String, Double> scores = new HashMap<>();
		if (network == null) return scores;
		Collection<CyColumn> columns = network.getDefaultEdgeTable().getColumns(ColumnNames.STRINGDB_NAMESPACE);
		if (columns == null || columns.size() == 0) return scores;
		for (CyColumn col: columns) {
			if (col.getNameOnly().equals("score") || !col.getType().equals(Double.class))
				continue;
			Double score = network.getRow(edge).get(col.getName(), Double.class);
			if (score != null)
				scores.put(col.getNameOnly(), score);
		}
		return scores;
	}

	public static void setConfidence(CyNetwork network, double confidence) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), Double.class, ColumnNames.CONFIDENCE);
		network.getRow(network).set(ColumnNames.CONFIDENCE, confidence);
	}

	public static Double getConfidence(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(ColumnNames.CONFIDENCE) == null)
			return null;
		return network.getRow(network).get(ColumnNames.CONFIDENCE, Double.class);
	}

	public static void setNetworkType(CyNetwork network, String networkType) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, ColumnNames.NETWORK_TYPE);
		network.getRow(network).set(ColumnNames.NETWORK_TYPE, networkType);
	}

	public static String getNetworkType(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(ColumnNames.NETWORK_TYPE) == null)
			return null;
		return network.getRow(network).get(ColumnNames.NETWORK_TYPE, String.class);
	}

	public static void setNetworkHasImages(CyNetwork network, boolean hasImages) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), Boolean.class, ColumnNames.NET_HAS_IMAGES);
		network.getRow(network).set(ColumnNames.NET_HAS_IMAGES, hasImages);
	}

	public static boolean getNetworkHasImages(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(ColumnNames.NET_HAS_IMAGES) == null)
			return false;
		return network.getRow(network).get(ColumnNames.NET_HAS_IMAGES, Boolean.class);
	}
		
	public static void updateNodeAttributes(CyRow row, Annotation annotation, boolean isQueryNode) {
		if (annotation.getAnnotation() != null) {
			row.set(ColumnNames.DESCRIPTION, annotation.getAnnotation());
		}
		if (annotation.getUniprot() != null) {
			row.set(ColumnNames.CANONICAL, annotation.getUniprot());
		}
		if (annotation.getSequence() != null) {
			row.set(ColumnNames.SEQUENCE, annotation.getSequence());
		}
		if (annotation.getImage() != null) {
			row.set(ColumnNames.IMAGE, annotation.getImage());
		}
		if (annotation.getColor() != null && isQueryNode) {
		 	row.set(ColumnNames.COLOR, annotation.getColor());
		}
		if (annotation.getUniprot() != null) {
			row.set(ColumnNames.CANONICAL, annotation.getUniprot());
		}
		if (annotation.getStructures() != null && annotation.getStructures().size() > 0) {
			row.set(ColumnNames.STRUCTURES, annotation.getStructures());
		}
	}

	// TODO: Should this be in it's own util class?
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
      createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, ColumnNames.TM_FOREGROUND);
      createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class, ColumnNames.TM_BACKGROUND);
    }

    /*
     * if (haveLinkout) { createColumnIfNeeded(network.getDefaultNodeTable(), String.class,
     * TM_LINKOUT); }
     */

    if (haveDisease)
      createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, ColumnNames.DISEASE_SCORE);
    else
      createColumnIfNeeded(network.getDefaultNodeTable(), Double.class, ColumnNames.TM_SCORE);

    for (CyNode node : network.getNodeList()) {
      CyRow row = network.getRow(node);
      String id = row.get(ColumnNames.STRINGID, String.class);
      if (resultsMap.containsKey(id)) {
        TextMiningResult result = resultsMap.get(id);
        if (result.getForeground() > 0)
          row.set(ColumnNames.TM_FOREGROUND, result.getForeground());
        if (result.getBackground() > 0)
          row.set(ColumnNames.TM_BACKGROUND, result.getBackground());
        /*
         * if (result.getLinkout() != null) row.set(TM_LINKOUT, result.getLinkout());
         */
        if (haveDisease)
          row.set(ColumnNames.DISEASE_SCORE, result.getScore());
        else
          row.set(ColumnNames.TM_SCORE, result.getScore());
        }
     }
  }


	public static String inferNetworkType(CyNetwork network) {
		int count_pp = 0;
		int count_ppp = 0; 
		int count_others = 0;
		for (CyEdge edge : network.getEdgeList()) {
			String type = network.getRow(edge).get(CyEdge.INTERACTION, String.class);
			if (type == null)
				continue;
			else if (type.equals("ppp") || type.equals("ppc"))
				count_ppp += 1;
			else if (type.equals("pp") || type.equals("pc") || type.equals("cc"))
				count_pp += 1;
			else
				count_others += 1;
		}
		if (count_ppp > (count_pp + count_others))
			return NetworkType.PHYSICAL.toString();
		else 
			return NetworkType.FUNCTIONAL.toString();
	}

	
	public static List<CyEdge> getStringNetEdges(CyNetwork network) {
		List<CyEdge> stringNetEdges = new ArrayList<CyEdge>();
		for (CyEdge edge: network.getEdgeList()) {
			// check interaction type and save all edges that are string edges
			String interactionType = network.getRow(edge).get(CyEdge.INTERACTION, String.class);
			if (interactionType == null || (!interactionType.equals("pp")
					&& !interactionType.equals("pc") && !interactionType.equals("cc")
					&& !interactionType.equals("ppp") && !interactionType.equals("ppc"))) {
				continue;
			}
			stringNetEdges.add(edge);
		}
		return stringNetEdges;
	}
	
	public static void setDatabase(CyNetwork network, String database) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, ColumnNames.DATABASE);
		network.getRow(network).set(ColumnNames.DATABASE, database);
	}

	public static String getDatabase(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(ColumnNames.DATABASE) == null)
			return null;
		return network.getRow(network).get(ColumnNames.DATABASE, String.class);
	}

	public static void setDataVersion(CyNetwork network, String dataVersion) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, ColumnNames.NET_DATAVERSION);
		network.getRow(network).set(ColumnNames.NET_DATAVERSION, dataVersion);
	}

	public static String getDataVersion(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(ColumnNames.NET_DATAVERSION) == null)
			return null;
		return network.getRow(network).get(ColumnNames.NET_DATAVERSION, String.class);
	}

	public static void setNetURI(CyNetwork network, String netURI) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, ColumnNames.NET_URI);
		network.getRow(network).set(ColumnNames.NET_URI, netURI);
	}

	public static String getNetURI(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(ColumnNames.NET_URI) == null)
			return null;
		return network.getRow(network).get(ColumnNames.NET_URI, String.class);
	}

	public static void setNetSpecies(CyNetwork network, String species) {
		createColumnIfNeeded(network.getDefaultNetworkTable(), String.class, ColumnNames.NET_SPECIES);
		network.getRow(network).set(ColumnNames.NET_SPECIES, species);
	}

	public static String getNetSpecies(CyNetwork network) {
		if (network.getDefaultNetworkTable().getColumn(ColumnNames.NET_SPECIES) == null)
			return null;
		return network.getRow(network).get(ColumnNames.NET_SPECIES, String.class);
	}

	public static String getNodeSpecies(CyNetwork network, CyNode node) {
		if (network.getDefaultNodeTable().getColumn(ColumnNames.SPECIES) == null)
			return null;
		return network.getRow(node).get(ColumnNames.SPECIES, String.class);
	}

	public static String getMostCommonNetSpecies(CyNetwork net) {
		Map<String, Integer> species = new HashMap<String, Integer>();
		for (CyNode node : net.getNodeList()) {
			String nSpecies = net.getRow(node).get(ColumnNames.SPECIES, String.class);
			if (nSpecies == null || nSpecies.equals(""))
				continue;
			if (!species.containsKey(nSpecies)) {
				species.put(nSpecies, Integer.valueOf(1));
			} else {
				int count = species.get(nSpecies).intValue() + 1;
				species.put(nSpecies, Integer.valueOf(count));
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
			String nSpecies = net.getRow(node).get(ColumnNames.SPECIES, String.class);
			if (nSpecies != null && !nSpecies.equals("") && !species.contains(nSpecies))
				species.add(nSpecies);
		}
		return species;
	}

	public static List<CyColumn> getGroupColumns(CyNetwork network) {
		Collection<CyColumn> colList = network.getDefaultNodeTable().getColumns();
		colList.remove(network.getDefaultNodeTable().getColumn(CyNetwork.SELECTED));
		colList.remove(network.getDefaultNodeTable().getColumn(CyNetwork.SUID));
		colList.removeAll(network.getDefaultNodeTable().getColumns(ColumnNames.STRINGDB_NAMESPACE));
		colList.removeAll(network.getDefaultNodeTable().getColumns(ColumnNames.TISSUE_NAMESPACE));
		colList.removeAll(network.getDefaultNodeTable().getColumns(ColumnNames.COMPARTMENT_NAMESPACE));
		colList.removeAll(network.getDefaultNodeTable().getColumns("target"));
		List<CyColumn> showList = new ArrayList<CyColumn>();
		int numValues = network.getNodeCount();
		for (CyColumn col : colList) {
			Set<?> colValues = new HashSet<>();			 
			if (col.getType().equals(String.class)) {
				colValues = new HashSet<String>(col.getValues(String.class));
			} else if (col.getType().equals(Integer.class)) {
				colValues = new HashSet<Integer>(col.getValues(Integer.class));
			} else if (col.getType().equals(Boolean.class)) {
				colValues = new HashSet<Boolean>(col.getValues(Boolean.class));
			} else if (col.getType().equals(Double.class)) {
				colValues = new HashSet<Double>(col.getValues(Double.class));
			}
			// skip column if it only contains unique values or only one value or unique values for more than half the nodes in the network 
			// filter for empty strings -> maybe enough to put a cutoff here?
			if (colValues.size() < 2 || colValues.size() == numValues || colValues.size() > numValues/2) {
				continue;
			}
			showList.add(col);
		}
		// sort attribute list
		Collections.sort(showList, new Comparator<CyColumn>() {
		    public int compare(CyColumn a, CyColumn b) {
		        return a.getName().compareToIgnoreCase(b.getName());
		    }
		});
		return showList;
	}
	
	public static void fetchImages(CyNetwork network) {
		fetchImages(network, network.getNodeList());
	}

	public static void fetchImages(CyNetwork network, List<CyNode> nodes) {
		// long startTime = System.nanoTime();

		// TODO: Do we need to catch possible exceptions here?
		int numThreads = Runtime.getRuntime().availableProcessors(); // Number of available CPU cores
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
		
		for (CyNode node : nodes) {
			Runnable task = () -> fetchImage(network, node);
		    executorService.submit(task);
		}
				
		executorService.shutdown();
		// long currentTime = System.nanoTime();
		// System.out.println("fetched images in " + (currentTime - startTime)/6000000000.0 + " sec");
		setNetworkHasImages(network, true);
	}

	public static void fetchImage(CyNetwork network, CyNode node) {
		CyRow row = network.getRow(node);
		// if image already exists, don't fetch it
		String existingImage = row.get(ColumnNames.STYLE, String.class);
		if (existingImage != null && !existingImage.equals("") && existingImage.contains("image/png;base64")) 
			return;
		String imageURL = row.get(ColumnNames.IMAGE, String.class);
		if (imageURL == null || imageURL.equals("")) {
			// ignore
			return;
		}
		String encodedImage = "string:data:image/png;base64,";
		try {
			URL url = new URL(imageURL);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream is = null;
			try {
			  is = url.openStream ();
			  byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
			  int n;

			  while ( (n = is.read(byteChunk)) > 0 ) {
			    baos.write(byteChunk, 0, n);
			  }
			}
			catch (IOException e) {
			  System.err.printf ("Failed while reading bytes from %s: %s", url.toExternalForm(), e.getMessage());
			  e.printStackTrace ();
			  // Perform any other exception handling that's appropriate.
			} finally {
				if (is != null) { 
					is.close(); 
				}
			}
	        encodedImage += Base64.getEncoder().encodeToString(baos.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}		
		row.set(ColumnNames.STYLE, encodedImage);
	}
	
	public static String formatForColumnNamespace(String columnName) {
		String formattedColumnName = columnName;
		if (columnName.contains("::")) {
			if (columnName.startsWith(ColumnNames.STRINGDB_NAMESPACE))
				formattedColumnName = columnName.substring(ColumnNames.STRINGDB_NAMESPACE.length() + 2);
			else
				formattedColumnName = columnName.replaceFirst("::", " ");
		}
		return formattedColumnName;
	}
	
	public static boolean isMergedStringNetwork(CyNetwork network) {
		CyTable nodeTable = network.getDefaultNodeTable();
		if (nodeTable.getColumn(ColumnNames.ID) == null)
			return false;
		// Enough to check for id in the node columns and score in the edge columns
		//if (nodeTable.getColumn(SPECIES) == null)
		//	return false;
		//if (nodeTable.getColumn(CANONICAL) == null)
		//	return false;
		CyTable edgeTable = network.getDefaultEdgeTable();
		if (edgeTable.getColumn(ColumnNames.SCORE) == null && edgeTable.getColumn(ColumnNames.SCORE_NO_NAMESPACE) == null)
			return false;
		return true;
	}

	public static boolean isStringNetwork(CyNetwork network) {
		// This is a string network only if we have a confidence score in the network table,
		// "@id" column in the node table, and a "score" column in the edge table
		if (network == null || network.getRow(network).get(ColumnNames.CONFIDENCE, Double.class) == null)
			return false;
		return isMergedStringNetwork(network);
	}
	
	// This method will tell us if we have the new side panel functionality (i.e. namespaces)
	public static boolean ifHaveStringNS(CyNetwork network) {
		if (network == null) return false;
		CyRow netRow = network.getRow(network);
		Collection<CyColumn> columns = network.getDefaultNodeTable().getColumns(ColumnNames.STRINGDB_NAMESPACE);
		if (netRow.isSet(ColumnNames.CONFIDENCE) && netRow.isSet(ColumnNames.NET_SPECIES) && columns != null && columns.size() > 0)
			return true;
		return false;
	}

	public static boolean isCurrentDataVersion(CyNetwork network) {
		if (network == null || network.getRow(network).get(ColumnNames.NET_DATAVERSION, String.class) == null
				|| !network.getRow(network).get(ColumnNames.NET_DATAVERSION, String.class)
						.equals(StringManager.DATAVERSION))
			return false;
		return true;
	}

	public static boolean isStitchNetwork(CyNetwork network) {
		if (network == null || network.getDefaultNodeTable().getColumn(ColumnNames.SMILES) == null)
			return false;
		return true;
	}

	public static String getExisting(CyNetwork network) {
		StringBuilder str = new StringBuilder();
		for (CyNode node : network.getNodeList()) {
			String stringID = network.getRow(node).get(ColumnNames.STRINGID, String.class);
			if (stringID != null && stringID.length() > 0)
				str.append(stringID + "\n");
		}
		return str.toString();
	}

	public static String getSelected(CyNetwork network, View<CyNode> nodeView) {
		StringBuilder selectedStr = new StringBuilder();
		if (nodeView != null) {
			String stringID = network.getRow(nodeView.getModel()).get(ColumnNames.STRINGID, String.class);
			selectedStr.append(stringID + "\n");
		}

		for (CyNode node : network.getNodeList()) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				String stringID = network.getRow(node).get(ColumnNames.STRINGID, String.class);
				if (stringID != null && stringID.length() > 0)
					selectedStr.append(stringID + "\n");
			}
		}
		return selectedStr.toString();
	}

	public static boolean isCompound(CyNetwork net, CyNode node) {
		if (net == null || node == null)
			return false;

		String type = net.getRow(node).get(ColumnNames.TYPE, String.class);
		return type != null && type.equals("compound");
	}

	public static String getType(String id) {
		// Get the namespace
		String namespace = id.substring(0, id.indexOf(":"));
		if (namespace.equals("stringdb"))
			return "protein";
		if (namespace.equals("stitchdb"))
			return "compound";
		return "unknown";
	}

	public static Double makeDouble(Object v) {
		if (v instanceof Long)
			return ((Long)v).doubleValue();
		if (v instanceof Double)
			return (Double)v;
		return null;
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
		return getString(network, ident, ColumnNames.DISPLAY);
	}

	public static String getString(CyNetwork network, CyIdentifiable ident, String column) {
		// System.out.println("network = "+network+", ident = "+ident+" column = "+column);
		try {
			if (network.getRow(ident, CyNetwork.DEFAULT_ATTRS) != null)
				return network.getRow(ident, CyNetwork.DEFAULT_ATTRS).get(column, String.class);
		} catch (Exception ex) {
			// ignore
		}
		return null;
	}

	public static Double getDouble(CyNetwork network, CyIdentifiable ident, String column) {
		try {
			if (network.getRow(ident, CyNetwork.DEFAULT_ATTRS) != null)
				return network.getRow(ident, CyNetwork.DEFAULT_ATTRS).get(column, Double.class);
		} catch (Exception ex) {
			// ignore
		}
		return null;
	}

	public static List<String> getNetworkSpeciesTaxons(CyNetwork network) {
		List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		if (nodes == null || nodes.size() == 0)
			nodes = network.getNodeList();
		List<String> netSpeciesNames = new ArrayList<String>();
		for (CyNode node : nodes) {
			final String species = network.getRow(node).get(ColumnNames.SPECIES, String.class);
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
		if (Species.getSpecies(netSp).isCustom())
			return availableTypes;
		availableTypes.add(COMPOUND);
		Set<String> spPartners = new TreeSet<String>();
		for (String sp : species) {
			List<String> partners = Species.getSpeciesPartners(sp);
			for (String spPartner : partners) {
				if (!species.contains(spPartner)) 
					spPartners.add(spPartner);
			}
		}
		if (spPartners.size() > 0) {
			availableTypes.add(EMPTYLINE);
			availableTypes.addAll(spPartners);
		}
		return availableTypes;
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
				row.set(ColumnNames.FULLNAME, row.get(ColumnNames.DISPLAY, String.class));
				row.set(ColumnNames.DISPLAY, shortName);
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
						row.set(ColumnNames.FULLNAME, row.get(ColumnNames.DISPLAY, String.class));
						row.set(ColumnNames.DISPLAY, shortName);
						// del full_names[cid]
						found = true;
						break;
					}
				}
				
			}
		}
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
			if (col.getName().equals(CyNetwork.SELECTED)) 
				continue;
			if (from.getClass().equals(CyNode.class) && col.getName().equals(CyRootNetwork.SHARED_NAME)) 
				continue;
			if (col.getName().equals(ColumnNames.QUERYTERM) || col.getName().equals(ColumnNames.DISPLAY) || col.getName().equals(ColumnNames.ID) || col.getName().equals(CyNetwork.NAME)) {
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

	public static void copyNodes(CyNetwork fromNetwork, CyNetwork toNetwork, Map<CyNode, CyNode> nodeMap, 
	                             String keyColumn, List<String> toColumns) {
		for (CyNode node: fromNetwork.getNodeList()) {
			String key = fromNetwork.getRow(node).get(keyColumn, String.class);
			// TODO: double-check what happens when key == null
			if (!nodeMap.containsKey(node)) {
				CyNode newNode = toNetwork.addNode();
				nodeMap.put(node, newNode);
				String name = fromNetwork.getRow(node).get(keyColumn, String.class);
				toNetwork.getRow(newNode).set(CyNetwork.NAME, name);
				for (String col: toColumns) {
					toNetwork.getRow(newNode).set(col, key);
				}
			}
		}
	}

	public static void createNodeMap(CyNetwork fromNetwork, CyNetwork toNetwork, Map<CyNode, CyNode> nodeMap, 
	                                 String column, String targetColumn) {
		Map<String, CyNode> keyMap = new HashMap<String, CyNode>();
		// Get all of the nodes in the new network
		for (CyNode node: toNetwork.getNodeList()) {
			String key = toNetwork.getRow(node).get(targetColumn, String.class);
			keyMap.put(key, node);
		}

		for (CyNode node: fromNetwork.getNodeList()) {
			String keyColumn = fromNetwork.getRow(node).get(column, String.class);
			if (keyMap.containsKey(keyColumn))
				nodeMap.put(node, keyMap.get(keyColumn));
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
			} else if (fqn.equals(ColumnNames.QUERYTERM) || fqn.equals(ColumnNames.DISPLAY) || fqn.equals(ColumnNames.ID) || fqn.equals(CyNetwork.NAME)) {
				toTable.createColumn(fqn + ".copy", col.getType(), col.isImmutable(), col.getDefaultValue());
				columns.add(fqn + ".copy");
			}
		}
		return columns;
	}

	public static void copyNodePositions(StringManager manager, CyNetwork from, CyNetwork to, 
	                                     Map<CyNode, CyNode> nodeMap, String column) {
		CyNetworkView fromView = getNetworkView(manager, from);
		CyNetworkView toView = getNetworkView(manager, to);
		if (fromView == null || toView == null) return;
		for (View<CyNode> nodeView: fromView.getNodeViews()) {
			// Get the to node
			CyNode fromNode = nodeView.getModel();
			if (!nodeMap.containsKey(fromNode))
				continue;
			View<CyNode> toNodeView = toView.getNodeView(nodeMap.get(fromNode));
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

	public static void overwriteDisplayName(StringManager manager, CyNetwork from, CyNetwork to,
										Map<CyNode, CyNode> nodeMap, String column) {
		for (CyNode fromNode: from.getNodeList()) {
			if (!nodeMap.containsKey(fromNode))
				continue;
			CyNode toNode = nodeMap.get(fromNode);
			String name_toNode = (String) to.getDefaultNodeTable().getRow(toNode.getSUID()).getRaw(ColumnNames.DISPLAY);
			if (name_toNode == null || name_toNode.equals("")) {
				String name_fromNode = (String) from.getDefaultNodeTable().getRow(fromNode.getSUID()).getRaw(column);
				to.getDefaultNodeTable().getRow(toNode.getSUID()).set(ColumnNames.DISPLAY, name_fromNode);
			}
		}
	}
	
	public static void copyEdges(CyNetwork fromNetwork, CyNetwork toNetwork, 
	                             Map<CyNode, CyNode> nodeMap, String column) {
		List<String> columnsCreated = copyColumns(fromNetwork.getDefaultEdgeTable(), toNetwork.getDefaultEdgeTable());
		List<CyEdge> edgeList = fromNetwork.getEdgeList();
		for (CyEdge edge: edgeList) {
			CyNode sourceNode = edge.getSource();
			CyNode targetNode = edge.getTarget();
			boolean isDirected = edge.isDirected();

			if (!nodeMap.containsKey(sourceNode) || !nodeMap.containsKey(targetNode))
				continue;

			CyNode newSource = nodeMap.get(sourceNode);
			CyNode newTarget = nodeMap.get(targetNode);

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
	                                      Map<CyNode, CyNode> nodeMap, String column) {
		// System.out.println("copyNodeAttributes");
		List<String> columnsCreated = copyColumns(from.getDefaultNodeTable(), to.getDefaultNodeTable());
		for (CyNode node: from.getNodeList()) {
			if (!nodeMap.containsKey(node))
				continue;
			CyNode newNode = nodeMap.get(node);
			copyRow(from.getDefaultNodeTable(), to.getDefaultNodeTable(), node, newNode, columnsCreated);
		}
	}

	public static List<String> getProteinNodes(CyNetwork network) {
		List<String> proteins = new ArrayList<String>();
		for (CyNode node: network.getNodeList()) {
			String type = network.getRow(node).get(ColumnNames.TYPE, String.class);
			if (type != null && type.equals("protein"))
				proteins.add(network.getRow(node).get(CyNetwork.NAME, String.class));
		}
		return proteins;
	}

	public static List<String> getCompoundNodes(CyNetwork network) {
		List<String> compounds = new ArrayList<String>();
		for (CyNode node: network.getNodeList()) {
			String type = network.getRow(node).get(ColumnNames.TYPE, String.class);
			if (type != null && type.equals("compound"))
				compounds.add(network.getRow(node).get(CyNetwork.NAME, String.class));
		}
		return compounds;
	}

}
