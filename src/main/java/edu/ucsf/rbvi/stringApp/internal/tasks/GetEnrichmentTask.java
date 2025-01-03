package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.command.StringToModel;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.task.analyze.AnalyzeNetworkCollectionTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.EnrichmentSAXHandler;
import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.EnrichmentUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.JSONUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetEnrichmentTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	final CyNetwork network;
	final CyNetworkView netView;
	final boolean publOnly;
	Map<String, List<EnrichmentTerm>> enrichmentResult;
	final Map<String, Long> stringNodesMap;
	final Map<String, CyNetwork> stringNetworkMap;
	final ShowEnrichmentPanelTaskFactory showFactoryEnrich;
	final ShowPublicationsPanelTaskFactory showFactoryPubl;
	private Map<String, String> ppiSummary;
	List<CyNode> analyzedNodes;
	String selected;
	TaskMonitor monitor;
	// boolean guiMode;

	// not needed since the API returns only values with fdr < 0.05
	// @Tunable(description = "Enrichment FDR value cutoff", 
	//         longDescription = "Sets the false discovery rate (FDR) value cutoff a term must reach in order to be included",
	//				 exampleStringValue = "0.05",
	//				 gravity = 1.0)
	// public double cutoff = 0.05;

	@Tunable(description="Network to be used as background"
		// longDescription = StringToModel.CY_NETWORK_VIEW_LONG_DESCRIPTION,
		// exampleStringValue = StringToModel.CY_NETWORK_VIEW_EXAMPLE_STRING,
	        )
	public ListSingleSelection<String> background = null;

	@Tunable(description = "Retrieve for selected nodes only",
	         longDescription = "Setting this to ```true``` and selecting a subset of the nodes will retrieve enrichment "+
	                           "only for the selected nodes.  If this is ```false``` enrichment will be retrieved for "+
	                           "all nodes in the network",
					 exampleStringValue = "false",
					 gravity = 2.0)
	public boolean selectedNodesOnly = true;
	
	@Tunable(description = "Retrieve for species", gravity = 3.0)
	public ListSingleSelection<String> allNetSpecies = new ListSingleSelection<String>();
	
	public CyTable enrichmentTable = null;

	public GetEnrichmentTask(StringManager manager, CyNetwork network, CyNetworkView netView,
			ShowEnrichmentPanelTaskFactory showEnrichmentFactory, ShowPublicationsPanelTaskFactory showFactoryPubl, boolean publOnly) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;

		this.showFactoryEnrich = showEnrichmentFactory;
		this.showFactoryPubl = showFactoryPubl;
		this.publOnly = publOnly;
		enrichmentResult = new HashMap<>();
		stringNodesMap = new HashMap<>();
		monitor = null;
		// Get list of (selected) nodes as a string!
		selected = getSelected(network).trim();
		if (selected.split("\n").length <= 1) {
		 	selectedNodesOnly = false;
		}
		List<String> speciesInNetwork = EnrichmentUtils.getEnrichmentNetSpecies(network);
		allNetSpecies = new ListSingleSelection<String>(speciesInNetwork);
		allNetSpecies.setSelectedValue(speciesInNetwork.get(0));

		stringNetworkMap = new HashMap<>();

		List<String> netList = new ArrayList<>();
		netList.add("genome");
		for (StringNetwork sn: manager.getStringNetworks()) {
			CyNetwork net = sn.getNetwork();
			String name = ModelUtils.getName(net, net);
			netList.add(name);
			stringNetworkMap.put(name, net);
		}

		background = new ListSingleSelection<String>(netList);
		// genome is always the default
		background.setSelectedValue("genome");

	}

	public void run(TaskMonitor monitor) throws Exception {
		this.monitor = monitor;
		monitor.setTitle(this.getTitle());

		if (!ModelUtils.isCurrentDataVersion(network)) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. Network appears to be an old STRING network.");
			// showError("Task cannot be performed. Network appears to be an old STRING network.");
			return;			
		}
		
		if (!selectedNodesOnly)
			selected = getExisting(network).trim();
		/*
		if (selected.length() == 0 && !selectedNodesOnly) {
			selected = getExisting(network).trim();
		}
		*/
		
		if (selected.length() == 0) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. No nodes selected for enrichment.");
			showError("Task cannot be performed. No nodes selected for enrichment.");
			return;
		} 

		// get background nodes
		String bgNodes = null;
		if (!background.getSelectedValue().equals("genome")) {
			CyNetwork bgNetwork = stringNetworkMap.get(background.getSelectedValue());
			if (analyzedNodes.containsAll(bgNetwork.getNodeList())) {
				monitor.showMessage(Level.ERROR,
						"Task cannot be performed. Nodes from the foreground are the same as the background.");
				showError("Task cannot be performed. Nodes from the foreground are the same as the background.");
				return;
			}
			bgNodes = getBackground(bgNetwork, network);
			if (bgNodes.equals("")) {
				monitor.showMessage(Level.ERROR,
						"Task cannot be performed. Nodes from the foreground are missing in the background.");
				showError("Task cannot be performed. Nodes from the foreground are missing in the background.");
				return;
			}
		}

		// define large networks and skip ppi enrichment for them
		boolean isLargeNetwork = false;
		if (analyzedNodes.size() > 2000) {
			isLargeNetwork = true;
			// Two ways to report errors.  If the user hasn't selected any nodes, then
			// suggest that the user selects some.  If they have selected some nodes,
			// then tell them that they need to select fewer.
			/*if (CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true).size() > 0) {
				monitor.showMessage(Level.ERROR, "Selection has too many nodes. Enrichment can be retrieved only for at most 2000 proteins.");
				showError("Selection has too many nodes. Enrichment can be retrieved only for at most 2000 proteins.");
			} else {
				monitor.showMessage(Level.ERROR, "Enrichment can be retrieved only for at most 2000 proteins. Please select fewer nodes.");
				showError("Enrichment can be retrieved only for at most 2000 proteins. Please select fewer nodes.");
			}
			return;*/
		}
		// System.out.println(selected);
		// List<String> netSpecies = ModelUtils.getNetworkSpeciesTaxons(network);
		Species selSpecies = Species.getSpecies(allNetSpecies.getSelectedValue());
		String species = String.valueOf(selSpecies.getTaxId());
		if (selSpecies.isCustom())
			species = selSpecies.toString();
		//if (netSpecies.size() == 1) {
		//	species = netSpecies.get(0);
		//} else {
		//	monitor.showMessage(Level.ERROR,
		//			"Task cannot be performed. Enrichment can be retrieved for networks that contain nodes from one species only.");
		//	return;
		// }

		// map of STRING ID to CyNodes
		// TODO: Remove specific nodes from selected?
		CyTable nodeTable = network.getDefaultNodeTable();
		for (final CyNode node : network.getNodeList()) {
			if (nodeTable.getColumn(ColumnNames.STRINGID) != null) {
				String stringid = nodeTable.getRow(node.getSUID()).get(ColumnNames.STRINGID,
						String.class);
				if (stringid != null) {
					stringNodesMap.put(stringid, node.getSUID());
				}
			}
		}

		// clear old results
		// TODO: [N] test deletion of old tables
		EnrichmentUtils.deleteMainEnrichmentTables(network, manager, publOnly);

		// retrieve enrichment (new API)
		getEnrichmentJSON(selected, species, bgNodes);
		
		// retrieve PPI enrichment
		if (!isLargeNetwork && !publOnly) 
			ppiSummary = getEnrichmentPPIJSON(selected, species, bgNodes);
		else
			ppiSummary = null;

		// save analyzed nodes in network table
		CyTable netTable = network.getDefaultNetworkTable();
		// create list of anlayzed nodes
		List<Long> analyzedNodesSUID = new ArrayList<Long>();
		for (CyNode node : analyzedNodes) {
			analyzedNodesSUID.add(node.getSUID());
		}
		// save into settings table for functional enrichment and into the network table for publications
		if (!publOnly) {
			String defaultGroup = TermCategory.ALL.getTable();
			// Add enrichment table All to the groups
			ModelUtils.createListColumnIfNeeded(netTable, String.class, ColumnNames.NET_ENRICHMENT_TABLES);
			List<String> enrichmentGroups = netTable.getRow(network.getSUID()).getList(ColumnNames.NET_ENRICHMENT_TABLES, String.class);
			if (enrichmentGroups == null) 
				enrichmentGroups = new ArrayList<String>();
			if (!enrichmentGroups.contains(defaultGroup))
				enrichmentGroups.add(defaultGroup);
			netTable.getRow(network.getSUID()).set(ColumnNames.NET_ENRICHMENT_TABLES, enrichmentGroups);
	
			// Create settings table if needed and save its SUID in the network table
			ModelUtils.createColumnIfNeeded(netTable, Long.class, ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE_SUID);
			CyTable settignsTable = EnrichmentUtils.getEnrichmentSettingsTable(manager, network);
			netTable.getRow(network.getSUID()).set(ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE_SUID, settignsTable.getSUID());
			// Save analyzed nodes into the new settings table
			ModelUtils.createListColumnIfNeeded(settignsTable, Long.class, ColumnNames.NET_ANALYZED_NODES);
			settignsTable.getRow(defaultGroup).set(ColumnNames.NET_ANALYZED_NODES, analyzedNodesSUID);
		} else {
			ModelUtils.createListColumnIfNeeded(netTable, Long.class, ColumnNames.NET_ANALYZED_NODES_PUBL);
			netTable.getRow(network.getSUID()).set(ColumnNames.NET_ANALYZED_NODES_PUBL, analyzedNodesSUID);
		}
			
		// save ppi enrichment in network table
		if (ppiSummary != null) {
			writeDouble(netTable, ppiSummary, ColumnNames.NET_PPI_ENRICHMENT);
			writeInteger(netTable, ppiSummary, ColumnNames.NET_ENRICHMENT_NODES);
			writeInteger(netTable, ppiSummary, ColumnNames.NET_ENRICHMENT_EXPECTED_EDGES);
			writeInteger(netTable, ppiSummary, ColumnNames.NET_ENRICHMENT_EDGES);
			writeDouble(netTable, ppiSummary, ColumnNames.NET_ENRICHMENT_CLSTR);
			writeDouble(netTable, ppiSummary, ColumnNames.NET_ENRICHMENT_DEGREE);
		} else {
			ModelUtils.deleteColumnIfExisting(netTable, ColumnNames.NET_PPI_ENRICHMENT);
			ModelUtils.deleteColumnIfExisting(netTable, ColumnNames.NET_ENRICHMENT_NODES);
			ModelUtils.deleteColumnIfExisting(netTable, ColumnNames.NET_ENRICHMENT_EXPECTED_EDGES);
			ModelUtils.deleteColumnIfExisting(netTable, ColumnNames.NET_ENRICHMENT_EDGES);
			ModelUtils.deleteColumnIfExisting(netTable, ColumnNames.NET_ENRICHMENT_CLSTR);
			ModelUtils.deleteColumnIfExisting(netTable, ColumnNames.NET_ENRICHMENT_DEGREE);
		}
		
		// show enrichment results
		if (enrichmentResult == null) {
			return;
		} else if (enrichmentResult.size() == 0) {
			// this should not happen anymore
			monitor.showMessage(Level.WARN,
					"Enrichment retrieval returned no results that met the criteria.");
		}
		SynchronousTaskManager<?> taskM = manager.getService(SynchronousTaskManager.class);
		if (showFactoryPubl != null && publOnly) {
			//TaskIterator ti = showFactoryPubl.createTaskIterator();
			//taskM.execute(ti);
			manager.showPublicationPanel();
		}
		if (showFactoryEnrich != null && !publOnly) {
			//TaskIterator ti = showFactoryEnrich.createTaskIterator();
			//taskM.execute(ti);
			manager.showEnrichmentPanel();
		} 
	}

	private void writeDouble(CyTable table, Map<String, String> data, String column) {
		ModelUtils.createColumnIfNeeded(table, Double.class, column);
		if (data.containsKey(column)) {
			Double v = Double.parseDouble(data.get(column));
			table.getRow(network.getSUID()).set(column, v);
		}
	}
	private void writeInteger(CyTable table, Map<String, String> data, String column) {
		ModelUtils.createColumnIfNeeded(table, Integer.class, column);
		if (data.containsKey(column)) {
			Integer v = Integer.parseInt(data.get(column));
			table.getRow(network.getSUID()).set(column, v);
		}
	}

	private void getEnrichmentJSON(String selected, String species, String backgroundNodes) {
		Map<String, String> args = new HashMap<String, String>();
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/enrichment";
		args.put("identifiers", selected);
		args.put("species", species);
		args.put("caller_identity", StringManager.CallerIdentity);
		if (backgroundNodes != null) {
			args.put("background_string_identifiers", backgroundNodes);
		}
		JSONObject results;
		try {
			results = HttpUtils.postJSON(url, args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			enrichmentResult = null;
			return;
		}
		if (results == null) {
			monitor.showMessage(Level.ERROR,
					"Enrichment retrieval returned no results, possibly due to an error.");
			enrichmentResult = null;
			return;
			// throw new RuntimeException("Enrichment retrieval returned no results, possibly due to an error.");
		}
		List<EnrichmentTerm> terms = JSONUtils.getEnrichmentFromJSON(manager, results, stringNodesMap, network);
		if (terms == null) {
			String errorMsg = JSONUtils.getErrorMessageFromJSON(manager, results);
			monitor.showMessage(Level.ERROR,
					"Enrichment retrieval returned no results, possibly due to an error. " + errorMsg);
			enrichmentResult = null;
			return;
			// throw new RuntimeException("Enrichment retrieval returned no results, possibly due to an error. " + errorMsg);
		} else {
			Collections.sort(terms);
			// separate terms into all and pmid
			List<EnrichmentTerm> termsAll = new ArrayList<EnrichmentTerm>();
			List<EnrichmentTerm> termsPubl = new ArrayList<EnrichmentTerm>();
			for (EnrichmentTerm term : terms) {
				// System.out.println(term.getCategory());
				if (term.getCategory().equals(TermCategory.PMID.getName())) {
					termsPubl.add(term);
				} else {
					termsAll.add(term);
				}
			}
			if (publOnly) {
				enrichmentResult.put(TermCategory.PMID.getKey(), termsPubl);
				saveEnrichmentTable(TermCategory.PMID.getTable(), TermCategory.PMID.getKey());				
			} else {
				enrichmentResult.put(TermCategory.ALL.getKey(), termsAll);
				saveEnrichmentTable(TermCategory.ALL.getTable(), TermCategory.ALL.getKey());
			}
			// info for the user
			if ((publOnly && termsPubl.size() == 0) || (!publOnly && termsAll.size() == 0))
				monitor.showMessage(Level.WARN, "Enrichment retrieval returned no results that met the criteria.");
			else
				monitor.showMessage(Level.INFO, "Enrichment retrieval successful.");
		}
	}

	private Map<String, String> getEnrichmentPPIJSON(String selected, String species, String backgroundNodes) {
		Map<String, String> args = new HashMap<String, String>();
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/ppi_enrichment";
		args.put("identifiers", selected);
		args.put("species", species);
		if (ModelUtils.getConfidence(network) == null) {
			monitor.showMessage(Level.WARN,
					"PPI enrichment cannot be retrieved because of missing confidence values.");
			return null;
		} else if ( ModelUtils.getConfidence(network).compareTo(0.999d) > 0 ) {
			monitor.showMessage(Level.WARN,
					"PPI enrichment cannot be retrieved for a network with a confidence of 1.0.");
			return null;	
		}
		// TODO: we should be sending an integer and not a float
		Double confidence = ModelUtils.getConfidence(network)*1000;
		args.put("required_score", confidence.toString());
		args.put("caller_identity", StringManager.CallerIdentity);
		if (backgroundNodes != null) {
			args.put("background_string_identifiers", backgroundNodes);
		}

		JSONObject results;
		try {
			results = HttpUtils.postJSON(url, args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return null;
		}
		if (results == null) {
			monitor.showMessage(Level.ERROR,
					"PPI enrichment retrieval returned no results, possibly due to an error.");
			// throw new RuntimeException("PPI enrichment retrieval returned no results, possibly due to an error.");
			return null;
		}
		Map<String, String> ppiEnrichment = 
						JSONUtils.getEnrichmentPPIFromJSON(manager, results, stringNodesMap, network);
		if (ppiEnrichment == null) {
			monitor.showMessage(Level.ERROR,
					"PPI Enrichment retrieval returned no results, possibly due to an error.");
			// throw new RuntimeException("PPI enrichment retrieval returned no results, possibly due to an error.");
			return null;
		}  else if (ppiEnrichment.containsKey("ErrorMessage")) {
			monitor.showMessage(Level.ERROR,
					"PPI Enrichment retrieval failed: "+ppiEnrichment.get("ErrorMessage"));
			// throw new RuntimeException("PPI Enrichment retrieval failed: "+ppiEnrichment.get("ErrorMessage"));
			return null;
		}
		return ppiEnrichment;
	}

	private void saveEnrichmentTable(String tableName, String enrichmentCategory) {
		CyTableFactory tableFactory = manager.getService(CyTableFactory.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);

		enrichmentTable = tableFactory.createTable(tableName, EnrichmentTerm.colID, Long.class, false,
				true);
		enrichmentTable.setSavePolicy(SavePolicy.SESSION_FILE);
		tableManager.addTable(enrichmentTable);
		EnrichmentUtils.setupEnrichmentTable(enrichmentTable);

		// Step 2: populate the table with some data
		List<EnrichmentTerm> processTerms = enrichmentResult.get(enrichmentCategory);
		if (processTerms == null) {
			return;
		}
		if (processTerms.size() == 0) {
			CyRow row = enrichmentTable.getRow((long) 0);
			row.set(EnrichmentTerm.colNetworkSUID, network.getSUID());
		}
		double maxFDRLogValue = EnrichmentUtils.getMaxFdrLogValue(processTerms);
		for (int i = 0; i < processTerms.size(); i++) {
			EnrichmentTerm term = processTerms.get(i);
			CyRow row = enrichmentTable.getRow((long) i);
			row.set(EnrichmentTerm.colName, term.getName());
			if (term.getName().length() > 4 && term.getName().startsWith("PMID")) {
				row.set(EnrichmentTerm.colIDPubl, term.getName().substring(5));
			} else {
				row.set(EnrichmentTerm.colIDPubl, "");
			}
			row.set(EnrichmentTerm.colYear, term.getYear());
			row.set(EnrichmentTerm.colDescription, term.getDescription());
			row.set(EnrichmentTerm.colCategory, term.getCategory());
			row.set(EnrichmentTerm.colFDR, term.getFDRPValue());
			row.set(EnrichmentTerm.colFDRTransf, -Math.log10(term.getFDRPValue())/maxFDRLogValue);
			row.set(EnrichmentTerm.colPvalue, term.getPValue());
			row.set(EnrichmentTerm.colGenesBG, term.getGenesBG());
			row.set(EnrichmentTerm.colGenesCount, term.getGenes().size());
			row.set(EnrichmentTerm.colGenes, term.getGenes());
			row.set(EnrichmentTerm.colGenesSUID, term.getNodesSUID());
			row.set(EnrichmentTerm.colNetworkSUID, network.getSUID());
			// row.set(EnrichmentTerm.colShowChart, false);
			row.set(EnrichmentTerm.colChartColor, "");
		}
		return;
	}

	protected void showError(String msg) {
	}

	private String getExisting(CyNetwork currentNetwork) {
		StringBuilder str = new StringBuilder();
		analyzedNodes = new ArrayList<CyNode>();
		for (CyNode node : currentNetwork.getNodeList()) {
			String stringID = currentNetwork.getRow(node).get(ColumnNames.STRINGID, String.class);
			String type = currentNetwork.getRow(node).get(ColumnNames.TYPE, String.class);
			if (stringID != null && stringID.length() > 0 
					&& type != null && type.equals("protein")) {
				Boolean considerForEnrichment = currentNetwork.getRow(node).get(ColumnNames.USE_ENRICHMENT, Boolean.class);
				if (considerForEnrichment != null && !considerForEnrichment.booleanValue())
					continue;
				str.append(stringID + "\n");
				analyzedNodes.add(node);
			}
		}
		return str.toString();
	}

	private String getBackground(CyNetwork bgNetwork, CyNetwork fgNetwork) {
		StringBuilder str = new StringBuilder();
		for (CyNode node : bgNetwork.getNodeList()) {
			String stringID = bgNetwork.getRow(node).get(ColumnNames.STRINGID, String.class);
			String type = bgNetwork.getRow(node).get(ColumnNames.TYPE, String.class);
			if (stringID != null && stringID.length() > 0 
					&& type != null && type.equals("protein")) {
				Boolean considerForEnrichment = bgNetwork.getRow(node).get(ColumnNames.USE_ENRICHMENT, Boolean.class);
				if (considerForEnrichment != null && !considerForEnrichment.booleanValue())
					continue;
				str.append(stringID + "\n");
			}
		}
		// check if foreground is contained in background
		for (CyNode fgNode : analyzedNodes) {
			if (str.indexOf(fgNetwork.getRow(fgNode).get(ColumnNames.STRINGID, String.class)) == -1) {
				System.out.println(fgNode.getSUID());
				return "";
			}
		}
		return str.toString();
	}

	private String getSelected(CyNetwork currentNetwork) {
		StringBuilder selectedStr = new StringBuilder();
		analyzedNodes = new ArrayList<CyNode>();
		for (CyNode node : currentNetwork.getNodeList()) {
			if (currentNetwork.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				String stringID = currentNetwork.getRow(node).get(ColumnNames.STRINGID, String.class);
				String type = currentNetwork.getRow(node).get(ColumnNames.TYPE, String.class);
				if (stringID != null && stringID.length() > 0 
						&& type != null && type.equals("protein")) {
					Boolean considerForEnrichment = currentNetwork.getRow(node).get(ColumnNames.USE_ENRICHMENT, Boolean.class);
					if (considerForEnrichment != null && !considerForEnrichment.booleanValue())
						continue;
					selectedStr.append(stringID + "\n");
					analyzedNodes.add(node);
				}
			}
		}
		return selectedStr.toString();
	}

	@ProvidesTitle
	public String getTitle() {
		if (publOnly)
			return "Retrieve enriched publications";
		return "Retrieve functional enrichment";
	}

	public static String EXAMPLE_JSON = 
					"{\"EnrichmentTable\": 101,"+
					"\""+ColumnNames.NET_PPI_ENRICHMENT+"\": 1e-16,"+
					"\""+ColumnNames.NET_ENRICHMENT_NODES+"\": 15,"+
					"\""+ColumnNames.NET_ENRICHMENT_EDGES+"\": 30,"+
					"\""+ColumnNames.NET_ENRICHMENT_EXPECTED_EDGES+"\": 57,"+
					"\""+ColumnNames.NET_ENRICHMENT_CLSTR+"\": 0.177,"+
					"\""+ColumnNames.NET_ENRICHMENT_DEGREE+"\": 2.66}";

	public static String EXAMPLE_JSON_PUBL = 
			"{\"EnrichmentTable\": 101}";

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(CyTable.class)) {
			return (R) enrichmentTable;
		} else if (clzz.equals(String.class)) {
			if (ppiSummary == null && enrichmentTable == null) 
				return (R)"No results";
			if (enrichmentTable != null)
				return (R)("\"EnrichmentTable\": "+enrichmentTable.getSUID());
			if (ppiSummary != null) {
				String result = "Enrichment results summary:";
				result = addStringResult(result, ColumnNames.NET_PPI_ENRICHMENT);
				result = addStringResult(result, ColumnNames.NET_ENRICHMENT_NODES);
				result = addStringResult(result, ColumnNames.NET_ENRICHMENT_EXPECTED_EDGES);
				result = addStringResult(result, ColumnNames.NET_ENRICHMENT_EDGES);
				result = addStringResult(result, ColumnNames.NET_ENRICHMENT_CLSTR);
				result = addStringResult(result, ColumnNames.NET_ENRICHMENT_DEGREE);
				return (R)result;
			}
		} else if (clzz.equals(Long.class)) {
			// if (ppiSummary == null) return null; 
			return (R) enrichmentTable.getSUID();
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				String result = "{";
				if (enrichmentTable != null)
					result += "\"EnrichmentTable\": "+enrichmentTable.getSUID();
				if (ppiSummary != null) {
					result = addResult(result, ColumnNames.NET_PPI_ENRICHMENT);
					result = addResult(result, ColumnNames.NET_ENRICHMENT_NODES);
					result = addResult(result, ColumnNames.NET_ENRICHMENT_EXPECTED_EDGES);
					result = addResult(result, ColumnNames.NET_ENRICHMENT_EDGES);
					result = addResult(result, ColumnNames.NET_ENRICHMENT_CLSTR);
					result = addResult(result, ColumnNames.NET_ENRICHMENT_DEGREE);
				}
				result += "}";
				// System.out.println(result);
				return result;
      };
      return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class, Long.class, CyTable.class);
	}

	private String addResult(String result, String key) {
		if (ppiSummary.containsKey(key))
			result += ", \""+key+"\": "+ppiSummary.get(key);
		return result;
	}
	private String addStringResult(String result, String key) {
		if (ppiSummary.containsKey(key)) {
			result += "\n   "+key+"="+ppiSummary.get(key);
		}
		return result;
	}


}
