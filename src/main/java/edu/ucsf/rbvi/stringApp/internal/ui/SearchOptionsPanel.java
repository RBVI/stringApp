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
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.model.CyNetwork;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

// TODO: [Optional] Improve non-gui mode
public class SearchOptionsPanel extends JPanel { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;

	JComboBox<Species> speciesCombo;
	JSlider confidenceSlider;
	JTextField confidenceValue;
	JSlider additionalNodesSlider;
	JTextField additionalNodesValue;
	NumberFormat formatter = new DecimalFormat("#0.00");
	NumberFormat intFormatter = new DecimalFormat("#0");
	private boolean ignore = false;
	private final boolean isDisease;
	private String netSpecies = "Homo sapiens";

	private Species species = null;
	private int additionalNodes = 0;
	private int confidence = 40;

	public SearchOptionsPanel(final StringManager manager, final boolean isPubMed, final boolean isDisease) {
		super(new GridBagLayout());
		this.manager = manager;
		this.isDisease = isDisease;
		if (isDisease || isPubMed) additionalNodes = 100;
		initOptions();
	}

	// Special constructor used for new NetworkSearchTaskFactory options.
	public SearchOptionsPanel(final StringManager manager) {
		this(manager, false, false);
	}

	private void initOptions() {
		setPreferredSize(new Dimension(700,200));
		EasyGBC c = new EasyGBC();
		List<Species> speciesList = getSpeciesList();
		JPanel speciesBox = createSpeciesComboBox(speciesList);
		add(speciesBox, c.expandHoriz().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel confidenceSlider = createConfidenceSlider();
		add(confidenceSlider, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the additional nodes
		JPanel additionalNodesSlider = createAdditionalNodesSlider();
		add(additionalNodesSlider, c.down().expandBoth().insets(5,5,0,5));

		// Add Query/Cancel buttons
		// JPanel buttonPanel =  createControlButtons(true);
		// add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
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

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox<Species>(speciesList.toArray(new Species[1]));

		if (species == null || isDisease) {
			// Set Human as the default
			for (Species s: speciesList) {
				if (s.toString().equals(netSpecies)) {
					speciesCombo.setSelectedItem(s);
					break;
				}
			}
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
		return (Species)speciesCombo.getSelectedItem();
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

					// What do we do here?
					((Window)getRootPane().getParent()).setVisible(false);
			 }
		});
		buttonPanel.add(closeButton);

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

	public int getConfidence() {
		return confidenceSlider.getValue();
	}
	
	JPanel createAdditionalNodesSlider() {
		JPanel additionalNodesPanel = new JPanel(new GridBagLayout());
		additionalNodesPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();

		Font labelFont;
		{
			c.anchor("west").noExpand().insets(0,5,0,5);
			JLabel additionalNodesLabel;
			if (isDisease)
				additionalNodesLabel = new JLabel("Maximum number of proteins:");
			else
				additionalNodesLabel = new JLabel("Maximum additional interactors:");

			labelFont = additionalNodesLabel.getFont();
			additionalNodesLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
			additionalNodesPanel.add(additionalNodesLabel, c);
		}

		{
			int maxValue = 100;
			if (isDisease)
				maxValue = 2000;
			additionalNodesSlider = new JSlider(0, maxValue, additionalNodes);
			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
			for (int value = 0; value <= maxValue; value += maxValue/10) {
				JLabel label = new JLabel(Integer.toString(value));
				label.setFont(valueFont);
				labels.put(value, label);
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

  public void cancel() {
    ((Window)getRootPane().getParent()).dispose();
  }


}
