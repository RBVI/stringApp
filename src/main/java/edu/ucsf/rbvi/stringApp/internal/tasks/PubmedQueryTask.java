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
import edu.ucsf.rbvi.stringApp.internal.utils.StringResults;

public class PubmedQueryTask extends AbstractTask implements ObservableTask {
	final StringManager manager;

	@Tunable(description = "Pubmed query", required = true, 
	         longDescription="Enter a pubmed query (see NCBI tutorials for information about "+
					                 "pubmed query syntax)",
					 exampleStringValue="krogan[au] AND morris[au] AND gulbahce[au] AND HIV[title]")
	public String pubmed = null;

	@Tunable(description = "Species", 
	         longDescription="Species name.  This should be the actual "+
					                "taxonomic name (e.g. homo sapiens, not human)",
					 exampleStringValue="homo sapiens")
	public String species = null;

	@Tunable (description="Taxon ID",
	          longDescription="The species taxonomy ID.  See the NCBI taxonomy home page for IDs",
						exampleStringValue="9606")
	public int taxonID = -1;

	@Tunable(description = "Maximum additional interactors",
	         longDescription="The maximum number of proteins to return in addition to the query set",
					 exampleStringValue="100")
	public BoundedInteger limit = new BoundedInteger(1, 100, 10000, false, false);

	@Tunable(description = "Confidence cutoff",
	         longDescription="The confidence score reflects the cumulated evidence that this "+
					                 "interaction exists.  Only interactions with scores greater than "+
													 "this cutoff will be returned",
	         exampleStringValue="0.4")
	public BoundedDouble cutoff = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	private List<Species> speciesList;

	private CyNetwork loadedNetwork;

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
			if (s.toString().equals(species) || s.getTaxId() == taxonID) {
				found = true;
				sp = s;
				break;
			}
		}
		if (sp == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Unknown or missing species");
			throw new RuntimeException("Query '"+pubmed+"' returned no results");
		}

		StringNetwork stringNetwork = new StringNetwork(manager);
		int confidence = (int)(cutoff.getValue()*100);
		// Create the network from a pubmed query
		AbstractTask getIds = 
						new GetStringIDsFromPubmedTask(stringNetwork, sp, limit.getValue(), confidence, pubmed);
		manager.execute(new TaskIterator(getIds), true);
		loadedNetwork = stringNetwork.getNetwork();
		if (loadedNetwork == null)
			throw new RuntimeException("Query '"+pubmed+"' returned no results");
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
