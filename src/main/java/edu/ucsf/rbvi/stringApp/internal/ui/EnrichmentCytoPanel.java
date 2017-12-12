package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class EnrichmentCytoPanel extends JPanel
		implements CytoPanelComponent2, ListSelectionListener, ActionListener, RowsSetListener {

	final StringManager manager;
	Map<String, JTable> enrichmentTables;
	JPanel topPanel;
	JPanel mainPanel;
	JScrollPane scrollPane;
	public final static String showTable = EnrichmentTerm.termTables[6];
	// JComboBox<String> boxTables;
	List<String> availableTables;
	// boolean createBoxTables = true;
	JButton butDrawCharts;
	JButton butAnalyzedNodes;
	JLabel labelPPIEnrichment;

	public EnrichmentCytoPanel(StringManager manager) {
		this.manager = manager;
		enrichmentTables = new HashMap<String, JTable>();
		this.setLayout(new BorderLayout());
		initPanel();
	}

	public String getIdentifier() {
		return "edu.ucsf.rbvi.stringApp.Enrichment";
	}

	public Component getComponent() {
		return this;
	}

	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.SOUTH;
	}

	public String getTitle() {
		return "STRING Enrichment";
	}

	public Icon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	// table selection handler
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		clearNetworkSelection(network);
		// TODO: clear table selection when switching
		JTable table = enrichmentTables.get(showTable);
		// No idea why this was needed...
		// table.getSelectedColumn() != 0 &&
		if (table.getSelectedColumn() != EnrichmentTerm.chartColumnSel
				&& table.getSelectedColumn() != EnrichmentTerm.chartColumnCol
				&& table.getSelectedColumnCount() == 1 && table.getSelectedRow() > -1) {
			// System.out.println("get value at " + table.getSelectedRow() + " and " +
			// EnrichmentTerm.nodeSUIDColumn);
			Object cellContent = table.getModel().getValueAt(table.getSelectedRow(),
					EnrichmentTerm.nodeSUIDColumn);
			if (cellContent instanceof List) {
				List<Long> nodeIDs = (List<Long>) cellContent;
				for (Long nodeID : nodeIDs) {
					network.getDefaultNodeTable().getRow(nodeID).set(CyNetwork.SELECTED, true);
					// System.out.println("select node: " + nodeID);
				}
			}
		}
	}

	// TODO: make this network-specific
	public void actionPerformed(ActionEvent e) {
		// if (e.getSource().equals(boxTables)) {
		// if (boxTables.getSelectedItem() == null) {
		// return;
		// }
		// // System.out.println("change selected table");
		// showTable = (String) boxTables.getSelectedItem();
		// // TODO: do some cleanup for old table?
		// createBoxTables = false;
		// initPanel();
		// createBoxTables = true;
		// } else
		if (e.getSource().equals(butDrawCharts)) {
			// do something fancy here...
			// piechart: attributelist="test3" colorlist="modulated" showlabels="false"
			Map<EnrichmentTerm, String> preselectedTerms = getUserSelectedTerms();
			drawCharts(preselectedTerms);
		} else if (e.getSource().equals(butAnalyzedNodes)) {
			CyNetwork network = manager.getCurrentNetwork();
			List<CyNode> analyzedNodes = ModelUtils.getEnrichmentNodes(network);  
			if (network == null || analyzedNodes == null)
				return;
			for (CyNode node : analyzedNodes) {
				network.getDefaultNodeTable().getRow(node.getSUID()).set(CyNetwork.SELECTED, true);
				// System.out.println("select node: " + nodeID);
			}

		}
	}

	public void initPanel() {
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		initPanel(network);
	}

	public void initPanel(CyNetwork network) {
		this.removeAll();

		Set<CyTable> currTables = ModelUtils.getEnrichmentTables(manager, network);
		availableTables = new ArrayList<String>();
		for (CyTable currTable : currTables) {
			createJTable(currTable);
			availableTables.add(currTable.getTitle());
		}
		if (availableTables.size() == 0) {
			mainPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("No enrichment has been retrieved for this network.",
					SwingConstants.CENTER);
			mainPanel.add(label, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);
			// return;
		} else {
			// Collections.sort(availableTables);
			// boxTables = new JComboBox<String>(availableTables.toArray(new String[0]));
			// if (createBoxTables) {
			//	if (availableTables.contains(EnrichmentTerm.termTables[0])) {
			//		showTable = EnrichmentTerm.termTables[0];
			//	} else {
			//		showTable = availableTables.get(0);
			//	}
			//}
			// boxTables.setSelectedItem(showTable);
			// boxTables.addActionListener(this);

			butDrawCharts = new JButton("Draw pie charts");
			butDrawCharts.addActionListener(this);

			butAnalyzedNodes = new JButton("Select analyzed nodes");
			butAnalyzedNodes.addActionListener(this);

			labelPPIEnrichment = new JLabel("PPI Enrichment: " + ppiEnrichment.toString());
			labelPPIEnrichment.setToolTipText(
					"<html>If the PPI enrichment is less or equal 0.05, this means that <br />"
							+ "your proteins have more interactions among themselves than what would be expected for a  <br />"
							+ "random set of proteins of similar size, drawn from the genome. Such an enrichment indicates  <br />"
							+ "that the proteins are at least partially biologically connected, as a group.</html>");

			topPanel = new JPanel(new BorderLayout());
			topPanel.add(butDrawCharts, BorderLayout.WEST);
			topPanel.add(labelPPIEnrichment, BorderLayout.CENTER);
			topPanel.add(butAnalyzedNodes, BorderLayout.EAST);
			// topPanel.add(boxTables, BorderLayout.EAST);
			this.add(topPanel, BorderLayout.NORTH);

			JTable currentTable = enrichmentTables.get(showTable);
			// System.out.println("show table: " + showTable);
			mainPanel = new JPanel(new BorderLayout());
			scrollPane = new JScrollPane(currentTable);
			mainPanel.setLayout(new GridLayout(1, 1));
			mainPanel.add(scrollPane, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);

			// JScrollPane scrollPane2 = new JScrollPane(currentTable);
			// scrollPane.setViewportView(jTable);
			// scrollPane2.add(currentTable);

			// subPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
			// showTable, TitledBorder.CENTER, TitledBorder.TOP));
			// subPanel.add(scrollPane2);
			// mainPanel.add(subPanel, BorderLayout.CENTER);
		}

		this.revalidate();
		this.repaint();
	}

	private void createJTable(CyTable cyTable) {
		EnrichmentTableModel tableModel = new EnrichmentTableModel(cyTable,
				EnrichmentTerm.swingColumns);
		JTable jTable = new JTable(tableModel);
		TableColumnModel tcm = jTable.getColumnModel();
		tcm.removeColumn(tcm.getColumn(EnrichmentTerm.nodeSUIDColumn));
		tcm.getColumn(EnrichmentTerm.fdrColumn).setCellRenderer(new DecimalFormatRenderer());
		// jTable.setDefaultEditor(Object.class, null);
		// jTable.setPreferredScrollableViewportSize(jTable.getPreferredSize());
		jTable.setFillsViewportHeight(true);
		jTable.setAutoCreateRowSorter(true);
		jTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jTable.getSelectionModel().addListSelectionListener(this);
		jTable.setDefaultRenderer(Color.class, new ColorRenderer(true));
		jTable.setDefaultEditor(Color.class, new ColorEditor());

		enrichmentTables.put(cyTable.getTitle(), jTable);
	}

	private void clearNetworkSelection(CyNetwork network) {
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node : nodes) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				network.getRow(node).set(CyNetwork.SELECTED, false);
			}
		}
	}

	public void handleEvent(RowsSetEvent rse) {
		CyNetworkManager networkManager = manager.getService(CyNetworkManager.class);
		CyNetwork selectedNetwork = null;
		if (rse.containsColumn(CyNetwork.SELECTED)) {
			Collection<RowSetRecord> columnRecords = rse.getColumnRecords(CyNetwork.SELECTED);
			for (RowSetRecord rec : columnRecords) {
				CyRow row = rec.getRow();
				if (row.toString().indexOf("FACADE") >= 0)
					continue;
				Long networkID = row.get(CyNetwork.SUID, Long.class);
				Boolean selectedValue = (Boolean) rec.getValue();
				if (selectedValue && networkManager.networkExists(networkID)) {
					selectedNetwork = networkManager.getNetwork(networkID);
				}
			}
		}
		if (selectedNetwork != null) {
			initPanel(selectedNetwork);
		}
	}

	static class DecimalFormatRenderer extends DefaultTableCellRenderer {
		private static final DecimalFormat formatter = new DecimalFormat("0.#####E0");

		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			try {
				if (value != null && (double) value < 0.001) {
					value = formatter.format((Number) value);
				}
			} catch (Exception ex) {
				// ignore and return original value
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);
		}
	}

	private void drawCharts(Map<EnrichmentTerm, String> selectedTerms) {
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null || selectedTerms.size() == 0)
			return;

		CyTable nodeTable = network.getDefaultNodeTable();
		// replace columns
		ModelUtils.replaceListColumnIfNeeded(nodeTable, String.class,
				EnrichmentTerm.colEnrichmentTermsNames);
		ModelUtils.replaceListColumnIfNeeded(nodeTable, Integer.class,
				EnrichmentTerm.colEnrichmentTermsIntegers);
		ModelUtils.replaceColumnIfNeeded(nodeTable, String.class,
				EnrichmentTerm.colEnrichmentPassthrough);

		// final String pieChart = "piechart: attributelist=\"enrichmentTermsIntegers\"
		// colorlist=\"modulated\" showlabels=\"false\" ";

		String colorList = "";
		for (EnrichmentTerm term : selectedTerms.keySet()) {
			// Color color = selectedTerms.get(term);
			String color = selectedTerms.get(term);
			if (color != null) {
				//String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(),
				//		color.getBlue());
				//colorList += hex + ",";
				colorList += color + ",";
			} else {
				colorList += "" + ",";
			}
			String selTerm = term.getName();
			List<Long> enrichedNodeSUIDs = term.getNodesSUID();
			for (CyNode node : network.getNodeList()) {
				List<Integer> nodeTermsIntegers = nodeTable.getRow(node.getSUID())
						.getList(EnrichmentTerm.colEnrichmentTermsIntegers, Integer.class);
				List<String> nodeTermsNames = nodeTable.getRow(node.getSUID())
						.getList(EnrichmentTerm.colEnrichmentTermsNames, String.class);
				if (nodeTermsIntegers == null)
					nodeTermsIntegers = new ArrayList<Integer>();
				if (nodeTermsNames == null) {
					nodeTermsNames = new ArrayList<String>();
				}
				if (enrichedNodeSUIDs.contains(node.getSUID())) {
					nodeTermsNames.add(selTerm);
					nodeTermsIntegers.add(new Integer(1));
				} else {
					nodeTermsNames.add("");
					nodeTermsIntegers.add(new Integer(0));
				}
				nodeTable.getRow(node.getSUID()).set(EnrichmentTerm.colEnrichmentTermsIntegers,
						nodeTermsIntegers);
				nodeTable.getRow(node.getSUID()).set(EnrichmentTerm.colEnrichmentTermsNames,
						nodeTermsNames);
			}
		}
		colorList = colorList.substring(0, colorList.length() - 1) + "\"";
		final String circChart = "circoschart: firstarc=1.0 arcwidth=0.4 attributelist=\"enrichmentTermsIntegers\" showlabels=\"false\" colorlist=\" "
				+ colorList;
		for (CyRow row : nodeTable.getAllRows()) {
			row.set(EnrichmentTerm.colEnrichmentPassthrough, circChart);
		}

		// System.out.println(selectedTerms);
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		CyNetworkViewManager netManager = manager.getService(CyNetworkViewManager.class);
		CyNetworkView netView = null;
		for (CyNetworkView currNetView : netManager.getNetworkViewSet()) {
			if (vmm.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_NAME) || vmm
					.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_NAME_ORG)) {
				netView = currNetView;
				ViewUtils.updatePieCharts(manager, vmm.getVisualStyle(currNetView), network, true);
			}
		}
		netView.updateView();
	}

	private Map<EnrichmentTerm, String> getUserSelectedTerms() {
		Map<EnrichmentTerm, String> selectedTerms = new HashMap<EnrichmentTerm, String>();
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return selectedTerms;

		// Set<CyTable> currTables = ModelUtils.getEnrichmentTables(manager, network);
		// for (CyTable currTable : currTables) {
		CyTable currTable = ModelUtils.getEnrichmentTable(manager, network,
				EnrichmentTerm.termTables[6]);
		if (currTable.getColumn(EnrichmentTerm.colShowChart) == null
				|| currTable.getRowCount() == 0) {
			return selectedTerms;
		}
		for (CyRow row : currTable.getAllRows()) {
			if (currTable.getColumn(EnrichmentTerm.colShowChart) != null
					&& row.get(EnrichmentTerm.colShowChart, Boolean.class) != null
					&& row.get(EnrichmentTerm.colShowChart, Boolean.class)) {
				String selTerm = row.get(EnrichmentTerm.colName, String.class);
				if (selTerm != null) {
					EnrichmentTerm enrTerm = new EnrichmentTerm(selTerm,
							row.get(EnrichmentTerm.colDescription, String.class),
							row.get(EnrichmentTerm.colCategory, String.class), -1.0, -1.0,
							row.get(EnrichmentTerm.colFDR, Double.class));
					enrTerm.setNodesSUID(row.getList(EnrichmentTerm.colGenesSUID, Long.class));
					
					if (currTable.getColumn(EnrichmentTerm.colChartColor) != null
							&& row.get(EnrichmentTerm.colChartColor, String.class) != null) {
						selectedTerms.put(enrTerm, row.get(EnrichmentTerm.colChartColor, String.class));
					}
				}
			}
		}
		// System.out.println(selectedTerms);
		return selectedTerms;
	}

}
