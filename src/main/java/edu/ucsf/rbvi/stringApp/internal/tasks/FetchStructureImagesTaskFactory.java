package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class FetchStructureImagesTaskFactory extends AbstractNetworkTaskFactory implements TaskFactory {
	final StringManager manager;

	public FetchStructureImagesTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return manager.haveURIs() && true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new FetchStructureImagesTask(manager, manager.getCurrentNetwork()));
	}

	public boolean isReady(CyNetwork network) {
		if (manager.haveURIs() && ModelUtils.isStringNetwork(network)) {
			return true;
		}
		return false;
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new FetchStructureImagesTask(manager, network));
	}

	public boolean isReady(CyNetworkView netView) {
		if (manager.haveURIs() && ModelUtils.isStringNetwork(netView.getModel())) {
			return true;
		}
		return false;
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new FetchStructureImagesTask(manager, netView.getModel()));
	}
}
