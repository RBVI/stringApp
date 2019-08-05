package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.util.UserAction;

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class RequeryTask extends AbstractTask implements ActionListener {

	final StringManager manager;
	final CyNetwork network;
	private JButton btnOK = null;
	private JButton btnCancel = null;
	private JDialog confirmDialog = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);
	private TaskMonitor monitor;

	//@Tunable(description = "Get the latest STRING network for your nodes", gravity = 1.0, required = true)
	//public boolean requery = true;

	//@Tunable(description = "Get the latest STRING network for your nodes", gravity = 1.0, required = true)
	//public UserAction requeryBtn = new UserAction(this);

	public RequeryTask(final StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
	}

	public void run(TaskMonitor aMonitor) {
		this.monitor = aMonitor;
		// monitor.setTitle("Re-query network");
		// int userChoice = JOptionPane.showConfirmDialog(null,
		// "<html>It appears that you have an old STRING network. Would you like to <br />"
		// + "get the latest STRING network for the nodes in your network?</html>",
		// "Re-query network?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		// System.out.println("user choice" + userChoice);
		JFrame parent = manager.getService(CySwingApplication.class).getJFrame();
		confirmDialog = new JDialog(parent, "Re-query");

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		JPanel panelLabel = new JPanel(new BorderLayout());
		JLabel usrMessage = new JLabel(
				"<html>It appears that you have an old STRING network. Would you like to <br />"
						+ "get the latest STRING v11 network for the nodes in your network?</html>");
		panelLabel.add(usrMessage, BorderLayout.CENTER);
		panelLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		panel.add(panelLabel, BorderLayout.CENTER);
		btnCancel = new JButton("No");
		btnCancel.addActionListener(this);
		btnOK = new JButton("Yes");
		btnOK.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(btnCancel);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(btnOK);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		confirmDialog.setContentPane(panel);
		// Pack it and display it
		confirmDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		confirmDialog.pack();
		confirmDialog.setLocationRelativeTo(null);
		confirmDialog.setVisible(true);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Re-query network";
	}

	@Override
	public void cancel() {

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(btnOK)) {
			// TODO: check for species and cutoff!
			StringifyTask strTask = new StringifyTask(manager, network, 0.4,
					Species.getSpecies("Homo sapiens"), "canonical name");
			strTask.run(monitor);
		}
		confirmDialog.dispose();
	}

}
