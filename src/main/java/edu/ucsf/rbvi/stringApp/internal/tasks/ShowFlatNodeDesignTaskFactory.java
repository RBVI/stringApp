package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowFlatNodeDesignTaskFactory extends AbstractNetworkViewTaskFactory implements TaskFactory {

	final StringManager manager;
	final boolean show;

	public ShowFlatNodeDesignTaskFactory(final StringManager manager) {
		this.manager = manager;
		this.show = false;
	}

	public ShowFlatNodeDesignTaskFactory(final StringManager manager, final boolean show) {
		this.manager = manager;
		this.show = show;
	}

	public boolean isReady(CyNetworkView netView) {
		if (netView == null)
			return false;
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	@Override
	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new ShowFlatNodeDesignTask(manager, netView, this));
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowFlatNodeDesignTask(manager, show, this));
	}

}
