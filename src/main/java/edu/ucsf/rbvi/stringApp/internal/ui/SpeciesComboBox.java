package edu.ucsf.rbvi.stringApp.internal.ui;

import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;

/*
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
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
*/
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class SpeciesComboBox extends JComboBox<Species> { 
	final StringManager manager;

	public SpeciesComboBox(final StringManager manager, String defaultSpecies) {
		super(getSpeciesArray());
		this.manager = manager;
		List<Species> speciesList = Species.getModelSpecies();
		Species def;

		if (defaultSpecies == null ) {
			def = Species.getSpecies(manager.getDefaultSpecies());
		} else {
			def = Species.getSpecies(defaultSpecies);
		}

		if (!speciesList.contains(def)) {
			speciesList.add(def);
			Collections.sort(speciesList);
		}

		this.setSelectedItem(def);

    JComboBoxDecorator decorator = new JComboBoxDecorator(this, true, true, speciesList);
		decorator.decorate(speciesList); 
		return;
	}

	public static Species[] getSpeciesArray() {
		List<Species> speciesList = Species.getModelSpecies();
		return speciesList.toArray(new Species[1]);
	}

}
