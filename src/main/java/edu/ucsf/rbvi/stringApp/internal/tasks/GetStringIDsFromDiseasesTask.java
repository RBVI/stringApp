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

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetStringIDsFromDiseasesTask extends AbstractTask implements ObservableTask {
	final StringNetwork stringNetwork;
	final StringManager manager;
	final Species species;
	final int limit;
	final int confidence;
	final String query;
	final String diseaseName;
	private List<TextMiningResult> tmResults;

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
		monitor.setTitle("Querying to get proteins associated with disease based on text mining");
		Map<String, String> args = new HashMap<>();
		args.put("type1", "-26");
		args.put("id1",query);
		args.put("format", "json");
		args.put("limit", Integer.toString(limit));
		args.put("type2", Integer.toString(species.getTaxId()));
		JSONObject tmobject = HttpUtils.postJSON(manager.getIntegrationURL(), args, manager);
		if (tmobject == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}

		tmResults = ModelUtils.getIdsFromJSON(manager, species.getTaxId(), tmobject, query, true);
		if (tmResults == null || tmResults.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}
		monitor.showMessage(TaskMonitor.Level.INFO,"Found "+tmResults.size()+" associated proteins");

		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = new ArrayList<>();
		for (TextMiningResult tm: tmResults) {
			stringIds.add(tm.getID());
		}

		// OK, if we got any results, fetch the network
		LoadInteractions liTask = new LoadInteractions(stringNetwork, species.getName(), species.getTaxId(), 
			                                             confidence, 0, stringIds, queryTermMap, diseaseName, Databases.STRING.getAPIName());
		AddTextMiningResultsTask atmTask = new AddTextMiningResultsTask(stringNetwork, tmResults);
		insertTasksAfterCurrentTask(liTask, atmTask);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Find proteins from text mining";
	}

	public List<TextMiningResult> getTextMiningResults() {
		return tmResults;
	}

	public <R> R getResults(Class<? extends R> type) {
		return null;
	}
}
