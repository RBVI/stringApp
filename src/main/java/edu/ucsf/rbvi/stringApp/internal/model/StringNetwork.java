package edu.ucsf.rbvi.stringApp.internal.model;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class StringNetwork {
	final StringManager manager;
	CyNetwork network;
	Map<String, List<String>> resolvedIdMap = null;
	Map<String, List<Annotation>> annotations = null;

	public StringNetwork(StringManager manager) {
		this.manager = manager;
		resolvedIdMap = null;
		annotations = null;
	}

	public void reset() {
		resolvedIdMap = null;
		annotations = null;
	}

	public StringManager getManager() { return manager; }

	public CyNetwork getNetwork() { return network; }

	public void setNetwork(CyNetwork network) {
		this.network = network;
	}

	public Map<String, List<Annotation>> getAnnotations() { return annotations; }

	public Map<String, List<Annotation>> getAnnotations(int taxon, final String terms, final String useDATABASE) {
		String encTerms = terms.trim();
		// try {
		// encTerms = URLEncoder.encode(terms.trim(), "UTF-8");
		// } catch (Exception e) {
		// return new HashMap<String, List<Annotation>>();
		// }

		// always call the string API first to resolve all potential protein IDs
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/resolveList";
		Map<String, String> args = new HashMap<>();
		args.put("species", Integer.toString(taxon));			
		args.put("identifiers", encTerms);
		args.put("caller_identity", StringManager.CallerIdentity);
		manager.info("URL: "+url+"?species="+Integer.toString(taxon)+"&caller_identity="+StringManager.CallerIdentity+"&identifiers="+encTerms);
		// Get the results
		JSONObject results = HttpUtils.postJSON(url, args, manager);

		if (results != null) {
			// System.out.println("Got results");
			annotations = Annotation.getAnnotations(results, terms);
			// System.out.println("Get annotations returns "+annotations.size());
		}
		results = null;
		
		// then, call other APIs to get resolve them
		// resolve compounds 
		if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
			url = manager.getResolveURL(Databases.STITCH.getAPIName())+"json/resolveList";
			args = new HashMap<>();
			args.put("species", "CIDm");
			args.put("identifiers", encTerms);
			args.put("caller_identity", StringManager.CallerIdentity);
			manager.info("URL: "+url+"?species="+Integer.toString(taxon)+"&caller_identity="+StringManager.CallerIdentity+"&identifiers="+encTerms);
			// Get the results
			results = HttpUtils.postJSON(url, args, manager);

			if (results != null) {
				updateAnnotations(results, terms);
			}
			results = null;
		} 
		
		// also call the viruses API
		if (manager.isVirusesEnabled()) {
			// http://viruses.string-db.org/cgi/webservice_handler.pl?species=11320&identifiers=NS1_I34A1
			// &caller_identity=string_app_v1_1_1&output=json&request=resolveList
			url = manager.getResolveURL(Databases.VIRUSES.getAPIName());
			args = new HashMap<>();
			args.put("species", Integer.toString(taxon));
			args.put("identifiers", encTerms);
			args.put("caller_identity", StringManager.CallerIdentity);
			args.put("output", "json");
			args.put("request", "resolveList");
			manager.info("URL:" + url + "?species=" + Integer.toString(taxon) + "&caller_identity="
					+ StringManager.CallerIdentity + "&identifiers=" + encTerms);
			// Get the results
			results = HttpUtils.postJSON(url, args, manager);

			if (results != null) {
				updateAnnotations(results, terms);
			}
			results = null;
		 }
		
		return annotations;
	}

	
	/*
	 * Maintenance of the resolveIdMap
	 */
	public boolean resolveAnnotations() {
		if (resolvedIdMap == null) resolvedIdMap = new HashMap<>();
		boolean noAmbiguity = true;
		for (String key: annotations.keySet()) {
			if (annotations.get(key).size() > 1) {
				noAmbiguity = false;
				break;
			} else {
				List<String> ids = new ArrayList<String>();
				ids.add (annotations.get(key).get(0).getStringId());
				resolvedIdMap.put(key, ids);
			}
		}

		// Now trim the key set
		if (resolvedIdMap.size() > 0) {
			for (String key: resolvedIdMap.keySet()) {
				if (annotations.containsKey(key))
					annotations.remove(key);
			}
		}
		return noAmbiguity;
	}

	public void addResolvedStringID(String term, String id) {
		if (!resolvedIdMap.containsKey(term))
			resolvedIdMap.put(term, new ArrayList<String>());
		resolvedIdMap.get(term).add(id);
	}

	public void removeResolvedStringID(String term, String id) {
		if (!resolvedIdMap.containsKey(term))
			return;
		List<String> ids = resolvedIdMap.get(term);
		ids.remove(id);
		if (ids.size() == 0) 
			resolvedIdMap.remove(term);
	}

	public boolean haveResolvedNames() {
		// allows users to not resolve some of the proteins but still needs at least one protein as input
		if (resolvedIdMap == null || resolvedIdMap.size() > 0)
			return true;
		return false;
	}

	public int getResolvedTerms() { 
		int i = 0;
		for (List<String> terms: resolvedIdMap.values())
			i += terms.size();
		return i; 
	}

	public List<String> combineIds(Map<String, String> reverseMap) {
		List<String> ids = new ArrayList<>();
		for (String term: resolvedIdMap.keySet()) {
			for (String id: resolvedIdMap.get(term)) {
				ids.add(id);
				reverseMap.put(id, term);
			}
		}
		return ids;
	}
	
	private void updateAnnotations(JSONObject results, String terms) {
		Map<String, List<Annotation>> newAnnotations = Annotation.getAnnotations(results,
				terms);
		for (String newAnn : newAnnotations.keySet()) {
			List<Annotation> allAnn = new ArrayList<Annotation>(newAnnotations.get(newAnn));
			if (annotations.containsKey(newAnn)) {
				allAnn.addAll(annotations.get(newAnn));
			}
			annotations.put(newAnn, allAnn);
		}
	}
	
}
