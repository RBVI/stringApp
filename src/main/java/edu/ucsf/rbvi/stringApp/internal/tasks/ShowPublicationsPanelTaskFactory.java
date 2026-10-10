package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowPublicationsPanelTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	boolean show = false;
	boolean noSignificant = false;

	public ShowPublicationsPanelTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowPublicationsPanelTask(manager, this, show, noSignificant));
	}


	public boolean isReady() {
		// We always want to be able to shut it off
		if (!show)
			return true;

		CyNetwork net = manager.getCurrentNetwork();
		if (net == null)
			return false;

		if (ModelUtils.isStringNetwork(net)) 
			return true;

		return false;
	}
}
