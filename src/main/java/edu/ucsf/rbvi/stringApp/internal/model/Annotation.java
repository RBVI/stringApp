package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class Annotation {
	String annotation;
	int taxId;
	String stringId;
	String query;
	String preferredName;

	// new fields from STRING-DB
	String taxonName;
	String uniprot;
	String sequence;
	//String color;
	String image;
	List<String> structures;
	boolean resolved;
	

	// Additional information for creating nodes from get_string_ids
	String description = null;

	public Annotation(String preferredName, String stringId, int taxId, String query, String annotation) {
		this.preferredName = preferredName;
		this.stringId = stringId;
		this.taxId = taxId;
		this.query = query;
		this.annotation = annotation;
		this.resolved = false;
		
		this.taxonName = "";
		this.uniprot = "";
		this.sequence = "";
		//this.color = "";
		this.image = "";
		this.structures = new ArrayList<>();
	}

	public String getPreferredName() { return preferredName; }
	// TODO: [Custom] figure out if we can save both the name and the ID of custom species
	// public int getTaxId() { return taxId; }
	public String getQueryString() { return query; }
	public String getStringId() { return stringId; }
	public String getAnnotation() { return annotation; }
	public String getDescription() { return description; }
	public String toString() {
		String res = "   Query: "+query+"\n";
		res += "   PreferredName: "+preferredName+"\n";
		res += "   Annotation: "+annotation+"\n";
		res += "   Description: "+description+"\n";
		res += "   Uniprot: "+uniprot+"\n";
		res += "   Sequence: "+sequence+"\n";
		res += "   Image: "+image+"\n";
		return res;
	}

	public String getTaxonName() {return taxonName;}
	public String getUniprot() {return uniprot;}
	public String getSequence() {return sequence;}
	// public String getColor() {return color;}
	public String getImage() {return image;}
	public List<String> getStructures() {return structures;}
	public boolean isResolved() {return resolved;}
	public void setResolved(boolean resolved) {this.resolved = resolved;}
	
	public static Map<String, List<Annotation>> getAnnotations(JSONObject json, String queryTerms, Species species) {
		Map<String, List<Annotation>> map = new HashMap<>();
		return getAnnotations(json, queryTerms, map, species);
	}

	public static boolean allResolved(List<Annotation> annotations) {
		for (Annotation a: annotations) {
			if (!a.isResolved())
				return false;
		}
		return true;
	}

	public static Map<String, List<Annotation>> getAnnotations(JSONObject json, String queryTerms,
	                                                           Map<String, List<Annotation>> map, Species species) {
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
			String description = null;
			String taxonName = null;
			int taxId = -1;
			int queryIndex = -1;
			String uniprot = null;
			String sequence = null;
			String image = null;

			if (ann.containsKey("preferredName"))
				preferredName = (String)ann.get("preferredName");
			if (ann.containsKey("annotation"))
				annotation = (String)ann.get("annotation");
			if (ann.containsKey("stringId"))
				stringId = (String)ann.get("stringId");
			//if (ann.containsKey("ncbiTaxonId"))
			//	taxId = ((Long)ann.get("ncbiTaxonId")).intValue();
			if (ann.containsKey("queryIndex")) {
				Object index = ann.get("queryIndex");
				if (index instanceof Long) {
					queryIndex = ((Long)index).intValue();
				} else {
					queryIndex = Integer.parseInt((String)index);
				}

				queryIndex = queryIndex - queryIndexStart;
			}
			// TODO: [Custom] was there ever a description field or is this the same as annotation? if yes, I will rather change all of them to description to avoid confusion
			if (ann.containsKey("description"))
				description = (String)ann.get("description");

			if (ann.containsKey("taxonName")) {
				taxonName = (String)ann.get("taxonName");
				if (species.isCustom() && species.getOfficialName() == species.getName()) {
					species.setOfficialName(taxonName);
				}
			}
			if (ann.containsKey("uniprot"))
				uniprot = (String)ann.get("uniprot");
			if (ann.containsKey("sequence"))
				sequence = (String)ann.get("sequence");
			if (ann.containsKey("image"))
				image = (String)ann.get("image");
			
			// TODO: [Custom] add parsing of color and structures!
			
			// Temporary HACK
			// if (stringId.startsWith("-1.CID1"))
			// 	stringId = stringId.replaceFirst("-1.CID1","CIDm");

			Annotation newAnnotation = new Annotation(preferredName, stringId, taxId, terms[queryIndex], annotation);

			// Node information
			if (description != null)
				newAnnotation.description = description;
			if (taxonName != null)
				newAnnotation.taxonName = taxonName;
			if (uniprot != null)
				newAnnotation.uniprot = uniprot;
			if (sequence != null)
				newAnnotation.sequence = sequence;
			if (image != null)
				newAnnotation.image = image;
			
			// TODO: [Custom] save color and structures in the annotation
			
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
