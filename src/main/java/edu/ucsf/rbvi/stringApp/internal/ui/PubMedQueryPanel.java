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

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

import edu.ucsf.rbvi.stringApp.internal.tasks.AddTextMiningResultsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.ImportNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetStringIDsFromPubmedTask;

import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

// TODO: [Optional] Improve non-gui mode
public class PubMedQueryPanel extends JPanel { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;

	JTextArea pubmedQuery;
	JPanel mainSearchPanel;
	JComboBox<Species> speciesCombo;
	JSlider limitSlider;
	JTextField limitValue;
	JSlider confidenceSlider;
	JTextField confidenceValue;
	JButton importButton;
	NumberFormat formatter = new DecimalFormat("#0.00");
	NumberFormat intFormatter = new DecimalFormat("#0");
	private boolean ignore = false;
	private final Species species;
	private int confidence = 40;
	private int proteinLimit = 100;

	public PubMedQueryPanel(final StringManager manager) {
		super(new GridBagLayout());
		this.manager = manager;
		this.species = null;
		init();
	}

	public PubMedQueryPanel(final StringManager manager, StringNetwork stringNetwork) {
		super(new GridBagLayout());
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.species = null;
		init();
	}

	public PubMedQueryPanel(final StringManager manager, StringNetwork stringNetwork, String query,
	                        final Species species, int confidence, int additionalNodes) {
		super(new GridBagLayout());
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.species = species;
		this.confidence = confidence;
		this.proteinLimit = additionalNodes;
		init();
		pubmedQuery.setText(query);
	}

	public void doImport() {
		importButton.doClick();
	}

	private void init() {
		// Create the surrounding panel
		setPreferredSize(new Dimension(600,400));
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
		JPanel speciesBox = createSpeciesComboBox(speciesList);
		add(speciesBox, c.expandHoriz().insets(0,5,0,5));

		// Create the search list panel
		mainSearchPanel = createSearchPanel();
		add(mainSearchPanel, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the limit 
		JPanel limitSlider = createLimitSlider();
		add(limitSlider, c.down().expandHoriz().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel confidenceSlider = createConfidenceSlider();
		add(confidenceSlider, c.down().expandHoriz().insets(5,5,0,5));

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
			// Set Human as the default
			for (Species s: speciesList) {
				if (s.toString().equals("Homo sapiens")) {
					speciesCombo.setSelectedItem(s);
					break;
				}
			}
		} else {
			speciesCombo.setSelectedItem(species);
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

	JPanel createLimitSlider() {
		JPanel limitPanel = new JPanel(new GridBagLayout());
		limitPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();

		Font labelFont;
		{
			c.anchor("west").noExpand().insets(0,5,0,5);
			JLabel limitLabel = new JLabel("Maximum number of proteins:");
			labelFont = limitLabel.getFont();
			limitLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
			limitPanel.add(limitLabel, c);
		}

		{
			limitSlider = new JSlider(0, 2000, proteinLimit);
			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
			for (int value = 0; value <= 2000; value += 400) {
				JLabel label = new JLabel(intFormatter.format(value));
				label.setFont(valueFont);
				labels.put(value, label);
			}
			limitSlider.setLabelTable(labels);
			limitSlider.setPaintLabels(true);

			limitSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (ignore) return;
					ignore = true;
					int value = limitSlider.getValue();
					limitValue.setText(intFormatter.format(value));
					ignore = false;
				}
			});
			// c.anchor("southwest").expandHoriz().insets(0,5,0,5);
			c.right().expandHoriz().insets(0,5,0,5);
			limitPanel.add(limitSlider, c);
		}

		{
			limitValue = new JTextField(4);
			limitValue.setHorizontalAlignment(JTextField.RIGHT);
			limitValue.setText(""+proteinLimit);
			c.right().noExpand().insets(0,5,0,5);
			limitPanel.add(limitValue, c);

			limitValue.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					limitValueChanged();
				}
			});

			limitValue.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					limitValueChanged();
				}
			});

		}
		return limitPanel;
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
			confidenceSlider.setValue(confidence);

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
			confidenceValue.setText(formatter.format(((double)confidence)/100.0));
			c.right().noExpand().insets(0,5,0,5);
			confidencePanel.add(confidenceValue, c);

			confidenceValue.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					confidenceValueChanged();
				}
			});

			confidenceValue.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					confidenceValueChanged();
				}
			});

		}
		return confidencePanel;
	}

	private void confidenceValueChanged() {
		if (ignore) return;
		ignore = true;
		String text = confidenceValue.getText();
		Number n = formatter.parse(text, new ParsePosition(0));
		double val = 0.0;
		if (n == null) {
			try {
				val = Double.valueOf(confidenceValue.getText());
			} catch (NumberFormatException nfe) {
				val = confidenceInputError();
			}
		} else if (n.doubleValue() > 1.0 || n.doubleValue() < 0.0) {
			val = confidenceInputError();
		} else {
			val = n.doubleValue();
		}

		val = val*100.0;
		confidenceSlider.setValue((int)val);
		ignore = false;
	}

	private double confidenceInputError() {
		confidenceValue.setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, 
				                          "Please enter a confidence cutoff between 0.0 and 1.0", 
											            "Alert", JOptionPane.ERROR_MESSAGE);
		confidenceValue.setBackground(UIManager.getColor("TextField.background"));

		// Reset the value to correspond to the current slider setting
		double val = ((double)confidenceSlider.getValue())/100.0;
		confidenceValue.setText(formatter.format(val));
		return val;
	}
	
	private void limitValueChanged() {
		if (ignore) return;
		ignore = true;
		String text = limitValue.getText();
		Number n = intFormatter.parse(text, new ParsePosition(0));
		int val = 0;
		if (n == null) {
			try {
				val = Integer.valueOf(limitValue.getText());
			} catch (NumberFormatException nfe) {
				val = limitInputError();
			}
		} else if (n.intValue() > 2000 || n.intValue() < 0) {
			val = limitInputError();
		} else {
			val = n.intValue();
		}

		limitSlider.setValue((int)val);
		ignore = false;
	}

	private int limitInputError() {
		limitValue.setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, 
				                          "Please enter maximum number of proteins between 100 and 10,000", 
											            "Alert", JOptionPane.ERROR_MESSAGE);
		limitValue.setBackground(UIManager.getColor("TextField.background"));

		// Reset the value to correspond to the current slider setting
		int val = limitSlider.getValue();
		limitValue.setText(intFormatter.format(val));
		return val;
	}

	public void cancel() {
		stringNetwork = initialStringNetwork;
		if (stringNetwork != null) stringNetwork.reset();
		importButton.setEnabled(true);
		importButton.setAction(new InitialAction());
		((Window)getRootPane().getParent()).dispose();
	}


	class InitialAction extends AbstractAction {
		public InitialAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
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

			int confidence = confidenceSlider.getValue();

			manager.info("Getting pubmed IDs for "+species.getName()+"query: "+query);

			// Launch a task to get the annotations. 
			manager.execute(new TaskIterator(new GetStringIDsFromPubmedTask(stringNetwork, species, 
		                                                                  limitSlider.getValue(), confidence, query)));
			cancel();
		}

	}

}
