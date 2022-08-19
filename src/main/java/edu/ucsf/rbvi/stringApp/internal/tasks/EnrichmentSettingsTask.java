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
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.model.ChartType;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class EnrichmentSettingsTask extends AbstractTask {

	private StringManager manager;
	private CyNetwork network;
	private String group;

	@ContainsTunables
	public EnrichmentSettings enrichmentSettings;

	@Tunable(description = "Make these settings the default",
	         longDescription = "Unless this is set to true, these settings only apply to the current network",
	         tooltip = "<html>Unless this is set to true, these settings only apply to the current network.</html>")
	public boolean makeDefault = false;

	// TOOD: [N] remove this one or not?
	// TOOD: [N] is it ok to use all here?
	public EnrichmentSettingsTask(StringManager manager) {
		this.network = manager.getCurrentNetwork();
		this.manager = manager;
		this.group = TermCategory.ALL.getTable();
		enrichmentSettings = new EnrichmentSettings(manager, network, group);
	}

	// TODO: [N] is it ok to just add the group here?
	public EnrichmentSettingsTask(StringManager manager, String group) {
		this.network = manager.getCurrentNetwork();
		this.manager = manager;
		this.group = group;
		enrichmentSettings = new EnrichmentSettings(manager, network, group);
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		arg0.setTitle("Enrichment settings");
		if (makeDefault) {
			manager.setTopTerms(null,enrichmentSettings.nTerms.getValue(), group);
			manager.setOverlapCutoff(null,enrichmentSettings.overlapCutoff.getValue(), group);
			manager.setEnrichmentPalette(null,enrichmentSettings.defaultEnrichmentPalette.getSelectedValue(), group);
			manager.setChartType(null,enrichmentSettings.chartType.getSelectedValue(), group);
			manager.updateSettings();
		}

		manager.setTopTerms(network,enrichmentSettings.nTerms.getValue(), group);
		manager.setOverlapCutoff(network,enrichmentSettings.overlapCutoff.getValue(), group);
		manager.setEnrichmentPalette(network,enrichmentSettings.defaultEnrichmentPalette.getSelectedValue(), group);
		manager.setChartType(network,enrichmentSettings.chartType.getSelectedValue(), group);

		// TODO: maybe this is a way to automatically apply settings?
		TaskManager<?, ?> tm = (TaskManager<?, ?>) manager.getService(TaskManager.class);
		tm.execute(new ShowChartsTaskFactory(manager).createTaskIterator());
	}

	@ProvidesTitle
	public String getTitle() {
		return "Network-specific settings for STRING Enrichment table";
	}	
}
