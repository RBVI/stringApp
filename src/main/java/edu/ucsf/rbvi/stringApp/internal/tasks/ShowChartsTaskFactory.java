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

/**
 * Task factory that creates a {@link ShowChartsTask} to (re)draw the enrichment
 * charts in the south {@link EnrichmentCytoPanel}. It is only ready when the
 * current network is a STRING network and the enrichment panel is currently
 * registered.
 */
public class ShowChartsTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	final CytoPanel cytoPanel;
	EnrichmentCytoPanel panel;

	public ShowChartsTaskFactory(final StringManager manager) {
		this.manager = manager;
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
	}

	public TaskIterator createTaskIterator() {
		if (!isReady())
			return new TaskIterator();
		return new TaskIterator(new ShowChartsTask(manager, panel));
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
