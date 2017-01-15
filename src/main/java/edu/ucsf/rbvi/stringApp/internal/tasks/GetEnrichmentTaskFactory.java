package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetEnrichmentTaskFactory extends AbstractNetworkTaskFactory
		implements NetworkViewTaskFactory {
	final StringManager manager;
	ShowEnrichmentPanelTaskFactory showFactory;

	public GetEnrichmentTaskFactory(final StringManager manager) {
		this.manager = manager;
		showFactory = null;
	}

	public boolean isReady(CyNetwork network) {
		return ModelUtils.isStringNetwork(network);
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new GetEnrichmentTask(manager, network, null, showFactory));
	}

	public boolean isReady(CyNetworkView netView) {
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new GetEnrichmentTask(manager, netView.getModel(), netView, showFactory));
	}

	public void setShowEnrichmentPanelFactory(ShowEnrichmentPanelTaskFactory showFactory) {
		this.showFactory = showFactory;
	}

}
