package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.model.ChartType;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class EnrichmentSettingsTask extends AbstractTask {

	private StringManager manager;
	private CyNetwork network;

	@ContainsTunables
	public EnrichmentSettings enrichmentSettings;

	@Tunable(description = "Make these settings the default",
	         longDescription = "Unless this is set to true, these settings only apply to the current network",
	         tooltip = "<html>Unless this is set to true, these settings only apply to the current network.</html>")
	public boolean makeDefault = false;

	public EnrichmentSettingsTask(StringManager manager) {
		this.network = manager.getCurrentNetwork();
		this.manager = manager;
		enrichmentSettings = new EnrichmentSettings(manager, network);
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		if (makeDefault) {
			manager.setTopTerms(null,enrichmentSettings.nTerms.getValue());
			manager.setOverlapCutoff(null,enrichmentSettings.overlapCutoff.getValue());
			manager.setBrewerPalette(null,enrichmentSettings.defaultPalette.getSelectedValue());
			manager.setChartType(null,enrichmentSettings.chartType.getSelectedValue());
			manager.updateSettings();
		} else {
			manager.setTopTerms(network,enrichmentSettings.nTerms.getValue());
			manager.setOverlapCutoff(network,enrichmentSettings.overlapCutoff.getValue());
			manager.setBrewerPalette(network,enrichmentSettings.defaultPalette.getSelectedValue());
			manager.setChartType(network,enrichmentSettings.chartType.getSelectedValue());
		}
		// TODO: maybe this is a way to automatically apply settings?
		// TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		// tm.execute(new ShowChartsTaskFactory(manager).createTaskIterator());
	}

	@ProvidesTitle
	public String getTitle() {
		return "Settings for STRING Enrichment table";
	}	
}
