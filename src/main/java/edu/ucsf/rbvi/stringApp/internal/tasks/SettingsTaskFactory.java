package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

/**
 * Task factory that creates the {@link SettingsTask} backing the stringApp
 * "Settings" dialog, used to edit the app's default query, view, and
 * enrichment preferences.
 */
public class SettingsTaskFactory extends AbstractTaskFactory {
	final StringManager manager;

	public SettingsTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new SettingsTask(manager));
	}

}
