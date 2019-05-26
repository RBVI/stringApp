package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;

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
	ShowPublicationsPanelTaskFactory showFactoryPubl;
	public static String EXAMPLE_JSON = GetEnrichmentTask.EXAMPLE_JSON;
	boolean hasGUI = false;

	public GetEnrichmentTaskFactory(final StringManager manager, boolean hasGUI) {
		this.manager = manager;
		showFactory = null;
		this.hasGUI = hasGUI;
	}

	public boolean isReady(CyNetwork network) {
		if (manager.haveURIs() && ModelUtils.isStringNetwork(network)) {
			List<String> netSpecies = ModelUtils.getEnrichmentNetSpecies(network);
			if (netSpecies.size() > 0) {
				return true;
			}
		}
		return false;
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		if (hasGUI) {
			return new TaskIterator(new GetEnrichmentTaskSwing(manager, network, null, showFactory, showFactoryPubl));
		} else {
			return new TaskIterator(new GetEnrichmentTask(manager, network, null, showFactory, showFactoryPubl));
		}
	}

	public boolean isReady(CyNetworkView netView) {
		if (manager.haveURIs() && ModelUtils.isStringNetwork(netView.getModel())) {
			List<String> netSpecies = ModelUtils.getEnrichmentNetSpecies(netView.getModel());
			if (netSpecies.size() > 0) {
				return true;
			}
		}
		return false;
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		if (hasGUI) {
			return new TaskIterator(new GetEnrichmentTaskSwing(manager, netView.getModel(), netView, showFactory, showFactoryPubl));
		} else {
			return new TaskIterator(new GetEnrichmentTask(manager, netView.getModel(), netView, showFactory, showFactoryPubl));
		}
	}

	public void setShowEnrichmentPanelFactory(ShowEnrichmentPanelTaskFactory showFactory) {
		this.showFactory = showFactory;
	}

	public void setShowPublicationsPanelFactory(ShowPublicationsPanelTaskFactory showFactory) {
		this.showFactoryPubl = showFactory;
	}
}
