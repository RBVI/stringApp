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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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

import org.cytoscape.model.CyNetwork;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

// TODO: [Optional] Improve non-gui mode
public class SearchOptionsPanel extends JPanel { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;

	JPanel speciesBox;
	JComboBox<Species> speciesCombo;
	JSlider confidenceSlider;
	JTextField confidenceValue;
	JSlider additionalNodesSlider;
	JTextField additionalNodesValue;
	JCheckBox useSmartDelimiters;
	JCheckBox loadEnrichment;
	JRadioButton physicalNetwork;
	JRadioButton functionalNetwork;
	JCheckBox createNetView;
	JPanel advancedOptions;
	NumberFormat formatter = new DecimalFormat("#0.00");
	NumberFormat intFormatter = new DecimalFormat("#0");
	private boolean ignore = false;
	private final boolean isDisease;
	private final boolean isPubMed;
	private final boolean showSpecies;
	private String netSpecies = "Homo sapiens";

	private Species species = null;
	private int additionalNodes = 0;
	private int confidence = 40;
	private NetworkType networkType = null;

	public SearchOptionsPanel(final StringManager manager, final boolean isPubMed, 
	                          final boolean isDisease, final boolean showSpecies) {
		super(new GridBagLayout());
		this.manager = manager;
		this.isDisease = isDisease;
		this.isPubMed = isPubMed;
		this.showSpecies = showSpecies;
		// System.out.println("SearchOptionsPanel("+isPubMed+","+isDisease+","+showSpecies+")");
		if (isDisease || isPubMed) 
				additionalNodes = manager.getDefaultMaxProteins();
		else
				additionalNodes = manager.getDefaultAdditionalProteins();
		confidence = (int)(manager.getDefaultConfidence()*100);
		this.networkType = manager.getDefaultNetworkType();
		initOptions();
	}

	public SearchOptionsPanel(final StringManager manager, final boolean isPubMed, final boolean isDisease) {
		this(manager, isPubMed, isDisease, true);
	}

	// Special constructor used for new NetworkSearchTaskFactory options.
	public SearchOptionsPanel(final StringManager manager) {
		this(manager, false, false);
	}

	private void initOptions() {
		setPreferredSize(new Dimension(700,200));
		EasyGBC c = new EasyGBC();
		if (showSpecies) {
			List<Species> speciesList = getSpeciesList();
			speciesBox = createSpeciesComboBox(speciesList);
			add(speciesBox, c.expandHoriz().insets(5,5,0,5));
		}

		// Create the radio buttons for the network type
		JPanel networkTypeRadioButtons = createNetworkTypeRadioGroup();
		add(networkTypeRadioButtons, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel confidenceSlider = createConfidenceSlider();
		add(confidenceSlider, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the additional nodes
		JPanel additionalNodesSlider = createAdditionalNodesSlider();
		add(additionalNodesSlider, c.down().expandBoth().insets(5,5,0,5));

		// Add some "advanced" options
		advancedOptions = createAdvancedOptions();
		add(advancedOptions, c.down().expandBoth().insets(5,5,0,5));

		// Add Query/Cancel buttons
		// JPanel buttonPanel =  createControlButtons(true);
		// add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
	}

	public void showSpeciesBox(boolean visible) {
		// System.out.println("showSpeciesBox: "+visible);
		if (speciesBox != null)
			speciesBox.setVisible(visible);
	}


	List<Species> getSpeciesList() {
		// Create the species panel
		List<Species> speciesList = Species.getSpecies();
		if (speciesList == null) {
			try {
				speciesList = Species.readSpecies(manager);
			} catch (Exception e) {
				manager.error("Unable to get species: "+e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
		return speciesList;
	}

	JPanel createAdvancedOptions() {
		JPanel advancedPanel = new JPanel(new GridBagLayout());
		advancedPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		JLabel optionsLabel = new JLabel("<html><b>Options:</b></html>");
		c.anchor("west").insets(0,5,0,5);
		advancedPanel.add(optionsLabel, c);
		if (!isDisease && !isPubMed) {
			c.right().noExpand().insets(0,10,0,5);
			useSmartDelimiters = new JCheckBox("Use Smart Delimiters", false);
			useSmartDelimiters.setToolTipText("<html>\"Smart\" delimiters attempts to provide flexibility "+
			                                  "<br/>to the format of the query terms.  If the entered query "+
			                                  "<br/>doesn't contain any newlines, then tabs, commas, and "+
			                                  "<br/>semicolins will be tried as delimiters, in that order.  "+
			                                  "<br/>Note that smart delimiters don't support quotes - to escape "+
			                                  "<br/>a delimiter, use backslash.</html>");
			advancedPanel.add(useSmartDelimiters, c);
		}
		c.right().expandHoriz().insets(0,10,0,5);
		loadEnrichment = new JCheckBox("Load Enrichment Data", false);
		advancedPanel.add(loadEnrichment, c);
		
		// if we add an option to create or not a network view
		//c.right().expandHoriz().insets(0,10,0,5);
		//createNetView = new JCheckBox("Create network view", true);
		//advancedPanel.add(createNetView, c);
		
		return advancedPanel;
	}

	public boolean getUseSmartDelimiters() {
		return useSmartDelimiters.isSelected();
	}

	public void setUseSmartDelimiters(boolean selected) {
		useSmartDelimiters.setSelected(selected);
	}

	public boolean getLoadEnrichment() {
		if (!loadEnrichment.isEnabled())
			return false;
		return loadEnrichment.isSelected();
	}

	public void setLoadEnrichment(boolean selected) {
		// System.out.println("Setting loadEnrichment to "+selected);
		loadEnrichment.setSelected(selected);
	}

	public void enableLoadEnrichment(boolean enable) {
		loadEnrichment.setEnabled(enable);
	}

	public boolean getCreateNetView() {
		if (!createNetView.isEnabled())
			return false;
		return createNetView.isSelected();
	}

	public void setCreateNetView(boolean selected) {
		// System.out.println("Setting loadEnrichment to "+selected);
		createNetView.setSelected(selected);
	}

	public void enableCreateNetView(boolean enable) {
		createNetView.setEnabled(enable);
	}

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox<Species>(speciesList.toArray(new Species[1]));

		if (isDisease) {
			// Set Human as the default
			for (Species s: speciesList) {
				if (s.toString().equals(netSpecies)) {
					speciesCombo.setSelectedItem(s);
					break;
				}
			}
		} else if (species == null ) {
			speciesCombo.setSelectedItem(manager.getDefaultSpecies());
		} else {
			speciesCombo.setSelectedItem(species);
		}
		JComboBoxDecorator.decorate(speciesCombo, true, true); 
		c.right().expandHoriz().insets(0,5,0,5);
		if (isDisease)
			speciesCombo.setEnabled(false);
		speciesPanel.add(speciesCombo, c);
		return speciesPanel;
	}

	public Species getSpecies() {
		if (speciesCombo != null)
			return (Species)speciesCombo.getSelectedItem();
		return null;
	}

	public String getSpeciesText() {
		if (speciesCombo != null)
			return speciesCombo.getSelectedItem().toString();
		return null;
	}

	public void setSpeciesText(String speciesText) {
		if (speciesCombo != null)
			speciesCombo.setSelectedItem(Species.getSpecies(speciesText));
	}

	public void setSpecies(Species species) {
		if (speciesCombo != null)
			speciesCombo.setSelectedItem(species);
	}

	JPanel createControlButtons(boolean optionsPanel) {
		JPanel buttonPanel = new JPanel();
		BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS);
		buttonPanel.setLayout(layout);
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent e) {
          cancel();
        }
      });

		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createHorizontalGlue());

		JButton closeButton = new JButton(new AbstractAction("Close") {
   	   @Override
   	   public void actionPerformed(ActionEvent e) {
					species = getSpecies();
					confidence = getConfidence();
					additionalNodes = getAdditionalNodes();
					networkType = getNetworkType();

					// What do we do here?
					((Window)getRootPane().getParent()).setVisible(false);
			 }
		});
		buttonPanel.add(closeButton);

		return buttonPanel;
	}

	JPanel createNetworkTypeRadioGroup() {

		JPanel netTypePanel = new JPanel(new GridBagLayout());
		netTypePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();
		
		c.anchor("west").noExpand().insets(0,5,0,5);
		JLabel netTypeLabel = new JLabel("Network type:");
		Font labelFont = netTypeLabel.getFont();
		netTypeLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
		netTypePanel.add(netTypeLabel, c);
		
		c.right().noExpand().insets(0,5,0,5);
		functionalNetwork = new JRadioButton(NetworkType.FUNCTIONAL.toString(), true);
		netTypePanel.add(functionalNetwork, c);

		c.right().expandHoriz().insets(0,5,0,5);
		physicalNetwork = new JRadioButton(NetworkType.PHYSICAL.toString(), false);
		netTypePanel.add(physicalNetwork, c);
		
		ButtonGroup group = new ButtonGroup();
		group.add(physicalNetwork);
		group.add(functionalNetwork);
		
		if (networkType.equals(NetworkType.PHYSICAL)) 
			physicalNetwork.setSelected(true);
		
		return netTypePanel;
	}
	
	public NetworkType getNetworkType() {
		if (physicalNetwork.isSelected())
			return NetworkType.PHYSICAL;
		else 
			return NetworkType.FUNCTIONAL;
	}

	public void setNetworkType(NetworkType type) {
		if (type.equals(NetworkType.FUNCTIONAL)) 
			functionalNetwork.setSelected(true);
		else 
			physicalNetwork.setSelected(true);
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

	public int getConfidence() {
		return confidenceSlider.getValue();
	}

	public void setConfidence(int conf) {
		confidenceSlider.setValue(conf);
	}

	public void showAdvancedOptions(boolean show) {
		advancedOptions.setVisible(show);
	}
	
	JPanel createAdditionalNodesSlider() {
		JPanel additionalNodesPanel = new JPanel(new GridBagLayout());
		additionalNodesPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();

		Font labelFont;
		{
			c.anchor("west").noExpand().insets(0,5,0,5);
			JLabel additionalNodesLabel;
			if (isDisease || isPubMed)
				additionalNodesLabel = new JLabel("Maximum number of proteins:");
			else
				additionalNodesLabel = new JLabel("Maximum additional interactors:");

			labelFont = additionalNodesLabel.getFont();
			additionalNodesLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
			additionalNodesPanel.add(additionalNodesLabel, c);
		}

		{
			int maxValue = 100;
			int minValue = 0;
			if (isDisease || isPubMed) {
				maxValue = 2000;
				minValue = 1;
			}
			additionalNodesSlider = new JSlider(minValue, maxValue, additionalNodes);
			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
			for (int value = 0; value <= maxValue; value += maxValue/10) {
				if (value == 0 && minValue == 1) {
					JLabel label = new JLabel(Integer.toString(1));
					label.setFont(valueFont);
					labels.put(value, label);
				} else {
					JLabel label = new JLabel(Integer.toString(value));
					label.setFont(valueFont);
					labels.put(value, label);
				}
			}
			additionalNodesSlider.setLabelTable(labels);
			additionalNodesSlider.setPaintLabels(true);
			additionalNodesSlider.setValue(additionalNodes);

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
			additionalNodesValue.setText(""+additionalNodes);
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

	public int getAdditionalNodes() {
		return additionalNodesSlider.getValue();
	}

	public void setAdditionalNodes(int additionalNodes) {
		additionalNodesSlider.setValue(additionalNodes);
	}

	public void enableAdditionalNodes(boolean enable) {
		if (!enable) {
			additionalNodesSlider.setValue(0);
			additionalNodesValue.setText("0");
		}
		additionalNodesSlider.setEnabled(enable);
		additionalNodesValue.setEnabled(enable);
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
				val = addNodesInputError(100);
			}
		} else if (n.intValue() < 0) {
			val = addNodesInputError(100);
		} else if (n.intValue() > 100 && !isDisease && !isPubMed ) {
			val = addNodesInputError(100);
		} else if (n.intValue() > 2000 && (isDisease || isPubMed)) {
			val = addNodesInputError(2000);
		} else {
			val = n.intValue();
		}

		val = val;
		additionalNodesSlider.setValue(val);
		ignore = false;
	}
	
	private int addNodesInputError(int maxAddNodes) {
		additionalNodesValue.setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, 
				                          "Please enter a number of additional nodes between 0 and " + maxAddNodes, 
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

  public void cancel() {
    ((Window)getRootPane().getParent()).dispose();
  }


}
