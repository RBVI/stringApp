package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

/**
 * Task factory for the "list species" command, creating a {@link GetSpeciesTask} that
 * returns the full set of species known to the current STRING/STITCH installation.
 * Ready as soon as the manager has resolved its database URIs.
 */
public class GetSpeciesTaskFactory extends AbstractTaskFactory {
	final StringManager manager;

	public GetSpeciesTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return manager.haveURIs() && true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new GetSpeciesTask(manager));
	}

}
