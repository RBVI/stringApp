package edu.ucsf.rbvi.stringApp.internal.tasks;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.List;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
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
		return new TaskIterator(new FilterEnrichmentTableTask(manager, panel));
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
