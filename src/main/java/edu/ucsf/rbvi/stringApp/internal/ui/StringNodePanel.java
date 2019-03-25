package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;


import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNode;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public class StringNodePanel extends AbstractStringPanel {

	private JCheckBox enableGlass;
	private JCheckBox showStructure;
	private JCheckBox stringLabels;
	private JCheckBox showSingletons;
	private JPanel tissuesPanel = null;
	private JPanel compartmentsPanel = null;
	private JPanel nodesPanel = null;
	private JButton highlightQuery;
	private boolean updating = false;
	private Color defaultBackground;
	private CyNode highlightNode = null;
	private JCheckBox highlightCheck = null;

	public StringNodePanel(final StringManager manager) {
		super(manager);
		filters.get(currentNetwork).put("tissue", new HashMap<>());
		filters.get(currentNetwork).put("compartment", new HashMap<>());
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
		setLayout(new GridBagLayout());

		EasyGBC c = new EasyGBC();
		add(new JSeparator(SwingConstants.HORIZONTAL), c.anchor("west").expandHoriz());

		JPanel controlPanel = createControlPanel();
		controlPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
		add(controlPanel, c.anchor("west").down().noExpand());

		JPanel mainPanel = new JPanel();
		{
			mainPanel.setLayout(new GridBagLayout());
			mainPanel.setBackground(defaultBackground);
			EasyGBC d = new EasyGBC();
			mainPanel.add(createTissuesPanel(), d.anchor("west").expandHoriz());

			mainPanel.add(createCompartmentsPanel(), d.down().anchor("west").expandHoriz());
			mainPanel.add(createNodesPanel(), d.down().anchor("west").expandHoriz());
			mainPanel.add(new JLabel(""), d.down().anchor("west").expandBoth());
		}
		JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(scrollPane, c.down().anchor("west").expandBoth());
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
		
		{
			showSingletons = new JCheckBox("Show singletons");
			showSingletons.setFont(labelFont);
			showSingletons.setSelected(true);
			showSingletons.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (updating) return;
					ViewUtils.hideSingletons(manager.getCurrentNetworkView(), showSingletons.isSelected());
				}
			});
			controlPanel.add(showSingletons);
		}

		{
			JButton getEnrichment = new JButton("Get Enrichment");
			getEnrichment.setFont(labelFont);
			controlPanel.add(getEnrichment);
			getEnrichment.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					GetEnrichmentTaskFactory tf = new GetEnrichmentTaskFactory(manager, true);
		      ShowEnrichmentPanelTaskFactory showTf = manager.getShowEnrichmentPanelTaskFactory();
					tf.setShowEnrichmentPanelFactory(showTf);
					manager.execute(tf.createTaskIterator(currentNetwork), false);
				}
			});
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
		controlPanel.setMaximumSize(new Dimension(300,100));
		controlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return controlPanel;
	}

	private JPanel createTissuesPanel() {
		tissuesPanel = new JPanel();
		tissuesPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		List<String> tissueList = ModelUtils.getTissueList(currentNetwork);
		for (String tissue: tissueList) {
			tissuesPanel.add(createFilterSlider("tissue", tissue, currentNetwork, true, Integer.class), 
			                 c.anchor("west").down().expandHoriz());
		}

		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Tissues Filters", tissuesPanel, true, 10);
		collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
		return collapsablePanel;
	}

	private void updateTissuesPanel() {
		if (tissuesPanel == null) return;
		tissuesPanel.removeAll();
		EasyGBC c = new EasyGBC();
		List<String> tissueList = ModelUtils.getTissueList(currentNetwork);
		for (String tissue: tissueList) {
			tissuesPanel.add(createFilterSlider("tissue", tissue, currentNetwork, true, Integer.class), 
			                 c.anchor("west").down().expandHoriz());
		}
		return;
	}

	private JPanel createCompartmentsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		List<String> compartmentList = ModelUtils.getCompartmentList(currentNetwork);
		for (String compartment: compartmentList) {
			panel.add(createFilterSlider("compartment", compartment, currentNetwork, true, Integer.class), 
			          c.anchor("west").down().expandHoriz());
		}
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Compartments Filter", panel, true, 10);
		collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
		return collapsablePanel;
	}

	private void updateCompartmentsPanel() {
		if (compartmentsPanel == null) return;
		compartmentsPanel.removeAll();
		EasyGBC c = new EasyGBC();
		List<String> compartmentsList = ModelUtils.getTissueList(currentNetwork);
		for (String compartments: compartmentsList) {
			compartmentsPanel.add(createFilterSlider("compartment", compartments, currentNetwork, true, Integer.class), 
			                      c.anchor("west").down().expandHoriz());
		}
		return;
	}

	private JPanel createNodesPanel() {
		nodesPanel = new JPanel();
		nodesPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		List<CyNode> nodes = CyTableUtil.getNodesInState(currentNetwork, CyNetwork.SELECTED, true);
		for (CyNode node: nodes) {
			JPanel newPanel = createNodePanel(node);
			newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

			nodesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
		}
		nodesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Selected Nodes", nodesPanel, false, 10);
		collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
		return collapsablePanel;
	}

	// Hide all nodes who's values are less than "value"
	void doFilter(String type) {
		Map<String, Long> filter = filters.get(currentNetwork).get(type);
		CyNetworkView view = manager.getCurrentNetworkView();
		for (CyNode node: currentNetwork.getNodeList()) {
			CyRow nodeRow = currentNetwork.getRow(node);
			boolean show = true;
			for (String lbl: filter.keySet()) {
				Long v = nodeRow.get(type, lbl, Long.class);
				long nv = filter.get(lbl);
				if ((v == null && nv > 0) || v < nv) {
					show = false;
					break;
				}
			}
			if (show) {
				view.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, true);
			} else {
				view.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, false);
			}
		}
	}

	private JPanel createNodePanel(CyNode node) {
		JPanel panel = new JPanel();
		StringNode sNode = new StringNode(manager.getStringNetwork(currentNetwork), node);
		EasyGBC c = new EasyGBC();
		panel.setLayout(new GridBagLayout());
		{
			JCheckBox highlightBox = new JCheckBox("Highlight first neighbors");
			highlightBox.setFont(labelFont);
			highlightBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							if (highlightCheck != null)
								highlightCheck.setSelected(false);
							ViewUtils.clearHighlight(manager, manager.getCurrentNetworkView(), highlightNode);
							ViewUtils.highlight(manager, manager.getCurrentNetworkView(), node);
							highlightNode = node;
							highlightCheck = (JCheckBox)e.getItem();
						} else {
							ViewUtils.clearHighlight(manager, manager.getCurrentNetworkView(), highlightNode);
							highlightNode = null;
							highlightCheck = null;
						}
					}
			});
			highlightBox.setAlignmentX( Component.LEFT_ALIGNMENT );
			highlightBox.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
			panel.add(highlightBox, c.anchor("northwest").down().expandHoriz());
		}

		{
			JLabel lbl = new JLabel("Crosslinks");
			lbl.setFont(labelFont);
			lbl.setAlignmentX( Component.LEFT_ALIGNMENT );
			lbl.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
			panel.add(lbl, c.anchor("west").down().noExpand());

			JPanel crosslinkPanel = new JPanel();
			GridLayout layout = new GridLayout(2,4);
			crosslinkPanel.setLayout(layout);
			crosslinkPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
			if (sNode.haveUniprot()) {
  			JLabel link = new SwingLink("Uniprot", sNode.getUniprotURL(), openBrowser);
				link.setFont(textFont);
				crosslinkPanel.add(link);
			}
			if (sNode.haveGeneCard()) {
  			JLabel link = new SwingLink("Gene card", sNode.getGeneCardURL(), openBrowser);
				link.setFont(textFont);
				crosslinkPanel.add(link);
			}
			if (sNode.haveCompartments()) {
  			JLabel link = new SwingLink("Compartments", sNode.getCompartmentsURL(), openBrowser);
				link.setFont(textFont);
				crosslinkPanel.add(link);
			}
			if (sNode.haveTissues()) {
  			JLabel link = new SwingLink("Tissues", sNode.getTissuesURL(), openBrowser);
				link.setFont(textFont);
				crosslinkPanel.add(link);
			}
			if (sNode.haveDisease()) {
  			JLabel link = new SwingLink("Disease", sNode.getDiseaseURL(), openBrowser);
				link.setFont(textFont);
				crosslinkPanel.add(link);
			}
			if (sNode.havePharos()) {
  			JLabel link = new SwingLink("Pharos", sNode.getPharosURL(), openBrowser);
				link.setFont(textFont);
				crosslinkPanel.add(link);
			}
			if (sNode.havePubChem()) {
  			JLabel link = new SwingLink("PubChem", sNode.getPubChemURL(), openBrowser);
				link.setFont(textFont);
				crosslinkPanel.add(link);
			}

			/*
			 * FIXME: Need to link to get to the STRING web site for this
			{
  			JLabel link = new SwingLink("STRING", sNode.getStringURL(), openBrowser);
				link.setFont(textFont);
				link.add(crosslinkPanel);
			}
			*/
			crosslinkPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(crosslinkPanel, c.anchor("west").down().noExpand());
		}

		{
			JLabel lbl = new JLabel("Description");
			lbl.setFont(labelFont);
			lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
			lbl.setBorder(BorderFactory.createEmptyBorder(10,0,5,0));
			panel.add(lbl, c.anchor("west").down().expandHoriz());

			JLabel description = new JLabel("<html><body style='width:250px;font-size:8px'>"+sNode.getDescription()+"</body></html>");
			description.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
			description.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(description, c.anchor("west").down().expandBoth());

		}

		{
			JLabel lbl = new JLabel("Structure");
			lbl.setFont(labelFont);
			lbl.setBorder(BorderFactory.createEmptyBorder(10,0,5,0));
			panel.add(lbl, c.anchor("west").down().expandHoriz());

			// Now add our image
			Image img = sNode.getStructureImage();
			if (img != null) {
				Image scaledImage = img.getScaledInstance(200,200,Image.SCALE_SMOOTH);
				JLabel label = new JLabel(new ImageIcon(scaledImage));
				// label.setPreferredSize(new Dimension(100,100));
				// label.setMinimumSize(new Dimension(100,100));
				label.setAlignmentX(Component.LEFT_ALIGNMENT);
				panel.add(label, c.anchor("west").down().noExpand());
			}
		}

		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, sNode.getDisplayName(), panel, false, 10);
		collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
		return collapsablePanel;
	}

	public void networkChanged(CyNetwork newNetwork) {
		this.currentNetwork = newNetwork;
		if (currentNetwork == null) {
			// Hide results panel?
			if (tissuesPanel != null)
				tissuesPanel.removeAll();
			if (compartmentsPanel != null)
				compartmentsPanel.removeAll();
			return;
		}

		if (!ModelUtils.haveQueryTerms(currentNetwork))
			highlightQuery.setEnabled(false);
		else
			highlightQuery.setEnabled(true);

		if (!filters.containsKey(currentNetwork)) {
			filters.put(currentNetwork, new HashMap<>());
			filters.get(currentNetwork).put("tissue", new HashMap<>());
			filters.get(currentNetwork).put("compartment", new HashMap<>());
		}

		updateTissuesPanel();
		updateCompartmentsPanel();
	}

	public void selectedNodes(Collection<CyNode> nodes) {
		// Clear the nodes panel
		nodesPanel.removeAll();
		EasyGBC c = new EasyGBC();

		for (CyNode node: nodes) {
			JPanel newPanel = createNodePanel(node);
			newPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

			nodesPanel.add(newPanel, c.anchor("west").down().expandHoriz());
		}
		revalidate();
		repaint();
	}
}
