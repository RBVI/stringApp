package edu.ucsf.rbvi.stringApp.internal.tasks;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class RequeryTask extends AbstractTask {

	final StringManager manager;
	final CyNetwork network;
	//private JButton btnOK = null;
	//private JButton btnCancel = null;
	//private JDialog confirmDialog = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);
	private TaskMonitor monitor;

	// @Tunable(description = "Get the latest STRING network for your nodes", gravity = 1.0,
	// required = true)
	// public boolean requery = true;

	// @Tunable(description = "Get the latest STRING network for your nodes", gravity = 1.0,
	// required = true)
	// public UserAction requeryBtn = new UserAction(this);

	public RequeryTask(final StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
	}

	public void run(TaskMonitor aMonitor) {
		this.monitor = aMonitor;
		monitor.setTitle("Re-query network");
		// JFrame parent = manager.getService(CySwingApplication.class).getJFrame();
		// confirmDialog = new JDialog(parent, "Re-query");
		// JPanel panel = new JPanel(new BorderLayout(10, 10));
		// JPanel panelLabel = new JPanel(new BorderLayout());
		// JLabel usrMessage = new JLabel(
		// "<html>It appears that you have an old STRING network. Would you like to <br />"
		// + "get the latest STRING v11 network for the nodes in your network?</html>");
		// panelLabel.add(usrMessage, BorderLayout.CENTER);
		// panelLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		// panel.add(panelLabel, BorderLayout.CENTER);
		// btnCancel = new JButton("No");
		// btnCancel.addActionListener(this);
		// btnOK = new JButton("Yes");
		// btnOK.addActionListener(this);
		// JPanel buttonPanel = new JPanel();
		// buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		// buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		// buttonPanel.add(Box.createHorizontalGlue());
		// buttonPanel.add(btnCancel);
		// buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		// buttonPanel.add(btnOK);
		// panel.add(buttonPanel, BorderLayout.SOUTH);
		// confirmDialog.setContentPane(panel);
		// // Pack it and display it
		// confirmDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		// confirmDialog.pack();
		// confirmDialog.setLocationRelativeTo(null);
		// confirmDialog.setVisible(true);
		String sp = ModelUtils.getNetSpecies(network);
		Double conf = ModelUtils.getConfidence(network);
		NetworkType type = NetworkType.getType(ModelUtils.getNetworkType(network));
		if (type == null)
			type = manager.getDefaultNetworkType();
		StringifyTask strTask = new StringifyTask(manager, network, conf.doubleValue(),
				Species.getSpecies(sp), ModelUtils.CANONICAL, type);
		strTask.run(monitor);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Re-query network";
	}

	@Override
	public void cancel() {

	}

	// @Override
	// public void actionPerformed(ActionEvent e) {
	// if (e.getSource().equals(btnOK)) {
	// // TODO: check for species and cutoff!
	// StringifyTask strTask = new StringifyTask(manager, network, 0.4,
	// Species.getSpecies("Homo sapiens"), "canonical name");
	// strTask.run(monitor);
	// }
	// confirmDialog.dispose();
	// }

}
