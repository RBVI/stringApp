package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class EnrichmentCytoPanel extends JPanel
		implements CytoPanelComponent2, ListSelectionListener, ActionListener, RowsSetListener {

	final StringManager manager;
	Map<String, JTable> enrichmentTables;
	JPanel topPanel;
	JPanel mainPanel;
	JScrollPane scrollPane;
	String showTable;
	JComboBox<String> boxTables;
	List<String> availableTables;

	public EnrichmentCytoPanel(StringManager manager) {
		this.manager = manager;
		enrichmentTables = new HashMap<String, JTable>();
		showTable = null;
		this.setLayout(new BorderLayout());
		init();
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
		if (table.getSelectedColumnCount() == 1 && table.getSelectedRow() > -1) {
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

	public void actionPerformed(ActionEvent e) {
		if (boxTables.getSelectedItem() == null) {
			return;
		}
		// System.out.println("change selected table");
		showTable = (String) boxTables.getSelectedItem();
		// TODO: do some cleanup for old table?
		initPanel();
	}

	private void init() {

	}

	public void initPanel() {
		// System.out.println("init enrichment panel");
		// if (boxTables != null) {
		// boxTables.removeActionListener(this);
		// }
		this.removeAll();
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;

		Set<CyTable> currTables = ModelUtils.getEnrichmentTables(manager, network);
		availableTables = new ArrayList<String>();
		for (CyTable currTable : currTables) {
			// System.out.println("createjTtable: " + currTable.getTitle());
			createJTable(currTable);
			availableTables.add(currTable.getTitle());
		}
		if (availableTables.size() == 0) {
			return;
		}
		boxTables = new JComboBox<String>(availableTables.toArray(new String[0]));
		if (showTable != null) {
			boxTables.setSelectedItem(showTable);
		} else if (boxTables.getSelectedItem() != null) {
			showTable = (String) boxTables.getSelectedItem();
		} else {
			showTable = availableTables.get(0);
		}
		boxTables.addActionListener(this);

		topPanel = new JPanel(new BorderLayout());
		topPanel.add(boxTables, BorderLayout.EAST);
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

		this.revalidate();
		this.repaint();
		// this.requestFocusInWindow();
	}

	private void createJTable(CyTable cyTable) {
		// Set<String> columns = CyTableUtil.getColumnNames(cyTable);
		// String[] columnNames = new String[columns.size()];
		// int c = 0;
		// for (String column : columns) {
		// columnNames[c] = column;
		// if (column.equals(EnrichmentTerm.colGenesSUID)) {
		// nodeColumn = c;
		// }
		// c++;
		// }
		List<CyRow> rows = cyTable.getAllRows();
		Object[][] data = new Object[rows.size()][EnrichmentTerm.swingColumns.length];
		int i = 0;
		for (CyRow row : rows) {
			int j = 0;
			for (String column : EnrichmentTerm.swingColumns) {
				data[i][j] = row.getRaw(column);
				j++;
			}
			i++;
		}

		JTable jTable = new JTable(data, EnrichmentTerm.swingColumns);
		TableColumnModel tcm = jTable.getColumnModel();
		tcm.removeColumn(tcm.getColumn(EnrichmentTerm.nodeSUIDColumn));
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
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		if (!rse.getSource().equals(network.getDefaultNetworkTable())
				|| !rse.containsColumn(CyNetwork.SELECTED))
			return;

		// for (RowSetRecord rsr : rse.getColumnRecords(CyNetwork.SELECTED)) {
		// if ((Boolean) rsr.getValue() == true) {
		// CyRow row = rsr.getRow();
		// // This is a hack to avoid double selection...
		// if (row.toString().indexOf("FACADE") >= 0)
		// continue;
		// System.out.println("selected: " + row.get(CyIdentifiable.SUID, Long.class));
		//
		// }
		// }
	}
}
