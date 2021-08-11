package edu.ucsf.rbvi.stringApp.internal.tasks;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ChangeNetTypeTaskFactory extends AbstractNetworkTaskFactory implements TaskFactory {
	final StringManager manager;

	public ChangeNetTypeTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ChangeNetTypeTask(manager, null, null));
	}

	public boolean isReady(CyNetwork network) {
		if (network == null)
			return false;
		return ModelUtils.isStringNetwork(network);
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (!ModelUtils.isCurrentDataVersion(network) && JOptionPane.showConfirmDialog(null,
				ModelUtils.REQUERY_MSG_USER, ModelUtils.REQUERY_TITLE, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
			return new TaskIterator(new RequeryTask(manager, network));
		} else {
			return new TaskIterator(new ChangeNetTypeTask(manager, network, null));
		}
	}

	public boolean isReady(CyNetworkView netView) {
		if (netView == null)
			return false;
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (!ModelUtils.isCurrentDataVersion(netView.getModel()) && JOptionPane.showConfirmDialog(null,
				ModelUtils.REQUERY_MSG_USER, ModelUtils.REQUERY_TITLE, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
			return new TaskIterator(new RequeryTask(manager, netView.getModel()));
		} else {
			return new TaskIterator(new ChangeNetTypeTask(manager, netView.getModel(), netView));
		}
	}

}

