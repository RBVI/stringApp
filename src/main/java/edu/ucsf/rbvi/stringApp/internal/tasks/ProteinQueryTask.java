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
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ProteinQueryTask extends AbstractTask implements ObservableTask {
	final StringManager manager;

	@Tunable(description = "Protein ", required = true)
	public String query = null;

	@Tunable(description = "Species", context = "nogui")
	public String species = null;

	@Tunable (description="Taxon ID", context="nogui")
	public int taxonID = -1;

	@Tunable(description = "Maximum additional interactors")
	public BoundedInteger limit = new BoundedInteger(0, 10, 10000, false, false);

	@Tunable(description = "Confidence cutoff")
	public BoundedDouble cutoff = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	private List<Species> speciesList;

	private CyNetwork loadedNetwork;

	public ProteinQueryTask(final StringManager manager) {
		this.manager = manager;
		speciesList = Species.getSpecies();
		// Set Human as the default
		for (Species s : speciesList) {
			if (s.toString().equals("Homo sapiens")) {
				species = s.toString();
				break;
			}
		}
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("STRING Protein Query");
		boolean found;
		Species sp = null;
		for (Species s : speciesList) {
			if (s.toString().equalsIgnoreCase(species) || s.getTaxId() == taxonID) {
				found = true;
				sp = s;
				break;
			}
		}
		if (sp == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Unknown or missing species or NCBI taxon ID");
			return;
		}

		StringNetwork stringNetwork = new StringNetwork(manager);
		int confidence = (int) (cutoff.getValue() * 100);

		// We want the query with newlines, so we need to convert
		query = query.replace(",", "\n");
		// Now, strip off any blank lines
		query = query.replaceAll("(?m)^\\s*", "");

		// Get the annotations
		Map<String, List<Annotation>> annotations = stringNetwork.getAnnotations(sp.getTaxId(),
				query, Databases.STRING.getAPIName());
		if (annotations == null || annotations.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,
					"Query '" + query + "' returned no results");
			return;
		}

		boolean resolved = stringNetwork.resolveAnnotations();

		if (!resolved) {
			// Resolve the annotations by choosing the first stringID for each
			for (String term : annotations.keySet()) {
				stringNetwork.addResolvedStringID(term, annotations.get(term).get(0).getStringId());
			}
		}

		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = stringNetwork.combineIds(queryTermMap);
		LoadInteractions load = new LoadInteractions(stringNetwork, sp.toString(), sp.getTaxId(),
				confidence, limit.getValue(), stringIds, queryTermMap, "", Databases.STRING.getAPIName());
		manager.execute(new TaskIterator(load), true);
		loadedNetwork = stringNetwork.getNetwork();
	}

	@Override
	public <R> R getResults(Class<? extends R> clzz) {
		// Return the network we created
		if (loadedNetwork == null)
			return null;

		if (clzz.equals(CyNetwork.class)) {
			return (R) loadedNetwork;
		} else if (clzz.equals(Long.class)) {
			return (R) loadedNetwork.getSUID();
		} else if (clzz.equals(String.class)) {
			String resp = "Loaded network '"
					+ loadedNetwork.getRow(loadedNetwork).get(CyNetwork.NAME, String.class);
			resp += "' with " + loadedNetwork.getNodeCount() + " nodes and "
					+ loadedNetwork.getEdgeCount() + " edges";
			return (R) resp;
		}
		return null;
	}

}
