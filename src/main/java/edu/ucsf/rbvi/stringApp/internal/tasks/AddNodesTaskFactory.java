package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class AddNodesTaskFactory extends AbstractNetworkTaskFactory implements NetworkViewTaskFactory, NodeViewTaskFactory {
	final StringManager manager;

	public AddNodesTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetwork network) {
		return ModelUtils.isStringNetwork(network);
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new AddNodesTask(manager, network, null));
	}

	public boolean isReady(CyNetworkView netView) {
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new AddNodesTask(manager, netView.getModel(), netView));
	}

	public boolean isReady(View<CyNode> nodeView, CyNetworkView netView) {
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView netView) {
		return new TaskIterator(new AddNodesTask(manager, netView.getModel(), netView, nodeView));
	}
}

