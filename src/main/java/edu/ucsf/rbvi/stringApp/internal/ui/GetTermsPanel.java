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

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

import edu.ucsf.rbvi.stringApp.internal.tasks.GetAnnotationsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.ImportNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

// TODO: [Optional] Improve non-gui mode
public class GetTermsPanel extends JPanel { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;
	// Map<String, List<String>> resolvedIdMap = null;
	// Map<String, List<Annotation>> annotations = null;

	JTextArea searchTerms;
	JPanel mainSearchPanel;
	JComboBox<Species> speciesCombo;
	JComboBox<String> speciesPartnerCombo;
	JSlider confidenceSlider;
	JTextField confidenceValue;
	JSlider additionalNodesSlider;
	JTextField additionalNodesValue;
	JCheckBox wholeOrgBox;
	JButton importButton;
	JButton backButton;
	NumberFormat formatter = new DecimalFormat("#0.00");
	NumberFormat intFormatter = new DecimalFormat("#0");
	private boolean ignore = false;
	private String useDATABASE = Databases.STRING.getAPIName();
	private String netSpecies = "Homo sapiens";
	private boolean queryAddNodes = false;

	public GetTermsPanel(final StringManager manager, final String useDATABASE, boolean queryAddNodes) {
		super(new GridBagLayout());
		this.manager = manager;
		this.useDATABASE = useDATABASE;
		this.queryAddNodes = queryAddNodes;
		init();
	}

	public GetTermsPanel(final StringManager manager, StringNetwork stringNetwork, String useDATABASE, String aNetSpecies, boolean queryAddNodes) {
		super(new GridBagLayout());
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.useDATABASE = useDATABASE;
		if (aNetSpecies != null) {
			this.netSpecies = aNetSpecies;
		}
		this.queryAddNodes = queryAddNodes;
		init();
	}

	private void init() {
		// Create the surrounding panel
		setPreferredSize(new Dimension(800,650));
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
				add(organismBox, c.down().expandBoth().insets(0, 5, 0, 5));
			}
		} else {
			JPanel speciesBox = createSpeciesPartnerComboBox(ModelUtils.getAvailableInteractionPartners(manager.getCurrentNetwork()));
			add(speciesBox, c.expandHoriz().insets(0,5,0,5));
		}
		
		// Create the search list panel
		mainSearchPanel = createSearchPanel();
		add(mainSearchPanel, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel confidenceSlider = createConfidenceSlider();
		add(confidenceSlider, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel additionalNodesSlider = createAdditionalNodesSlider();
		add(additionalNodesSlider, c.down().expandBoth().insets(5,5,0,5));

		// Create the evidence types buttons
		// createEvidenceButtons(manager.getEvidenceTypes());

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
	}

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox<Species>(speciesList.toArray(new Species[1]));
		JComboBoxDecorator.decorate(speciesCombo, true, true); 

		// Set Human as the default
		for (Species s: speciesList) {
			if (s.toString().equals(netSpecies)) {
				speciesCombo.setSelectedItem(s);
				break;
			}
		}
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
		// orgBox = new JCheckBox("All proteins of this species");
		wholeOrgBox = new JCheckBox(new AbstractAction("All proteins of this species") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (wholeOrgBox.isSelected()) {
					searchTerms.setText("");
					searchTerms.setEditable(false);
					additionalNodesSlider.setValue(0);
					additionalNodesSlider.setEnabled(false);
					additionalNodesValue.setText("0");
					additionalNodesValue.setEnabled(false);
				} else {
					searchTerms.setEditable(true);
					additionalNodesSlider.setEnabled(true);
					additionalNodesValue.setEnabled(true);
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

	JPanel createConfidenceSlider() {
		JPanel confidencePanel = new JPanel(new GridBagLayout());
		confidencePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();

		Font labelFont;
		{
			c.anchor("west").noExpand().insets(0,5,0,5);
			JLabel confidenceLabel = new JLabel("Confidence (score) cutoff:");
			labelFont = confidenceLabel.getFont();
			confidenceLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
			confidencePanel.add(confidenceLabel, c);
		}

		{
			confidenceSlider = new JSlider();
			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
			for (int value = 0; value <= 100; value += 10) {
				double labelValue = (double)value/100.0;
				JLabel label = new JLabel(formatter.format(labelValue));
				label.setFont(valueFont);
				labels.put(value, label);
			}
			confidenceSlider.setLabelTable(labels);
			confidenceSlider.setPaintLabels(true);
			confidenceSlider.setValue(40);

			confidenceSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (ignore) return;
					ignore = true;
					int value = confidenceSlider.getValue();
					confidenceValue.setText(formatter.format(((double)value)/100.0));
					ignore = false;
				}
			});
			// c.anchor("southwest").expandHoriz().insets(0,5,0,5);
			c.right().expandHoriz().insets(0,5,0,5);
			confidencePanel.add(confidenceSlider, c);
		}

		{
			confidenceValue = new JTextField(4);
			confidenceValue.setHorizontalAlignment(JTextField.RIGHT);
			confidenceValue.setText("0.40");
			c.right().noExpand().insets(0,5,0,5);
			confidencePanel.add(confidenceValue, c);

			confidenceValue.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					textFieldValueChanged();
				}
			});

			confidenceValue.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					textFieldValueChanged();
				}
			});

		}
		return confidencePanel;
	}
	
	JPanel createAdditionalNodesSlider() {
		JPanel additionalNodesPanel = new JPanel(new GridBagLayout());
		additionalNodesPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();

		Font labelFont;
		{
			c.anchor("west").noExpand().insets(0,5,0,5);
			JLabel additionalNodesLabel = new JLabel("Maximum number of interactors:");
			labelFont = additionalNodesLabel.getFont();
			additionalNodesLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
			additionalNodesPanel.add(additionalNodesLabel, c);
		}

		{
			additionalNodesSlider = new JSlider();
			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
			for (int value = 0; value <= 100; value += 10) {
				JLabel label = new JLabel(Integer.toString(value));
				label.setFont(valueFont);
				labels.put(value, label);
			}
			additionalNodesSlider.setLabelTable(labels);
			additionalNodesSlider.setPaintLabels(true);
			additionalNodesSlider.setValue(0);

			additionalNodesSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (ignore) return;
					ignore = true;
					int value = additionalNodesSlider.getValue();
					additionalNodesValue.setText(Integer.toString(value));
					ignore = false;
				}
			});
			// c.anchor("southwest").expandHoriz().insets(0,5,0,5);
			c.right().expandHoriz().insets(0,5,0,5);
			additionalNodesPanel.add(additionalNodesSlider, c);
		}

		{
			additionalNodesValue = new JTextField(4);
			additionalNodesValue.setHorizontalAlignment(JTextField.RIGHT);
			additionalNodesValue.setText("0");
			c.right().noExpand().insets(0,5,0,5);
			additionalNodesPanel.add(additionalNodesValue, c);

			additionalNodesValue.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					addNodesFieldValueChanged();
				}
			});

			additionalNodesValue.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					addNodesFieldValueChanged();
				}
			});

		}
		return additionalNodesPanel;
	}

	private void addNodesFieldValueChanged() {
		if (ignore) return;
		ignore = true;
		String text = additionalNodesValue.getText();
		Number n = intFormatter.parse(text, new ParsePosition(0));
		int val = 0;
		if (n == null) {
			try {
				val = Integer.valueOf(additionalNodesValue.getText());
			} catch (NumberFormatException nfe) {
				val = addNodesInputError();
			}
		} else if (n.intValue() > 100 || n.intValue() < 0) {
			val = addNodesInputError();
		} else {
			val = n.intValue();
		}

		val = val;
		additionalNodesSlider.setValue(val);
		ignore = false;
	}
	
	private int addNodesInputError() {
		additionalNodesValue.setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, 
				                          "Please enter a number of additional nodes between 0 and 100", 
											            "Alert", JOptionPane.ERROR_MESSAGE);
		additionalNodesValue.setBackground(UIManager.getColor("TextField.background"));

		// Reset the value to correspond to the current slider setting
		int val = additionalNodesSlider.getValue();
		additionalNodesValue.setText(Integer.toString(val));
		return val;
	}
	
	private void textFieldValueChanged() {
		if (ignore) return;
		ignore = true;
		String text = confidenceValue.getText();
		Number n = formatter.parse(text, new ParsePosition(0));
		double val = 0.0;
		if (n == null) {
			try {
				val = Double.valueOf(confidenceValue.getText());
			} catch (NumberFormatException nfe) {
				val = inputError();
			}
		} else if (n.doubleValue() > 1.0 || n.doubleValue() < 0.0) {
			val = inputError();
		} else {
			val = n.doubleValue();
		}

		val = val*100.0;
		confidenceSlider.setValue((int)val);
		ignore = false;
	}

	private double inputError() {
		confidenceValue.setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, 
				                          "Please enter a confence cutoff between 0.0 and 1.0", 
											            "Alert", JOptionPane.ERROR_MESSAGE);
		confidenceValue.setBackground(UIManager.getColor("TextField.background"));

		// Reset the value to correspond to the current slider setting
		double val = ((double)confidenceSlider.getValue())/100.0;
		confidenceValue.setText(formatter.format(val));
		return val;
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
		manager.execute(factory.createTaskIterator());
	}

	void createResolutionPanel() {
		mainSearchPanel.removeAll();
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
		final Map<String, ResolveTableModel> tableModelMap = new HashMap<>();
		for (String term: stringNetwork.getAnnotations().keySet()) {
			tableModelMap.put(term, new ResolveTableModel(this, term, stringNetwork.getAnnotations().get(term)));
		}
		mainSearchPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		{
			String label = "<html><b>Multiple possible matches for some terms:</b> ";
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
			termScroller.setMinimumSize(new Dimension(100, 350));
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


	class InitialAction extends AbstractAction implements TaskObserver {
		public InitialAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			// Start our task cascade
    		int taxon = 0;
    		String speciesName = "";
    		if (!queryAddNodes) {
				Species species = (Species)speciesCombo.getSelectedItem();
				speciesName = species.getName();
				taxon = species.getTaxId();
    		} else {
    			speciesName = (String)speciesPartnerCombo.getSelectedItem();
    			taxon = Species.getSpeciesTaxId(speciesName);
    		}
			if (stringNetwork == null)
				stringNetwork = new StringNetwork(manager);

			String terms = searchTerms.getText();
			if (wholeOrgBox != null && wholeOrgBox.isSelected()) {
				importNetwork(taxon, confidenceSlider.getValue(), 0, wholeOrgBox.isSelected());
				return;
			}
			if (terms == null || terms.length() == 0) {
				JOptionPane.showMessageDialog(null, "No terms were entered -- nothing to search for",
							                        "Nothing entered", JOptionPane.ERROR_MESSAGE); 
				return;
			}
			
			// Strip off any blank lines
			terms = terms.replaceAll("(?m)^\\s*", "");
			manager.info("Getting annotations for "+speciesName+"terms: "+terms);

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
				int additionalNodes = additionalNodesSlider.getValue();
				// This mimics the String web site behavior
				if (stringNetwork.getResolvedTerms() == 1 && additionalNodes == 0) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(null, 
													"This will return only one node (Hint: increase maximum interactors slider?)",
										       "Hint", JOptionPane.WARNING_MESSAGE); 
						}
					});
				}
				//	additionalNodes = 10;

				final int addNodes = additionalNodes;

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						importNetwork(taxon, confidenceSlider.getValue(), addNodes, wholeOrgBox.isSelected());
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
			Species species = (Species)speciesCombo.getSelectedItem();

			int additionalNodes = additionalNodesSlider.getValue();

			if (stringNetwork.getResolvedTerms() == 1 && additionalNodes == 0) {
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

			int taxon = species.getTaxId();
			importNetwork(taxon, confidenceSlider.getValue(), additionalNodes, wholeOrgBox.isSelected());
		}
	}

}
