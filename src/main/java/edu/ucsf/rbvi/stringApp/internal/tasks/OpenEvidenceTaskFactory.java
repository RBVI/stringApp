package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class OpenEvidenceTaskFactory extends AbstractNodeViewTaskFactory {
	final StringManager manager;

	public OpenEvidenceTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean isReady(View<CyNode> nodeView, CyNetworkView netView) {
		if (netView == null || nodeView == null) return false;

		CyNetwork network = netView.getModel();
		CyNode node = nodeView.getModel();
		if (ModelUtils.isStringNetwork(network) == false) return false;

		String url = getURL(network, node);
		if (url == null) return false;
		return true;
	}

	@Override
	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView netView) {
		CyNetwork network = netView.getModel();
		CyNode node = nodeView.getModel();
		String url = getURL(network, node);

		return new TaskIterator(new OpenEvidenceTask(manager, url));
	}

	private String getURL(CyNetwork network, CyNode node) {
		String url = network.getRow(node).get(ModelUtils.TM_LINKOUT, String.class);
		return url;
	}


}

