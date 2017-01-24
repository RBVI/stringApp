package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.cytoscape.work.Tunable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	public boolean pfam = false;

	@Tunable(description = "InterPro domains", gravity = 7.0)
	public boolean interPro = false;

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
			System.out.println(
					"None or more than one species in the network. Enrichment will not be retrieved.");
			return;
		}

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

		// TODO: clear old results
		deleteOldTables();

		String[] selectedNodes = selected.split("\n");
		if (goProcess) {
			monitor.setStatusMessage("Retrieving functional enrichment for GO Biological Process.");
			getEnrichment(selectedNodes, "fat", species, EnrichmentTerm.termCategories[0]);
			saveEnrichmentTable(EnrichmentTerm.termTables[0], EnrichmentTerm.termCategories[0]);
		}
		if (goFunction) {
			monitor.setStatusMessage("Retrieving functional enrichment for GO Molecular Function.");
			getEnrichment(selectedNodes, "fat", species, EnrichmentTerm.termCategories[1]);
			saveEnrichmentTable(EnrichmentTerm.termTables[1], EnrichmentTerm.termCategories[1]);
		}
		if (goCompartment) {
			monitor.setStatusMessage(
					"Retrieving functional enrichment for GO Cellular Compartment.");
			getEnrichment(selectedNodes, "fat", species, EnrichmentTerm.termCategories[2]);
			saveEnrichmentTable(EnrichmentTerm.termTables[2], EnrichmentTerm.termCategories[2]);
		}
		if (kegg) {
			monitor.setStatusMessage("Retrieving functional enrichment for KEGG Pathways.");
			getEnrichment(selectedNodes, "", species, EnrichmentTerm.termCategories[3]);
			saveEnrichmentTable(EnrichmentTerm.termTables[3], EnrichmentTerm.termCategories[3]);
		}
		if (pfam) {
			monitor.setStatusMessage("Retrieving functional enrichment for PFAM Protein Domains.");
			getEnrichment(selectedNodes, "", species, EnrichmentTerm.termCategories[4]);
			saveEnrichmentTable(EnrichmentTerm.termTables[4], EnrichmentTerm.termCategories[4]);
		}
		if (interPro) {
			monitor.setStatusMessage(
					"Retrieving functional enrichment for INTERPRO Protein Domains and Features.");
			getEnrichment(selectedNodes, "", species, EnrichmentTerm.termCategories[5]);
			saveEnrichmentTable(EnrichmentTerm.termTables[5], EnrichmentTerm.termCategories[5]);
		}

		if (enrichmentResult.size() > 0) {
			SynchronousTaskManager<?> taskM = manager.getService(SynchronousTaskManager.class);
			TaskIterator ti = showFactory.createTaskIterator(true);
			taskM.execute(ti);
		}
	}

	private void getEnrichment(String[] selectedNodes, String filter, String species,
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
		// TODO: Change to use SAXParser
		Object results = HttpUtils.postXML(EnrichmentTerm.enrichmentURL, queryMap, manager);
		if (!(results instanceof Document)) {
			return;
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
							return;
						}
					}
					if (((Element) nNode).getElementsByTagName("warning").getLength() > 0) {
						String warning = ((Element) nNode).getElementsByTagName("warning").item(0)
								.getTextContent();
						System.out.println("Warning from enrichment server: " + warning);
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
							String enrGene = ((Element) geneNode).getTextContent();
							if (enrGene != null) {
								String nodeName = enrGene;
								if (stringNodesMap.containsKey(enrGene)) {
									final Long nodeSUID = stringNodesMap.get(enrGene);
									enrNodes.add(nodeSUID);
									if (network.getDefaultNodeTable()
											.getColumn(CyNetwork.NAME) != null) {
										nodeName = network.getDefaultNodeTable().getRow(nodeSUID)
												.get(CyNetwork.NAME, String.class);
									}
								}
								enrGenes.add(nodeName);
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
		}
		monitor.setStatusMessage("Number of terms: " + enrichmentTerms.size());

		if (!enrichmentResult.containsKey(enrichmentCategory)) {
			enrichmentResult.put(enrichmentCategory, enrichmentTerms);
		} else {
			// TODO: could it happen?
			monitor.setStatusMessage("Retrieved terms for the same category already ...");
		}

		// print enriched terms
		// Collections.sort(enrichmentTerms);
		// for (EnrichmentTerm term : enrichmentTerms) {
		// if (term.getFDRPValue() <= cutoff) {
		// System.out.println(term);
		// } else {
		// break;
		// }
		// }
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

	@ProvidesTitle
	public String getTitle() {
		return "Retrieve functional enrichment";
	}

}
