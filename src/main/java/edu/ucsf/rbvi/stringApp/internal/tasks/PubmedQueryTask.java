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
import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class PubmedQueryTask extends AbstractTask implements ObservableTask {
	final StringManager manager;

	@Tunable (description="Pubmed query", required=true)
	public String pubmed = null;

	@Tunable (description="Species", context="nogui")
	public String species = null;

	@Tunable (description="Number of proteins")
	public BoundedInteger limit = new BoundedInteger(1, 10, 10000, false, false);

	@Tunable (description="Confidence cutoff")
	public BoundedDouble cutoff = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	private List<Species> speciesList;

	private CyNetwork stringNetwork;

	public PubmedQueryTask(final StringManager manager) {
		this.manager = manager;
		speciesList = Species.getSpecies();
		// Set Human as the default
		for (Species s: speciesList) {
			if (s.toString().equals("Homo sapiens")) {
				species = s.toString();
				break;
			}
		}
	}

	public void run(TaskMonitor monitor)  {
		monitor.setTitle("STRING Query");
		boolean found;
		Species sp = null;
		for (Species s: speciesList) {
			if (s.toString().equals(species)) {
				found = true;
				sp = s;
				break;
			}
		}
		if (sp == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Unknown or missing species");
			return;
		}

		StringNetwork stringNetwork = new StringNetwork(manager);
		int confidence = (int)(cutoff.getValue()*100);
		// Create the network from a pubmed query
		AbstractTask getIds = new GetStringIDsFromPubmedTask(stringNetwork, sp, limit.getValue(), confidence, pubmed);
		insertTasksAfterCurrentTask(getIds);
	}

	@Override
	public <R> R getResults(Class<? extends R> clzz) {
		// Return the network we created
		if (stringNetwork == null) return null;

		if (clzz.equals(CyNetwork.class)) {
			return (R)stringNetwork;
		} else if (clzz.equals(Long.class)) {
			return (R)stringNetwork.getSUID();
		} else if (clzz.equals(String.class)) {
			return (R)stringNetwork.getRow(stringNetwork).get(CyNetwork.NAME, String.class);
		}
		return null;
	}

}
