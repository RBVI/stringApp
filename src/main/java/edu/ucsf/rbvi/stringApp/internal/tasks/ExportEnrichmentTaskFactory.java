package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.EnrichmentUtils;

/**
 * Network task factory backing the command to export a network's STRING functional enrichment
 * results. It is ready only when the network has at least one enrichment table, and creates an
 * {@link ExportEnrichmentTableTask} for the "all categories" enrichment table, wired to the
 * south {@link EnrichmentCytoPanel} if one is open so the filtered-terms-only option works.
 */
public class ExportEnrichmentTaskFactory extends AbstractNetworkTaskFactory {
	// implements ExportTableTaskFactory {

	private StringManager manager;
	final CytoPanel cytoPanel;
	EnrichmentCytoPanel panel;

	public ExportEnrichmentTaskFactory(StringManager manager) {
		this.manager = manager;
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
		if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") > 0) {
			panel = (EnrichmentCytoPanel) cytoPanel.getComponentAt(
					cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
		}
	}

	public boolean isReady(CyNetwork network) {
		if (EnrichmentUtils.getAllEnrichmentTables(manager, network, EnrichmentTerm.ENRICHMENT_TABLE_PREFIX).size() > 0)
			return true;
		else
			return false;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new ExportEnrichmentTableTask(manager, network, panel,
				EnrichmentUtils.getEnrichmentTable(manager, network, TermCategory.ALL.getTable()), false));
	}

}
