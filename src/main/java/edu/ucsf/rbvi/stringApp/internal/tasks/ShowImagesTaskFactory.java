package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowImagesTaskFactory extends AbstractNetworkViewTaskFactory implements TaskFactory {
	final StringManager manager;
	final boolean show;

	public ShowImagesTaskFactory(final StringManager manager) {
		this.manager = manager;
		this.show = false;
	}

	public ShowImagesTaskFactory(final StringManager manager, final boolean show) {
		this.manager = manager;
		this.show = show;
	}

	@Override
	public boolean isReady(CyNetworkView netView) {
		if (netView == null) return false;
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	@Override
	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new ShowImagesTask(manager, netView, this));
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowImagesTask(manager, show, this));
	}

}

