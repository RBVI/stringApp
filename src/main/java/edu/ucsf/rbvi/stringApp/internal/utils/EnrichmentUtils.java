package edu.ucsf.rbvi.stringApp.internal.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class EnrichmentUtils {


	public static List<String> getEnrichmentNetSpecies(CyNetwork net) {
		List<String> species = new ArrayList<String>();
		for (CyNode node : net.getNodeList()) {
			String nSpecies = net.getRow(node).get(ColumnNames.SPECIES, String.class);
			if (nSpecies != null && !nSpecies.equals("") && !species.contains(nSpecies)) {
				Species theSpecies = Species.getSpecies(nSpecies);

				// TODO: This is kind of a hack for now and will be updated once we get the kingdom data from the server 
				if (theSpecies != null
						&& (theSpecies.getType().equals("core") || theSpecies.getType().equals("periphery")
								|| theSpecies.getType().equals("mapped") || theSpecies.getType().equals("custom")))
					species.add(nSpecies);
			}
		}
		Collections.sort(species);
		return species;
	}

	public static List<CyNode> getEnrichmentNodes(StringManager manager, CyNetwork net, String tableName) {
		List<CyNode> analyzedNodes = new ArrayList<CyNode>();
		if (net != null) {
			List<Long> nodesSUID = new ArrayList<Long>();
			// we handle it differently for publications table and for enrichemnt table
			if (tableName.equals(TermCategory.PMID.getTable())) {
				CyTable netTable = net.getDefaultNetworkTable();
				if (netTable.getColumn(ColumnNames.NET_ANALYZED_NODES_PUBL) != null) {
					nodesSUID = (List<Long>) netTable.getRow(net.getSUID()).get(ColumnNames.NET_ANALYZED_NODES_PUBL, List.class);
				}
			} else {
				CyTable settignsTable = getEnrichmentSettingsTable(manager, net);
				if (settignsTable.getColumn(ColumnNames.NET_ANALYZED_NODES) != null) {
					nodesSUID = (List<Long>) settignsTable.getRow(tableName)
							.get(ColumnNames.NET_ANALYZED_NODES, List.class);
				}
			}
			if (nodesSUID != null) {
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
			if (netTable.getColumn(ColumnNames.NET_PPI_ENRICHMENT) != null) {
				return (Double) netTable.getRow(net.getSUID()).get(ColumnNames.NET_PPI_ENRICHMENT,
						Double.class);
			}
		}
		return null;
	}
	
	public static List<String> getVisualizedEnrichmentTerms(CyNetwork net) {
		if (net != null) {
			CyTable netTable = net.getDefaultNetworkTable();
			if (netTable.getColumn(ColumnNames.NET_ENRICHMENT_VISTEMRS) != null) {
				return netTable.getRow(net.getSUID()).getList(ColumnNames.NET_ENRICHMENT_VISTEMRS,
						String.class);
			}
		}
		return null;
		
	}

	// This method returns only the main tables, e.g. STRING Enrichment: All and STRING Enrichment: PMID 
	public static Set<CyTable> getMainEnrichmentTables(StringManager manager, CyNetwork network) {
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

	// This method now returns all enrichment tables that start with the given prefix and are associated with the given network
	// If the prefix is EnrichmentTerm.ENRICHMENT_TABLE_PREFIX, it returns all enrichment tables
	// If the prefix is the name of the table it returns that table
	// If the prefix is the enrichment table prefix + group name, it returns all tables for a given group
	public static Set<CyTable> getAllEnrichmentTables(StringManager manager, CyNetwork network, String tablePrefix) {
		Set<CyTable> netTables = new HashSet<CyTable>();
		CyTableManager tableManager = manager.getService(CyTableManager.class); 
		Set<CyTable> currTables = tableManager.getAllTables(true);
		for (CyTable current : currTables) {
			if ((current.getTitle().startsWith(tablePrefix))
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

	public static void deleteMainEnrichmentTables(CyNetwork network, StringManager manager, boolean publOnly) {
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		CyTable netTable = network.getDefaultNetworkTable();
		List<String> groups = netTable.getRow(network.getSUID()).getList(ColumnNames.NET_ENRICHMENT_TABLES, String.class);
		Set<CyTable> oldTables = getMainEnrichmentTables(manager, network);
		for (CyTable table : oldTables) {
			if (publOnly && !table.getTitle().equals(TermCategory.PMID.getTable())) {
				continue;
			} 
			// first delete the table name from the list of groups in the network table
			if (!publOnly && groups != null && groups.contains(table.getTitle())) {
				groups.remove(table.getTitle());
			}			
			// them delete the enrichment table
			tableManager.deleteTable(table.getSUID());
			manager.flushEvents();				
		}
		// TODO: [N] Is it ok to set the  NET_ENRICHMENT_TABLES to an empty list
		netTable.getRow(network.getSUID()).set(ColumnNames.NET_ENRICHMENT_TABLES, groups);
		// TODO: [N] do we need to delete settings from the settings table
	}

	public static void deleteGroupEnrichmentTables(CyNetwork network, StringManager manager, String groupPrefix) {
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		CyTable netTable = network.getDefaultNetworkTable();
		List<String> groups = netTable.getRow(network.getSUID()).getList(ColumnNames.NET_ENRICHMENT_TABLES, String.class);
		Set<CyTable> oldTables = getAllEnrichmentTables(manager, network, groupPrefix);
		for (CyTable table : oldTables) {
			// first delete the table name from the list of groups in the network table
			if (groups != null && groups.contains(table.getTitle())) {
				groups.remove(table.getTitle());
			}
			// then delete the enrichment table
			tableManager.deleteTable(table.getSUID());
			manager.flushEvents();				
		}
		// TODO: [N] Is it ok to set the  NET_ENRICHMENT_TABLES to an empty list
		netTable.getRow(network.getSUID()).set(ColumnNames.NET_ENRICHMENT_TABLES, groups);
		// TODO: [N] do we need to delete settings from the settings table
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


	public static void updateEnrichmentSettingsTableGroup(StringManager manager, CyNetwork network, String group, Map<String, String> groupSettings) {
		String setting = "";
		int index = 0;
		for (String key: groupSettings.keySet()) {
			if (index > 0) {
				setting += ";";
			}
			setting += key+"="+groupSettings.get(key);
			index ++;
		}
		CyTable settingsTable = getEnrichmentSettingsTable(manager, network);
		settingsTable.getRow(group).set(ColumnNames.NET_ENRICHMENT_SETTINGS, setting);
	}

	public static Map<String, String> getEnrichmentSettingsTableGroup(StringManager manager, CyNetwork network, String group) {
		Map<String, String> settings = new HashMap<String, String>();
		CyTable settingsTable = getEnrichmentSettingsTable(manager, network);
		// TODO: [N] Test, not sure why this has to be checked...
		if (settingsTable == null) {
			return settings;
		}
		String setting = settingsTable.getRow(group).get(ColumnNames.NET_ENRICHMENT_SETTINGS, String.class);
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

	public static CyTable getEnrichmentSettingsTable(StringManager manager, CyNetwork network) {
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		Long tableSUID = network.getRow(network).get(ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE_SUID, Long.class);
		if (tableSUID != null) {
			return tableManager.getTable(tableSUID.longValue());
		} else {
			CyTable netTable = network.getDefaultNetworkTable();
			ModelUtils.createColumnIfNeeded(netTable, Long.class, ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE_SUID);
			CyTable settingsTable = createEnrichmentSettingsTable(manager, network);
			netTable.getRow(network.getSUID()).set(ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE_SUID, settingsTable.getSUID());
			return settingsTable;
		}
	}

	public static CyTable createEnrichmentSettingsTable(StringManager manager, CyNetwork network) {
		CyTableFactory tableFactory = manager.getService(CyTableFactory.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);

		CyTable settingsTable = tableFactory.createTable(ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE, ColumnNames.NET_ENRICHMENT_GROUP, String.class, false, true);
		// settingsTable.setSavePolicy(SavePolicy.SESSION_FILE);
		tableManager.addTable(settingsTable);
		ModelUtils.createColumnIfNeeded(settingsTable, String.class, ColumnNames.NET_ENRICHMENT_SETTINGS);
		ModelUtils.createListColumnIfNeeded(settingsTable, Long.class, ColumnNames.NET_ANALYZED_NODES);
		ModelUtils.createListColumnIfNeeded(settingsTable, String.class, ColumnNames.NET_ENRICHMENT_VISTEMRS);
		ModelUtils.createListColumnIfNeeded(settingsTable, String.class, ColumnNames.NET_ENRICHMENT_VISCOLORS);
		return settingsTable;
	}

}
