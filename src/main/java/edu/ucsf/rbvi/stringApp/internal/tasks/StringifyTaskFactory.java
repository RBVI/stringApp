package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringifyTaskFactory extends AbstractNetworkTaskFactory implements TaskFactory {
	final StringManager manager;

	public StringifyTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetwork net) {
		if (!manager.haveURIs() || net == null) return false;

		// Are we already a string network?
		if (ModelUtils.isStringNetwork(net)) return false;

		return true;
	}

	public TaskIterator createTaskIterator(CyNetwork net) {
		return new TaskIterator(new StringifyTask(manager, net));
	}

	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new StringifyTask(manager, null));
	}

}

