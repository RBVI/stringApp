package edu.ucsf.rbvi.stringApp.internal.ui;

import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

/**
 * A {@link JComboBox} pre-populated with the list of STRING model species
 * (adding the given default species if it isn't already in that list) and
 * decorated via {@link JComboBoxDecorator} so the user can type to search.
 * Used as the species selector in panels such as {@link GetTermsPanel} and
 * {@link SearchOptionsPanel}.
 */
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
