package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListMultipleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class EnrichmentMapAdvancedTask extends AbstractTask implements TaskObserver{

	private final StringManager manager;
	private final CyTable filteredEnrichmentTable;
	private final CyNetwork network;

	private double defaultSimCutoff = 0.8;
	
	private CyTable customTable;
	private final String geneName = "gene name";
	private final String geneDescription = "gene description";
	
	// Network name
	@Tunable(description="Enrichment Map name")
	public String mapName = "Enrichment Map - String Network";

	// Similarity cutoff
	@Tunable(description="Connectivity cutoff (Jaccard similarity)", 
	         longDescription="The cutoff for the lowest Jaccard similarity between terms.  "+
						"Higher values mean more sparse connectivity while lower "
						+ "values mean more dense network cobnnectivity. ",
						//+ "Good values for disease networks are 0.8 or 0.9.",
			tooltip="<html>The cutoff for the lowest Jaccard similarity of terms. <br /> "+
					"Higher values mean more sparse connectivity while lower <br /> "
					+ "values mean more dense network cobnnectivity. <br /> "
					//+ "Good values for disease networks are 0.8 or 0.9." 
					+ "</html>", params="slider=true")
	public BoundedDouble similarity = new BoundedDouble(0.0, defaultSimCutoff, 1.0, true, true);

	@Tunable(description = "Select columns", 
	         tooltip = "Select the columns to export to EnrichmentMap",
	         longDescription = "Select the columns to export to EnrichmentMap",
	         exampleStringValue = "")
	public ListMultipleSelection<String> columns = new ListMultipleSelection<>();
	
	public EnrichmentMapAdvancedTask(final StringManager manager, final CyNetwork network, final CyTable filteredEnrichmentTable, boolean filtered) {
		this.manager = manager;
		this.filteredEnrichmentTable = filteredEnrichmentTable;
		if (filtered) {
			similarity.setBounds(0.0, manager.getOverlapCutoff(network));
			similarity.setValue(defaultSimCutoff*manager.getOverlapCutoff(network));
		}
		this.network = network;
		String netName = network.getRow(network).get(CyNetwork.NAME, String.class);
		mapName = "Enrichment Map - "+ netName;
		if (netName.startsWith("String Network") && netName.length() > 15) {
			mapName = "Enrichment Map "+netName.substring(15);
		}
		List<String> availableColumns = new ArrayList<String>();
		for (CyColumn col : network.getDefaultNodeTable().getColumns()) {
			if (col.getType().equals(Double.class)) {
				availableColumns.add(col.getName());
			}
		}
		columns = new ListMultipleSelection<>(availableColumns);
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		// build map with arguments
		Map<String, Object> args = new HashMap<>();
		args.put("networkName", mapName);
		args.put("pvalueColumn", EnrichmentTerm.colPvalue);
		args.put("qvalueColumn", EnrichmentTerm.colFDR);
		args.put("genesColumn", EnrichmentTerm.colGenes);
		args.put("nameColumn", EnrichmentTerm.colName);
		args.put("descriptionColumn",EnrichmentTerm.colDescription);
		args.put("table","SUID:"+String.valueOf(filteredEnrichmentTable.getSUID()));
		args.put("coefficients","JACCARD");
		args.put("similaritycutoff",String.valueOf(similarity.getValue()));
		args.put("pvalue",0.05);
		args.put("qvalue",0.05);
		
		// expression data
		// args.put("exprTable","SUID:"+String.valueOf(network.getDefaultNodeTable().getSUID()));
		// args.put("exprGeneNameColumn", "display name");
		// args.put("exprDescriptionColumn", "stringdb::description");

		List<String> columnsToImport = columns.getSelectedValues();
		List<String> columnsToImportRenamed = setUpCustomTable(columnsToImport);
		args.put("exprTable","SUID:"+String.valueOf(customTable.getSUID()));
		args.put("exprGeneNameColumn", geneName);
		args.put("exprDescriptionColumn", geneDescription);
		args.put("exprValueColumns", String.join(",", columnsToImportRenamed));
		args.put("filterByExpressions", "false");
		
		// run task
		insertTasksAfterCurrentTask(manager.getCommandTaskIterator("enrichmentmap", "build-table", args, this));
		
	}

	public List<String> setUpCustomTable(List<String> columnsToImport) {
		List<String> columnsToImportRenamed = new ArrayList<String>();

		// create table
		CyTableFactory tableFactory = manager.getService(CyTableFactory.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		customTable = tableFactory.createTable("STRING data for EnrichmentMap", "colID", Long.class, false, true);
		customTable.setTitle("STRING data for EnrichmentMap");
		customTable.setSavePolicy(SavePolicy.DO_NOT_SAVE);
		tableManager.addTable(customTable);
		
		// get node table to copy stuff from
		CyTable nodeTable = network.getDefaultNodeTable();
		List<CyRow> nodeTableRows = nodeTable.getAllRows();
		
		// set up table
		customTable.createColumn(geneName, String.class, false);
		customTable.createColumn(geneDescription, String.class, false);
		for (String col : columnsToImport) {
			String colRenamed = col;
			if (col.contains("::")) {
				colRenamed = col.substring(col.indexOf("::")+2);
			}
			customTable.createColumn(colRenamed, Double.class, false);
			columnsToImportRenamed.add(colRenamed);
		}

		// fill table
		long i = 0;
		for (CyRow row : nodeTableRows) {
			CyRow customRow = customTable.getRow(i);
			i++;
			customRow.set(geneName, row.get("display name", String.class));
			customRow.set(geneDescription, row.get("stringdb::description", String.class));
			for (int j = 0; j < columnsToImport.size(); j++) {
				customRow.set(columnsToImportRenamed.get(j), row.get(columnsToImport.get(j), Double.class));
			}
		}

		// return renamed columns
		return columnsToImportRenamed;
	}

	@ProvidesTitle
	public String getTitle() {
		return "Create ErncihmentMap network";
	}

	@Override
	public void allFinished(FinishStatus arg0) {
	}

	@Override
	public void taskFinished(ObservableTask arg0) {
		// remove node coloring
		//CyNetworkManager netManager = (CyNetworkManager) manager.getService(CyNetworkManager.class);
		//for (CyNetwork net : netManager.getNetworkSet()) {
		//	String netName = ModelUtils.getName(net, net);
		//	 if (netName.startsWith("Enrichment Map")) {
		//		 System.out.println(netName);
		//	}
		//}

		//Map<String, Object> args = new HashMap<>();
		//args = new HashMap<>();
		//args.put("network", mapName);
		//args.put("data", "NONE");
		// insertTasksAfterCurrentTask(manager.getCommandTaskIterator("enrichmentmap", "chart", args, null));
	}

	
}