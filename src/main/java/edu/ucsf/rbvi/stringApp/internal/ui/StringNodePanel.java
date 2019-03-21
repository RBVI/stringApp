package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.OpenBrowser;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public class StringNodePanel extends JPanel {

	final StringManager manager;
	final OpenBrowser openBrowser;
	final Font iconFont;
	final Font labelFont;
	final Font textFont;

	private JCheckBox enableGlass;
	private JCheckBox showStructure;
	private JCheckBox stringLabels;
	private JButton highlightQuery;
	private boolean updating = false;
	private CyNetwork currentNetwork;

	public StringNodePanel(final StringManager manager) {
		this.manager = manager;
		this.openBrowser = manager.getService(OpenBrowser.class);
		this.currentNetwork = manager.getCurrentNetwork();
		IconManager iconManager = manager.getService(IconManager.class);
		iconFont = iconManager.getIconFont(17.0f);
		labelFont = new Font("SansSerif", Font.BOLD, 10);
		textFont = new Font("SansSerif", Font.PLAIN, 10);
		init();
		revalidate();
		repaint();
	}

	public void updateControls() {
		updating = true;
		enableGlass.setSelected(manager.showGlassBallEffect());
		showStructure.setSelected(manager.showImage());
		stringLabels.setSelected(manager.showEnhancedLabels());
		if (!manager.showGlassBallEffect())
			showStructure.setEnabled(false);
		else
			showStructure.setEnabled(true);
		updating = false;
	}

	private void init() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(createControlPanel(), BorderLayout.NORTH);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		mainPanel.add(createTissuesPanel());
		mainPanel.add(createCompartmentsPanel());
		mainPanel.add(createNodesPanel());
		mainPanel.add(Box.createVerticalGlue());
		JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);
	}

	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel();
		GridLayout layout = new GridLayout(3,2);
		layout.setVgap(0);
		controlPanel.setLayout(layout);
		{
			enableGlass = new JCheckBox("Glass Ball Effect");
			enableGlass.setFont(labelFont);
			enableGlass.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (updating) return;
					manager.execute(
						manager.getGlassBallTaskFactory().createTaskIterator(manager.getCurrentNetworkView()), true);
				}
			});
			controlPanel.add(enableGlass);
		}
		
		{
			showStructure = new JCheckBox("Structure Images");
			showStructure.setFont(labelFont);
			// showStructure.setBorder(null);
			showStructure.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (updating) return;
					manager.execute(
						manager.getImagesTaskFactory().createTaskIterator(manager.getCurrentNetworkView()), true);
				}
			});
			controlPanel.add(showStructure);
		}
		
		{
			stringLabels = new JCheckBox("String-Style Labels");
			stringLabels.setFont(labelFont);
			stringLabels.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (updating) return;
					manager.execute(
						manager.getEnhancedLabelsTaskFactory().createTaskIterator(manager.getCurrentNetworkView()), true);
				}
			});
			controlPanel.add(stringLabels);
		}

		controlPanel.add(new JLabel(""));

		{
			JButton getEnrichment = new JButton("Get Enrichment");
			getEnrichment.setFont(labelFont);
			controlPanel.add(getEnrichment);
		}

		{
			highlightQuery = new JButton("Select Query");
			highlightQuery.setFont(labelFont);

			// See if we have anything in "query term"
			if (!ModelUtils.haveQueryTerms(currentNetwork))
				highlightQuery.setEnabled(false);

			highlightQuery.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ModelUtils.selectQueryTerms(currentNetwork);
				}
			});
			controlPanel.add(highlightQuery);
		}

		updateControls();
		return controlPanel;
	}

	private JPanel createTissuesPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		List<String> tissueList = ModelUtils.getTissueList(currentNetwork);
		for (String tissue: tissueList) {
			panel.add(createFilterSlider(tissue));
		}

		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Tissues Filters", panel, true, 10);
		collapsablePanel.setMaximumSize(new Dimension(500, 20));
		collapsablePanel.setBorder(BorderFactory.createEmptyBorder());
		return collapsablePanel;
	}

	private JPanel createCompartmentsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		List<String> compartmentList = ModelUtils.getCompartmentList(currentNetwork);
		for (String compartment: compartmentList) {
			panel.add(createFilterSlider(compartment));
		}
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Compartments Filter", panel, true, 10);
		collapsablePanel.setMaximumSize(new Dimension(500, 20));
		collapsablePanel.setBorder(BorderFactory.createEmptyBorder());
		return collapsablePanel;
	}

	private JPanel createNodesPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Selected Nodes", panel, true, 10);
		collapsablePanel.setMaximumSize(new Dimension(500, 20));
		collapsablePanel.setBorder(BorderFactory.createEmptyBorder());
		return collapsablePanel;
	}

	private JComponent createFilterSlider(String text) {
		Box box = Box.createHorizontalBox();
		JLabel label = new JLabel(text);
		label.setFont(labelFont);
		label.setPreferredSize(new Dimension(100,20));
		box.add(Box.createRigidArea(new Dimension(10,0)));
		box.add(label);
		box.add(Box.createHorizontalGlue());
		JSlider slider = new JSlider(0,5,5);
		slider.setPreferredSize(new Dimension(100,20));
		box.add(slider);
		box.add(Box.createHorizontalGlue());
		JTextField textField = new JTextField("5",1);
		textField.setFont(textFont);
		textField.setPreferredSize(new Dimension(10,20));
		textField.setMaximumSize(new Dimension(10,20));
		box.add(textField);
		// Hook it up
		addChangeListeners("tissues", text, slider, textField);
		return box;
	}

	private void addChangeListeners(String type, String label, JSlider slider, JTextField textField) {
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider sl = (JSlider)e.getSource();
				int value = sl.getValue();
				textField.setText(Integer.toString(value));
				doFilter(type, label, value);
			}
		});

		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JTextField field = (JTextField)e.getSource();
				try {
					int value = Integer.parseInt(field.getText());
					slider.setValue(value);
				} catch (Exception ex) {
					// not an int?
					field.setText(Integer.toString(slider.getValue()));
				}
			}
		});
	}

	private void doFilter(String type, String label, int value) {
	}

	public void networkChanged(CyNetwork newNetwork) {
		this.currentNetwork = newNetwork;
		if (!ModelUtils.haveQueryTerms(currentNetwork))
			highlightQuery.setEnabled(false);
		else
			highlightQuery.setEnabled(true);
	}

	public void selectedNodes(Collection<CyNode> nodes) {
	}
}
