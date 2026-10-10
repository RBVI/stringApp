package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedDouble;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class EnrichmentMapTask extends AbstractTask {

	private final StringManager manager;
	private final CyTable filteredEnrichmentTable;

	private double defaultSimCutoff = 0.8;
	
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

	// FDR cutoff
	
	public EnrichmentMapTask(final StringManager manager, final CyNetwork network, final CyTable filteredEnrichmentTable, boolean filtered) {
		this.manager = manager;
		this.filteredEnrichmentTable = filteredEnrichmentTable;
		if (filtered) {
			// TODO: [N] do we not use this class anymore?
		}
		String netName = network.getRow(network).get(CyNetwork.NAME, String.class);
		mapName = "Enrichment Map - "+ netName;
		if (netName.startsWith("String Network")) {
			mapName = "Enrichment Map "+netName.substring(15);
		}
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		Map<String, Object> args = new HashMap<>();
		args.put("networkName", mapName);
		args.put("pvalueColumn", EnrichmentTerm.colFDR);
		args.put("genesColumn", EnrichmentTerm.colGenes);
		args.put("nameColumn", EnrichmentTerm.colName);
		args.put("descriptionColumn",EnrichmentTerm.colDescription);
		args.put("table","SUID:"+String.valueOf(filteredEnrichmentTable.getSUID()));
		args.put("coefficients","JACCARD");
		args.put("similaritycutoff",String.valueOf(similarity.getValue()));
		args.put("pvalue",0.05);
		insertTasksAfterCurrentTask(manager.getCommandTaskIterator("enrichmentmap", "build-table", args, null));
	}

}
