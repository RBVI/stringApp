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
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

import edu.ucsf.rbvi.stringApp.internal.tasks.GetEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetStringIDsFromPubmedTask;


// TODO: [Optional] Improve non-gui mode
public class PubMedQueryPanel extends JPanel { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;

	JTextArea pubmedQuery;
	JPanel mainSearchPanel;
	JComboBox<Species> speciesCombo;
	JButton importButton;
	SearchOptionsPanel optionsPanel;
	NumberFormat formatter = new DecimalFormat("#0.00");
	NumberFormat intFormatter = new DecimalFormat("#0");

	private boolean ignore = false;
	private String species;

	private int confidence = 40;
	private NetworkType networkType = NetworkType.FUNCTIONAL; 
	private int additionalNodes = 100;

	private boolean loadEnrichment = false;

	public PubMedQueryPanel(final StringManager manager) {
		super(new GridBagLayout());
		this.manager = manager;
		this.species = null;
		this.confidence = (int)(manager.getDefaultConfidence()*100);
		this.networkType = manager.getDefaultNetworkType();
		this.additionalNodes = manager.getDefaultMaxProteins();
		init();
	}

	public PubMedQueryPanel(final StringManager manager, StringNetwork stringNetwork) {
		super(new GridBagLayout());
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.species = null;
		this.confidence = (int)(manager.getDefaultConfidence()*100);
		this.networkType =  manager.getDefaultNetworkType();
		this.additionalNodes = manager.getDefaultMaxProteins();
		init();
	}

	public PubMedQueryPanel(final StringManager manager, StringNetwork stringNetwork, String query, 
	                        SearchOptionsPanel searchOptions) {
		this(manager, stringNetwork, query, 
		     searchOptions.getSpecies(),
		     searchOptions.getConfidence(), searchOptions.getAdditionalNodes(), searchOptions.getNetworkType());		
		loadEnrichment = searchOptions.getLoadEnrichment();
		optionsPanel.setLoadEnrichment(loadEnrichment);
	}

	public PubMedQueryPanel(final StringManager manager, StringNetwork stringNetwork, String query,
	                        final Species species, int confidence, int additionalNodes, NetworkType netType) {
		super(new GridBagLayout());
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.species = species.getName();
		this.confidence = confidence;
		this.networkType = netType;
		this.additionalNodes = additionalNodes;
		init();
		pubmedQuery.setText(query);
	}

	public void doImport() {
		importButton.doClick();
	}

	private void init() {
		// Create the surrounding panel
		setPreferredSize(new Dimension(800,600));
		EasyGBC c = new EasyGBC();

		// Create the species panel
		// Retrieve only the list of main species for now, otherwise the dialogs are very slow
		List<Species> speciesList = Species.getModelSpecies();
		JPanel speciesBox = createSpeciesComboBox(speciesList);
		add(speciesBox, c.expandHoriz().insets(0,5,0,5));

		// Create the search list panel
		mainSearchPanel = createSearchPanel();
		add(mainSearchPanel, c.down().expandBoth().insets(5,5,0,5));

		optionsPanel = new SearchOptionsPanel(manager, true, false, false);
		optionsPanel.setMinimumSize(new Dimension(400, 150));
		optionsPanel.setConfidence(confidence);
		optionsPanel.setNetworkType(networkType);
		optionsPanel.setAdditionalNodes(additionalNodes);
		add(optionsPanel, c.down().expandHoriz().insets(5,5,0,5));

		// Add Query/Cancel buttons
		JPanel buttonPanel =  createControlButtons();
		add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
	}

	JPanel createSearchPanel() {
		String ttText = "<html>Enter any PubMed query, but remember to quote multiple-word terms e.g.:"+
										"<dl><dd>\"drug metabolism\"</dd>"+
										"<dd>(\"Science\")[Journal] AND cancer[Title/Abstract]</dd>"+
										"<dd>Ideker[Author]</dd></dl></html>";
		JPanel queryPanel = new JPanel(new GridBagLayout());
		queryPanel.setPreferredSize(new Dimension(600,300));
		EasyGBC c = new EasyGBC();

		JLabel queryLabel = new JLabel("Pubmed Query:");
		queryLabel.setToolTipText(ttText);

		c.noExpand().anchor("northwest").insets(0,5,0,5);
		queryPanel.add(queryLabel, c);
		pubmedQuery = new JTextArea();
		pubmedQuery.setToolTipText(ttText);
		JScrollPane jsp = new JScrollPane(pubmedQuery);
		c.down().expandBoth().insets(5,10,5,10);
		queryPanel.add(jsp, c);
		return queryPanel;
	}

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox<Species>(speciesList.toArray(new Species[0]));

		if (species == null) {
			speciesCombo.setSelectedItem(Species.getSpecies(manager.getDefaultSpecies()));
		} else {
			speciesCombo.setSelectedItem(Species.getSpecies(species));
		}
		JComboBoxDecorator.decorate(speciesCombo, true, true); 
		c.right().expandHoriz().insets(0,5,0,5);
		speciesPanel.add(speciesCombo, c);
		return speciesPanel;
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

		importButton = new JButton(new InitialAction());

		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(importButton);
		return buttonPanel;
	}

	public void cancel() {
		stringNetwork = initialStringNetwork;
		if (stringNetwork != null) stringNetwork.reset();
		importButton.setEnabled(true);
		importButton.setAction(new InitialAction());
		((Window)getRootPane().getParent()).dispose();
	}


	class InitialAction extends AbstractAction implements TaskObserver {
		public InitialAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			loadEnrichment = optionsPanel.getLoadEnrichment();
			// Start our task cascade
			Species species = (Species)speciesCombo.getSelectedItem();
			if (stringNetwork == null)
				stringNetwork = new StringNetwork(manager);

			int taxon = species.getTaxId();
			String query = pubmedQuery.getText();
			if (query == null || query.length() == 0) {
				JOptionPane.showMessageDialog(null, "No query was entered -- nothing to do",
							                        "Nothing entered", JOptionPane.ERROR_MESSAGE); 
				return;
			}

			confidence = optionsPanel.getConfidence();
			additionalNodes = optionsPanel.getAdditionalNodes();
			networkType = optionsPanel.getNetworkType();
			
			manager.info("Getting pubmed IDs for "+species.getName()+"query: "+query);

			// Launch a task to get the annotations. 
			manager.execute(new TaskIterator(new GetStringIDsFromPubmedTask(stringNetwork, species, 
		                                                                  additionalNodes, confidence, 
		                                                                  query, networkType)), this);
			// cancel();
			((Window)getRootPane().getParent()).dispose();
		}

		@Override
		public void taskFinished(ObservableTask task) {
			if(task instanceof GetStringIDsFromPubmedTask) {
				GetStringIDsFromPubmedTask specificTask = (GetStringIDsFromPubmedTask) task;
				
				if(specificTask.hasError()) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(null, specificTask.getErrorMessage(),
										                        "Error", JOptionPane.ERROR_MESSAGE); 
						}
					});
					return;
				}
			}
		}

		@Override
		public void allFinished(FinishStatus finishStatus) {
			//
			if (loadEnrichment) {
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
				((Window)getRootPane().getParent()).dispose();
			}
		}


	}

}
