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
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
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

	@Tunable(description="Network view to set enhanced labels on",
	         // longDescription = StringToModel.CY_NETWORK_VIEW_LONG_DESCRIPTION,
	         // exampleStringValue = StringToModel.CY_NETWORK_VIEW_EXAMPLE_STRING,
	         context = "nogui")
  public CyNetworkView view = null;

	// not needed since the API returns only values with fdr < 0.05
	// @Tunable(description = "Enrichment FDR value cutoff", 
	//         longDescription = "Sets the false discovery rate (FDR) value cutoff a term must reach in order to be included",
	//				 exampleStringValue = "0.05",
	//				 gravity = 1.0)
	// public double cutoff = 0.05;

	@Tunable(description="Background"
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
		if (view != null) {
			this.netView = view;
			this.network = view.getModel();
		} else {
			this.network = network;
			this.netView = netView;
		}

		this.showFactoryEnrich = showEnrichmentFactory;
		this.showFactoryPubl = showFactoryPubl;
		this.publOnly = publOnly;
		enrichmentResult = new HashMap<>();
		stringNodesMap = new HashMap<>();
		monitor = null;
		// Get list of (selected) nodes
		selected = getSelected(network).trim();
		if (selected.length() <= 1) {
		 	selectedNodesOnly = false;
		}
		allNetSpecies = new ListSingleSelection<String>(ModelUtils.getEnrichmentNetSpecies(network));

		stringNetworkMap = new HashMap<>();

		List<String> netList = new ArrayList<>();
		netList.add("genome");
		for (StringNetwork sn: manager.getStringNetworks()) {
			CyNetwork net = sn.getNetwork();
			String name = ModelUtils.getName(net, net);
			netList.add(name);
			stringNetworkMap.put(name, net);
		}

		background = new ListSingleSelection(netList);
		// genome is always the default
		background.setSelectedValue("genome");

	}

	public void run(TaskMonitor monitor) throws Exception {
		this.monitor = monitor;
		monitor.setTitle(this.getTitle());

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
		String species = String.valueOf(Species.getSpeciesTaxId(allNetSpecies.getSelectedValue()));
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
			if (nodeTable.getColumn(ModelUtils.STRINGID) != null) {
				String stringid = nodeTable.getRow(node.getSUID()).get(ModelUtils.STRINGID,
						String.class);
				if (stringid != null) {
					stringNodesMap.put(stringid, node.getSUID());
				}
			}
		}

		// clear old results
		ModelUtils.deleteEnrichmentTables(network, manager);

		// retrieve enrichment (new API)
		getEnrichmentJSON(selected, species, background.getSelectedValue());
		if (!isLargeNetwork && !publOnly) 
			ppiSummary = getEnrichmentPPIJSON(selected, species, background.getSelectedValue());
		else
			ppiSummary = null;

		// save analyzed nodes in network table
		CyTable netTable = network.getDefaultNetworkTable();
		ModelUtils.createListColumnIfNeeded(netTable, Long.class, ModelUtils.NET_ANALYZED_NODES);
		List<Long> analyzedNodesSUID = new ArrayList<Long>();
		for (CyNode node : analyzedNodes) {
			analyzedNodesSUID.add(node.getSUID());
		}
		netTable.getRow(network.getSUID()).set(ModelUtils.NET_ANALYZED_NODES, analyzedNodesSUID);

		// save ppi enrichment in network table
		if (ppiSummary != null) {
			writeDouble(netTable, ppiSummary, ModelUtils.NET_PPI_ENRICHMENT);
			writeInteger(netTable, ppiSummary, ModelUtils.NET_ENRICHMENT_NODES);
			writeInteger(netTable, ppiSummary, ModelUtils.NET_ENRICHMENT_EXPECTED_EDGES);
			writeInteger(netTable, ppiSummary, ModelUtils.NET_ENRICHMENT_EDGES);
			writeDouble(netTable, ppiSummary, ModelUtils.NET_ENRICHMENT_CLSTR);
			writeDouble(netTable, ppiSummary, ModelUtils.NET_ENRICHMENT_DEGREE);
		} else {
			ModelUtils.deleteColumnIfExisting(netTable, ModelUtils.NET_PPI_ENRICHMENT);
			ModelUtils.deleteColumnIfExisting(netTable, ModelUtils.NET_ENRICHMENT_NODES);
			ModelUtils.deleteColumnIfExisting(netTable, ModelUtils.NET_ENRICHMENT_EXPECTED_EDGES);
			ModelUtils.deleteColumnIfExisting(netTable, ModelUtils.NET_ENRICHMENT_EDGES);
			ModelUtils.deleteColumnIfExisting(netTable, ModelUtils.NET_ENRICHMENT_CLSTR);
			ModelUtils.deleteColumnIfExisting(netTable, ModelUtils.NET_ENRICHMENT_DEGREE);
		}
		
		// show enrichment results
		boolean noSig = false;
		if (enrichmentResult == null) {
			return;
		} else if (enrichmentResult.size() == 0) {
			noSig = true;
			monitor.showMessage(Level.WARN,
					"Enrichment retrieval returned no results that met the criteria.");
		}
		SynchronousTaskManager<?> taskM = manager.getService(SynchronousTaskManager.class);
		if (showFactoryPubl != null && publOnly) {
			TaskIterator ti = showFactoryPubl.createTaskIterator(true, noSig);
			taskM.execute(ti);
		}
		if (showFactoryEnrich != null && !publOnly) {
			TaskIterator ti = showFactoryEnrich.createTaskIterator(true, noSig);
			taskM.execute(ti);
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

	private void getEnrichmentJSON(String selected, String species, String backgroundNetwork) {
		Map<String, String> args = new HashMap<String, String>();
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/enrichment";
		args.put("identifiers", selected);
		args.put("species", species);
		args.put("caller_identity", StringManager.CallerIdentity);
		if (!backgroundNetwork.equals("genome")) {
			String bg = getExisting(stringNetworkMap.get(backgroundNetwork));
			args.put("background_string_identifiers", bg);
		}
		JSONObject results = HttpUtils.postJSON(url, args, manager);
		if (results == null) {
			monitor.showMessage(Level.ERROR,
					"Enrichment retrieval returned no results, possibly due to an error.");
			enrichmentResult = null;
			return;
			// throw new RuntimeException("Enrichment retrieval returned no results, possibly due to an error.");
		}
		List<EnrichmentTerm> terms = ModelUtils.getEnrichmentFromJSON(manager, results, stringNodesMap, network);
		if (terms == null) {
			String errorMsg = ModelUtils.getErrorMessageFromJSON(manager, results);
			monitor.showMessage(Level.ERROR,
					"Enrichment retrieval returned no results, possibly due to an error. " + errorMsg);
			enrichmentResult = null;
			return;
			// throw new RuntimeException("Enrichment retrieval returned no results, possibly due to an error. " + errorMsg);
		} else if (terms.size() > 0) {
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
		}
	}

	private Map<String, String> getEnrichmentPPIJSON(String selected, String species, String backgroundNetwork) {
		Map<String, String> args = new HashMap<String, String>();
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/ppi_enrichment";
		args.put("identifiers", selected);
		args.put("species", species);
		if (ModelUtils.getConfidence(network) == null) {
			monitor.showMessage(Level.ERROR,
					"PPI enrichment cannot be retrieved because of missing confidence values.");
			return null;
		}
		Double confidence = ModelUtils.getConfidence(network)*1000;
		args.put("required_score", confidence.toString());
		args.put("caller_identity", StringManager.CallerIdentity);
		if (!backgroundNetwork.equals("genome")) {
			String bg = getExisting(stringNetworkMap.get(backgroundNetwork));
			args.put("background_string_identifiers", bg);
		}

		JSONObject results = HttpUtils.postJSON(url, args, manager);
		if (results == null) {
			monitor.showMessage(Level.ERROR,
					"PPI enrichment retrieval returned no results, possibly due to an error.");
			// throw new RuntimeException("PPI enrichment retrieval returned no results, possibly due to an error.");
			return null;
		}
		Map<String, String> ppiEnrichment = 
						ModelUtils.getEnrichmentPPIFromJSON(manager, results, stringNodesMap, network);
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

	// private boolean getEnrichment(String[] selectedNodes, String filter, String species,
	// String enrichmentCategory) throws Exception {
	// Map<String, String> queryMap = new HashMap<String, String>();
	// String xmlQuery = "<experiment>";
	// if (filter.length() > 0) {
	// xmlQuery += "<filter>" + filter + "</filter>";
	// }
	// xmlQuery += "<tax_id>" + species + "</tax_id>";
	// xmlQuery += "<category>" + enrichmentCategory + "</category>";
	// xmlQuery += "<hits>";
	// for (String selectedNode : selectedNodes) {
	// xmlQuery += "<gene>" + selectedNode + "</gene>";
	// }
	// xmlQuery += "</hits></experiment>";
	// // System.out.println(xmlQuery);
	// queryMap.put("xml", xmlQuery);
	//
	// // get and parse enrichment results
	// List<EnrichmentTerm> enrichmentTerms = null;
	// // System.out.println(enrichmentCategory);
	// // double time = System.currentTimeMillis();
	// // parse using DOM
	// //Object results = HttpUtils.postXMLDOM(EnrichmentTerm.enrichmentURL, queryMap, manager);
	// //enrichmentTerms = ModelUtils.parseXMLDOM(results, cutoff, network, stringNodesMap,
	// manager);
	// //System.out.println("dom output: " + enrichmentTerms.size());
	// // System.out
	// // .println("from dom document to java structure: " + (System.currentTimeMillis() - time) /
	// // 1000 + " seconds.");
	// // time = System.currentTimeMillis();
	// // parse using SAX
	// EnrichmentSAXHandler myHandler = new EnrichmentSAXHandler(network, stringNodesMap,
	// enrichmentCategory);
	// // TODO: change for release
	// HttpUtils.postXMLSAX(EnrichmentTerm.enrichmentURL, queryMap, manager, myHandler);
	// if (!myHandler.isStatusOK()) {
	// // monitor.showMessage(Level.ERROR, "Error returned by enrichment webservice: " +
	// // myHandler.getStatusCode());
	// // return false;
	// if (myHandler.getMessage().equals("No genes found in the XML")) {
	// throw new RuntimeException(
	// "Task cannot be performed. Current node identifiers were not recognized by the enrichment
	// service.");
	// }
	// else if (myHandler.getStatusCode() != null)
	// throw new RuntimeException(
	// "Task cannot be performed. Error returned by enrichment webservice: " +
	// myHandler.getMessage());
	// else
	// throw new RuntimeException(
	// "Task cannot be performed. Uknown error while receiving or parsing output from the enrichment
	// service.");
	// } else if (myHandler.getWarning() != null) {
	// monitor.showMessage(Level.WARN,
	// "Warning returned by enrichment webservice: " + myHandler.getWarning());
	// }
	// enrichmentTerms = myHandler.getParsedData();
	//
	// // save results
	// if (enrichmentTerms == null) {
	// // monitor.showMessage(Level.ERROR,
	// // "No terms retrieved from the enrichment webservice for this category.");
	// throw new RuntimeException(
	// "No terms retrieved from the enrichment webservice for this category.");
	// // return false;
	// } else {
	// enrichmentResult.put(enrichmentCategory, enrichmentTerms);
	// if (enrichmentTerms.size() == 0) {
	// monitor.showMessage(Level.WARN,
	// "No significant terms for this enrichment category and cut-off.");
	// } else {
	// monitor.setStatusMessage("Retrieved " + enrichmentTerms.size()
	// + " significant terms for this enrichment category and cut-off.");
	// }
	// }
	// return true;
	// }

	private void saveEnrichmentTable(String tableName, String enrichmentCategory) {
		CyTableFactory tableFactory = manager.getService(CyTableFactory.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);

		enrichmentTable = tableFactory.createTable(tableName, EnrichmentTerm.colID, Long.class, false,
				true);
		enrichmentTable.setSavePolicy(SavePolicy.SESSION_FILE);
		tableManager.addTable(enrichmentTable);

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

		// Step 2: populate the table with some data
		List<EnrichmentTerm> processTerms = enrichmentResult.get(enrichmentCategory);
		if (processTerms == null) {
			return;
		}
		if (processTerms.size() == 0) {
			CyRow row = enrichmentTable.getRow((long) 0);
			row.set(EnrichmentTerm.colNetworkSUID, network.getSUID());
		}
		for (int i = 0; i < processTerms.size(); i++) {
			EnrichmentTerm term = processTerms.get(i);
			CyRow row = enrichmentTable.getRow((long) i);
			row.set(EnrichmentTerm.colName, term.getName());
			if (term.getName().length() > 4 && term.getName().startsWith("PMID.")) {
				row.set(EnrichmentTerm.colIDPubl, term.getName().substring(5));
			} else {
				row.set(EnrichmentTerm.colIDPubl, "");
			}
			row.set(EnrichmentTerm.colYear, term.getYear());
			row.set(EnrichmentTerm.colDescription, term.getDescription());
			row.set(EnrichmentTerm.colCategory, term.getCategory());
			row.set(EnrichmentTerm.colFDR, term.getFDRPValue());
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

	private String getExisting(CyNetwork network) {
		StringBuilder str = new StringBuilder();
		analyzedNodes = new ArrayList<CyNode>();
		for (CyNode node : network.getNodeList()) {
			String stringID = network.getRow(node).get(ModelUtils.STRINGID, String.class);
			String type = network.getRow(node).get(ModelUtils.TYPE, String.class);
			if (stringID != null && stringID.length() > 0 && type != null
					&& type.equals("protein")) {
				str.append(stringID + "\n");
				analyzedNodes.add(node);
			}
		}
		return str.toString();
	}

	private String getSelected(CyNetwork network) {
		StringBuilder selectedStr = new StringBuilder();
		analyzedNodes = new ArrayList<CyNode>();
		for (CyNode node : network.getNodeList()) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				String stringID = network.getRow(node).get(ModelUtils.STRINGID, String.class);
				String type = network.getRow(node).get(ModelUtils.TYPE, String.class);
				if (stringID != null && stringID.length() > 0 && type != null
						&& type.equals("protein")) {
					selectedStr.append(stringID + "\n");
					analyzedNodes.add(node);
				}
			}
		}
		return selectedStr.toString();
	}

	@ProvidesTitle
	public String getTitle() {
		return "Retrieve functional enrichment";
	}

	public static String EXAMPLE_JSON = 
					"{\"EnrichmentTable\": 101,"+
					"\""+ModelUtils.NET_PPI_ENRICHMENT+"\": 1e-16,"+
					"\""+ModelUtils.NET_ENRICHMENT_NODES+"\": 15,"+
					"\""+ModelUtils.NET_ENRICHMENT_EDGES+"\": 30,"+
					"\""+ModelUtils.NET_ENRICHMENT_EXPECTED_EDGES+"\": 57,"+
					"\""+ModelUtils.NET_ENRICHMENT_CLSTR+"\": 0.177,"+
					"\""+ModelUtils.NET_ENRICHMENT_DEGREE+"\": 2.66}";

	public static String EXAMPLE_JSON_PUBL = 
			"{\"EnrichmentTable\": 101}";

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(CyTable.class)) {
			return (R) enrichmentTable;
		} else if (clzz.equals(String.class)) {
			if (ppiSummary == null) return (R)"No results";
			String result = "Enrichment results summary:";
			result = addStringResult(result, ModelUtils.NET_PPI_ENRICHMENT);
			result = addStringResult(result, ModelUtils.NET_ENRICHMENT_NODES);
			result = addStringResult(result, ModelUtils.NET_ENRICHMENT_EXPECTED_EDGES);
			result = addStringResult(result, ModelUtils.NET_ENRICHMENT_EDGES);
			result = addStringResult(result, ModelUtils.NET_ENRICHMENT_CLSTR);
			result = addStringResult(result, ModelUtils.NET_ENRICHMENT_DEGREE);
			return (R)result;
		} else if (clzz.equals(Long.class)) {
			if (ppiSummary == null) return null; 
			return (R) enrichmentTable.getSUID();
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				if (enrichmentTable == null || ppiSummary == null) return "{}";
        String result = "{\"EnrichmentTable\": "+enrichmentTable.getSUID();

				result = addResult(result, ModelUtils.NET_PPI_ENRICHMENT);
				result = addResult(result, ModelUtils.NET_ENRICHMENT_NODES);
				result = addResult(result, ModelUtils.NET_ENRICHMENT_EXPECTED_EDGES);
				result = addResult(result, ModelUtils.NET_ENRICHMENT_EDGES);
				result = addResult(result, ModelUtils.NET_ENRICHMENT_CLSTR);
				result = addResult(result, ModelUtils.NET_ENRICHMENT_DEGREE);
				result += "}";
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
