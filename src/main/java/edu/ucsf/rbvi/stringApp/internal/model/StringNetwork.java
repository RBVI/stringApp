package edu.ucsf.rbvi.stringApp.internal.model;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public Map<String, List<Annotation>> getAnnotations(int taxon, final String terms, final boolean useSTITCH) {
		String encTerms;
		try {
			encTerms = URLEncoder.encode(terms.trim(), "UTF-8");
		} catch (Exception e) {
			return new HashMap<String, List<Annotation>>();
		}

		String url = manager.getResolveURL(useSTITCH)+"json/resolveList";
		Map<String, String> args = new HashMap<>();
		args.put("species", Integer.toString(taxon));
		args.put("identifiers", encTerms);
		args.put("caller_identity", StringManager.CallerIdentity);
		System.out.println("URL: "+url+"?species="+Integer.toString(taxon)+"&caller_identity="+StringManager.CallerIdentity+"&identifiers="+encTerms);
		Object results;
		// Get the results
		//
		/* At one point STITCH didn't work with POST
		if (!useSTITCH) {
			results = HttpUtils.postJSON(url, args, manager);
		} else {
			results = HttpUtils.postJSON(url, args, manager);
		}
		*/
		results = HttpUtils.postJSON(url, args, manager);

		if (results == null) return null;
		// System.out.println("Got results");
		annotations = Annotation.getAnnotations(results, terms);
		// System.out.println("Get annotations returns "+annotations.size());
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
		if (resolvedIdMap == null)
			return true;

		for (String key: annotations.keySet()) {
			if (!resolvedIdMap.containsKey(key) || resolvedIdMap.get(key).size() == 0) {
				return false;
			}
		}
		return true;
	}

	public int getResolvedTerms() { return resolvedIdMap.size(); }

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
}
