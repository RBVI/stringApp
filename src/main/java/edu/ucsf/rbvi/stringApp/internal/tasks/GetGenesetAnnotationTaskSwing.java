package edu.ucsf.rbvi.stringApp.internal.tasks;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetGenesetAnnotationTaskSwing extends GetGenesetAnnotationTask {

	public GetGenesetAnnotationTaskSwing(StringManager manager, CyNetwork network, CyNetworkView netView) {
		super(manager, network, netView);
	}

	@Override
	protected void showError(String msg) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, msg, "Unable to get annotation", JOptionPane.ERROR_MESSAGE);
				}
			}
		);
	}

}
