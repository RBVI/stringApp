package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.write.ExportTableTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ExportEnrichmentTask extends AbstractTask {

	private StringManager manager;
	private CyNetwork network;
	private Set<CyTable> enrichmentTables;

	@Tunable(description = "Select a table to export", gravity = 1.0)
	public ListSingleSelection<String> availableTables = new ListSingleSelection<String>();;

	@Tunable(description = "Save Table as", params = "input=true", gravity = 2.0)
	// public String name = "test";
	public File file = null;

	public ExportEnrichmentTask(StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
		getAvailableTables();
		// file = new File("test.csv");
	}

	public ExportEnrichmentTask(StringManager manager, CyNetwork network, CyTable table,
			File file) {
		this.manager = manager;
		this.network = network;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		ExportTableTaskFactory exportTF = manager.getService(ExportTableTaskFactory.class);
		String selectedTable = availableTables.getSelectedValue();
		if (selectedTable != null && file != null && file.canWrite()) {
			for (CyTable enTable : enrichmentTables) {
				if (enTable.getTitle().equals(selectedTable)) {
					System.out.println(
							"export table " + selectedTable + " to " + file.getAbsolutePath());
					exportTF.createTaskIterator(enTable, file);
				}
			}
		}
	}

	private void getAvailableTables() {
		enrichmentTables = ModelUtils.getEnrichmentTables(this.manager, this.network);
		List<String> enrichmentTableNames = new ArrayList<String>();
		for (CyTable table : enrichmentTables) {
			enrichmentTableNames.add(table.getTitle());
		}
		availableTables = new ListSingleSelection<>(enrichmentTableNames);
		if (enrichmentTableNames.size() > 0)
			availableTables.setSelectedValue(enrichmentTableNames.get(0));

	}

	@ProvidesTitle
	public String getTitle() {
		return "Export STRING Enrichment tables";
	}
}
