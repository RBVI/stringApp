package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;

import javax.swing.JOptionPane;

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
	ShowEnrichmentPanelTaskFactory showFactoryEnrich;
	ShowPublicationsPanelTaskFactory showFactoryPubl;
	public static String EXAMPLE_JSON = GetEnrichmentTask.EXAMPLE_JSON;
	boolean hasGUI = false;

	public GetEnrichmentTaskFactory(final StringManager manager, boolean hasGUI) {
		this.manager = manager;
		showFactoryEnrich = null;
		showFactoryPubl = null;
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
			// check if we have a current STRING network and if not, notify user and ask to requery
			if (!ModelUtils.isCurrentDataVersion(network) && JOptionPane.showConfirmDialog(null,
					ModelUtils.REQUERY_MSG_USER, ModelUtils.REQUERY_TITLE, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
				return new TaskIterator(new RequeryTask(manager, network));
			} else {
				return new TaskIterator(new GetEnrichmentTaskSwing(manager, network, null, showFactoryEnrich, null, false));
			}				
		} else {
			return new TaskIterator(new GetEnrichmentTask(manager, network, null, showFactoryEnrich, null, false));
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
			// check if we have a current STRING network and if not, notify user and ask to requery
			if (!ModelUtils.isCurrentDataVersion(netView.getModel()) && JOptionPane.showConfirmDialog(null,
					ModelUtils.REQUERY_MSG_USER, ModelUtils.REQUERY_TITLE, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
				return new TaskIterator(new RequeryTask(manager, netView.getModel()));
			} else {
				return new TaskIterator(new GetEnrichmentTaskSwing(manager, netView.getModel(), netView, showFactoryEnrich, null, false));
			}
		} else {
			return new TaskIterator(new GetEnrichmentTask(manager, netView.getModel(), netView, showFactoryEnrich, null, false));
		}
	}

	public void setShowEnrichmentPanelFactory(ShowEnrichmentPanelTaskFactory showFactoryEnrich) {
		this.showFactoryEnrich = showFactoryEnrich;
	}

}
