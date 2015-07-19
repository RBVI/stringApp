package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ResolveNamesTask extends AbstractTask {
	final StringManager manager;
	final int taxon;
	final int confidence;
	final int additionalNodes;
	final String terms;

	public ResolveNamesTask(StringManager manager, int taxonId, int confidence, 
	                        int additionalNodes, String terms) {
		this.manager = manager;
		this.taxon = taxonId;
		this.confidence = confidence;
		this.additionalNodes = additionalNodes;
		this.terms = terms;
	}

	public void run(TaskMonitor monitor) {
		boolean noAmbiguity = true;

		Map<String,List<Annotation>> annotations = manager.getAnnotations(taxon, terms);

		List<String> stringIds = new ArrayList<>();

		// For each key, if we only have one match, use it, otherwise we need to disambiguate
		for (String key: annotations.keySet()) {
			if (annotations.get(key).size() > 1) {
				noAmbiguity = false;
				break;
			} else {
				stringIds.add(annotations.get(key).get(0).getStringId());
			}
		}

		// Check for the easy matches
		if (noAmbiguity) {
			insertTasksAfterCurrentTask(new LoadInteractions(manager, taxon, confidence, additionalNodes, stringIds));
		} else {
			// insertTasksAfterCurrentTask(new ResolveAmbiguityTask(manager, taxonId, map));
		}
	}

}

