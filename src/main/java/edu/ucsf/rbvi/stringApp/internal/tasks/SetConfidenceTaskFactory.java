package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class SetConfidenceTaskFactory extends AbstractNetworkTaskFactory {
	final StringManager manager;

	public SetConfidenceTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetwork net) {
		if (!manager.haveURIs() || net == null) return false;

		// Are we already a string network?
		if (ModelUtils.isStringNetwork(net)) return false;

		return ModelUtils.isMergedStringNetwork(net);
	}

	public TaskIterator createTaskIterator(CyNetwork net) {
		return new TaskIterator(new SetConfidenceTask(manager, net));
	}

}

