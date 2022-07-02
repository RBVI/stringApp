package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetStringIDsFromPubmedTask extends AbstractTask implements ObservableTask {
	final StringNetwork stringNetwork;
	final StringManager manager;
	final Species species;
	final int limit;
	final int confidence;
	final String query;
	NetworkType netType; 
	private List<TextMiningResult> tmResults;
	String errorMsg;

	public GetStringIDsFromPubmedTask(final StringNetwork stringNetwork, final Species species, final int limit, 
            final int confidence, final String query, final NetworkType netType) {
		this(stringNetwork, species, limit, confidence, query);
		this.netType = netType;
	}
	
	public GetStringIDsFromPubmedTask(final StringNetwork stringNetwork, final Species species, final int limit, 
	                                    final int confidence, final String query) {
		this.stringNetwork = stringNetwork;
		manager = stringNetwork.getManager();
		this.species = species;
		this.confidence = confidence;
		this.limit = limit;
		this.query = query;
		this.errorMsg = null;
	}
	
	public boolean hasError() {
		return this.errorMsg != null;
	}
	
	public String getErrorMessage() {
		return this.errorMsg;
	}
	
	public void run(TaskMonitor monitor) {
		monitor.setTitle("Loading STRING network from PubMed query");
		Map<String, String> args = new HashMap<>();
		args.put("db", "pubmed");
		args.put("retmode","json");
		args.put("retmax","40000");
		args.put("term",query);
		monitor.setTitle("Querying PubMed");
		JSONObject object = null;
		
		try {
			object = HttpUtils.getJSON("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi",
		                                      args, manager);
		} catch(ConnectionException e) {
			this.errorMsg = e.getMessage();
			monitor.showMessage(TaskMonitor.Level.ERROR, this.errorMsg);
			return;
		} catch (Exception e) {
			// Probably a timeout, but it shouldn't happen
			return;
		}
		
		if (object == null) {
			this.errorMsg = "Error trying to fetch results.";
			monitor.showMessage(TaskMonitor.Level.ERROR, this.errorMsg);
			return;
		}
		
		JSONObject result = ModelUtils.getResultsFromJSON(object, JSONObject.class);
		if (result == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"Pubmed returned no results");
			// System.out.println("object wrong type: "+object.toString());
			return;
		}
		JSONObject json = (JSONObject)result.get("esearchresult");
		if (json == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"Pubmed returned no results");
			// System.out.println("object doesn't contain esearchresult: "+object.toString());
			return;
		}

		// Get the total number of results
		int count = Integer.parseInt((String)json.get("count"));
		if (count == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"Pubmed returned no results");
			// System.out.println("object doesn't contain count: "+json.toString());
			return;
		}

		JSONArray ids = (JSONArray)json.get("idlist");

		monitor.showMessage(TaskMonitor.Level.INFO,"Pubmed returned "+count+" results, of which we downloaded "+ids.size());

		StringBuilder sb = new StringBuilder();
		for (Object id: ids) {
			sb.append(id.toString()+" ");
		}

		args.clear();
		args.put("documents", sb.toString());
		args.put("format", "json");
		args.put("limit", Integer.toString(limit));
		args.put("type2", Integer.toString(species.getTaxId()));
		monitor.setTitle("Querying STRING");
		JSONObject tmobject = null;
		try {
			tmobject = HttpUtils.postJSON(manager.getTextMiningURL(), args, manager);
		} catch(ConnectionException e) {
			monitor.showMessage(Level.ERROR, e.getMessage());
			return;
		}
		if (tmobject == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}

		tmResults = ModelUtils.getIdsFromJSON(manager, species.getTaxId(), tmobject, query, false);

		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = new ArrayList<>();
		for (TextMiningResult tm: tmResults) {
			stringIds.add(tm.getID());
		}

		// OK, if we got any results, fetch the network
		String netName = query;
		if (query.length() > 18)
			netName = query.substring(0, 15)+"...";
		LoadInteractions liTask = new LoadInteractions(stringNetwork, species.getName(), species.getTaxId(), 
			                                             confidence, 0, stringIds, queryTermMap, netName, 
			                                             Databases.STRING.getAPIName(), netType);
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
