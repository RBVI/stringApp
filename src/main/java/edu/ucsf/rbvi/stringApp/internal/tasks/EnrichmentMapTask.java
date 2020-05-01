package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedDouble;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class EnrichmentMapTask extends AbstractTask {

	private final StringManager manager;
	private final CyTable filteredEnrichmentTable;

	// Network name
	@Tunable(description="Enrichment Map name")
	public String mapName = "STRING Enrichment Map";

	// Similarity cutoff
	@Tunable(description="Similarity cutoff", params="slider=true")
	public BoundedDouble similarity = new BoundedDouble(0.0, 0.5, 1.0, true, true);

	// FDR cutoff
	@Tunable(description="FDR cutoff", params="slider=true")
	public BoundedDouble FDRcutoff = new BoundedDouble(0.0, 0.05, 0.5, true, false);
	
	public EnrichmentMapTask(final StringManager manager, final CyNetwork network, final CyTable filteredEnrichmentTable) {
		this.manager = manager;
		this.filteredEnrichmentTable = filteredEnrichmentTable;
		String netName = network.getRow(network).get(CyNetwork.NAME, String.class);
		if (netName.startsWith("String Network")) {
			mapName = "Enrichment Map "+netName.substring(15);
		} else {
			mapName = "Enrichment Map - "+netName;
		}
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		Map<String, Object> args = new HashMap<>();
		args.put("pvalueColumn","FDR value");
		args.put("genesColumn","genes");
		args.put("nameColumn","term name");
		args.put("descriptionColumn","description");
		args.put("table","SUID:"+String.valueOf(filteredEnrichmentTable.getSUID()));
		args.put("coefficients","JACCARD");
		args.put("similaritycutoff",String.valueOf(similarity.getValue()));
		args.put("pvalue",FDRcutoff.getValue());
		insertTasksAfterCurrentTask(manager.getCommandTaskIterator("enrichmentmap", "build-table", args, null));
	}

}
