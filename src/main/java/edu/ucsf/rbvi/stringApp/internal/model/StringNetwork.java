package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.util.color.Palette;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

// import org.jcolorbrewer.ColorBrewer;

public class StringNetwork {
	final StringManager manager;
	CyNetwork network;
	Map<String, List<String>> resolvedIdMap = null;
	Map<String, List<Annotation>> annotations = null;
	// Map<String, String> settings = null;
	Map<String, Map<String, String>> settingsGroups = null;

	// Default enrichment table options for this network
	private int topTerms = -1;
	private double overlapCutoff = -1;
	private Palette brewerPalette = null;
	private List<TermCategory> categoryFilter = null;
	private ChartType chartType = null;
	private boolean removeOverlap = false;

	// Enrichment table options for groups of this network
	private HashMap<String, Integer> topTermsGroups = new HashMap<>();
	private HashMap<String, Double> overlapCutoffGroups = new HashMap<>();
	private HashMap<String, Palette> brewerPaletteGroups = new HashMap<>();
	private HashMap<String, List<TermCategory>> categoryFilterGroups = new HashMap<>();
	private HashMap<String, ChartType> chartTypeGroups = new HashMap<>();
	private HashMap<String, Boolean> removeOverlapGroups = new HashMap<>();

	public StringNetwork(StringManager manager) {
		this.manager = manager;
		resolvedIdMap = null;
		annotations = null;
		// TODO: [N] Is it ok to set the group to null here?
		topTerms = manager.getTopTerms(null, null);
		overlapCutoff = manager.getOverlapCutoff(null, null);
		brewerPalette = manager.getEnrichmentPalette(null, null);
		categoryFilter = manager.getCategoryFilter(null, null);
		removeOverlap = manager.getRemoveOverlap(null, null);
		chartType = manager.getChartType(null, null);
		// settings = new HashMap<>();
		settingsGroups = new HashMap<>();
	}

	public void reset() {
		resolvedIdMap = null;
		annotations = null;
	}

	public StringManager getManager() { return manager; }

	public CyNetwork getNetwork() { return network; }

	public void setNetwork(CyNetwork network) {
		this.network = network;
		
		// get group names from network table and set up group settings for each group
		CyTable netTable = network.getDefaultNetworkTable();
		ModelUtils.createListColumnIfNeeded(netTable, String.class, ModelUtils.NET_ENRICHMENT_TABLES);
		List<String> groups = netTable.getRow(network.getSUID()).getList(ModelUtils.NET_ENRICHMENT_TABLES, String.class);
		// TODO: [N] Test handling of old sessions here, possibly revise later on
		// TODO: [N] do we need to delete old columns, e.g. anlayzedNodes and vsiualizedTerms and enrichmentSettings..?
		if (groups == null) {
			// this means there is no enrichment/publications done for this network
			if (netTable.getColumn(ModelUtils.NET_ANALYZED_NODES) == null)
				return;
			List<Long> analyzedNodes = netTable.getRow(network.getSUID()).getList(ModelUtils.NET_ANALYZED_NODES, Long.class);
			// otherwise we have an old session with settings for the Enrichment: All
			groups = new ArrayList<String>();
			CyTableManager tableManager = manager.getService(CyTableManager.class);
			Set<CyTable> currTables = tableManager.getAllTables(true);
			for (CyTable current : currTables) {
				// ignore if not one of our enrichment tables
				if (!current.getTitle().startsWith(EnrichmentTerm.ENRICHMENT_TABLE_PREFIX) 
						|| current.getColumn(EnrichmentTerm.colNetworkSUID) == null
						|| current.getAllRows().size() == 0) {
					continue;
				}
				// ignore if not associated with this network 
				CyRow tempRow = current.getAllRows().get(0);
				if (tempRow.get(EnrichmentTerm.colNetworkSUID, Long.class) == null || !tempRow
						.get(EnrichmentTerm.colNetworkSUID, Long.class).equals(network.getSUID())) {
					continue;
				}				
				// do things differently depending on if publication or enrichemnt table
				if (current.getTitle().equals(TermCategory.ALL.getTable())) {
					String defaultGroup = TermCategory.ALL.getTable();
					// add table name to the groups
					groups.add(defaultGroup);
					netTable.getRow(network.getSUID()).set(ModelUtils.NET_ENRICHMENT_TABLES, groups);
					// Create settings table and add it to this network's attributes 
					ModelUtils.createColumnIfNeeded(netTable, Long.class, ModelUtils.NET_ENRICHMENT_SETTINGS_TABLE_SUID);
					CyTable settignsTable = ModelUtils.getEnrichmentSettingsTable(manager, network);
					netTable.getRow(network.getSUID()).set(ModelUtils.NET_ENRICHMENT_SETTINGS_TABLE_SUID, settignsTable.getSUID());
					// Copy analyzed nodes and enrichment settings to new settings table for this network
					// ModelUtils.createListColumnIfNeeded(settignsTable, Long.class, ModelUtils.NET_ANALYZED_NODES);
					settignsTable.getRow(defaultGroup).set(ModelUtils.NET_ANALYZED_NODES, analyzedNodes);
					String enrichmentSettings = network.getRow(network).get(ModelUtils.NET_ENRICHMENT_SETTINGS, String.class);
					if (enrichmentSettings != null) {
						// ModelUtils.createListColumnIfNeeded(settignsTable, String.class, ModelUtils.NET_ENRICHMENT_SETTINGS);
						settignsTable.getRow(defaultGroup).set(ModelUtils.NET_ENRICHMENT_SETTINGS, enrichmentSettings);
					}					
				} else if (current.getTitle().equals(TermCategory.PMID.getTable())) {
					ModelUtils.createListColumnIfNeeded(netTable, Long.class, ModelUtils.NET_ANALYZED_NODES_PUBL);
					netTable.getRow(network.getSUID()).set(ModelUtils.NET_ANALYZED_NODES_PUBL, analyzedNodes);
				}
			}
		}
		for (String group : groups) {
			// Load our options
			Map<String, String> settings = ModelUtils.getEnrichmentSettingsTableGroup(manager, network, group);
			if (settings.size() == 0) {
				System.out.println("found no settings for " + group + ".");
			} else {
				System.out.println("found settings for " + group);
				settingsGroups.put(group, settings);
				if (settings.containsKey("overlapCutoff")) {
					overlapCutoff = Double.valueOf(settings.get("overlapCutoff"));
					System.out.println(overlapCutoff);
				}
				overlapCutoffGroups.put(group, overlapCutoff);
				if (settings.containsKey("topTerms")) {
					topTerms = Integer.valueOf(settings.get("topTerms"));
					System.out.println(topTerms);
				}
				topTermsGroups.put(group, topTerms);
				if (settings.containsKey("removeOverlap")) {
					removeOverlap = Boolean.valueOf(settings.get("removeOverlap"));
					System.out.println(removeOverlap);
				}
				removeOverlapGroups.put(group, removeOverlap);
				if (settings.containsKey("categoryFilter")) {
					List<String> strFilters = ModelUtils.stringToList(settings.get("categoryFilter"));
					categoryFilter = new ArrayList<>();
					for (String filter: strFilters) {
						categoryFilter.add(Enum.valueOf(TermCategory.class, filter));
					}
					System.out.println(categoryFilter);
				}
				categoryFilterGroups.put(group, categoryFilter);
				if (settings.containsKey("chartType")) {
					chartType = Enum.valueOf(ChartType.class, settings.get("chartType"));
					System.out.println(chartType);
				}
				chartTypeGroups.put(group, chartType);
		
				// FIXME
				/*
				if (settings.containsKey("brewerPalette")) {
					brewerPalette = Enum.valueOf(ColorBrewer.class, settings.get("brewerPalette"));
				}
				*/
				// TODO: why do we set this one here and none of the others?
				if (settings.containsKey("brewerPalette")) {
					System.out.println(settings.get("brewerPalette"));
					manager.setBrewerPalette(network, settings.get("brewerPalette"), group);
				}
				// brewerPaletteGroups.put(defaultGroup, brewerPalette);
			}
		}
	}

	//public double getOverlapCutoff() { return overlapCutoff; }
	//public void setOverlapCutoff(double cutoff) { overlapCutoff = cutoff; update(); }

	//public int getTopTerms() { return topTerms; }
	//public void setTopTerms(int tt) { topTerms = tt; update(); }

	//public List<TermCategory> getCategoryFilter() { return categoryFilter; }
	//public void setCategoryFilter(List<TermCategory> categories) { categoryFilter = categories; update(); }

	//public Palette getEnrichmentPalette() { return brewerPalette; }
	//public void setEnrichmentPalette(Palette palette) { brewerPalette = palette; update(); }

	//public ChartType getChartType() { return chartType; }
	//public void setChartType(ChartType type) { chartType = type; update(); }

	//public boolean getRemoveOverlap() { return removeOverlap; }
	//public void setRemoveOverlap(boolean remove) { removeOverlap = remove; update(); }


	// Update our settings in the network table
//	private void update() {
//		settings.put("overlapCutoff", Double.toString(overlapCutoff));
//		settings.put("topTerms", Integer.toString(topTerms));
//		settings.put("removeOverlap", Boolean.toString(removeOverlap));
//		{
//			List<String> filters = new ArrayList<>();
//			for (TermCategory cat: categoryFilter) {
//				filters.add(cat.name());
//			}
//			settings.put("categoryFilter", ModelUtils.listToString(filters));
//		}
//		settings.put("brewerPalette", brewerPalette.toString());
//		settings.put("chartType", chartType.name());
//		ModelUtils.updateEnrichmentSettings(network, settings);
//
//	}

	public double getOverlapCutoff(String group) { 
		if (overlapCutoffGroups.containsKey(group)) 
			return overlapCutoffGroups.get(group).doubleValue();
		else 
			return overlapCutoff;
	}
	public void setOverlapCutoff(String group, double cutoff) { 
		overlapCutoffGroups.put(group, new Double(cutoff)); 
		updateGroup(group); 
	}

	public int getTopTerms(String group) { 
		if (topTermsGroups.containsKey(group))
			return topTermsGroups.get(group).intValue();
		else 
			return topTerms; 
	}
	public void setTopTerms(String group, int tt) { 
		topTermsGroups.put(group, new Integer(tt));
		updateGroup(group); 
	}

	public List<TermCategory> getCategoryFilter(String group) {
		if (categoryFilterGroups.containsKey(group))
			return categoryFilterGroups.get(group);
		else 
			return categoryFilter; 
	}
	public void setCategoryFilter(String group, List<TermCategory> categories) {
		categoryFilterGroups.put(group, categories);
		updateGroup(group); 
	}

	public Palette getEnrichmentPalette(String group) { 
		if (brewerPaletteGroups.containsKey(group))
			return brewerPaletteGroups.get(group);
		else
			return brewerPalette; 
	}
	public void setEnrichmentPalette(String group, Palette palette) { 
		brewerPaletteGroups.put(group, palette); 
		updateGroup(group); 
	}

	public ChartType getChartType(String group) {
		if (chartTypeGroups.containsKey(group))
			return chartTypeGroups.get(group);
		else
			return chartType; 
	}
	public void setChartType(String group, ChartType type) { 
		chartTypeGroups.put(group, type); 
		updateGroup(group); 
	}

	public boolean getRemoveOverlap(String group) { 
		if (removeOverlapGroups.containsKey(group)) 
			return removeOverlapGroups.get(group).booleanValue(); 
		else 
			return removeOverlap; 
	}
	public void setRemoveOverlap(String group, boolean remove) { 
		removeOverlapGroups.put(group, new Boolean(remove)); 
		updateGroup(group); 
	}


	// Update our settings in the network table
	private void updateGroup(String group) {
		System.out.println("update settings for " + group);
		Map<String, String> groupSettings = new HashMap<String, String>();
		if (overlapCutoffGroups.containsKey(group))
			groupSettings.put("overlapCutoff", Double.toString(overlapCutoffGroups.get(group)));
		else 
			groupSettings.put("overlapCutoff", Double.toString(overlapCutoff));
		
		if (topTermsGroups.containsKey(group))
			groupSettings.put("topTerms", Integer.toString(topTermsGroups.get(group)));
		else 
			groupSettings.put("topTerms", Integer.toString(topTerms));
		
		if (removeOverlapGroups.containsKey(group))
			groupSettings.put("removeOverlap", Boolean.toString(removeOverlapGroups.get(group)));
		else 
			groupSettings.put("removeOverlap", Boolean.toString(removeOverlap));
		
		{
			List<TermCategory> catFilter = categoryFilter;
			if (categoryFilterGroups.containsKey(group))
				catFilter = categoryFilterGroups.get(group);
			List<String> filters = new ArrayList<>();
			for (TermCategory cat: catFilter) {
				filters.add(cat.name());
			}
			groupSettings.put("categoryFilter", ModelUtils.listToString(filters));
		}
		
		if (brewerPaletteGroups.containsKey(group))
			groupSettings.put("brewerPalette", brewerPaletteGroups.get(group).toString());
		else 
			groupSettings.put("brewerPalette", brewerPalette.toString());
		
		if (chartTypeGroups.containsKey(group))
			groupSettings.put("chartType", chartTypeGroups.get(group).name());
		else
			groupSettings.put("chartType", chartType.name());
		
		settingsGroups.put(group, groupSettings);
		ModelUtils.updateEnrichmentSettingsTableGroup(manager, network, group, groupSettings);
	}

	public Map<String, List<Annotation>> getAnnotations() { return annotations; }

	public Map<String, List<Annotation>> getAnnotations(StringManager manager, int taxon, final String terms, 
	                                                    final String useDATABASE, boolean includeViruses) throws ConnectionException {
		String encTerms = terms.trim();
		// try {
		// encTerms = URLEncoder.encode(terms.trim(), "UTF-8");
		// } catch (Exception e) {
		// return new HashMap<String, List<Annotation>>();
		// }

		// make list of terms unique
		Set<String> termsSet = new HashSet<String>(Arrays.asList(encTerms.split("\n")));
		Object[] termsArray = termsSet.toArray();
		
		// Split the terms up into groups of 5000
		annotations = new HashMap<>();
		for (int i = 0; i < termsArray.length; i = i + 5000) {
			String termsBatch = getTerms(termsArray, i, i + 5000, termsArray.length);
			annotations = getAnnotationBatch(taxon, termsBatch, useDATABASE, includeViruses);
		}
		// check which identifiers could not be mapped
		termsSet.removeAll(annotations.keySet());
		manager.info("List of nodes that could not be mapped to any STRING identifier: " + termsSet);
		return annotations;
	}

	private Map<String, List<Annotation>> getAnnotationBatch(int taxon, final String encTerms, 
	                                                         String useDATABASE, boolean includeViruses) throws ConnectionException {
		// always call the string API first to resolve all potential protein IDs
		// new API 
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/get_string_ids";
		// String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/resolveList";
		Map<String, String> args = new HashMap<>();
		args.put("species", Integer.toString(taxon));			
		args.put("identifiers", encTerms);
		// args.put("limit", "");
		args.put("caller_identity", StringManager.CallerIdentity);
		manager.info("URL: "+url+"?species="+Integer.toString(taxon)+"&caller_identity="+StringManager.CallerIdentity+"&identifiers="+encTerms);
		// Get the results
		// System.out.println("Getting STRING term resolution");
		JSONObject results = HttpUtils.postJSON(url, args, manager);
		// System.out.println("Results: "+results);

		if (results != null) {
			// System.out.println("Got results");
			annotations = Annotation.getAnnotations(results, encTerms, annotations);
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
			manager.info("URL: "+url+"?species="+Integer.toString(taxon)+"&caller_identity="+StringManager.CallerIdentity+"&identifiers="+HttpUtils.truncate(encTerms));
			// Get the results
			// System.out.println("Getting STITCH term resolution");
			results = HttpUtils.postJSON(url, args, manager);

			if (results != null) {
				updateAnnotations(results, encTerms);
			}
			results = null;
		} 
		
		// also call the viruses API
		if (manager.isVirusesEnabled() && annotations.size() == 0 && includeViruses) {
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
					+ StringManager.CallerIdentity + "&identifiers=" + HttpUtils.truncate(encTerms));
			// Get the results
			// System.out.println("Getting VIRUSES term resolution");
			results = HttpUtils.postJSON(url, args, manager);

			if (results != null) {
				updateAnnotations(results, encTerms);
			}
			results = null;
		 }
		
		return annotations;
	}

	private String getTerms(Object[] termsArray, int start, int end, int length) {
		if (length == 1) return (String)termsArray[0];
		if (end > length) end = length;
		StringBuilder terms = null;
		for (int i = start; i < (end); i++) {
			if (terms == null) {
				terms = new StringBuilder();
				terms.append(termsArray[i]);
			} else {
				terms.append("\n"+termsArray[i]);
			}
		}
		return terms.toString();
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
