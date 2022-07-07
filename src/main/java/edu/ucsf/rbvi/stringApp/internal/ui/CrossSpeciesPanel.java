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
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;

import java.util.ArrayList;
import java.util.Collections;
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
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
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
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
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
public class CrossSpeciesPanel extends JPanel implements TaskObserver { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	// Map<String, List<String>> resolvedIdMap = null;
	// Map<String, List<Annotation>> annotations = null;

	JTextArea searchTerms;
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
	// from stringify
	private String netName = "";
	Task additionalTask = null;

	public CrossSpeciesPanel(final StringManager manager, final String useDATABASE, boolean queryAddNodes) {
		super(new GridBagLayout());
		// System.out.println("Simple terms panel");
		this.manager = manager;
		this.useDATABASE = useDATABASE;
		this.queryAddNodes = queryAddNodes;
		optionsPanel = new SearchOptionsPanel(manager, false, false, true, false);
		optionsPanel.setConfidence((int)(manager.getDefaultConfidence()*100));
		optionsPanel.setNetworkType(manager.getDefaultNetworkType());
		init();
	}

	public CrossSpeciesPanel(final StringManager manager, StringNetwork stringNetwork, 
	                     String useDATABASE, String aNetSpecies, boolean queryAddNodes) {
		this(manager, stringNetwork, useDATABASE, queryAddNodes, null);
		if (aNetSpecies != null) {
			this.netSpecies = aNetSpecies;
			// optionsPanel.setSpeciesText(aNetSpecies);
		}
	}

	/*
	public GetTermsPanel(final StringManager manager, StringNetwork stringNetwork, 
	                     String useDATABASE, String aNetSpecies, boolean queryAddNodes,
											 int confidence, int additionalNodes) {
		optionsPanel = new SearchOptionsPanel(manager, false, false, false);
		optionsPanel.setConfidence(confidence);
		optionsPanel.setAdditionalNodes(additionalNodes);
		optionsPanel.setSpecies(aNetSpecies);
		this(manager, stringNetwork, useDATABASE, queryAddNodes, optionsPanel);
	}
	*/

	public CrossSpeciesPanel(final StringManager manager, StringNetwork stringNetwork, 
	                     String useDATABASE, boolean queryAddNodes, SearchOptionsPanel panel) {
		this(manager, stringNetwork, useDATABASE, queryAddNodes, panel, "", null);
	}

	public CrossSpeciesPanel(final StringManager manager, StringNetwork stringNetwork, 
	                     String useDATABASE, boolean queryAddNodes, SearchOptionsPanel panel,
	                     String netName, Task additionalTask) {
		super(new GridBagLayout());
		// System.out.println("Terms panel");
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.netName = netName;
		this.useDATABASE = useDATABASE;
		if (panel == null) {
			panel = new SearchOptionsPanel(manager, false, false, false);
			panel.setConfidence((int)(manager.getDefaultConfidence()*100));
			panel.setNetworkType(manager.getDefaultNetworkType());
			panel.setAdditionalNodes(manager.getDefaultAdditionalProteins());
			if (!useDATABASE.equals(Databases.STITCH.getAPIName()))
				panel.setUseSmartDelimiters(true);
		}
		if (panel.getSpeciesText() != null) {
			this.netSpecies = panel.getSpeciesText();
		}
		this.queryAddNodes = queryAddNodes;
		optionsPanel = panel;
		this.additionalTask = additionalTask;
		init();
	}

	private void init() {
		// Create the surrounding panel
		setPreferredSize(new Dimension(800,600));
		EasyGBC c = new EasyGBC();

		// Create the species panel
		// Retrieve only the list of main species for now, otherwise the dialogs are very slow
		List<Species> speciesList = Species.getModelSpecies();
		JPanel organismBox = createOrgBox();

		JPanel speciesBox = createSpeciesComboBox(speciesList);
		add(speciesBox, c.expandHoriz().insets(0,5,0,5));

		JPanel species2Box = createSpeciesPartnerComboBox(Species.getSpeciesPartners(Species.getHumanSpecies().toString()));
		add(speciesBox, c.expandHoriz().insets(0,5,0,5));

		// Create whole organism checkbox
		add(organismBox, c.down().expandHoriz().insets(0, 5, 0, 5));

		// optionsPanel = new SearchOptionsPanel(manager, false, false, false);
		// if (!useDATABASE.equals(Databases.STITCH.getAPIName()))
		// 	optionsPanel.setUseSmartDelimiters(true);
		optionsPanel.setMinimumSize(new Dimension(400, 150));
		optionsPanel.showSpeciesBox(false); // We don't want to show two of these
		add(optionsPanel, c.down().expandHoriz().insets(5,5,0,5));

		// Add Query/Cancel buttons
		JPanel buttonPanel =  createControlButtons();
		add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
	}

	JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel(new GridBagLayout());
		searchPanel.setPreferredSize(new Dimension(600,250));
		EasyGBC c = new EasyGBC();

		String label = "Enter protein names or identifiers:";
		if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
			searchPanel.setPreferredSize(new Dimension(600,300));
			label = "Enter protein or compound names or identifiers:";
		}
		JLabel searchLabel = new JLabel(label);
		c.noExpand().anchor("northwest").insets(0,5,0,5);
		searchPanel.add(searchLabel, c);
		searchTerms = new JTextArea();
		JScrollPane jsp = new JScrollPane(searchTerms);
		c.down().expandBoth().insets(5,10,5,10);
		searchPanel.add(jsp, c);
		return searchPanel;
	}

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species 1:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox<Species>(speciesList.toArray(new Species[1]));

		Species defaultSpecies = Species.getHumanSpecies();
		speciesCombo.setSelectedItem(defaultSpecies);


		JComboBoxDecorator.decorate(speciesCombo, true, true); 
		c.right().expandHoriz().insets(0,5,0,5);
		speciesPanel.add(speciesCombo, c);

		speciesCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				DefaultComboBoxModel<String> model = (DefaultComboBoxModel)speciesPartnerCombo.getModel();
				model.removeAllElements();
				List<String> crossList = Species.getSpeciesPartners(speciesCombo.getSelectedItem().toString());
				model.addAll(crossList);
			}
		});
		return speciesPanel;
	}

	JPanel createSpeciesPartnerComboBox(List<String> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species 2:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesPartnerCombo = new JComboBox<String>(speciesList.toArray(new String[1]));

		if (speciesList.contains("Plasmodium falciparum"))
			speciesPartnerCombo.setSelectedItem("Plasmodium falciparum");

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

	void importNetwork(int taxon, int confidence, int additionalNodes, boolean wholeOrg, NetworkType netType) {
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
			                                       queryTermMap, netName, useDATABASE, netType);
		} else {
			factory = new ImportNetworkTaskFactory(stringNetwork, (String)speciesPartnerCombo.getSelectedItem(), 
			                                       taxon, confidence, additionalNodes, stringIds,
			                                       queryTermMap, netName, useDATABASE, netType);
		}
		cancel();
		TaskIterator ti = factory.createTaskIterator();
		if (additionalTask != null)
			ti.append(additionalTask);

		if (optionsPanel.getLoadEnrichment())
			manager.execute(ti, this);
		else
			manager.execute(ti);
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
		importButton.setEnabled(true);
		backButton.setEnabled(false);
		importButton.setAction(new InitialAction());
		((Window)getRootPane().getParent()).dispose();
	}

	public Object[] getTermList() {
			List<String> unresolvedTerms = new ArrayList<>();
			Map<String, List<Annotation>> map = stringNetwork.getAnnotations();
			for (String key: map.keySet()) {
				if (map.get(key).size() > 1)
					unresolvedTerms.add(key);
			}
			return unresolvedTerms.toArray();
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
		optionsPanel.showSpeciesBox(true); // Turn this back on
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
		}

		@Override
		public void allFinished(FinishStatus finishStatus) {
			optionsPanel.showSpeciesBox(true); // Turn this back on
		}

		@Override
		public void taskFinished(ObservableTask task) {
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
				/*
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, 
												"This will return only one node (Hint: increase maximum interactors slider?)",
									       "Hint", JOptionPane.WARNING_MESSAGE); 
					}
				});
				*/
				additionalNodes = 10;
				logger.warn("STRING: Only one protein or compound was selected -- additional interactions set to 10");

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

			importNetwork(taxon, optionsPanel.getConfidence(), additionalNodes, wholeOrgBox.isSelected(), optionsPanel.getNetworkType());
			optionsPanel.showSpeciesBox(true); // Turn this back on
		}
	}

}
