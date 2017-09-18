package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.OpenBrowser;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public class StringCytoPanel extends JPanel 
                          implements CytoPanelComponent2, RowsSetListener {

	final StringManager manager;
	final OpenBrowser openBrowser;
	final Font iconFont;
	final Map<StringNetwork, Map<CyNode, JPanel>> networkMap;

	private JPanel topPanel;
	private JScrollPane scrollPane;

	public StringCytoPanel(final StringManager manager) {
		this.manager = manager;
		this.openBrowser = manager.getService(OpenBrowser.class);
		IconManager iconManager = manager.getService(IconManager.class);
		iconFont = iconManager.getIconFont(17.0f);
		setLayout(new BorderLayout());
		add(createButtonBox(), BorderLayout.NORTH);
		topPanel = new JPanel();
		topPanel.setLayout(new GridBagLayout());
		scrollPane = new JScrollPane(topPanel);
		add(scrollPane, BorderLayout.CENTER);
		networkMap = new HashMap<>();
		getCurrentSelectedNodes();
		revalidate();
		repaint();
	}

	public String getIdentifier() {
		return "edu.ucsf.rbvi.stringApp.String";
	}

	private void getCurrentSelectedNodes() {
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null) return;

		StringNetwork stringNetwork = manager.getStringNetwork(network);
		if (stringNetwork == null) return;

		if (!networkMap.containsKey(stringNetwork)) {
			networkMap.put(stringNetwork, new TreeMap<CyNode, JPanel>(new NodeComparator(stringNetwork)));
		}

		Map<CyNode, JPanel> selectedNodes = networkMap.get(stringNetwork);

		List<CyNode> selections = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		for (CyNode node: selections) {
			selectedNodes.put(node, new StringPanel(openBrowser, stringNetwork, node));
		}

		addNodePanels(network, selectedNodes);
	}

	public void handleEvent(RowsSetEvent arg0) {

		// Clear the list of nodes...
		topPanel.removeAll();

		try {
			CyNetwork network = manager.getCurrentNetwork();
			// If we're not getting selection, we're not interested
			if (network == null || !arg0.containsColumn(CyNetwork.SELECTED)) return;

			StringNetwork stringNetwork = manager.getStringNetwork(network);
			if (stringNetwork == null) return;

			if (!networkMap.containsKey(stringNetwork)) {
				networkMap.put(stringNetwork, new TreeMap<CyNode, JPanel>(new NodeComparator(stringNetwork)));
			}

			Map<CyNode, JPanel> selectedNodes = networkMap.get(stringNetwork);

			CyTable table = network.getDefaultNodeTable();
			String message = "";
			Collection<RowSetRecord> record = arg0.getPayloadCollection();
			for (RowSetRecord r: record) {
				if (!r.getColumn().equals(CyNetwork.SELECTED)) {
					continue;
				}
				
				// Get the node
				CyNode node = network.getNode(r.getRow().get(CyNetwork.SUID, Long.class));
				if (node == null) continue;
				String name = ModelUtils.getDisplayName(network, node);
				if ((Boolean)r.getValue() == Boolean.FALSE) {
					selectedNodes.remove(node);
				} else {
					selectedNodes.put(node, new CollapsablePanel(iconFont, name, new StringPanel(openBrowser, stringNetwork, node), false));
				}
			}

			addNodePanels(network, selectedNodes);

			topPanel.revalidate();
			topPanel.repaint();
		} catch (Exception e){e.printStackTrace();}
	}

	private void addNodePanels(CyNetwork network, Map<CyNode, JPanel> selectedNodes) {
		EasyGBC c = new EasyGBC();
		for (CyNode node: selectedNodes.keySet()) {
			JPanel panel = selectedNodes.get(node);
			topPanel.add(panel, c.expandHoriz());
			c.down();
		}
		// Add dummy panel to fill space
		topPanel.add(new JPanel(), c.expandBoth());
	}

	public Component getComponent() {
		// TODO Auto-generated method stub
		return this;
	}

	public CytoPanelName getCytoPanelName() {
		// TODO Auto-generated method stub
		return CytoPanelName.EAST;
	}

	public Icon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTitle() {
		return "String";
	}

	private JPanel createButtonBox() {
		JPanel buttonBox = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonBox.setBorder(BorderFactory.createEtchedBorder());
		JButton expand = new JButton("Expand All");
		expand.addActionListener(new AllAction(true));
		JButton collapse = new JButton("Collapse All");
		collapse.addActionListener(new AllAction(false));
		buttonBox.add(expand);
		buttonBox.add(collapse);
		return buttonBox;
	}

	private class NodeComparator implements Comparator<CyNode> {
		StringNetwork stringNetwork;
		public NodeComparator(StringNetwork stringNetwork) {
			this.stringNetwork = stringNetwork;
		}

		public int compare(CyNode o1, CyNode o2) {
			CyNetwork net = stringNetwork.getNetwork();
			String n1 = ModelUtils.getName(net, (CyIdentifiable)o1);
			String n2 = ModelUtils.getName(net, (CyIdentifiable)o2);
			return n1.compareTo(n2);
		}
	}

	private class AllAction implements ActionListener {
		final boolean expand;
		public AllAction(boolean ex) { expand = ex; }

		public void actionPerformed(ActionEvent ae) {
			CyNetwork network = manager.getCurrentNetwork();
			// If we're not getting selection, we're not interested
			if (network == null) return;

			StringNetwork stringNetwork = manager.getStringNetwork(network);
			if (stringNetwork == null) return;

			if (!networkMap.containsKey(stringNetwork)) {
				return;
			}

			Map<CyNode, JPanel> selectedNodes = networkMap.get(stringNetwork);
			for (JPanel nodePanel: selectedNodes.values()) {
				CollapsablePanel p = (CollapsablePanel)nodePanel;
				if (expand)
					p.expand();
				else
					p.collapse();
			}

		}

	}


}
