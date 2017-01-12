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
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class FindProteinsTask extends AbstractTask {
	final StringManager manager;

	@Tunable (description="Enter pubmed query")
	public String query;

	@Tunable (description="Species")
	public ListSingleSelection<Species> species;

	@Tunable (description="Taxon ID", context="nogui")
	public int taxonID = -1;

	@Tunable (description="Number of proteins")
	public BoundedInteger limit = new BoundedInteger(10, 100, 10000, false, false);

	public FindProteinsTask(final StringManager manager) {
		this.manager = manager;
		species = new ListSingleSelection<>(Species.getSpecies());
		// Set Human as the default
		for (Species s: Species.getSpecies()) {
			if (s.toString().equals("Homo sapiens")) {
				species.setSelectedValue(s);
				break;
			}
		}
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Querying pubmed");
		Map<String, String> args = new HashMap<>();
		args.put("db", "pubmed");
		args.put("retmode","json");
		args.put("retmax","10000");
		args.put("term","\""+query+"\"");
		JSONObject object = HttpUtils.getJSON("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi",
		                                  args, manager);
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
		monitor.showMessage(TaskMonitor.Level.INFO,"Pubmed returned "+count+" results");

		JSONArray ids = (JSONArray)json.get("idlist");
		StringBuilder sb = new StringBuilder();
		for (Object id: ids) {
			sb.append(id.toString()+" ");
		}

		args.clear();
		args.put("documents", sb.toString());
		args.put("format", "json");
		args.put("limit", limit.getValue().toString());
		if (taxonID != -1)
			args.put("type2", Integer.toString(taxonID));
		else
			args.put("type2", Integer.toString(species.getSelectedValue().getTaxId()));
		monitor.setTitle("Querying STRING");
		JSONObject tmobject = HttpUtils.postJSON(manager.getTextMiningURL(), args, manager);
		if (tmobject == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}

		ModelUtils.createTMNetworkFromJSON(manager, species.getSelectedValue(), tmobject, query, Databases.STRING.getAPIName());
	}

	@ProvidesTitle
	public String getTitle() {
		return "Find proteins from text mining";
	}
}
