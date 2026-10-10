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

public class HideChartsTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	final CytoPanel cytoPanel;
	EnrichmentCytoPanel panel;

	public HideChartsTaskFactory(final StringManager manager) {
		this.manager = manager;
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
	}

	public TaskIterator createTaskIterator() {
		if (!isReady())
			return new TaskIterator();
		return new TaskIterator(new HideChartsTask(manager, panel));
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

		return true;
	}
}
