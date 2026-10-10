package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Task factory, constructed with the app's version string, that creates a
 * {@link VersionTask} reporting the stringApp version (backs the "version"
 * command).
 */
public class VersionTaskFactory extends AbstractTaskFactory {

	final String version;
	public VersionTaskFactory(final String version) {
			this.version = version;
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new VersionTask(version));
	}
}
