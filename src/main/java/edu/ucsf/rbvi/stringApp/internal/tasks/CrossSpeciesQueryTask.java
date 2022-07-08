package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.StringResults;

public class CrossSpeciesQueryTask extends AbstractTask implements ObservableTask {
	final StringManager manager;

	@Tunable(description = "Species 1", 
	         longDescription="Name of the first species.  This should be the actual "+
					                "taxonomic name (e.g. homo sapiens, not human)", 
					 exampleStringValue="homo sapiens")
	public String species1 = null;

	@Tunable (description="Taxon ID 1",
	          longDescription="The taxonomy ID of the first species.  See the NCBI taxonomy home page for IDs",
						exampleStringValue="9606")
	public int taxonID1 = -1;

	@Tunable(description = "Species 2", 
	         longDescription="Name of the second species.  This should be the actual "+
					                "taxonomic name (e.g. homo sapiens, not human)",
					 exampleStringValue="homo sapiens")
	public String species2 = null;

	@Tunable (description="Taxon ID 2",
	          longDescription="The taxonomy ID of the second species.  See the NCBI taxonomy home page for IDs",
						exampleStringValue="9606")
	public int taxonID2 = -1;

	@Tunable(description = "Confidence cutoff",
	         longDescription="The confidence score reflects the cumulated evidence that this "+
					                 "interaction exists.  Only interactions with scores greater than "+
													 "this cutoff will be returned",
	         exampleStringValue="0.4")
	public BoundedDouble cutoff = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	@Tunable(description = "Type of edges to retrieve",
	         longDescription="By default, the query will retrieve functional associations from STRING, but "
	         		+ "it can be set to physical interactions using this option. ",
	         exampleStringValue="full STRING network")
	public ListSingleSelection<NetworkType> networkType;

	private List<Species> speciesList;

	private CyNetwork loadedNetwork;

	public CrossSpeciesQueryTask(final StringManager manager) {
		this.manager = manager;
		speciesList = Species.getSpecies();
		// Set Human as the default
		species1 = Species.getHumanSpecies().toString();
		species2 = null;
		networkType = new ListSingleSelection<>(NetworkType.values());
		networkType.setSelectedValue(NetworkType.FUNCTIONAL);
	}

	public void run(TaskMonitor monitor)  {
		monitor.setTitle("Cross Species Query");
		boolean found;
		Species sp1 = getSpecies(species1, taxonID1);
		if (sp1 == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Unknown or missing species 1");
			throw new RuntimeException("Unknown or missing species 1");
		}

		// See if we have a second species
		Species sp2 = getSpecies(species2, taxonID2);

		StringNetwork stringNetwork = new StringNetwork(manager);
		int confidence = (int)(cutoff.getValue()*100);

		// Create the network from a cross-species query

		LoadSpeciesInteractions loadInteractions = 
						new LoadSpeciesInteractions(stringNetwork, sp1, sp2, confidence, networkType.getSelectedValue());

		manager.execute(new TaskIterator(loadInteractions), true);
		if(loadInteractions.hasError()) {
			monitor.showMessage(Level.ERROR, loadInteractions.getErrorMessage());
			return;
		}
		loadedNetwork = stringNetwork.getNetwork();
		if (loadedNetwork == null)
			throw new RuntimeException("Could not find interactions between "+sp1+" and "+sp2);
	}

	private Species getSpecies(String species, int taxonID) {
		if ((species == null || species.length() == 0) && (taxonID <= 0))
			return null;

		for (Species s: speciesList) {
			if (s.toString().equals(species) || s.getTaxId() == taxonID) {
				return s;
			}
		}
		return null;
	}

	@Override
	public <R> R getResults(Class<? extends R> clzz) {
		return StringResults.getResults(clzz,loadedNetwork);
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return StringResults.getResultClasses();
	}

}
