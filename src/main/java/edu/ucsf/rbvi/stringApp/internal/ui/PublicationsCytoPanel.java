package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.command.AvailableCommands;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.jcolorbrewer.ColorBrewer;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.tasks.ExportEnrichmentTableTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.ExportEnrichmentTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.FilterEnrichmentTableTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.EnrichmentSettingsTask;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class PublicationsCytoPanel extends JPanel
		implements CytoPanelComponent2, ListSelectionListener, ActionListener, RowsSetListener {
	// TableModelListener
	final StringManager manager;
	// Map<String, JTable> enrichmentTables;
	JTable publicationsTable;
	JPanel topPanel;
	JPanel mainPanel;
	JScrollPane scrollPane;
	// public final static String showTable = TermCategory.PMID.getTable();
	boolean clearSelection = false;
	// List<String> availableTables;
	// JButton butSettings;
	// JButton butDrawCharts;
	// JButton butResetCharts;
	JButton butAnalyzedNodes;
	JButton butExportTable;
	// JButton butFilter;
	// JLabel labelPPIEnrichment;
	// JMenuItem menuItemReset;
	JPopupMenu popupMenu;
	EnrichmentTableModel tableModel;
	private static final Icon chartIcon = new ImageIcon(
			PublicationsCytoPanel.class.getResource("/images/chart20.png"));
	final Font iconFont;

	final String colEnrichmentTerms = "enrichmentTerms";
	final String colEnrichmentTermsPieChart = "enrichmentTermsPieChart";
	final String colEnrichmentPieChart = "enrichmentPieChart";

	// final String butSettingsName = "Network-specific enrichment panel settings";
	// final String butFilterName = "Filter enrichment table";
	// final String butDrawChartsName = "Draw charts using default color palette";
	// final String butResetChartsName = "Reset charts";
	final String butAnalyzedNodesName = "Select all analyzed nodes";
	final String butExportTableDescr = "Export publications table";

	public PublicationsCytoPanel(StringManager manager, boolean noSignificant) {
		this.manager = manager;
		// enrichmentTables = new HashMap<String, JTable>();
		publicationsTable = null;
		this.setLayout(new BorderLayout());
		IconManager iconManager = manager.getService(IconManager.class);
		iconFont = iconManager.getIconFont(22.0f);
		initPanel(noSignificant);
	}

	public String getIdentifier() {
		return "edu.ucsf.rbvi.stringApp.Publications";
	}

	public Component getComponent() {
		return this;
	}

	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.SOUTH;
	}

	public String getTitle() {
		return "STRING Publications";
	}

	public Icon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	public EnrichmentTableModel getTableModel() {
		return tableModel;
	}

	// table selection handler
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		// TODO: clear table selection when switching
		if (publicationsTable.getSelectedColumnCount() == 1
				&& publicationsTable.getSelectedRow() > -1) {
			if (publicationsTable.getSelectedColumn() != EnrichmentTerm.chartColumnCol) {
				// Only clear the network selection if it's our first selected row
				if (publicationsTable.getSelectedRowCount() == 1)
					clearNetworkSelection(network);
				for (int row : publicationsTable.getSelectedRows()) {
					Object cellContent = publicationsTable.getModel().getValueAt(
							publicationsTable.convertRowIndexToModel(row),
							EnrichmentTerm.nodeSUIDColumnPubl);
					if (cellContent instanceof List) {
						List<Long> nodeIDs = (List<Long>) cellContent;
						for (Long nodeID : nodeIDs) {
							network.getDefaultNodeTable().getRow(nodeID).set(CyNetwork.SELECTED,
									true);
						}
					}
				}
			}
		}
	}

	// @Override
	// public void tableChanged(TableModelEvent e) {
	// int column = e.getColumn();
	// if (column != EnrichmentTerm.chartColumnCol)
	// return;
	// Map<EnrichmentTerm, String> preselectedTerms = getUserSelectedTerms();
	// if (preselectedTerms.size() > 0) {
	// CyNetwork network = manager.getCurrentNetwork();
	// ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network));
	// }
	// }

	// TODO: make this network-specific
	public void actionPerformed(ActionEvent e) {
		TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		CyNetwork network = manager.getCurrentNetwork();
		// if (e.getSource().equals(butDrawCharts)) {
		// resetCharts();
		// // do something fancy here...
		// // piechart: attributelist="test3" colorlist="modulated" showlabels="false"
		// Map<EnrichmentTerm, String> preselectedTerms = getUserSelectedTerms();
		// if (preselectedTerms.size() == 0) {
		// preselectedTerms = getAutoSelectedTopTerms(manager.getTopTerms(network));
		// }
		// AvailableCommands availableCommands = (AvailableCommands) manager
		// .getService(AvailableCommands.class);
		// if (!availableCommands.getNamespaces().contains("enhancedGraphics")) {
		// JOptionPane.showMessageDialog(null,
		// "Charts will not be displayed. You need to install enhancedGraphics from the App Manager
		// or Cytoscape App Store.",
		// "No results", JOptionPane.WARNING_MESSAGE);
		// return;
		// }
		// ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network));
		// } else if (e.getSource().equals(butResetCharts)) {
		// // reset colors and selection
		// resetCharts();
		// } else
		if (e.getSource().equals(butAnalyzedNodes)) {
			List<CyNode> analyzedNodes = ModelUtils.getEnrichmentNodes(network);
			if (network == null || analyzedNodes == null)
				return;
			for (CyNode node : analyzedNodes) {
				network.getDefaultNodeTable().getRow(node.getSUID()).set(CyNetwork.SELECTED, true);
				// System.out.println("select node: " + nodeID);
			}
			// } else if (e.getSource().equals(butFilter)) {
			// tm.execute(new TaskIterator(new FilterEnrichmentTableTask(manager, this)));
			// } else if (e.getSource().equals(butSettings)) {
			// tm.execute(new TaskIterator(new EnrichmentSettingsTask(manager)));
		} else if (e.getSource().equals(butExportTable)) {
			if (network != null)
				tm.execute(new TaskIterator(new ExportEnrichmentTableTask(manager, network,
						ModelUtils.getEnrichmentTable(manager, network,
								TermCategory.PMID.getTable()))));
			// } else if (e.getSource().equals(menuItemReset)) {
			// // System.out.println("reset color now");
			// Component c = (Component)e.getSource();
			// JPopupMenu popup = (JPopupMenu)c.getParent();
			// JTable table = (JTable)popup.getInvoker();
			// // System.out.println("action listener: " + table.getSelectedRow() + " : " +
			// table.getSelectedColumn());
			// if (table.getSelectedRow() > -1) {
			// resetColor(table.getSelectedRow());
			// }
		}
	}

	public void initPanel(boolean noSignificant) {
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		initPanel(network, noSignificant);
	}

	public void initPanel(CyNetwork network, boolean noSignificant) {
		this.removeAll();

		Set<CyTable> currTables = ModelUtils.getEnrichmentTables(manager, network);
		// availableTables = new ArrayList<String>();
		for (CyTable currTable : currTables) {
			System.out.println(currTable.getTitle());
			if (currTable.getTitle().equals(TermCategory.PMID.getTable())) {
				publicationsTable = createJTable(currTable);
			}
			// availableTables.add(currTable.getTitle());
		}
		if (noSignificant) {
			mainPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel(
					"Enrichment retrieval returned no results that met the criteria.",
					SwingConstants.CENTER);
			mainPanel.add(label, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);
		} else if (publicationsTable == null) {
			mainPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("No enrichment has been retrieved for this network.",
					SwingConstants.CENTER);
			mainPanel.add(label, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);
			// return;
		} else {
			// JPanel buttonsPanelLeft = new JPanel(new GridLayout(1, 3));
			// butFilter = new JButton(IconManager.ICON_FILTER);
			// butFilter.setFont(iconFont);
			// butFilter.addActionListener(this);
			// butFilter.setToolTipText(butFilterName);
			// butFilter.setBorderPainted(false);
			// butFilter.setContentAreaFilled(false);
			// butFilter.setFocusPainted(false);
			// butFilter.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
			//
			// butDrawCharts = new JButton(chartIcon);
			// butDrawCharts.addActionListener(this);
			// butDrawCharts.setToolTipText(butDrawChartsName);
			// butDrawCharts.setBorderPainted(false);
			// butDrawCharts.setContentAreaFilled(false);
			// butDrawCharts.setFocusPainted(false);
			// butDrawCharts.setBorder(BorderFactory.createEmptyBorder(2,0,2,2));
			//
			//
			// butResetCharts = new JButton(IconManager.ICON_CIRCLE_O);
			// butResetCharts.setFont(iconFont);
			// butResetCharts.addActionListener(this);
			// butResetCharts.setToolTipText(butResetChartsName);
			// butResetCharts.setBorderPainted(false);
			// butResetCharts.setContentAreaFilled(false);
			// butResetCharts.setFocusPainted(false);
			// butResetCharts.setBorder(BorderFactory.createEmptyBorder(2,2,2,20));
			//
			// buttonsPanelLeft.add(butFilter);
			// buttonsPanelLeft.add(butDrawCharts);
			// buttonsPanelLeft.add(butResetCharts);

			JPanel buttonsPanelRight = new JPanel(new GridLayout(1, 2));
			butAnalyzedNodes = new JButton(IconManager.ICON_CHECK_SQUARE_O);
			butAnalyzedNodes.addActionListener(this);
			butAnalyzedNodes.setFont(iconFont);
			butAnalyzedNodes.setToolTipText(butAnalyzedNodesName);
			butAnalyzedNodes.setBorderPainted(false);
			butAnalyzedNodes.setContentAreaFilled(false);
			butAnalyzedNodes.setFocusPainted(false);
			butAnalyzedNodes.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

			butExportTable = new JButton(IconManager.ICON_SAVE);
			butExportTable.addActionListener(this);
			butExportTable.setFont(iconFont);
			butExportTable.setToolTipText(butExportTableDescr);
			butExportTable.setBorderPainted(false);
			butExportTable.setContentAreaFilled(false);
			butExportTable.setFocusPainted(false);
			butExportTable.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 20));

			// butSettings = new JButton(IconManager.ICON_COG);
			// butSettings.setFont(iconFont);
			// butSettings.addActionListener(this);
			// butSettings.setToolTipText(butSettingsName);
			// butSettings.setBorderPainted(false);
			// butSettings.setContentAreaFilled(false);
			// butSettings.setFocusPainted(false);
			// butSettings.setBorder(BorderFactory.createEmptyBorder(2,2,2,20));

			buttonsPanelRight.add(butAnalyzedNodes);
			buttonsPanelRight.add(butExportTable);
			// buttonsPanelRight.add(butSettings);

			// Double ppiEnrichment = ModelUtils.getPPIEnrichment(network);
			// labelPPIEnrichment = new JLabel();
			// if (ppiEnrichment != null) {
			// labelPPIEnrichment = new JLabel("PPI Enrichment: " + ppiEnrichment.toString());
			// labelPPIEnrichment.setToolTipText(
			// "<html>If the PPI enrichment is less or equal 0.05, your proteins have more
			// interactions among themselves <br />"
			// + "than what would be expected for a random set of proteins of similar size, drawn
			// from the genome. Such <br />"
			// + "an enrichment indicates that the proteins are at least partially biologically
			// connected, as a group.</html>");
			// }

			topPanel = new JPanel(new BorderLayout());
			// topPanel.add(buttonsPanelLeft, BorderLayout.WEST);
			// topPanel.add(labelPPIEnrichment, BorderLayout.CENTER);
			topPanel.add(buttonsPanelRight, BorderLayout.EAST);
			// topPanel.add(boxTables, BorderLayout.EAST);
			this.add(topPanel, BorderLayout.NORTH);

			mainPanel = new JPanel(new BorderLayout());
			scrollPane = new JScrollPane(publicationsTable);
			mainPanel.setLayout(new GridLayout(1, 1));
			mainPanel.add(scrollPane, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);

		}

		if (tableModel != null)
			tableModel.filter(manager.getCategoryFilter(network), manager.getRemoveOverlap(network),
					manager.getOverlapCutoff(network));
		this.revalidate();
		this.repaint();
	}

	private JTable createJTable(CyTable cyTable) {
		tableModel = new EnrichmentTableModel(cyTable, EnrichmentTerm.swingColumnsPublications);
		JTable jTable = new JTable(tableModel);
		TableColumnModel tcm = jTable.getColumnModel();
		tcm.removeColumn(tcm.getColumn(EnrichmentTerm.nodeSUIDColumnPubl));
		tcm.getColumn(EnrichmentTerm.fdrColumnPubl).setCellRenderer(new DecimalFormatRenderer());
		// jTable.setDefaultEditor(Object.class, null);
		// jTable.setPreferredScrollableViewportSize(jTable.getPreferredSize());
		jTable.setFillsViewportHeight(true);
		jTable.setAutoCreateRowSorter(true);
		jTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		jTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		jTable.getSelectionModel().addListSelectionListener(this);
		// jTable.getModel().addTableModelListener(this);
		jTable.setDefaultRenderer(Color.class, new ColorRenderer(true));
		jTable.setDefaultEditor(Color.class, new ColorEditor());
		popupMenu = new JPopupMenu();
		// menuItemReset = new JMenuItem("Remove color");
		// menuItemReset.addActionListener(this);
		// popupMenu.add(menuItemReset);
		jTable.setComponentPopupMenu(popupMenu);
		jTable.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
					JTable source = (JTable) e.getSource();
					int row = source.rowAtPoint(e.getPoint());
					int column = source.columnAtPoint(e.getPoint());
					if (!source.isRowSelected(row)) {
						source.changeSelection(row, column, false, false);
					}
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
					JTable source = (JTable) e.getSource();
					int row = source.rowAtPoint(e.getPoint());
					int column = source.columnAtPoint(e.getPoint());
					if (!source.isRowSelected(row)) {
						source.changeSelection(row, column, false, false);
					}
				}
			}
		});
		// jTable.addMouseListener(new TableMouseListener(jTable));

		// enrichmentTables.put(cyTable.getTitle(), jTable);
		return jTable;
	}

	private void clearNetworkSelection(CyNetwork network) {
		List<CyNode> nodes = network.getNodeList();
		clearSelection = true;
		for (CyNode node : nodes) {
			if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
				network.getRow(node).set(CyNetwork.SELECTED, false);
			}
		}
		clearSelection = false;
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
			initPanel(selectedNetwork, false);
			return;
		}
		// experimental: clear term selection when all network nodes are unselected
		CyNetwork network = manager.getCurrentNetwork();
		// JTable currentTable = enrichmentTables.get(showTable);
		if (!clearSelection && network != null && publicationsTable != null) {
			List<CyNode> nodes = network.getNodeList();
			for (CyNode node : nodes) {
				if (network.getRow(node).get(CyNetwork.SELECTED, Boolean.class)) {
					return;
				}
			}
			publicationsTable.clearSelection();
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

	// public void resetColor(int currentRow) {
	// // JTable currentTable = enrichmentTables.get(showTable);
	// // currentRow = currentTable.getSelectedRow();
	// CyNetwork network = manager.getCurrentNetwork();
	// if (network == null || tableModel == null)
	// return;
	// CyTable enrichmentTable = ModelUtils.getEnrichmentTable(manager, network,
	// TermCategory.ALL.getTable());
	// Color color = (Color)publicationsTable.getModel().getValueAt(
	// publicationsTable.convertRowIndexToModel(currentRow),
	// EnrichmentTerm.chartColumnCol);
	// String termName = (String)publicationsTable.getModel().getValueAt(
	// publicationsTable.convertRowIndexToModel(currentRow),
	// EnrichmentTerm.nameColumn);
	// if (color == null || termName == null)
	// return;
	//
	// //currentTable.getModel().setValueAt(Color.OPAQUE,
	// currentTable.convertRowIndexToModel(currentRow),
	// // EnrichmentTerm.chartColumnCol);
	// for (CyRow row : enrichmentTable.getAllRows()) {
	// if (enrichmentTable.getColumn(EnrichmentTerm.colName) != null
	// && row.get(EnrichmentTerm.colName, String.class) != null
	// && row.get(EnrichmentTerm.colName, String.class).equals(termName)) {
	// row.set(EnrichmentTerm.colChartColor, "");
	// }
	// }
	// tableModel.fireTableDataChanged();
	//
	// // re-draw charts if the user changed the color
	// Map<EnrichmentTerm, String> preselectedTerms = getUserSelectedTerms();
	// if (preselectedTerms.size() > 0)
	// ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network));
	// }
	//
	// public void resetCharts() {
	// CyNetwork network = manager.getCurrentNetwork();
	// if (network == null || tableModel == null)
	// return;
	//
	// CyTable nodeTable = network.getDefaultNodeTable();
	// // replace columns
	// ModelUtils.replaceListColumnIfNeeded(nodeTable, String.class,
	// EnrichmentTerm.colEnrichmentTermsNames);
	// ModelUtils.replaceListColumnIfNeeded(nodeTable, Integer.class,
	// EnrichmentTerm.colEnrichmentTermsIntegers);
	// ModelUtils.replaceColumnIfNeeded(nodeTable, String.class,
	// EnrichmentTerm.colEnrichmentPassthrough);
	//
	// // remove colors from table?
	// CyTable currTable = ModelUtils.getEnrichmentTable(manager, network,
	// showTable);
	// if (currTable == null || currTable.getRowCount() == 0) {
	// return;
	// }
	// for (CyRow row : currTable.getAllRows()) {
	// if (currTable.getColumn(EnrichmentTerm.colChartColor) != null
	// && row.get(EnrichmentTerm.colChartColor, String.class) != null
	// && !row.get(EnrichmentTerm.colChartColor, String.class).equals("")) {
	// row.set(EnrichmentTerm.colChartColor, "");
	// }
	// }
	// // initPanel();
	// tableModel.fireTableDataChanged();
	// }

	// public void drawCharts() {
	// CyNetwork network = manager.getCurrentNetwork();
	// if (network == null)
	// return;
	//
	// resetCharts();
	// Map<EnrichmentTerm, String> preselectedTerms = getUserSelectedTerms();
	// if (preselectedTerms.size() == 0) {
	// preselectedTerms = getAutoSelectedTopTerms(manager.getTopTerms(network));
	// }
	// ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network));
	// }
	//
	// private Map<EnrichmentTerm, String> getUserSelectedTerms() {
	// Map<EnrichmentTerm, String> selectedTerms = new LinkedHashMap<EnrichmentTerm, String>();
	// CyNetwork network = manager.getCurrentNetwork();
	// if (network == null)
	// return selectedTerms;
	//
	// // Set<CyTable> currTables = ModelUtils.getEnrichmentTables(manager, network);
	// // for (CyTable currTable : currTables) {
	// CyTable currTable = ModelUtils.getEnrichmentTable(manager, network,
	// TermCategory.ALL.getTable());
	// // currTable.getColumn(EnrichmentTerm.colShowChart) == null ||
	// if (currTable == null || currTable.getRowCount() == 0) {
	// return selectedTerms;
	// }
	// for (CyRow row : currTable.getAllRows()) {
	// if (currTable.getColumn(EnrichmentTerm.colChartColor) != null
	// && row.get(EnrichmentTerm.colChartColor, String.class) != null
	// && !row.get(EnrichmentTerm.colChartColor, String.class).equals("")
	// && !row.get(EnrichmentTerm.colChartColor, String.class).equals("#ffffff")) {
	// String selTerm = row.get(EnrichmentTerm.colName, String.class);
	// if (selTerm != null) {
	// EnrichmentTerm enrTerm = new EnrichmentTerm(selTerm,
	// row.get(EnrichmentTerm.colDescription, String.class),
	// row.get(EnrichmentTerm.colCategory, String.class), -1.0, -1.0,
	// row.get(EnrichmentTerm.colFDR, Double.class), row.get(EnrichmentTerm.colGenesBG,
	// Integer.class));
	// enrTerm.setNodesSUID(row.getList(EnrichmentTerm.colGenesSUID, Long.class));
	// selectedTerms.put(enrTerm, row.get(EnrichmentTerm.colChartColor, String.class));
	// }
	// }
	// }
	// // System.out.println(selectedTerms);
	// return selectedTerms;
	// }

	// private Map<EnrichmentTerm, String> getAutoSelectedTopTerms(int termNumber) {
	// Map<EnrichmentTerm, String> selectedTerms = new LinkedHashMap<EnrichmentTerm, String>();
	// CyNetwork network = manager.getCurrentNetwork();
	// if (network == null || tableModel == null)
	// return selectedTerms;
	//
	// CyTable currTable = ModelUtils.getEnrichmentTable(manager, network,
	// TermCategory.ALL.getTable());
	// if (currTable == null || currTable.getRowCount() == 0) {
	// return selectedTerms;
	// }
	//
	// // List<CyRow> rows = currTable.getAllRows();
	// Color[] colors =
	// manager.getBrewerPalette(network).getColorPalette(manager.getTopTerms(network));
	// Long[] rowNames = tableModel.getRowNames();
	// for (int i = 0; i < manager.getTopTerms(network); i++) {
	// if (i >= rowNames.length)
	// continue;
	// CyRow row = currTable.getRow(rowNames[i]);
	// String selTerm = row.get(EnrichmentTerm.colName, String.class);
	// if (selTerm != null) {
	// EnrichmentTerm enrTerm = new EnrichmentTerm(selTerm,
	// row.get(EnrichmentTerm.colDescription, String.class),
	// row.get(EnrichmentTerm.colCategory, String.class), -1.0, -1.0,
	// row.get(EnrichmentTerm.colFDR, Double.class), row.get(EnrichmentTerm.colGenesBG,
	// Integer.class));
	// enrTerm.setNodesSUID(row.getList(EnrichmentTerm.colGenesSUID, Long.class));
	// String color = String.format("#%02x%02x%02x", colors[i].getRed(), colors[i].getGreen(),
	// colors[i].getBlue());
	// row.set(EnrichmentTerm.colChartColor, color);
	// selectedTerms.put(enrTerm, color);
	// }
	// }
	// // initPanel();
	// tableModel.fireTableDataChanged();
	// return selectedTerms;
	// }

}
