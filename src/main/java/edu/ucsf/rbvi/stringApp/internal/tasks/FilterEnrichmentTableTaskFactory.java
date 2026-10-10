package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class FilterEnrichmentTableTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	boolean show = false;
	final CytoPanel cytoPanel;
	EnrichmentCytoPanel panel;

	public FilterEnrichmentTableTaskFactory(final StringManager manager) {
		this.manager = manager;
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
	}

	public TaskIterator createTaskIterator() {
		if (panel != null)
			return new TaskIterator(new FilterEnrichmentTableTask(manager, panel));
		else 
			return new TaskIterator(new FilterEnrichmentTableTask(manager));
	}

	public boolean isReady() {
		CyNetwork net = manager.getCurrentNetwork();
		if (net == null)
			return false;

		if (!ModelUtils.isStringNetwork(net)) 
			return false;

		if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") < 0) {
			return false;
		}

		panel = (EnrichmentCytoPanel) cytoPanel.getComponentAt(
					cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));

		if (panel == null) return false;

		return true;
	}
}
