package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

/**
 * Task factory that creates an {@link AddNamespacesTask} to migrate STRING networks created
 * with an older data version to the current namespaced column naming scheme (e.g. prefixing
 * node/edge columns with the STRINGDB, tissue, or compartment namespace). It can be used both
 * for a specific set of networks and, via the no-argument variant, for networks discovered
 * elsewhere in the app.
 */
public class AddNamespacesTaskFactory extends AbstractTaskFactory implements TaskFactory {
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


	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AddNamespacesTask(manager));
	}

}

