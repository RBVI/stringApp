package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetSpeciesDetailedTaskFactory extends AbstractTaskFactory {
	final StringManager manager;

	public GetSpeciesDetailedTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady() {
		return manager.haveURIs() && true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new GetSpeciesDetailedTask(manager));
	}

}
