package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedFloat;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.ui.GetTermsPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class AddTermsTask extends AbstractTask {
	final StringManager manager;
	final CyNetwork network;
	CyNetworkView netView;

	public AddTermsTask(final StringManager manager, final CyNetwork network, CyNetworkView netView) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;
	}

	public void run(TaskMonitor monitor) {
		StringNetwork stringNetwork = manager.getStringNetwork(network);
		JFrame parent = manager.getService(CySwingApplication.class).getJFrame();
		// Get AddTerms dialog
		JDialog termsDialog = new JDialog(parent, "Query for additional nodes");
		String database = ModelUtils.getDatabase(network);
		GetTermsPanel termsPanel = new GetTermsPanel(manager, stringNetwork, database);
		termsDialog.setContentPane(termsPanel);
		// Pack it and display it
		termsDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		termsDialog.pack(); 
		termsDialog.setVisible(true);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Query for additional nodes";
	}
}
