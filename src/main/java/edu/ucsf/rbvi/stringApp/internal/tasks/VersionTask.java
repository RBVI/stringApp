package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

public class VersionTask extends AbstractTask implements ObservableTask {

	final String version;
	public VersionTask(final String version) {
			this.version = version;
	}

	public void run(TaskMonitor monitor) {}

	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(String.class)) {
			String response = "Version: "+version+"\n";
			return (R)response;
		}
		return null;
	}
}
