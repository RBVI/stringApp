package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetSelectedSpeciesTaskFactory extends AbstractTaskFactory {
	final StringManager manager;

	public GetSelectedSpeciesTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return manager.haveURIs() && true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new GetSelectedSpeciesTask(manager));
	}

}
