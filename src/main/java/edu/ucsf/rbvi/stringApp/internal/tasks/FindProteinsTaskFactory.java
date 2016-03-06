package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class FindProteinsTaskFactory extends AbstractTaskFactory {
	final StringManager manager;

	public FindProteinsTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new FindProteinsTask(manager));
	}
}

