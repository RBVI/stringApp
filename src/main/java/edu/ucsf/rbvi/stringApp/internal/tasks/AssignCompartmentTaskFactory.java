package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class AssignCompartmentTaskFactory extends AbstractNetworkTaskFactory implements TaskFactory {

	final StringManager manager;
	
	public AssignCompartmentTaskFactory(StringManager manager) {
		this.manager = manager;
	}
	
	@Override
	public boolean isReady() {
		return true;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AssignCompartmentTask(manager, null));	
	}

	public boolean isReady(CyNetwork network) {
		return manager.haveURIs() && ModelUtils.isStringNetwork(network);
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new AssignCompartmentTask(manager, network));	}

}
