package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
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
import edu.ucsf.rbvi.stringApp.internal.utils.StringResults;

public class ProteinQueryTask extends AbstractTask implements ObservableTask {
	final StringManager manager;

	@Tunable(description = "Protein query", required = true, 
	         longDescription="Comma separated list of protein names or identifiers",
					 exampleStringValue="EGFR,BRCA1,BRCA2,TP53")
	public String query = null;

	@Tunable(description = "Species", 
	         longDescription="Species name.  This should be the actual "+
					                "taxonomic name (e.g. homo sapiens, not human)",
					 exampleStringValue="homo sapiens")
	public String species = null;

	@Tunable (description="Taxon ID",
	          longDescription="The species taxonomy ID.  See the NCBI taxonomy home page for IDs. If both species and taxonID are set to a different species, the taxonID has priority.",
						exampleStringValue="9606")
	public int taxonID = -1;

	@Tunable(description = "Maximum additional interactors",
	         longDescription="The maximum number of proteins to return in addition to the query set",
					 exampleStringValue="100")
	public BoundedInteger limit = new BoundedInteger(0, 10, 10000, false, false);

	@Tunable(description = "Confidence cutoff",
	         longDescription="The confidence score reflects the cumulated evidence that this "+
					                 "interaction exists.  Only interactions with scores greater than "+
													 "this cutoff will be returned",
	         exampleStringValue="0.4")
	public BoundedDouble cutoff = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	@Tunable(description="String network to add additional nodes to",
	         longDescription=StringToModel.CY_NETWORK_LONG_DESCRIPTION,
					 exampleStringValue="current")
	public CyNetwork network = null;

	@Tunable(description = "Query includes virus protein identifiers",
	         longDescription="By default, a query will search for identifiers in both the protein and virus "+
	                         "databases.  By changing this to 'false', only the protein database will be"+
	                         "searched",
	         exampleStringValue="false")
	public boolean includesViruses = true;

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
		Species sp = Species.getSpecies(species);
		if (taxonID != -1) {
			for (Species s : speciesList) {
				if (s.getTaxId() == taxonID) {
					if (!s.toString().equals(species)) {
						monitor.showMessage(TaskMonitor.Level.WARN, "Species name and NCBI taxon ID do not match, taxon ID will be used.");
					}
					sp = s;
					break;
				}
			}
		}
		if (sp == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Unknown or missing species or NCBI taxon ID");
			return;
		}

		StringNetwork stringNetwork = new StringNetwork(manager);
		int confidence = (int) (cutoff.getValue() * 100);
		if (network != null) {
			// Make sure it's a string network
			stringNetwork.setNetwork(network);
		}

		// We want the query with newlines, so we need to convert
		query = query.replace(",", "\n");
		// Now, strip off any blank lines
		query = query.replaceAll("(?m)^\\s*", "");

		// Get the annotations
		Map<String, List<Annotation>> annotations = stringNetwork.getAnnotations(sp.getTaxId(),
				query, Databases.STRING.getAPIName(), includesViruses);
		if (annotations == null || annotations.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,
					"Query '" + trunc(query) + "' returned no results");
			throw new RuntimeException("Query '"+trunc(query)+"' returned no results");
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
		if (loadedNetwork == null) {
			throw new RuntimeException("Query '"+query+"' returned no results");
		}
	}

	private String trunc(String str) {
		if (str.length() > 1000)
			return str.substring(0,1000)+"...";
		return str;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		return StringResults.getResults(clzz, loadedNetwork);
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return StringResults.getResultClasses();
	}

}
