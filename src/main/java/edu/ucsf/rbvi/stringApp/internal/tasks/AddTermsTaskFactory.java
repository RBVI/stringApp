package edu.ucsf.rbvi.stringApp.internal.tasks;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Network and network-view task factory backing the "Query for additional nodes" command, which
 * lets the user add more STRING/STITCH terms to an existing STRING network. If the network's
 * data version is out of date it offers to requery via {@link RequeryTask}; otherwise it creates
 * an {@link AddTermsTask} that opens the terms-query dialog for the given network (and view).
 */
public class AddTermsTaskFactory extends AbstractNetworkTaskFactory
		implements NetworkViewTaskFactory {
	final StringManager manager;

	public AddTermsTaskFactory(final StringManager manager) {
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
			return new TaskIterator(new AddTermsTask(manager, network, null));
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
			return new TaskIterator(new AddTermsTask(manager, netView.getModel(), netView));
		}
	}
}
