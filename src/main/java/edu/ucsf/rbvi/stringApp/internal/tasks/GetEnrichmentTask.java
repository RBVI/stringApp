package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.io.EnrichmentSAXHandler;
import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetEnrichmentTask extends AbstractTask {
	final StringManager manager;
	final CyNetwork network;
	final CyNetworkView netView;
	final Map<String, List<EnrichmentTerm>> enrichmentResult;
	final Map<String, Long> stringNodesMap;
	final ShowEnrichmentPanelTaskFactory showFactory;
	TaskMonitor monitor;
	// boolean guiMode;

	@Tunable(description = "Enrichment cutoff", gravity = 1.0)
	public double cutoff = 0.05;

	@Tunable(description = "GO Biological Process", gravity = 2.0)
	public boolean goProcess = true;

	@Tunable(description = "GO Molecular Function", gravity = 3.0)
	public boolean goFunction = true;

	@Tunable(description = "GO Cellular Compartment", gravity = 4.0)
	public boolean goCompartment = true;

	@Tunable(description = "KEGG Pathways", gravity = 5.0)
	public boolean kegg = true;

	@Tunable(description = "Pfam domains", gravity = 6.0)
	public boolean pfam = true;

	@Tunable(description = "InterPro domains", gravity = 7.0)
	public boolean interPro = true;

	public GetEnrichmentTask(StringManager manager, CyNetwork network, CyNetworkView netView,
			ShowEnrichmentPanelTaskFactory showFactory) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;
		this.showFactory = showFactory;
		enrichmentResult = new HashMap<>();
		stringNodesMap = new HashMap<>();
		monitor = null;
	}

	public void run(TaskMonitor monitor) throws Exception {
		this.monitor = monitor;
		monitor.setTitle(this.getTitle());

		// Get list of (selected) nodes
		String selected = ModelUtils.getSelected(network, null).trim();
		if (selected.length() == 0) {
			selected = ModelUtils.getExisting(network).trim();
		}
		if (selected.length() == 0) {
			return;
		}
		List<String> netSpecies = ModelUtils.getNetworkSpeciesTaxons(network);
		String species = null;
		if (netSpecies.size() == 1) {
			species = netSpecies.get(0);
		} else {
			monitor.showMessage(Level.ERROR,
					"None or more than one species in the network. Enrichment will not be retrieved.");
			System.out.println(
					"None or more than one species in the network. Enrichment will not be retrieved.");
			return;
		}
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
		deleteOldTables();

		// retrieve enrichment
		String[] selectedNodes = selected.split("\n");
		if (goProcess) {
			monitor.setStatusMessage("Retrieving functional enrichment for GO Biological Process.");
			if (getEnrichment(selectedNodes, "fat", species, EnrichmentTerm.termCategories[0]))
				saveEnrichmentTable(EnrichmentTerm.termTables[0], EnrichmentTerm.termCategories[0]);
		}
		if (goCompartment) {
			monitor.setStatusMessage(
					"Retrieving functional enrichment for GO Cellular Compartment.");
			if (getEnrichment(selectedNodes, "fat", species, EnrichmentTerm.termCategories[2]))
				saveEnrichmentTable(EnrichmentTerm.termTables[2], EnrichmentTerm.termCategories[2]);
		}
		if (goFunction) {
			monitor.setStatusMessage("Retrieving functional enrichment for GO Molecular Function.");
			if (getEnrichment(selectedNodes, "fat", species, EnrichmentTerm.termCategories[1]))
				saveEnrichmentTable(EnrichmentTerm.termTables[1], EnrichmentTerm.termCategories[1]);
		}
		if (interPro) {
			monitor.setStatusMessage(
					"Retrieving functional enrichment for INTERPRO Protein Domains and Features.");
			if (getEnrichment(selectedNodes, "", species, EnrichmentTerm.termCategories[5]))
				saveEnrichmentTable(EnrichmentTerm.termTables[5], EnrichmentTerm.termCategories[5]);
		}
		if (kegg) {
			monitor.setStatusMessage("Retrieving functional enrichment for KEGG Pathways.");
			if (getEnrichment(selectedNodes, "", species, EnrichmentTerm.termCategories[3]))
				saveEnrichmentTable(EnrichmentTerm.termTables[3], EnrichmentTerm.termCategories[3]);
		}
		if (pfam) {
			monitor.setStatusMessage("Retrieving functional enrichment for PFAM Protein Domains.");
			if (getEnrichment(selectedNodes, "", species, EnrichmentTerm.termCategories[4]))
				saveEnrichmentTable(EnrichmentTerm.termTables[4], EnrichmentTerm.termCategories[4]);
		}

		if (enrichmentResult.size() > 0) {
			SynchronousTaskManager<?> taskM = manager.getService(SynchronousTaskManager.class);
			TaskIterator ti = showFactory.createTaskIterator(true);
			taskM.execute(ti);
		}
	}

	private boolean getEnrichment(String[] selectedNodes, String filter, String species,
			String enrichmentCategory) {
		Map<String, String> queryMap = new HashMap<String, String>();
		String xmlQuery = "<experiment>";
		if (filter.length() > 0) {
			xmlQuery += "<filter>" + filter + "</filter>";
		}
		xmlQuery += "<tax_id>" + species + "</tax_id>";
		xmlQuery += "<category>" + enrichmentCategory + "</category>";
		xmlQuery += "<hits>";
		for (String selectedNode : selectedNodes) {
			xmlQuery += "<gene>" + selectedNode + "</gene>";
		}
		xmlQuery += "</hits></experiment>";
		// System.out.println(xmlQuery);
		queryMap.put("xml", xmlQuery);
		
		// get and parse enrichment results
		List<EnrichmentTerm> enrichmentTerms = null;
		// System.out.println(enrichmentCategory);
		// double time = System.currentTimeMillis();
		// parse using DOM
		// Object results = HttpUtils.postXMLDOM(EnrichmentTerm.enrichmentURL, queryMap, manager);
		// System.out.println(
		//		"get dom xml data as a dom document: " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		// time = System.currentTimeMillis();
		// enrichmentTerms = ModelUtils.parseXMLDOM(results, cutoff, network, stringNodesMap, manager);
		// System.out
		// 		.println("from dom document to java structure: " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		// time = System.currentTimeMillis();
		// parse using SAX
		EnrichmentSAXHandler myHandler = new EnrichmentSAXHandler(network, stringNodesMap, cutoff);
		// TODO: change for release
		HttpUtils.postXMLSAX(EnrichmentTerm.enrichmentURLTest, queryMap, manager, myHandler);
		enrichmentTerms = myHandler.getParsedData();

		// save results
		if (enrichmentTerms == null) {
			monitor.showMessage(Level.ERROR,
					"No terms retrieved from the server for this enrichment category.");
			return false;
		} else {
			enrichmentResult.put(enrichmentCategory, enrichmentTerms);
			if (enrichmentTerms.size() == 0) {
				monitor.setStatusMessage(
						"No significant terms for this enrichment category and cut-off.");
			} else {
				monitor.setStatusMessage("Retrieved " + enrichmentTerms.size()
						+ " significant terms for this enrichment category and cut-off.");
			}
		}
		return true;
	}

	private void saveEnrichmentTable(String tableName, String enrichmentCategory) {
		CyTableFactory tableFactory = manager.getService(CyTableFactory.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);

		CyTable table = tableFactory.createTable(tableName, EnrichmentTerm.colID, Long.class, false,
				true);
		table.setSavePolicy(SavePolicy.SESSION_FILE);
		tableManager.addTable(table);

		if (table.getColumn(EnrichmentTerm.colGenesSUID) == null) {
			table.createListColumn(EnrichmentTerm.colGenesSUID, Long.class, false);
		}
		if (table.getColumn(EnrichmentTerm.colNetworkSUID) == null) {
			table.createColumn(EnrichmentTerm.colNetworkSUID, Long.class, false);
		}
		if (table.getColumn(EnrichmentTerm.colName) == null) {
			table.createColumn(EnrichmentTerm.colName, String.class, false);
		}
		if (table.getColumn(EnrichmentTerm.colDescription) == null) {
			table.createColumn(EnrichmentTerm.colDescription, String.class, false);
		}
		if (table.getColumn(EnrichmentTerm.colFDR) == null) {
			table.createColumn(EnrichmentTerm.colFDR, Double.class, false);
		}
		if (table.getColumn(EnrichmentTerm.colGenesCount) == null) {
			table.createColumn(EnrichmentTerm.colGenesCount, Integer.class, false);
		}
		if (table.getColumn(EnrichmentTerm.colGenes) == null) {
			table.createListColumn(EnrichmentTerm.colGenes, String.class, false);
		}

		// table.createColumn(EnrichmentTerm.colPvalue, Double.class, false);
		// table.createColumn(EnrichmentTerm.colBonferroni, Double.class, false);

		// Step 2: populate the table with some data
		List<EnrichmentTerm> processTerms = enrichmentResult.get(enrichmentCategory);
		if (processTerms == null) {
			return;
		}
		if (processTerms.size() == 0) {
			CyRow row = table.getRow((long) 0);
			row.set(EnrichmentTerm.colNetworkSUID, network.getSUID());
		}
		for (int i = 0; i < processTerms.size(); i++) {
			EnrichmentTerm term = processTerms.get(i);
			CyRow row = table.getRow((long) i);
			row.set(EnrichmentTerm.colName, term.getName());
			row.set(EnrichmentTerm.colDescription, term.getDescription());
			row.set(EnrichmentTerm.colFDR, term.getFDRPValue());
			row.set(EnrichmentTerm.colGenesCount, term.getGenes().size());
			row.set(EnrichmentTerm.colGenes, term.getGenes());
			row.set(EnrichmentTerm.colGenesSUID, term.getNodesSUID());
			row.set(EnrichmentTerm.colNetworkSUID, network.getSUID());
		}
	}

	private void deleteOldTables() {
		CyEventHelper eventHelper = manager.getService(CyEventHelper.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		Set<CyTable> oldTables = ModelUtils.getEnrichmentTables(manager, network);
		for (CyTable table : oldTables) {
			tableManager.deleteTable(table.getSUID());
			eventHelper.flushPayloadEvents();
		}
	}

	private void showStatusReport() {
		StringBuilder sb = new StringBuilder();
		for (String enrCat : enrichmentResult.keySet()) {
			sb.append(enrCat);
			sb.append("\t");
			sb.append(enrichmentResult.get(enrCat).size());
			sb.append("\n");
		}
		System.out.println(sb.toString());
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
		JOptionPane.showMessageDialog(cytoPanel.getSelectedComponent(), sb.toString(),
				"Retrieve functional enrichment status", JOptionPane.INFORMATION_MESSAGE);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Retrieve functional enrichment";
	}

}
