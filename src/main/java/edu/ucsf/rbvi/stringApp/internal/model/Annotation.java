package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

	public static Map<String, List<Annotation>> getAnnotations(Object json, String queryTerms) {
		String[] terms = queryTerms.trim().split("\n");
		if ((json == null) || !(json instanceof JSONArray)) {
			return null;
		}

		Map<String, List<Annotation>> map = new HashMap<>();

		for (Object annObject: (JSONArray) json) {
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
			if (ann.containsKey("queryIndex"))
				queryIndex = ((Long)ann.get("queryIndex")).intValue() + 1; // String queryIndex starts at -1!

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
