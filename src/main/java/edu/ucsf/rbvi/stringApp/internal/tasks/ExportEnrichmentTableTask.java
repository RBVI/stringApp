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
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ExportEnrichmentTableTask extends AbstractTask {

	private StringManager manager;
	private EnrichmentCytoPanel enrichmentPanel;
	private CyTable selectedTable;

	@Tunable(description = "Save Table as", params = "input=false", 
	         tooltip="<html>Note: for convenience spaces are replaced by underscores.</html>", gravity = 2.0)
	public File fileName = null;

	@Tunable(description = "Filtered terms only",  
			longDescription = "Save only the enrichment terms after filtering.",
			 exampleStringValue = "false", gravity = 3.0)
	public boolean filtered = false;

	public ExportEnrichmentTableTask(StringManager manager, CyNetwork network, EnrichmentCytoPanel panel, CyTable table) {
		this.manager = manager;
		this.enrichmentPanel = panel;
		this.selectedTable = table;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		ExportTableTaskFactory exportTF = manager.getService(ExportTableTaskFactory.class);
		
		if (selectedTable != null && fileName != null) {
			File file = fileName;
			if (filtered && enrichmentPanel != null) {
				selectedTable = enrichmentPanel.getFilteredTable();
			}
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
