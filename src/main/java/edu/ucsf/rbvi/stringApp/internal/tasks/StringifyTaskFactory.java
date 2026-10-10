package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Task factory, usable both as a network task factory and a no-argument
 * {@link TaskFactory} (for command-line use), that creates a
 * {@link StringifyTask} to convert an existing network's nodes into a STRING
 * network by mapping a chosen node column to STRING/STITCH identifiers. It is
 * ready for any network that is not already recognized as a STRING network.
 */
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

