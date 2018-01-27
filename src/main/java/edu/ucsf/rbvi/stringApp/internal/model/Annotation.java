package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class Annotation {
	String annotation;
	int taxId;
	String stringId;
	String query;
	String preferredName;

	public Annotation(String preferredName, String stringId, int taxId, String query, String annotation) {
		this.preferredName = preferredName;
		this.stringId = stringId;
		this.taxId = taxId;
		this.query = query;
		this.annotation = annotation;
	}

	public String getPreferredName() { return preferredName; }
	public int getTaxId() { return taxId; }
	public String getQueryString() { return query; }
	public String getStringId() { return stringId; }
	public String getAnnotation() { return annotation; }
	public String toString() {
		String res = "   Query: "+query+"\n";
		res += "   PreferredName: "+preferredName+"\n";
		res += "   Annotation: "+annotation;
		return res;
	}

	public static Map<String, List<Annotation>> getAnnotations(JSONObject json, String queryTerms) {
		Map<String, List<Annotation>> map = new HashMap<>();
		return getAnnotations(json, queryTerms, map);
	}

	public static Map<String, List<Annotation>> getAnnotations(JSONObject json, String queryTerms,
	                                                           Map<String, List<Annotation>> map) {
		String[] terms = queryTerms.trim().split("\n");
		JSONArray annotationArray = ModelUtils.getResultsFromJSON(json, JSONArray.class);
		Integer version = ModelUtils.getVersionFromJSON(json);

		// If we switch the API back to use a start of 0, this will need to change
		int queryIndexStart = 0;
		if (version != null && version == 1)
			queryIndexStart = -1;

		for (Object annObject: annotationArray) {
			JSONObject ann = (JSONObject)annObject;
			String annotation = null;
			String stringId = null;
			String preferredName = null;
			int taxId = -1;
			int queryIndex = -1;

			if (ann.containsKey("preferredName"))
				preferredName = (String)ann.get("preferredName");
			if (ann.containsKey("annotation"))
				annotation = (String)ann.get("annotation");
			if (ann.containsKey("stringId"))
				stringId = (String)ann.get("stringId");
			if (ann.containsKey("ncbiTaxonId"))
				taxId = ((Long)ann.get("ncbiTaxonId")).intValue();
			if (ann.containsKey("queryIndex")) {
				Object index = ann.get("queryIndex");
				if (index instanceof Long) {
					queryIndex = ((Long)index).intValue();
				} else {
					queryIndex = Integer.parseInt((String)index);
				}

				queryIndex = queryIndex - queryIndexStart;
			}

			// Temporary HACK
			// if (stringId.startsWith("-1.CID1"))
			// 	stringId = stringId.replaceFirst("-1.CID1","CIDm");

			Annotation newAnnotation = new Annotation(preferredName, stringId, taxId, terms[queryIndex], annotation);
			if (!map.containsKey(terms[queryIndex])) {
				map.put(terms[queryIndex], new ArrayList<Annotation>());
			}

			// Now, look for direct matches
			List<Annotation> annList = map.get(terms[queryIndex]);
			if (annList.size() > 0 && annList.get(0).getPreferredName().equalsIgnoreCase(terms[queryIndex])) {
				continue;
			} else if (preferredName.equalsIgnoreCase(terms[queryIndex])) {
				map.put(terms[queryIndex], Collections.singletonList(newAnnotation));
			} else {
				map.get(terms[queryIndex]).add(newAnnotation);
			}

		}
		return map;
	}
}
