package edu.ucsf.rbvi.stringApp.internal.ui;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.ENRICH_LAYERS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.getIconFont;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.command.AvailableCommands;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.util.swing.CyColorPaletteChooserFactory;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
// import org.jcolorbrewer.ColorBrewer;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.tasks.EnrichmentMapAdvancedTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.EnrichmentSettingsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.ExportEnrichmentTableTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.FilterEnrichmentTableTask;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.TextIcon;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class EnrichmentCytoPanel extends JPanel
		implements CytoPanelComponent2, ListSelectionListener, ActionListener,
		TableModelListener, SelectedNodesAndEdgesListener, SetCurrentNetworkListener {

	// enrichment settings and analyzed nodes are now saved in a network-specific table
	// TODO: [Feature] make visualized terms work with groups
	// implemented backwards compatibility for old sessions, the info is copied from the network table to the enrichment info table

	final StringManager manager;
	private boolean registered = false;
	Map<String, JTable> enrichmentTables;
	Map<String, EnrichmentTableModel> enrichmentTableModels;
	JPanel topPanel;
	JPanel mainPanel;
	JScrollPane scrollPane;
	public static String showTable = TermCategory.ALL.getTable();
	boolean clearSelection = false;
	JComboBox<String> boxTables;
	List<String> availableTables;
	boolean createBoxTables = true;
	JButton butSettings;
	JButton butDrawCharts;
	JButton butResetCharts;
	JButton butAnalyzedNodes;
	JButton butExportTable;
	JButton butFilter;
	JButton butEnrichmentMap;
	JLabel labelPPIEnrichment;
	JLabel labelRows;
	JPopupMenu popupMenu;

	final CyColorPaletteChooserFactory colorChooserFactory;
	private static final Icon chartIcon = new ImageIcon(
			EnrichmentCytoPanel.class.getResource("/images/chart20.png"));
	final Font iconFont;

	final String colEnrichmentTerms = "enrichmentTerms";
	final String colEnrichmentTermsPieChart = "enrichmentTermsPieChart";
	final String colEnrichmentPieChart = "enrichmentPieChart";

	final String butSettingsName = "Network-specific enrichment panel settings";
	final String butFilterName = "Filter enrichment table";
	final String butFilterNodeName = "Filter enrichment table by node";
	final String butDrawChartsName = "Draw charts using default color palette";
	final String butResetChartsName = "Reset charts";
	final String butEnrichmentMapName = "Create EnrichmentMap";
	final String butAnalyzedNodesName = "Select all analyzed nodes";
	final String butExportTableDescr = "Export enrichment table";

	private static final Icon icon = new TextIcon(ENRICH_LAYERS, getIconFont(20.0f), STRING_COLORS,
			14, 14);

	public EnrichmentCytoPanel(StringManager manager, boolean noSignificant) {
		this.manager = manager;
		enrichmentTables = new HashMap<String, JTable>();
		enrichmentTableModels = new HashMap<String, EnrichmentTableModel>();
		this.setLayout(new BorderLayout());
		IconManager iconManager = manager.getService(IconManager.class);
		colorChooserFactory = manager.getService(CyColorPaletteChooserFactory.class);
		iconFont = iconManager.getIconFont(22.0f);
		manager.setEnrichPanel(this);
		manager.registerService(this, SetCurrentNetworkListener.class, new Properties());
		// manager.registerService(this, RowsSetListener.class, new Properties());
		manager.registerService(this, SelectedNodesAndEdgesListener.class, new Properties());
		registered = true;
		initPanel(noSignificant);
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
		return icon;
	}

	public EnrichmentTableModel getTableModel() {
		return enrichmentTableModels.get(showTable);
	}

	// TODO: [N] Test thoroughly if it works to use showTable as the group 
	public String getTable() {
		return showTable;
	}

	// network selected listener
	public void handleEvent(SetCurrentNetworkEvent event) {
		CyNetwork network = event.getNetwork();
		if (ModelUtils.ifHaveStringNS(network)) {
			if (!registered) {
				// System.out.println("found string network with unregistered enrich panel");
				showCytoPanel();
			} else {
				initPanel(network, false);
			}
		} else {
			hideCytoPanel();
		}

	}

	public void showCytoPanel() {
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
		if (!registered) {
			// System.out.println("panel: register enrichment panel");
			manager.registerService(this, CytoPanelComponent.class, new Properties());
			registered = true;
		}
		if (cytoPanel.getState() == CytoPanelState.HIDE)
			cytoPanel.setState(CytoPanelState.DOCK);

		initPanel(false);
		cytoPanel
				.setSelectedIndex(cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
	}

	public void hideCytoPanel() {
		// System.out.println("panel: unregister enrichment panel");
		manager.unregisterService(this, CytoPanelComponent.class);
		registered = false;
	}

	// table selection handler
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		// TODO: clear table selection when switching
		JTable table = enrichmentTables.get(showTable);
		if (table.getSelectedColumnCount() == 1 && table.getSelectedRow() > -1) {
			if (table.getSelectedColumn() != EnrichmentTerm.chartColumnCol) {
				// Only clear the network selection if it's our first selected row
				if (table.getSelectedRowCount() == 1)
					clearNetworkSelection(network);
				for (int row : table.getSelectedRows()) {
					Object cellContent = table.getModel().getValueAt(
							table.convertRowIndexToModel(row), EnrichmentTerm.nodeSUIDColumn);
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

	// filter enrichment terms on node selection as long as no terms are selected
	@Override
	public void handleEvent(SelectedNodesAndEdgesEvent event) {
		// only consider when nodes are selected
		if (!event.nodesChanged())
			return;

		// if (!registered) return;
		if (!enrichmentTables.containsKey(showTable))
			return;

		JTable table = enrichmentTables.get(showTable);
		if (table.getSelectedRow() > -1 && table.getSelectedColumnCount() == 1
				&& table.getSelectedColumn() != EnrichmentTerm.chartColumnCol) {
			return;
		}

		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		List<Long> nodesToFilterSUID = new ArrayList<Long>();
		for (final CyNode node : event.getSelectedNodes()) {
			nodesToFilterSUID.add(node.getSUID());
		}
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		tableModel.filterByNodeSUID(nodesToFilterSUID, true, manager.getCategoryFilter(network, showTable),
				manager.getRemoveOverlap(network, showTable), manager.getOverlapCutoff(network, showTable));
		updateLabelRows();
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		int column = e.getColumn();
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;

		updateLabelRows();

		if (column == EnrichmentTerm.chartColumnCol) {
			Map<EnrichmentTerm, String> preselectedTerms = getUserSelectedTerms();
			if (preselectedTerms.size() > 0) {
				ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network, showTable));
			}
		}

		updateFilteredEnrichmentTable(getFilteredTable());

		JTable currentTable = enrichmentTables.get(showTable);
		currentTable.tableChanged(e);
	}

	// TODO: make this network-specific
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(boxTables)) {
			if (boxTables.getSelectedItem() == null) {
				return;
			}
			// System.out.println("change selected table");
			showTable = (String) boxTables.getSelectedItem();
			// TODO: do some cleanup for old table?
			createBoxTables = false;
			initPanel(false);
			createBoxTables = true;
		}
		TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		CyNetwork network = manager.getCurrentNetwork();
		if (e.getSource().equals(butDrawCharts)) {
			// TODO: [Feature] draw charts currently only works on the main network, why? do something else instead of resetting or not?
			resetCharts();
			// do something fancy here...
			// piechart: attributelist="test3" colorlist="modulated" showlabels="false"
			Map<EnrichmentTerm, String> preselectedTerms = getAllUserSelectedTerms();
			if (preselectedTerms.size() == 0) {
				preselectedTerms = getAutoSelectedTopTerms(manager.getTopTerms(network, showTable));
			}
			AvailableCommands availableCommands = (AvailableCommands) manager
					.getService(AvailableCommands.class);
			if (!availableCommands.getNamespaces().contains("enhancedGraphics")) {
				JOptionPane.showMessageDialog(null,
						"Charts will not be displayed. You need to install enhancedGraphics from the App Manager or Cytoscape App Store.",
						"No results", JOptionPane.WARNING_MESSAGE);
				return;
			}
			ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network, showTable));
		} else if (e.getSource().equals(butResetCharts)) {
			// reset colors and selection
			resetCharts();
		} else if (e.getSource().equals(butEnrichmentMap)) {
			// create enrichment map network
			drawEnrichmentMap();
		} else if (e.getSource().equals(butAnalyzedNodes)) {
			List<CyNode> analyzedNodes = ModelUtils.getEnrichmentNodes(manager, network, showTable);
			if (network == null || analyzedNodes == null)
				return;
			for (CyNode node : analyzedNodes) {
				network.getDefaultNodeTable().getRow(node.getSUID()).set(CyNetwork.SELECTED, true);
				// System.out.println("select node: " + nodeID);
			}
		} else if (e.getSource().equals(butFilter)) {
			// filter table
			tm.execute(new TaskIterator(new FilterEnrichmentTableTask(manager, this)));
			// } else if (e.getSource().equals(butFilterNodes)) {
			// // filter table based on node selection
			// tm.execute(new TaskIterator(new NodeFilterEnrichmentTableTask(manager, this)));
		} else if (e.getSource().equals(butSettings)) {
			tm.execute(new TaskIterator(new EnrichmentSettingsTask(manager, showTable)));
		} else if (e.getSource().equals(butExportTable)) {
			EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
			// System.out.println(showTable);
			if (network != null && tableModel != null) {
				if (tableModel.getAllRowCount() != tableModel.getRowCount())
					tm.execute(new TaskIterator(new ExportEnrichmentTableTask(manager, network,
							this, ModelUtils.getEnrichmentTable(manager, network, showTable), true)));
				else
					tm.execute(new TaskIterator(new ExportEnrichmentTableTask(manager, network,
							this, ModelUtils.getEnrichmentTable(manager, network, showTable), false)));
			}
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

		Set<CyTable> currTables = ModelUtils.getAllEnrichmentTables(manager, network, EnrichmentTerm.ENRICHMENT_TABLE_PREFIX);
		availableTables = new ArrayList<String>();
		for (CyTable currTable : currTables) {
			// System.out.println("found table " + currTable.getTitle());
			if (!currTable.getTitle().equals(TermCategory.PMID.getTable())
					&& !currTable.getTitle().endsWith(EnrichmentTerm.ENRICHMENT_TABLE_FILTERED_SUFFIX)) {
				if (currTable.getRowCount() > 0) {
					// System.out.println("adding table: " + currTable.getTitle());
					createJTable(currTable, ModelUtils.getDataVersion(network));
					availableTables.add(currTable.getTitle());
				} else {
					noSignificant = true;
				}
			}
		}
		if (noSignificant) {
			mainPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel(
					"Enrichment retrieval returned no results that met the criteria.",
					SwingConstants.CENTER);
			mainPanel.add(label, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);
		} else if (availableTables.size() == 0) {
			mainPanel = new JPanel(new BorderLayout());
			JLabel label = new JLabel("No enrichment has been retrieved for this network.",
					SwingConstants.CENTER);
			mainPanel.add(label, BorderLayout.CENTER);
			this.add(mainPanel, BorderLayout.CENTER);
			// return;
		} else {
			Collections.sort(availableTables);
			boxTables = new JComboBox<String>(availableTables.toArray(new String[0]));
			if (createBoxTables) {
				if (availableTables.contains(TermCategory.ALL.getTable()))
					showTable = availableTables.get(availableTables.indexOf(TermCategory.ALL.getTable()));
				else 
					showTable = availableTables.get(0);
			}
			boxTables.setSelectedItem(showTable);
			boxTables.addActionListener(this);

			// JPanel buttonsPanelLeft = new JPanel(new GridLayout(1, 3));
			JPanel buttonsPanelLeft = new JPanel();
			BoxLayout layoutLeft = new BoxLayout(buttonsPanelLeft, BoxLayout.LINE_AXIS);
			buttonsPanelLeft.setLayout(layoutLeft);
			butFilter = new JButton(IconManager.ICON_FILTER);
			butFilter.setFont(iconFont);
			butFilter.addActionListener(this);
			butFilter.setToolTipText(butFilterName);
			butFilter.setBorderPainted(false);
			butFilter.setContentAreaFilled(false);
			butFilter.setFocusPainted(false);
			butFilter.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
			
			if (showTable.equals(TermCategory.ALL.getTable())) {
				butDrawCharts = new JButton(chartIcon);
				butDrawCharts.addActionListener(this);
				butDrawCharts.setToolTipText(butDrawChartsName);
				butDrawCharts.setBorderPainted(false);
				butDrawCharts.setContentAreaFilled(false);
				butDrawCharts.setFocusPainted(false);
				butDrawCharts.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 10));
	
				butResetCharts = new JButton(IconManager.ICON_CIRCLE_O);
				butResetCharts.setFont(iconFont);
				butResetCharts.addActionListener(this);
				butResetCharts.setToolTipText(butResetChartsName);
				butResetCharts.setBorderPainted(false);
				butResetCharts.setContentAreaFilled(false);
				butResetCharts.setFocusPainted(false);
				butResetCharts.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 10));
			}
			
			// Add enrichment map button here if EnrichmentMap is loaded
			butEnrichmentMap = new JButton(
					new ImageIcon(getClass().getClassLoader().getResource("/images/em_logo.png")));
			butEnrichmentMap.addActionListener(this);
			butEnrichmentMap.setToolTipText(butEnrichmentMapName);
			butEnrichmentMap.setBorderPainted(false);
			butEnrichmentMap.setContentAreaFilled(false);
			butEnrichmentMap.setFocusPainted(false);
			butEnrichmentMap.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 20));

			buttonsPanelLeft.add(butFilter);
			if (showTable.equals(TermCategory.ALL.getTable())) {
				buttonsPanelLeft.add(butDrawCharts);
				buttonsPanelLeft.add(butResetCharts);
			}
			if (manager.haveEnrichmentMap())
				buttonsPanelLeft.add(butEnrichmentMap);

			// JPanel buttonsPanelRight = new JPanel(new GridLayout(1, 3));
			JPanel buttonsPanelRight = new JPanel();
			BoxLayout layoutRight = new BoxLayout(buttonsPanelRight, BoxLayout.LINE_AXIS);
			buttonsPanelRight.setLayout(layoutRight);
			butAnalyzedNodes = new JButton(IconManager.ICON_CHECK_SQUARE_O);
			butAnalyzedNodes.addActionListener(this);
			butAnalyzedNodes.setFont(iconFont);
			butAnalyzedNodes.setToolTipText(butAnalyzedNodesName);
			butAnalyzedNodes.setBorderPainted(false);
			butAnalyzedNodes.setContentAreaFilled(false);
			butAnalyzedNodes.setFocusPainted(false);
			butAnalyzedNodes.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 10));

			butExportTable = new JButton(IconManager.ICON_SAVE);
			butExportTable.addActionListener(this);
			butExportTable.setFont(iconFont);
			butExportTable.setToolTipText(butExportTableDescr);
			butExportTable.setBorderPainted(false);
			butExportTable.setContentAreaFilled(false);
			butExportTable.setFocusPainted(false);
			butExportTable.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 10));

			butSettings = new JButton(IconManager.ICON_COG);
			butSettings.setFont(iconFont);
			butSettings.addActionListener(this);
			butSettings.setToolTipText(butSettingsName);
			butSettings.setBorderPainted(false);
			butSettings.setContentAreaFilled(false);
			butSettings.setFocusPainted(false);
			butSettings.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 10));

			buttonsPanelRight.add(butAnalyzedNodes);
			buttonsPanelRight.add(butExportTable);
			buttonsPanelRight.add(butSettings);

			JPanel panelMiddle = new JPanel(new BorderLayout());
			Double ppiEnrichment = ModelUtils.getPPIEnrichment(network);
			labelPPIEnrichment = new JLabel();
			if (ppiEnrichment != null && showTable.equals(TermCategory.ALL.getTable())) {
				labelPPIEnrichment = new JLabel("PPI Enrichment: " + ppiEnrichment.toString());
				labelPPIEnrichment.setToolTipText(
						"<html>If the PPI enrichment is less or equal 0.05, your proteins have more interactions among themselves <br />"
								+ "than what would be expected for a random set of proteins of similar size, drawn from the genome. Such <br />"
								+ "an enrichment indicates that the proteins are at least partially biologically connected, as a group.</html>");
			}
			panelMiddle.add(labelPPIEnrichment, BorderLayout.WEST);
			// get the table
			JTable currentTable = enrichmentTables.get(showTable);
			EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
			// System.out.println("show table: " + showTable);
			if (tableModel != null) {
				tableModel.filter(manager.getCategoryFilter(network, showTable),
						manager.getRemoveOverlap(network, showTable), manager.getOverlapCutoff(network, showTable));
				getFilteredTable();
			}

			panelMiddle.add(boxTables, BorderLayout.CENTER);

			labelRows = new JLabel("");
			updateLabelRows();
			labelRows.setHorizontalAlignment(JLabel.RIGHT);
			Font labelFont = labelRows.getFont();
			labelRows.setFont(labelFont.deriveFont((float) (labelFont.getSize() * 0.8)));
			panelMiddle.add(labelRows, BorderLayout.EAST);

			topPanel = new JPanel(new BorderLayout());
			topPanel.add(buttonsPanelLeft, BorderLayout.WEST);
			topPanel.add(panelMiddle, BorderLayout.CENTER);
			topPanel.add(buttonsPanelRight, BorderLayout.EAST);
			this.add(topPanel, BorderLayout.NORTH);

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

	private void createJTable(CyTable cyTable, String currentVersion) {
		EnrichmentTableModel tableModel = null;
		if (currentVersion == null || currentVersion.equals(manager.getOldDataVersion()))
			tableModel = new EnrichmentTableModel(cyTable,
					EnrichmentTerm.swingColumnsEnrichmentOld);
		else
			tableModel = new EnrichmentTableModel(cyTable, EnrichmentTerm.swingColumnsEnrichment);
		
		JTable jTable = new JTable(tableModel);
		TableColumnModel tcm = jTable.getColumnModel();
		tcm.removeColumn(tcm.getColumn(EnrichmentTerm.nodeSUIDColumn));
		tcm.getColumn(EnrichmentTerm.fdrColumn).setCellRenderer(new DecimalFormatRenderer());
		// jTable.setDefaultEditor(Object.class, null);
		// jTable.setPreferredScrollableViewportSize(jTable.getPreferredSize());
		jTable.setFillsViewportHeight(true);
		jTable.setAutoCreateRowSorter(true);
		jTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		jTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		jTable.getSelectionModel().addListSelectionListener(this);
		jTable.getModel().addTableModelListener(this);
		jTable.setDefaultRenderer(Color.class, new ColorRenderer(true));
		CyNetwork network = manager.getCurrentNetwork();
		// TODO: [N] Should we use showTable or cyTable title here? -> seems to work as it is
		jTable.setDefaultEditor(Color.class,
				new ColorEditor(manager, this, colorChooserFactory, network, cyTable.getTitle()));
		popupMenu = new JPopupMenu();
		JMenuItem menuItemReset = new JMenuItem("Remove color");
		menuItemReset.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				JPopupMenu popup = (JPopupMenu) c.getParent();
				JTable table = (JTable) popup.getInvoker();
				if (table.getSelectedRow() > -1) {
					resetColor(table.getSelectedRow());
				}
			}
		});
		popupMenu.add(menuItemReset);

		JMenuItem menuItemAddNodesToNet = new JMenuItem("Add term(s) to network");
		menuItemAddNodesToNet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				JPopupMenu popup = (JPopupMenu) c.getParent();
				JTable table = (JTable) popup.getInvoker();
				if (table.getSelectedRow() > -1) {
					addTermsToNetworkAsNodes(table.getSelectedRows());
				}
			}
		});
		popupMenu.add(menuItemAddNodesToNet);

		JMenuItem menuItemAddAnnotToNet = new JMenuItem("Add term(s) as annotations");
		menuItemAddAnnotToNet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				JPopupMenu popup = (JPopupMenu) c.getParent();
				JTable table = (JTable) popup.getInvoker();
				if (table.getSelectedRow() > -1) {
					addTermsToNetworkAsAnnot(table.getSelectedRows());
				}
			}
		});
		popupMenu.add(menuItemAddAnnotToNet);

		JMenuItem menuItemClearRowSelection = new JMenuItem("Clear row selection");
		menuItemClearRowSelection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				JPopupMenu popup = (JPopupMenu) c.getParent();
				JTable table = (JTable) popup.getInvoker();
				table.clearSelection();
				EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
				tableModel.filterByNodeSUID(null, true, manager.getCategoryFilter(network, showTable),
								manager.getRemoveOverlap(network, showTable), manager.getOverlapCutoff(network, showTable));
				updateLabelRows();
			}
		});
		popupMenu.add(menuItemClearRowSelection);
		
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

		enrichmentTables.put(cyTable.getTitle(), jTable);
		enrichmentTableModels.put(cyTable.getTitle(), tableModel);
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


	public void addTermsToNetworkAsAnnot(int[] selectedRows) {
		JTable currentTable = enrichmentTables.get(showTable);
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		CyNetwork network = manager.getCurrentNetwork();
		CyNetworkView view = manager.getCurrentNetworkView();
		if (network == null || view == null || tableModel == null)
			return;
		
		AnnotationManager annotManager = manager.getService(AnnotationManager.class);
		AnnotationFactory<TextAnnotation> textFactory = 
				(AnnotationFactory<TextAnnotation>) manager.getService(AnnotationFactory.class, "(type=TextAnnotation.class)");
		if (annotManager == null || textFactory == null) {
			System.out.println("AnnotationManager or textFactory is null");
			return;
		}
		
		for (int i : selectedRows) {
			// extract term infos
			String termName = (String) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.nameColumn);
			String termDesc = (String) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.descColumn);
			// Double termFDR = (Double) currentTable.getModel()
			//		.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.fdrColumn);
			//String termCat = (String) currentTable.getModel()
			//		.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.catColumn);
			//Integer termGenes = (Integer) currentTable.getModel()
			//		.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.genesColumn);
			//Integer termBG = (Integer) currentTable.getModel()
			//		.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.bgColumn);
			
			// get nodes associated with term and use them to determine the location for annotations
			Object cellContent = currentTable.getModel().getValueAt(
					currentTable.convertRowIndexToModel(i), EnrichmentTerm.nodeSUIDColumn);
			List<CyNode> termNodes = new ArrayList<CyNode>();
			if (cellContent instanceof List) {
				List<Long> nodeIDs = (List<Long>) cellContent;
				for (Long nodeID : nodeIDs) {
					CyNode stringNode = network.getNode(nodeID);
					termNodes.add(stringNode);
				}
			}
			
			final VisualProperty<Double> xLoc = BasicVisualLexicon.NODE_X_LOCATION;
			final VisualProperty<Double> yLoc = BasicVisualLexicon.NODE_Y_LOCATION;
			Set<Double> xPos = new HashSet<Double>();
			Set<Double> yPos = new HashSet<Double>();
			for (View<CyNode> nodeView : view.getNodeViews()) {
				if (termNodes.contains(nodeView.getModel())) {
					xPos.add(nodeView.getVisualProperty(xLoc));
					yPos.add(nodeView.getVisualProperty(yLoc));
				}
			}
			double xSpan = Collections.max(xPos) - Collections.min(xPos);
			double ySpan = Collections.max(yPos) - Collections.min(yPos);
			// double scaling = view.getNodeViews().size()/(double)termNodes.size();
			
			// create annotaion
			Map<String, String> args = new HashMap<>();
			args.put(TextAnnotation.X, String.valueOf(Collections.min(xPos) - xSpan/8.0));
			args.put(TextAnnotation.Y, String.valueOf(Collections.min(yPos) - ySpan/8.0));
			//args.put(TextAnnotation.Z, String.valueOf(-1));
			args.put(TextAnnotation.TEXT, termDesc);
			//args.put(TextAnnotation.FONTSIZE, String.valueOf(this.font.getSize()));
			//args.put(TextAnnotation.FONTFAMILY, this.font.getFamily());
			//args.put(TextAnnotation.FONTSTYLE, String.valueOf(this.font.getStyle()));
			//args.put(TextAnnotation.COLOR, Color.BLACK.toString());
			
			TextAnnotation annotation = textFactory.createAnnotation(TextAnnotation.class, view, args);
			annotation.setName("stringApp_" + termName);
			annotManager.addAnnotation(annotation);
		}

	}
	
	public void addTermsToNetworkAsNodes(int[] selectedRows) {
		JTable currentTable = enrichmentTables.get(showTable);
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		// currentRow = currentTable.getSelectedRow();
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null || tableModel == null)
			return;
		// create needed columns
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class,
				ModelUtils.DISPLAY);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class,
				ModelUtils.TYPE);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Double.class,
				ModelUtils.NODE_ENRICHMENT_FDR);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), String.class,
				ModelUtils.NODE_ENRICHMENT_CAT);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class,
				ModelUtils.NODE_ENRICHMENT_GENES);
		ModelUtils.createColumnIfNeeded(network.getDefaultNodeTable(), Integer.class,
				ModelUtils.NODE_ENRICHMENT_BG);
		// iterate over all selected row and create nodes and edges
		for (int i : selectedRows) {
			// extract term infos
			String termName = (String) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.nameColumn);
			String termDesc = (String) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.descColumn);
			Double termFDR = (Double) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.fdrColumn);
			String termCat = (String) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.catColumn);
			Integer termGenes = (Integer) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.genesColumn);
			Integer termBG = (Integer) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.bgColumn);
			Color termColor = (Color) currentTable.getModel()
					.getValueAt(currentTable.convertRowIndexToModel(i), EnrichmentTerm.chartColumnCol);
			// create node
			CyNode node = network.addNode();
			// set node infos
			CyRow row = network.getRow(node);
			row.set(CyNetwork.NAME, termName);
			row.set(ModelUtils.DISPLAY, termDesc);
			row.set(ModelUtils.TYPE, "enriched_term");
			row.set(ModelUtils.NODE_ENRICHMENT_FDR, termFDR);
			row.set(ModelUtils.NODE_ENRICHMENT_CAT, termCat);
			row.set(ModelUtils.NODE_ENRICHMENT_GENES, termGenes);
			row.set(ModelUtils.NODE_ENRICHMENT_BG, termBG);
			// get nodes associated with term
			Object cellContent = currentTable.getModel().getValueAt(
					currentTable.convertRowIndexToModel(i), EnrichmentTerm.nodeSUIDColumn);
			// create edges for all associated nodes
			if (cellContent instanceof List) {
				List<Long> nodeIDs = (List<Long>) cellContent;
				for (Long nodeID : nodeIDs) {
					CyNode stringNode = network.getNode(nodeID);
					CyEdge edge = network.addEdge(node, stringNode, false);
					network.getRow(edge).set(CyNetwork.NAME, termName + " (" + "annotates" + ") "
							+ network.getRow(stringNode).get(CyNetwork.NAME, String.class));
					network.getRow(edge).set(CyEdge.INTERACTION, "enrichment");
				}
			}

			if (termColor != null) {
				manager.flushEvents();
				CyNetworkView view = manager.getCurrentNetworkView();
				view.updateView();
				View<CyNode> nodeView = view.getNodeView(node);
				nodeView.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, termColor);
			}
		}
	}

	public void resetColor(int currentRow) {
		JTable currentTable = enrichmentTables.get(showTable);
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		// currentRow = currentTable.getSelectedRow();
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null || tableModel == null)
			return;
		CyTable enrichmentTable = ModelUtils.getEnrichmentTable(manager, network, showTable);
		Color color = (Color) currentTable.getModel().getValueAt(
				currentTable.convertRowIndexToModel(currentRow), EnrichmentTerm.chartColumnCol);
		String termName = (String) currentTable.getModel().getValueAt(
				currentTable.convertRowIndexToModel(currentRow), EnrichmentTerm.nameColumn);
		if (color == null || termName == null)
			return;

		// currentTable.getModel().setValueAt(Color.OPAQUE,
		// currentTable.convertRowIndexToModel(currentRow),
		// EnrichmentTerm.chartColumnCol);
		for (CyRow row : enrichmentTable.getAllRows()) {
			if (enrichmentTable.getColumn(EnrichmentTerm.colName) != null
					&& row.get(EnrichmentTerm.colName, String.class) != null
					&& row.get(EnrichmentTerm.colName, String.class).equals(termName)) {
				row.set(EnrichmentTerm.colChartColor, "");
			}
		}
		tableModel.fireTableDataChanged();

		// re-draw charts if the user changed the color
		Map<EnrichmentTerm, String> preselectedTerms = getUserSelectedTerms();
		if (preselectedTerms.size() > 0)
			ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network, showTable));
	}

	public void resetCharts() {
		CyNetwork network = manager.getCurrentNetwork();
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		if (network == null || tableModel == null)
			return;

		CyTable nodeTable = network.getDefaultNodeTable();
		// replace columns
		ModelUtils.replaceListColumnIfNeeded(nodeTable, String.class,
				EnrichmentTerm.colEnrichmentTermsNames);
		ModelUtils.replaceListColumnIfNeeded(nodeTable, Integer.class,
				EnrichmentTerm.colEnrichmentTermsIntegers);
		ModelUtils.replaceColumnIfNeeded(nodeTable, String.class,
				EnrichmentTerm.colEnrichmentPassthrough);

		// remove colors from table?
		CyTable currTable = ModelUtils.getEnrichmentTable(manager, network, showTable);
		if (currTable == null || currTable.getRowCount() == 0) {
			return;
		}
		for (CyRow row : currTable.getAllRows()) {
			if (currTable.getColumn(EnrichmentTerm.colChartColor) != null
					&& row.get(EnrichmentTerm.colChartColor, String.class) != null
					&& !row.get(EnrichmentTerm.colChartColor, String.class).equals("")) {
				row.set(EnrichmentTerm.colChartColor, "");
			}
		}
		// initPanel();
		tableModel.fireTableDataChanged();
	}

	public void drawCharts() {
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;

		// resetCharts();
		Map<EnrichmentTerm, String> preselectedTerms = getAllUserSelectedTerms();
		if (preselectedTerms.size() == 0) {
			preselectedTerms = getAutoSelectedTopTerms(manager.getTopTerms(network, showTable));
		}
		ViewUtils.drawCharts(manager, preselectedTerms, manager.getChartType(network, showTable));
	}

	public void drawEnrichmentMap() {
		CyNetwork network = manager.getCurrentNetwork();
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		if (network == null || tableModel == null)
			return;
		if (tableModel.getAllRowCount() != tableModel.getRowCount())
			manager.execute(new TaskIterator(
					new EnrichmentMapAdvancedTask(manager, network, getFilteredTable(), true, showTable)));
		else
			manager.execute(new TaskIterator(
					new EnrichmentMapAdvancedTask(manager, network, getFilteredTable(), false, showTable)));
	}

	public void updateLabelRows() {
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		if (tableModel == null)
			return;
		String labelTxt = "";
		if (tableModel.getAllRowCount() != tableModel.getRowCount()) {
			labelTxt = tableModel.getRowCount() + " rows (" + tableModel.getAllRowCount()
					+ " before filtering)";
			// System.out.println("filtered:" + labelTxt);
		} else {
			labelTxt = tableModel.getAllRowCount() + " rows";
			// System.out.println("total rows: " + labelTxt);
		}
		if (labelRows != null)
			labelRows.setText(labelTxt);
	}

	private Map<EnrichmentTerm, String> getUserSelectedTerms() {
		Map<EnrichmentTerm, String> selectedTerms = new LinkedHashMap<EnrichmentTerm, String>();
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return selectedTerms;

		// Set<CyTable> currTables = ModelUtils.getEnrichmentTables(manager, network);
		// for (CyTable currTable : currTables) {
		CyTable currTable = ModelUtils.getEnrichmentTable(manager, network, showTable);
		// currTable.getColumn(EnrichmentTerm.colShowChart) == null ||
		if (currTable == null || currTable.getRowCount() == 0) {
			return selectedTerms;
		}
		for (CyRow row : currTable.getAllRows()) {
			if (currTable.getColumn(EnrichmentTerm.colChartColor) != null
					&& row.get(EnrichmentTerm.colChartColor, String.class) != null
					&& !row.get(EnrichmentTerm.colChartColor, String.class).equals("")
					&& !row.get(EnrichmentTerm.colChartColor, String.class).equals("#ffffff")) {
				String selTerm = row.get(EnrichmentTerm.colName, String.class);
				if (selTerm != null) {
					EnrichmentTerm enrTerm = new EnrichmentTerm(selTerm, 0,
							row.get(EnrichmentTerm.colDescription, String.class),
							row.get(EnrichmentTerm.colCategory, String.class), -1.0, -1.0,
							row.get(EnrichmentTerm.colFDR, Double.class),
							row.get(EnrichmentTerm.colGenesBG, Integer.class));
					enrTerm.setNodesSUID(row.getList(EnrichmentTerm.colGenesSUID, Long.class));
					selectedTerms.put(enrTerm, row.get(EnrichmentTerm.colChartColor, String.class));
				}
			}
		}
		// System.out.println(selectedTerms);
		return selectedTerms;
	}

	private Map<EnrichmentTerm, String> getAllUserSelectedTerms() {
		Map<EnrichmentTerm, String> selectedTerms = new LinkedHashMap<EnrichmentTerm, String>();
		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return selectedTerms;

		// TODO: [Feature] change getAllUserSelectedTerms() to work with groups groups 
		Set<CyTable> currTables = ModelUtils.getMainEnrichmentTables(manager, network);
		for (CyTable currTable : currTables) {
			// currTable = ModelUtils.getEnrichmentTable(manager, network, showTable);
			// currTable.getColumn(EnrichmentTerm.colShowChart) == null ||
			if (currTable.getRowCount() == 0) {
				continue;
			}
			for (CyRow row : currTable.getAllRows()) {
				if (currTable.getColumn(EnrichmentTerm.colChartColor) != null
						&& row.get(EnrichmentTerm.colChartColor, String.class) != null
						&& !row.get(EnrichmentTerm.colChartColor, String.class).equals("")
						&& !row.get(EnrichmentTerm.colChartColor, String.class).equals("#ffffff")) {
					String selTerm = row.get(EnrichmentTerm.colName, String.class);
					if (selTerm != null) {
						EnrichmentTerm enrTerm = new EnrichmentTerm(selTerm, 0,
								row.get(EnrichmentTerm.colDescription, String.class),
								row.get(EnrichmentTerm.colCategory, String.class), -1.0, -1.0,
								row.get(EnrichmentTerm.colFDR, Double.class),
								row.get(EnrichmentTerm.colGenesBG, Integer.class));
						enrTerm.setNodesSUID(row.getList(EnrichmentTerm.colGenesSUID, Long.class));
						selectedTerms.put(enrTerm, row.get(EnrichmentTerm.colChartColor, String.class));
					}
				}
			}
		}
		// System.out.println(selectedTerms);
		return selectedTerms;
	}
	
	private Map<EnrichmentTerm, String> getAutoSelectedTopTerms(int termNumber) {
		Map<EnrichmentTerm, String> selectedTerms = new LinkedHashMap<EnrichmentTerm, String>();
		CyNetwork network = manager.getCurrentNetwork();
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		if (network == null || tableModel == null)
			return selectedTerms;

		CyTable currTable = ModelUtils.getEnrichmentTable(manager, network, showTable);
		if (currTable == null || currTable.getRowCount() == 0) {
			return selectedTerms;
		}

		// List<CyRow> rows = currTable.getAllRows();
		Color[] colors = manager.getEnrichmentPalette(network, showTable).getColors(manager.getTopTerms(network, showTable));
		Long[] rowNames = tableModel.getRowNames();
		for (int i = 0; i < manager.getTopTerms(network, showTable); i++) {
			if (i >= rowNames.length)
				continue;
			CyRow row = currTable.getRow(rowNames[i]);
			String selTerm = row.get(EnrichmentTerm.colName, String.class);
			if (selTerm != null) {
				EnrichmentTerm enrTerm = new EnrichmentTerm(selTerm, 0,
						row.get(EnrichmentTerm.colDescription, String.class),
						row.get(EnrichmentTerm.colCategory, String.class), -1.0, -1.0,
						row.get(EnrichmentTerm.colFDR, Double.class),
						row.get(EnrichmentTerm.colGenesBG, Integer.class));
				enrTerm.setNodesSUID(row.getList(EnrichmentTerm.colGenesSUID, Long.class));
				String color = String.format("#%02x%02x%02x", colors[i].getRed(),
						colors[i].getGreen(), colors[i].getBlue());
				row.set(EnrichmentTerm.colChartColor, color);
				selectedTerms.put(enrTerm, color);
			}
		}
		// initPanel();
		tableModel.fireTableDataChanged();
		return selectedTerms;
	}

	public CyTable getFilteredTable() {
		// Map<EnrichmentTerm, String> selectedTerms = new LinkedHashMap<EnrichmentTerm, String>();
		CyNetwork network = manager.getCurrentNetwork();
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		if (network == null || tableModel == null)
			//return selectedTerms;
			return null;

		String filteredTableName = showTable + EnrichmentTerm.ENRICHMENT_TABLE_FILTERED_SUFFIX;
		CyTable filteredEnrichmentTable = ModelUtils.getEnrichmentTable(manager, network, filteredTableName);
		if (filteredEnrichmentTable != null) {
			return filteredEnrichmentTable;
		}
		CyTable currTable = ModelUtils.getEnrichmentTable(manager, network, showTable);

		if (currTable == null || currTable.getRowCount() == 0) {
			return null;
		}

		CyTableFactory tableFactory = manager.getService(CyTableFactory.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);
		
		filteredEnrichmentTable = tableFactory.createTable(filteredTableName, EnrichmentTerm.colID, Long.class, false, true);
		filteredEnrichmentTable.setTitle(filteredTableName);
		filteredEnrichmentTable.setSavePolicy(SavePolicy.DO_NOT_SAVE);
		tableManager.addTable(filteredEnrichmentTable);
		ModelUtils.setupEnrichmentTable(filteredEnrichmentTable);

		updateFilteredEnrichmentTable(filteredEnrichmentTable);

		return filteredEnrichmentTable;
	}

	public void updateFilteredEnrichmentTable(CyTable filteredEnrichmentTable) {
		if (filteredEnrichmentTable == null)
			filteredEnrichmentTable = getFilteredTable();

		CyNetwork network = manager.getCurrentNetwork();
		if (network == null)
			return;
		
		CyTable currTable = ModelUtils.getEnrichmentTable(manager, network, showTable);
		EnrichmentTableModel tableModel = enrichmentTableModels.get(showTable);
		if (currTable == null || tableModel == null) return;

		filteredEnrichmentTable.deleteRows(filteredEnrichmentTable.getPrimaryKey().getValues(Long.class));

		Long[] rowNames = tableModel.getRowNames();
		for (int i = 0; i < rowNames.length; i++) {
			CyRow row = currTable.getRow(rowNames[i]);
			CyRow filtRow = filteredEnrichmentTable.getRow(rowNames[i]);
			filtRow.set(EnrichmentTerm.colName, row.get(EnrichmentTerm.colName, String.class));
			filtRow.set(EnrichmentTerm.colIDPubl, "");
			filtRow.set(EnrichmentTerm.colYear, row.get(EnrichmentTerm.colYear, Integer.class));
			filtRow.set(EnrichmentTerm.colDescription, row.get(EnrichmentTerm.colDescription, String.class));
			filtRow.set(EnrichmentTerm.colCategory, row.get(EnrichmentTerm.colCategory, String.class));
			filtRow.set(EnrichmentTerm.colFDR, row.get(EnrichmentTerm.colFDR, Double.class));
			filtRow.set(EnrichmentTerm.colFDRTransf, row.get(EnrichmentTerm.colFDRTransf, Double.class));
			filtRow.set(EnrichmentTerm.colPvalue, row.get(EnrichmentTerm.colPvalue, Double.class));
			filtRow.set(EnrichmentTerm.colGenesBG, row.get(EnrichmentTerm.colGenesBG, Integer.class));
			filtRow.set(EnrichmentTerm.colGenesCount, row.get(EnrichmentTerm.colGenesCount, Integer.class));
			filtRow.set(EnrichmentTerm.colGenes, row.getList(EnrichmentTerm.colGenes, String.class));
			filtRow.set(EnrichmentTerm.colGenesSUID, row.getList(EnrichmentTerm.colGenesSUID, Long.class));
			filtRow.set(EnrichmentTerm.colNetworkSUID, row.get(EnrichmentTerm.colNetworkSUID, Long.class));
			// filtRow.set(EnrichmentTerm.colShowChart, false);
			filtRow.set(EnrichmentTerm.colChartColor, "");
		}
	}
}
