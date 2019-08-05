package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ChangeConfidenceTaskFactory extends AbstractNetworkTaskFactory implements TaskFactory {
	final StringManager manager;

	public ChangeConfidenceTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ChangeConfidenceTask(manager, null, null));
	}

	public boolean isReady(CyNetwork network) {
		if (network == null)
			return false;
		return ModelUtils.isStringNetwork(network);
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (ModelUtils.isCurrentDataVersion(network)) {
			return new TaskIterator(new ChangeConfidenceTask(manager, network, null));
		} else {
			return new TaskIterator(new RequeryTask(manager, network));
		}
	}

	public boolean isReady(CyNetworkView netView) {
		if (netView == null)
			return false;
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (ModelUtils.isCurrentDataVersion(netView.getModel())) {
			return new TaskIterator(new ChangeConfidenceTask(manager, netView.getModel(), netView));
		} else {
			return new TaskIterator(new RequeryTask(manager, netView.getModel()));
		}
	}

}

