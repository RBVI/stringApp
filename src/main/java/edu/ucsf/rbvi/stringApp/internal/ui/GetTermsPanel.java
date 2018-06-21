package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

import edu.ucsf.rbvi.stringApp.internal.tasks.GetAnnotationsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ImportNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.TextUtils;

// TODO: [Optional] Improve non-gui mode
public class GetTermsPanel extends JPanel implements TaskObserver { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	// Map<String, List<String>> resolvedIdMap = null;
	// Map<String, List<Annotation>> annotations = null;

	JTextArea searchTerms;
	JPanel mainSearchPanel;
	JComboBox<Species> speciesCombo;
	JComboBox<String> speciesPartnerCombo;
	JCheckBox wholeOrgBox;
	JButton importButton;
	JButton backButton;
	SearchOptionsPanel optionsPanel;
	NumberFormat formatter = new DecimalFormat("#0.00");
	NumberFormat intFormatter = new DecimalFormat("#0");
	private boolean ignore = false;
	private String useDATABASE = Databases.STRING.getAPIName();
	private String netSpecies = null;
	private boolean queryAddNodes = false;
	private int confidence = 40;
	private int additionalNodes = 0;

	public GetTermsPanel(final StringManager manager, final String useDATABASE, boolean queryAddNodes) {
		super(new GridBagLayout());
		this.manager = manager;
		this.useDATABASE = useDATABASE;
		this.queryAddNodes = queryAddNodes;
		init();
	}

	public GetTermsPanel(final StringManager manager, StringNetwork stringNetwork, 
	                     String useDATABASE, String aNetSpecies, boolean queryAddNodes) {
		this(manager, stringNetwork, useDATABASE, aNetSpecies, queryAddNodes, 
		     (int)(manager.getDefaultConfidence()*100), manager.getDefaultAdditionalProteins());
	}

	public GetTermsPanel(final StringManager manager, StringNetwork stringNetwork, 
	                     String useDATABASE, boolean queryAddNodes, SearchOptionsPanel panel) {
		this(manager, stringNetwork, useDATABASE, panel.getSpeciesText(), 
		     queryAddNodes, panel.getConfidence(), panel.getAdditionalNodes());
		optionsPanel.setLoadEnrichment(panel.getLoadEnrichment());
	}

	public GetTermsPanel(final StringManager manager, StringNetwork stringNetwork, 
	                     String useDATABASE, String aNetSpecies, boolean queryAddNodes,
											 int confidence, int additionalNodes) {
		super(new GridBagLayout());
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.confidence = confidence;
		this.additionalNodes = additionalNodes;
		this.useDATABASE = useDATABASE;
		if (aNetSpecies != null) {
			this.netSpecies = aNetSpecies;
		}
		this.queryAddNodes = queryAddNodes;
		init();
	}

	private void init() {
		// Create the surrounding panel
		setPreferredSize(new Dimension(800,600));
		EasyGBC c = new EasyGBC();

		// Create the species panel
		List<Species> speciesList = Species.getSpecies();
		if (speciesList == null) {
			try {
				speciesList = Species.readSpecies(manager);
			} catch (Exception e) {
				manager.error("Unable to get species: "+e.getMessage());
				e.printStackTrace();
				return;
			}
		}
		JPanel organismBox = createOrgBox();
		if (!queryAddNodes) {
			JPanel speciesBox = createSpeciesComboBox(speciesList);
			add(speciesBox, c.expandHoriz().insets(0,5,0,5));

			// Create whole organism checkbox
			if (!useDATABASE.equals(Databases.STITCH.getAPIName())) {
				add(organismBox, c.down().expandHoriz().insets(0, 5, 0, 5));
			}
		} else {
			JPanel speciesBox = createSpeciesPartnerComboBox(ModelUtils.getAvailableInteractionPartners(manager.getCurrentNetwork()));
			add(speciesBox, c.expandHoriz().insets(0,5,0,5));
		}

		// Create the search list panel
		mainSearchPanel = createSearchPanel();
		add(mainSearchPanel, c.down().expandBoth().insets(5,5,0,5));

		optionsPanel = new SearchOptionsPanel(manager, false, false, false);
		if (!useDATABASE.equals(Databases.STITCH.getAPIName()))
			optionsPanel.setUseSmartDelimiters(true);
		optionsPanel.setMinimumSize(new Dimension(400, 150));
		add(optionsPanel, c.down().expandHoriz().insets(5,5,0,5));

		// Add Query/Cancel buttons
		JPanel buttonPanel =  createControlButtons();
		add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
	}

	JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel(new GridBagLayout());
		searchPanel.setPreferredSize(new Dimension(600,400));
		EasyGBC c = new EasyGBC();

		String label = "Enter protein names or identifiers:";
		if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			label = "Enter protein or compound names or identifiers:";
		JLabel searchLabel = new JLabel(label);
		c.noExpand().anchor("northwest").insets(0,5,0,5);
		searchPanel.add(searchLabel, c);
		searchTerms = new JTextArea();
		JScrollPane jsp = new JScrollPane(searchTerms);
		c.down().expandBoth().insets(5,10,5,10);
		searchPanel.add(jsp, c);
		return searchPanel;
	}

	void replaceSearchPanel() {
		mainSearchPanel.removeAll();
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
		mainSearchPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		String label = "Enter protein names or identifiers:";
		if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			label = "Enter protein or compound names or identifiers:";
		JLabel searchLabel = new JLabel(label);
		c.noExpand().anchor("northwest").insets(0,5,0,5);
		mainSearchPanel.add(searchLabel, c);
		searchTerms = new JTextArea();
		JScrollPane jsp = new JScrollPane(searchTerms);
		c.down().expandBoth().insets(5,10,5,10);
		mainSearchPanel.add(jsp, c);
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
		optionsPanel.showAdvancedOptions(true);
	}

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox<Species>(speciesList.toArray(new Species[1]));

		if (netSpecies == null)
			speciesCombo.setSelectedItem(manager.getDefaultSpecies());
		else {
			for (Species s: speciesList) {
				if (s.toString().equals(netSpecies)) {
					speciesCombo.setSelectedItem(s);
					break;
				}
			}
		}
		JComboBoxDecorator.decorate(speciesCombo, true, true); 
		c.right().expandHoriz().insets(0,5,0,5);
		speciesPanel.add(speciesCombo, c);
		return speciesPanel;
	}

	JPanel createSpeciesPartnerComboBox(List<String> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesPartnerCombo = new JComboBox<String>(speciesList.toArray(new String[1]));

		// Set Human as the default
		for (String s: speciesList) {
			if (s.equals(netSpecies)) {
				speciesPartnerCombo.setSelectedItem(s);
				break;
			}
		}
		c.right().expandHoriz().insets(0,5,0,5);
		speciesPanel.add(speciesPartnerCombo, c);
		return speciesPanel;
	}
 
	JPanel createOrgBox() {
		JPanel boxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		wholeOrgBox = new JCheckBox(new AbstractAction("All proteins of this species") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (wholeOrgBox.isSelected()) {
					searchTerms.setText("");
					searchTerms.setEditable(false);
					optionsPanel.enableAdditionalNodes(false);
					optionsPanel.enableLoadEnrichment(false);
				} else {
					searchTerms.setEditable(true);
					optionsPanel.enableAdditionalNodes(true);
					optionsPanel.enableLoadEnrichment(true);
				}
			}
		});
		wholeOrgBox.setSelected(false);
		boxPanel.add(wholeOrgBox);
		return boxPanel;
	}


	JPanel createControlButtons() {
		JPanel buttonPanel = new JPanel();
		BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS);
		buttonPanel.setLayout(layout);
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent e) {
          cancel();
        }
      });

		backButton = new JButton(new AbstractAction("Back") {
        @Override
        public void actionPerformed(ActionEvent e) {
					stringNetwork.reset();
					replaceSearchPanel();
					importButton.setEnabled(true);
					backButton.setEnabled(false);
					importButton.setAction(new InitialAction());
					speciesCombo.setEnabled(true);
					wholeOrgBox.setEnabled(true);
					getParent().revalidate();
        }
			});
		backButton.setEnabled(false);

		importButton = new JButton(new InitialAction());

		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createHorizontalGlue());
		// buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(backButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(importButton);
		return buttonPanel;
	}

	void importNetwork(int taxon, int confidence, int additionalNodes, boolean wholeOrg) {
		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = null;
		if (wholeOrg) {
			// stringIds = ModelUtils.readOrganimsIDs(queryTermMap);
			stringIds = null;
			// stringIds = stringNetwork.combineIds(queryTermMap);
		} else {
			stringIds = stringNetwork.combineIds(queryTermMap);
		}
		// System.out.println("Importing "+stringIds);
		TaskFactory factory = null;
		if (!queryAddNodes) {
			factory = new ImportNetworkTaskFactory(stringNetwork, speciesCombo.getSelectedItem().toString(), 
			                                       taxon, confidence, additionalNodes, stringIds,
			                                       queryTermMap, useDATABASE);
		} else {
			factory = new ImportNetworkTaskFactory(stringNetwork, (String)speciesPartnerCombo.getSelectedItem(), 
			                                       taxon, confidence, additionalNodes, stringIds,
			                                       queryTermMap, useDATABASE);
		}
		cancel();
		if (optionsPanel.getLoadEnrichment())
			manager.execute(factory.createTaskIterator(), this);
		else
			manager.execute(factory.createTaskIterator());
	}

	public void createResolutionPanel() {
		if (!queryAddNodes) 
			speciesCombo.setEnabled(false);
		else
			speciesPartnerCombo.setEditable(false);
		wholeOrgBox.setEnabled(false);
		mainSearchPanel.removeAll();
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
		optionsPanel.showAdvancedOptions(false);
		final Map<String, ResolveTableModel> tableModelMap = new HashMap<>();
		for (String term: stringNetwork.getAnnotations().keySet()) {
			tableModelMap.put(term, new ResolveTableModel(this, term, stringNetwork.getAnnotations().get(term)));
		}
		mainSearchPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		{
			String label = "<html><b>Multiple possible matches found for some terms:</b> ";
			label += "Select the term in the left column to see the possibilities, then select the correct term from the table";
			label += "</html>";

			JLabel lbl = new JLabel(label);
			c.anchor("northeast").expandHoriz();
			mainSearchPanel.add(lbl, c);
		}

		{
			JPanel annPanel = new JPanel(new GridBagLayout());
			EasyGBC ac = new EasyGBC();

			final JTable table = new JTable();
			table.setRowSelectionAllowed(false);

			final JPanel selectPanel = new JPanel(new FlowLayout());
			final JButton selectAllButton = new JButton(new SelectEverythingAction(tableModelMap));
			final JButton clearAllButton = new JButton(new ClearEverythingAction(tableModelMap));
			final JButton selectAllTermButton = new JButton("Select All in Term");
			final JButton clearAllTermButton = new JButton("Clear All in Term");
			selectAllTermButton.setEnabled(false);
			clearAllTermButton.setEnabled(false);
			selectPanel.add(selectAllButton);
			selectPanel.add(clearAllButton);
			selectPanel.add(selectAllTermButton);
			selectPanel.add(clearAllTermButton);

			Object[] terms = stringNetwork.getAnnotations().keySet().toArray();
			final JList termList = new JList(terms);
			termList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			termList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					String term = (String)termList.getSelectedValue();
					showTableRow(table, term, tableModelMap);
					selectAllTermButton.setAction(new SelectAllTermAction(term, tableModelMap));
					selectAllTermButton.setEnabled(true);
					clearAllTermButton.setAction(new ClearAllTermAction(term, tableModelMap));
					clearAllTermButton.setEnabled(true);
				}
			});
			termList.setFixedCellWidth(95);
			termList.setMinimumSize(new Dimension(100,100));

			JScrollPane termScroller = new JScrollPane(termList);
			termScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			termScroller.setPreferredSize(new Dimension(100, 350));
			termScroller.setMinimumSize(new Dimension(100, 200));
			ac.anchor("east").expandVert();
			annPanel.add(termScroller, ac);

			JScrollPane tableScroller = new JScrollPane(table);
			ac.right().expandBoth().insets(0,5,0,5);
			annPanel.add(tableScroller, ac);

			c.down().expandBoth().insets(5,0,5,0);
			mainSearchPanel.add(annPanel, c);

			// Now, select the first term
			termList.setSelectedIndex(0);

			c.down().spanHoriz(2).expandHoriz().insets(0,5,0,5);
			mainSearchPanel.add(selectPanel, c);
		}

		importButton.setAction(new ResolvedAction());
		backButton.setEnabled(true);

		revalidate();
		if (stringNetwork.haveResolvedNames()) {
			importButton.setEnabled(true);
		} else
			importButton.setEnabled(false);
	}

	public void addResolvedStringID(String term, String id) {
		stringNetwork.addResolvedStringID(term, id);
		if (stringNetwork.haveResolvedNames()) {
			importButton.setEnabled(true);
		} else
			importButton.setEnabled(false);
	}

	public void removeResolvedStringID(String term, String id) {
		stringNetwork.removeResolvedStringID(term, id);
		if (stringNetwork.haveResolvedNames()) {
			importButton.setEnabled(true);
		} else
			importButton.setEnabled(false);
	}

	private void showTableRow(JTable table, String term, Map<String, ResolveTableModel> tableModelMap) {
		TableRowSorter sorter = new TableRowSorter(tableModelMap.get(term));
		sorter.setSortable(0, false);
		sorter.setSortable(1, true);
		sorter.setSortable(2, false);
		table.setModel(tableModelMap.get(term));
		table.setRowSorter(sorter);
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		table.getColumnModel().getColumn(2).setCellRenderer(new TextAreaRenderer());
		table.getColumnModel().getColumn(0).setPreferredWidth(50);
		table.getColumnModel().getColumn(1).setPreferredWidth(75);
		table.getColumnModel().getColumn(2).setPreferredWidth(525);
	}

	public void cancel() {
		stringNetwork = initialStringNetwork;
		if (stringNetwork != null) stringNetwork.reset();
		replaceSearchPanel();
		importButton.setEnabled(true);
		backButton.setEnabled(false);
		importButton.setAction(new InitialAction());
		((Window)getRootPane().getParent()).dispose();
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
		//
		if (optionsPanel.getLoadEnrichment()) {
			GetEnrichmentTaskFactory tf = new GetEnrichmentTaskFactory(manager, true);
			ShowEnrichmentPanelTaskFactory showTf = manager.getShowEnrichmentPanelTaskFactory();
			tf.setShowEnrichmentPanelFactory(showTf);
			TunableSetter setter = manager.getService(TunableSetter.class);
			Map<String, Object> valueMap = new HashMap<>();
			valueMap.put("cutoff", 0.05);
			TaskIterator newIterator = 
							setter.createTaskIterator(tf.createTaskIterator(manager.getCurrentNetwork()), valueMap);
			// System.out.println("stringNetwork network = "+stringNetwork.getNetwork());
			manager.execute(newIterator);
		}
	}

	@Override
	public void taskFinished(ObservableTask task) {
	}

	class InitialAction extends AbstractAction implements TaskObserver {
		public InitialAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			// Start our task cascade
    		String speciesName = "";
    		if (!queryAddNodes) {
					speciesName = speciesCombo.getSelectedItem().toString();
    		} else {
    			speciesName = (String)speciesPartnerCombo.getSelectedItem();
    		}
			int taxon = Species.getSpeciesTaxId(speciesName);
			if (taxon == -1) {
				// Oops -- unknown species
				JOptionPane.showMessageDialog(null, "Unknown species: '"+speciesName+"'",
							                        "Unknown species", JOptionPane.ERROR_MESSAGE); 
				return;
			}
			if (stringNetwork == null)
				stringNetwork = new StringNetwork(manager);

			if (wholeOrgBox != null && wholeOrgBox.isSelected()) {
				importNetwork(taxon, optionsPanel.getConfidence(), 0, wholeOrgBox.isSelected());
				return;
			}

			String terms = searchTerms.getText();
			// Strip off any blank lines as well as trailing spaces
			terms = terms.replaceAll("(?m)^\\s*", "");
			terms = terms.replaceAll("(?m)\\s*$", "");
			if (optionsPanel.getUseSmartDelimiters())
				terms = TextUtils.smartDelimit(terms);
			if (terms == null || terms.length() == 0) {
				JOptionPane.showMessageDialog(null, "No terms were entered -- nothing to search for",
							                        "Nothing entered", JOptionPane.ERROR_MESSAGE); 
				return;
			}

			manager.info("Getting annotations for "+speciesName+" terms: "+terms);

			// Launch a task to get the annotations. 
			manager.execute(new TaskIterator(new GetAnnotationsTask(stringNetwork, taxon, terms, useDATABASE)),this);
		}

		@Override
		public void allFinished(FinishStatus finishStatus) {
		}

		@Override
		public void taskFinished(ObservableTask task) {
			if (!(task instanceof GetAnnotationsTask)) {
				return;
			}
			GetAnnotationsTask annTask = (GetAnnotationsTask)task;

			final int taxon = annTask.getTaxon();
			if (stringNetwork.getAnnotations() == null || stringNetwork.getAnnotations().size() == 0) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, "Your query returned no results",
									                        "No results", JOptionPane.ERROR_MESSAGE); 
					}
				});
				return;
			}
			boolean noAmbiguity = stringNetwork.resolveAnnotations();
			if (noAmbiguity) {
				int additionalNodes = optionsPanel.getAdditionalNodes();
				// This mimics the String web site behavior
				if (stringNetwork.getResolvedTerms() == 1 && additionalNodes == 0 && !queryAddNodes) {
					additionalNodes = 10;
					logger.warn("STRING: Only one protein or compound was selected -- additional interactions set to 10");
				}
				//	additionalNodes = 10;

				final int addNodes = additionalNodes;

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						importNetwork(taxon, optionsPanel.getConfidence(), addNodes, wholeOrgBox.isSelected());
					}
				});
			} else {
				createResolutionPanel();
			}
		}
	}

	class ResolvedAction extends AbstractAction {
		public ResolvedAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			int taxon = 0;
			if (!queryAddNodes) 
				taxon = ((Species)speciesCombo.getSelectedItem()).getTaxId();
			else
				taxon = Species.getSpeciesTaxId((String)speciesPartnerCombo.getSelectedItem());
			
			int additionalNodes = optionsPanel.getAdditionalNodes();

			if (stringNetwork.getResolvedTerms() == 1 && additionalNodes == 0 && !queryAddNodes) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, 
												"This will return only one node (Hint: increase maximum interactors slider?)",
									       "Hint", JOptionPane.WARNING_MESSAGE); 
					}
				});
			}

			// if (stringNetwork.getResolvedTerms() == 1)
			// 	additionalNodes = 10;

			if (wholeOrgBox.isSelected()) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, 
												"This will return a network for the whole organims and might take a while!",
									       "Hint", JOptionPane.WARNING_MESSAGE); 
					}
				});
			}

			importNetwork(taxon, optionsPanel.getConfidence(), additionalNodes, wholeOrgBox.isSelected());
		}
	}

}
