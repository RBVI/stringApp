package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetSpeciesTaskFactory extends AbstractTaskFactory {
	final StringManager manager;

	public GetSpeciesTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new GetSpeciesTask(manager));
	}

}
