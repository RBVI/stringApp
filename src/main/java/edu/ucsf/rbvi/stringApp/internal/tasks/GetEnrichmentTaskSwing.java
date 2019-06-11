package edu.ucsf.rbvi.stringApp.internal.tasks;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetEnrichmentTaskSwing extends GetEnrichmentTask {

	public GetEnrichmentTaskSwing(StringManager manager, CyNetwork network, CyNetworkView netView,
			ShowEnrichmentPanelTaskFactory showFactory, ShowPublicationsPanelTaskFactory showFactoryPubl, boolean publOnly) {
		super(manager, network, netView, showFactory, showFactoryPubl, publOnly);
	}

	@Override
	protected void showError(String msg) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, msg, "Unable to get enrichment", JOptionPane.ERROR_MESSAGE);
				}
			}
		);
	}

}
