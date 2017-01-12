package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetDiseaseTermsTask extends AbstractTask implements ObservableTask {
	final StringManager stringManager;
	final int taxon;
	final String term;
	List<EntityIdentifier> matches = null;

	public GetDiseaseTermsTask(StringManager stringManager, int taxon, String term) {
		this.stringManager = stringManager;
		this.taxon = taxon;
		this.term = term;
	}

	@Override
	public void run(TaskMonitor monitor) {
		monitor.setTitle("Getting disease terms");
		String url = stringManager.getEntityQueryURL();

		Map<String, String> args = new HashMap<>();
		args.put("limit", "100");
		args.put("types", "-26");
		args.put("format", "json");
		args.put("query", term);
		// String response = "[[{"type":-26,"id":"DOID:1307","matched":"dementia","primary":"dementia"},{"type":-26,"id":"DOID:11870","matched":"Dementia in Pick's disease ","primary":"Pick's disease"},{"type":-26,"id":"DOID:12217","matched":"Dementia with Lewy bodies","primary":"Lewy body dementia"}],false]"
		//
		// Get the results
		JSONObject results = HttpUtils.getJSON(url, args, stringManager);
		// Object results = HttpUtils.testJSON(url, args, stringManager, response);
		if (results == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}

		matches = ModelUtils.getEntityIdsFromJSON(stringManager, results);
		
	}

	public List<EntityIdentifier> getMatchedTerms() { return matches; }

	public int getTaxon() { return taxon; }

	@Override
	public <T> T getResults(Class<? extends T> type) {
		return null;
	}
}

