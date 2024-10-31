package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;

import edu.ucsf.rbvi.stringApp.internal.utils.JSONUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.TextUtils;

public class GetStringIDsFromDiseasesTask extends AbstractTask implements ObservableTask {
	final StringNetwork stringNetwork;
	final StringManager manager;
	final Species species;
	final int limit;
	final int confidence;
	final String query;
	final String diseaseName;
	NetworkType netType; 
	private List<TextMiningResult> tmResults;

	public GetStringIDsFromDiseasesTask(final StringNetwork stringNetwork, final Species species, final int limit, 
            final int confidence, final String query, final String diseaseName, final NetworkType netType) {
		this(stringNetwork, species, limit, confidence, query, diseaseName);
		this.netType = netType;
	}

	public GetStringIDsFromDiseasesTask(final StringNetwork stringNetwork, final Species species, final int limit, 
	                                    final int confidence, final String query, final String diseaseName) {
		this.stringNetwork = stringNetwork;
		manager = stringNetwork.getManager();
		this.species = species;
		this.limit = limit;
		this.confidence = confidence;
		this.query = query;
		this.diseaseName = diseaseName;
	}
	public void run(TaskMonitor monitor) {
		monitor.setTitle("Loading STRING network with disease associated proteins");
		monitor.setTitle("Querying to get proteins associated with disease from DISEASES database");
		Map<String, String> args = new HashMap<>();
		args.put("type1", "-26");
		args.put("id1",query);
		args.put("format", "json");
		args.put("limit", Integer.toString(limit));
		args.put("type2", Integer.toString(species.getTaxId()));
		JSONObject tmobject;
		try {
			tmobject = HttpUtils.postJSON(manager.getIntegrationURL(), args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}
		if (tmobject == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"DISEASES returned no results for this disease query '" + query + "'.");
			return;
		}

		tmResults = JSONUtils.getIdsFromJSON(manager, species.getTaxId(), tmobject, query, true);
		if (tmResults == null || tmResults.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"DISEASES returned no results for this disease query '" + query + "'.");
			return;
		}
		monitor.showMessage(TaskMonitor.Level.INFO,"Found "+tmResults.size()+" associated proteins.");

		// TODO: [move] we need to call getAnnotations before loading interactions --> added below, still needs to be double checked, code copied from StringifyTask
		List<String> stringIdsTM = new ArrayList<>();
		for (TextMiningResult tm: tmResults) {
			stringIdsTM.add(tm.getID());
		}

		String terms = ModelUtils.listToString(stringIdsTM);

		// We want the query with newlines, so we need to convert
		terms = terms.replace(",", "\n");
		// Now, strip off any blank lines
		terms = terms.replaceAll("(?m)^\\s*", "");
		
		// Get the annotations
		String useDatabase =  Databases.STRINGDB.getAPIName();
		Map<String, List<Annotation>> annotations;
		try {
			annotations = stringNetwork.getAnnotations(manager, species, terms, useDatabase, false);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(TaskMonitor.Level.ERROR,
					"Cannot connect to " + useDatabase);
			throw new RuntimeException("Cannot connect to " + useDatabase);
		}

		if (annotations == null || annotations.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,
					"Query '" + TextUtils.trunc(terms) + "' returned no results");
			throw new RuntimeException("Query '"+ TextUtils.trunc(terms)+"' returned no results");
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

		// OK, if we got any results, fetch the network
		LoadInteractions liTask = new LoadInteractions(stringNetwork, species.getName(), species, 
			                                             confidence, 0, stringIds, queryTermMap, diseaseName, 
			                                            useDatabase, netType);
		AddTextMiningResultsTask atmTask = new AddTextMiningResultsTask(stringNetwork, tmResults);
		insertTasksAfterCurrentTask(liTask, atmTask);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Find proteins from DISEASES";
	}

	public List<TextMiningResult> getTextMiningResults() {
		return tmResults;
	}

	public <R> R getResults(Class<? extends R> type) {
		return null;
	}
}
