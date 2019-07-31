package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class AddNamespacesTaskFactory extends AbstractNetworkTaskFactory implements TaskFactory {
	final StringManager manager;

	public AddNamespacesTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(List<CyNetwork> networks) {
		if (!manager.haveURIs() || networks.size() == 0) return false;

		return true;
	}

	public TaskIterator createTaskIterator(Set<CyNetwork> networks) {
		return new TaskIterator(new AddNamespacesTask(manager, networks));
	}


	public boolean isReady(CyNetwork net) {
		if (!manager.haveURIs() || net == null) return false;

		return true;
	}

	public TaskIterator createTaskIterator(CyNetwork net) {
		return new TaskIterator(new AddNamespacesTask(manager, net));
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AddNamespacesTask(manager));
	}

}

