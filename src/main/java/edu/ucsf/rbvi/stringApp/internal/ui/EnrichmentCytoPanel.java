package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class EnrichmentCytoPanel extends JPanel
		implements CytoPanelComponent2, ListSelectionListener {

	final StringManager manager;
	Map<String, JTable> enrichmentTables;
	int nodeColumn = 0;

	public EnrichmentCytoPanel(StringManager manager) {
		this.manager = manager;
		enrichmentTables = new HashMap<String, JTable>();
		init();
		// revalidate();
		// repaint();
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

	private void init() {
		this.setLayout(new BorderLayout());
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridLayout(4, 1, 1, 5));
		JScrollPane scrollPane = new JScrollPane(topPanel);
		add(scrollPane, BorderLayout.CENTER);

		CyNetworkTableManager netTableManager = manager.getService(CyNetworkTableManager.class);
		CyNetwork network = manager.getCurrentNetwork();
		// If we're not getting selection, we're not interested
		if (network == null)
			return;

		Map<String, CyTable> tables = netTableManager.getTables(network, CyNetwork.class);
		for (String tableName : EnrichmentTerm.termTables) {
			if (tables.keySet().contains(tableName)) {
				CyTable table = tables.get(tableName);
				System.out.println("add table: " + tableName);
				addTablePanel(table, topPanel);
			}
		}
	}

	private void addTablePanel(CyTable cyTable, JPanel panel) {
		Set<String> columns = CyTableUtil.getColumnNames(cyTable);
		String[] columnNames = new String[columns.size()];
		int c = 0;
		for (String column : columns) {
			columnNames[c] = column;
			if (column.equals(EnrichmentTerm.colGenesSUID)) {
				nodeColumn = c;
			}
			c++;
		}
		List<CyRow> rows = cyTable.getAllRows();
		Object[][] data = new Object[rows.size()][columns.size()];
		int i = 0;
		for (CyRow row : rows) {
			int j = 0;
			for (String column : columns) {
				data[i][j] = row.getRaw(column);
				j++;
			}
			i++;
		}

		JTable jTable = new JTable(data, columnNames);
		jTable.setDefaultEditor(Object.class, null);
		// jTable.setPreferredScrollableViewportSize(jTable.getPreferredSize());
		jTable.setFillsViewportHeight(true);
		jTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		jTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jTable.getSelectionModel().addListSelectionListener(this);

		enrichmentTables.put(cyTable.getTitle(), jTable);
		// DefaultTableModel tableModel = new DefaultTableModel(data, columnNames) {
		// @Override
		// public boolean isCellEditable(int row, int column) {
		// // all cells false
		// return false;
		// }
		//
		// };
		// jTable.setModel(tableModel);

		JScrollPane scrollPane = new JScrollPane(jTable);
		// scrollPane.setViewportView(jTable);
		// scrollPane.add(jTable);

		JPanel subPanel = new JPanel(new GridLayout(1, 1));
		subPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
				cyTable.getTitle(), TitledBorder.CENTER, TitledBorder.TOP));
		subPanel.add(scrollPane);

		panel.add(subPanel);
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		clearNetworkSelection(network);
		// TODO: clear table selection when switching
		for (String tableName : enrichmentTables.keySet()) {
			JTable table = enrichmentTables.get(tableName);
			if (table.getSelectedColumnCount() == 1 && table.getSelectedRow() > -1) {
				System.out.println("get value at " + table.getSelectedRow() + " and " + nodeColumn);
				Object cellContent = table.getValueAt(table.getSelectedRow(), nodeColumn);
				if (cellContent instanceof List) {
					List<Long> nodeIDs = (List<Long>) cellContent;
					for (Long nodeID : nodeIDs) {
						network.getDefaultNodeTable().getRow(nodeID).set(CyNetwork.SELECTED, true);
						System.out.println("select node: " + nodeID);
					}
				}
			}
		}
	}

	private void clearNetworkSelection(CyNetwork network) {
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node : nodes) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				network.getRow(node).set(CyNetwork.SELECTED, false);
			}
		}
	}

}
