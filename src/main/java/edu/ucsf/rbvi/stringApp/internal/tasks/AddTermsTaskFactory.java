package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class AddTermsTaskFactory extends AbstractNetworkTaskFactory implements NetworkViewTaskFactory {
	final StringManager manager;

	public AddTermsTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetwork network) {
		return manager.haveURIs() && ModelUtils.isStringNetwork(network);
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (ModelUtils.isCurrentDataVersion(network)) {
			return new TaskIterator(new AddTermsTask(manager, network, null));
		} else {
			return new TaskIterator(new RequeryTask(manager, network));
		}
	}

	public boolean isReady(CyNetworkView netView) {
		return manager.haveURIs() && ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		// check if we have a current STRING network and if not, notify user and ask to requery
		if (ModelUtils.isCurrentDataVersion(netView.getModel())) {
			return new TaskIterator(new AddTermsTask(manager, netView.getModel(), netView));
		} else {
			return new TaskIterator(new RequeryTask(manager, netView.getModel()));
		}
	}
}

