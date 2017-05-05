package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class SetLabelAttributeTaskFactory extends AbstractNetworkTaskFactory {

	final StringManager manager;

	public SetLabelAttributeTaskFactory(StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetwork network) {
		return (ModelUtils.isStringNetwork(network) && manager.showEnhancedLabels());
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new SetLabelAttributeTask(manager, network));
	}

}
