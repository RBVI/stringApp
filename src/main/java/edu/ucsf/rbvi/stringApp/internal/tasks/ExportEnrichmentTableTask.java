package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.write.ExportTableTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListMultipleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ExportEnrichmentTableTask extends AbstractTask {

	private StringManager manager;
	private CyNetwork network;

	@Tunable(description = "Save Table as", params = "input=false", 
	         tooltip="<html>Note: for convenience spaces are replaced by underscores.</html>", gravity = 2.0)
	public File fileName = null;

	public ExportEnrichmentTableTask(StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
		// file = new File("test.csv");
	}

	public ExportEnrichmentTableTask(StringManager manager, CyNetwork network, CyTable table,
			File file) {
		this.manager = manager;
		this.network = network;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		ExportTableTaskFactory exportTF = manager.getService(ExportTableTaskFactory.class);
		CyTable selectedTable = ModelUtils.getEnrichmentTable(manager, network,
                TermCategory.ALL.getTable());

		if (selectedTable != null && fileName != null) {
			File file = fileName;
			taskMonitor.showMessage(TaskMonitor.Level.INFO,
					"export table " + selectedTable + " to " + file.getAbsolutePath());
			TaskIterator ti = exportTF.createTaskIterator(selectedTable, file);
			insertTasksAfterCurrentTask(ti);
		}
	}

	@ProvidesTitle
	public String getTitle() {
		return "Export STRING Enrichment table";
	}
}
