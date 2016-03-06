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
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.TextMiningResult;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetStringIDsFromPubmedTask extends AbstractTask implements ObservableTask {
	final StringNetwork stringNetwork;
	final StringManager manager;
	final int taxon;
	final int limit;
	final String query;
	private List<TextMiningResult> tm_results;

	public GetStringIDsFromPubmedTask(final StringNetwork stringNetwork, final int taxon, final int limit, final String query) {
		this.stringNetwork = stringNetwork;
		manager = stringNetwork.getManager();
		this.taxon = taxon;
		this.limit = limit;
		this.query = query;
	}
	public void run(TaskMonitor monitor) {
		monitor.setTitle("Querying pubmed");
		Map<String, String> args = new HashMap<>();
		args.put("db", "pubmed");
		args.put("retmode","json");
		args.put("retmax","10000");
		args.put("term","\""+query+"\"");
		Object object = HttpUtils.getJSON("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi",
		                                  args, manager);
		if (!(object instanceof JSONObject)) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"Pubmed returned no results");
			System.out.println("object wrong type: "+object.toString());
			return;
		}
		JSONObject json = (JSONObject)((JSONObject) object).get("esearchresult");
		if (json == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"Pubmed returned no results");
			System.out.println("object doesn't contain esearchresult: "+object.toString());
			return;
		}

		// Get the total number of results
		int count = Integer.parseInt((String)json.get("count"));
		if (count == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"Pubmed returned no results");
			System.out.println("object doesn't contain count: "+json.toString());
			return;
		}
		monitor.showMessage(TaskMonitor.Level.INFO,"Pubmed returned "+count+" results");

		JSONArray ids = (JSONArray)json.get("idlist");
		StringBuilder sb = new StringBuilder();
		for (Object id: ids) {
			sb.append(id.toString()+" ");
		}

		args.clear();
		args.put("documents", sb.toString());
		args.put("format", "json");
		args.put("limit", Integer.toString(limit));
		args.put("type2", Integer.toString(taxon));
		monitor.setTitle("Querying STRING");
		Object tmobject = HttpUtils.postJSON(manager.getTextMiningURL(), args, manager);
		if (tmobject == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}

		tm_results = ModelUtils.getIdsFromJSON(manager, taxon, tmobject, query);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Find proteins from text mining";
	}

	public List<TextMiningResult> getTextMiningResults() {
		return tm_results;
	}

	public <R> R getResults(Class<? extends R> type) {
		return null;
	}
}
