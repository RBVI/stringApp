package edu.ucsf.rbvi.stringApp.internal.tasks;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ExpandNetworkTaskFactory extends AbstractNetworkTaskFactory implements NetworkViewTaskFactory, NodeViewTaskFactory, TaskFactory {
	final StringManager manager;

	public ExpandNetworkTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetwork network) {
		return manager.haveURIs() && ModelUtils.isStringNetwork(network);
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (!ModelUtils.isCurrentDataVersion(network) && JOptionPane.showConfirmDialog(null,
				ModelUtils.REQUERY_MSG_USER, ModelUtils.REQUERY_TITLE, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
			return new TaskIterator(new RequeryTask(manager, network));
		} else {
			return new TaskIterator(new ExpandNetworkTask(manager, network, null));
		}
	}

	public boolean isReady(CyNetworkView netView) {
		return manager.haveURIs() && ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (!ModelUtils.isCurrentDataVersion(netView.getModel()) && JOptionPane.showConfirmDialog(null,
				ModelUtils.REQUERY_MSG_USER, ModelUtils.REQUERY_TITLE, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
			return new TaskIterator(new RequeryTask(manager, netView.getModel()));
		} else {
			return new TaskIterator(new ExpandNetworkTask(manager, netView.getModel(), netView));
		}
	}

	public boolean isReady(View<CyNode> nodeView, CyNetworkView netView) {
		return manager.haveURIs() && ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView netView) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (!ModelUtils.isCurrentDataVersion(netView.getModel()) && JOptionPane.showConfirmDialog(null,
				ModelUtils.REQUERY_MSG_USER, ModelUtils.REQUERY_TITLE, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
			return new TaskIterator(new RequeryTask(manager, netView.getModel()));
		} else {
			return new TaskIterator(new ExpandNetworkTask(manager, netView.getModel(), netView, nodeView));
		}
	}

	public boolean isReady() {
		return manager.haveURIs() && true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ExpandNetworkTask(manager, null, null));
	}
}

