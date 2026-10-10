package edu.ucsf.rbvi.stringApp.internal.ui;

import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;

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
