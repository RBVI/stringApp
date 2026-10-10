package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Task factory, usable both as a network-view task factory and a no-argument
 * {@link TaskFactory} (for command-line use), that creates a
 * {@link ShowGlassBallEffectTask} to toggle STRING's glass ball node effect.
 * The view-based form is ready only for STRING network views.
 */
public class ShowGlassBallEffectTaskFactory extends AbstractNetworkViewTaskFactory implements TaskFactory {

	final StringManager manager;
	final boolean show;

	public ShowGlassBallEffectTaskFactory(final StringManager manager) {
		this.manager = manager;
		this.show = false;
	}

	public ShowGlassBallEffectTaskFactory(final StringManager manager, final boolean show) {
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
		return new TaskIterator(new ShowGlassBallEffectTask(manager, netView, this));
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowGlassBallEffectTask(manager, show, this));
	}


}
