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
import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils.UnknownSpeciesException;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.EnrichmentUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;


public class StringNetwork {
	final StringManager manager;
	CyNetwork network;
	Map<String, List<String>> resolvedIdMap = null;
	Map<String, List<Annotation>> annotations = null;
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
		ModelUtils.createListColumnIfNeeded(netTable, String.class, ColumnNames.NET_ENRICHMENT_TABLES);
		List<String> groups = netTable.getRow(network.getSUID()).getList(ColumnNames.NET_ENRICHMENT_TABLES, String.class);
		// TODO: [N] Test handling of old sessions here, possibly revise later on
		// TODO: [N] do we need to delete old columns, e.g. anlayzedNodes and vsiualizedTerms and enrichmentSettings..?
		if (groups == null) {
			// this means there is no enrichment/publications done for this network
			if (netTable.getColumn(ColumnNames.NET_ANALYZED_NODES) == null)
				return;
			List<Long> analyzedNodes = netTable.getRow(network.getSUID()).getList(ColumnNames.NET_ANALYZED_NODES, Long.class);
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
					netTable.getRow(network.getSUID()).set(ColumnNames.NET_ENRICHMENT_TABLES, groups);
					// Create settings table and add it to this network's attributes 
					ModelUtils.createColumnIfNeeded(netTable, Long.class, ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE_SUID);
					CyTable settignsTable = EnrichmentUtils.getEnrichmentSettingsTable(manager, network);
					netTable.getRow(network.getSUID()).set(ColumnNames.NET_ENRICHMENT_SETTINGS_TABLE_SUID, settignsTable.getSUID());
					// Copy analyzed nodes and enrichment settings to new settings table for this network
					settignsTable.getRow(defaultGroup).set(ColumnNames.NET_ANALYZED_NODES, analyzedNodes);
					String enrichmentSettings = network.getRow(network).get(ColumnNames.NET_ENRICHMENT_SETTINGS, String.class);
					if (enrichmentSettings != null) {
						settignsTable.getRow(defaultGroup).set(ColumnNames.NET_ENRICHMENT_SETTINGS, enrichmentSettings);
					}					
				} else if (current.getTitle().equals(TermCategory.PMID.getTable())) {
					ModelUtils.createListColumnIfNeeded(netTable, Long.class, ColumnNames.NET_ANALYZED_NODES_PUBL);
					netTable.getRow(network.getSUID()).set(ColumnNames.NET_ANALYZED_NODES_PUBL, analyzedNodes);
				}
			}
		}
		for (String group : groups) {
			if (group.equals("")) {
				continue; 
			}
			// Load our options
			Map<String, String> settings = EnrichmentUtils.getEnrichmentSettingsTableGroup(manager, network, group);
			if (settings.size() > 0) {
				settingsGroups.put(group, settings);
				if (settings.containsKey("overlapCutoff")) {
					overlapCutoff = Double.valueOf(settings.get("overlapCutoff"));
				}
				overlapCutoffGroups.put(group, overlapCutoff);
				if (settings.containsKey("topTerms")) {
					topTerms = Integer.valueOf(settings.get("topTerms"));
				}
				topTermsGroups.put(group, topTerms);
				if (settings.containsKey("removeOverlap")) {
					removeOverlap = Boolean.valueOf(settings.get("removeOverlap"));
				}
				removeOverlapGroups.put(group, removeOverlap);
				if (settings.containsKey("categoryFilter")) {
					List<String> strFilters = ModelUtils.stringToList(settings.get("categoryFilter"));
					categoryFilter = new ArrayList<>();
					for (String filter: strFilters) {
						categoryFilter.add(Enum.valueOf(TermCategory.class, filter));
					}
				}
				categoryFilterGroups.put(group, categoryFilter);
				if (settings.containsKey("chartType")) {
					chartType = Enum.valueOf(ChartType.class, settings.get("chartType"));
				}
				chartTypeGroups.put(group, chartType);
		
				// FIXME
				// TODO: why do we set this one here and none of the others?
				if (settings.containsKey("brewerPalette")) {
					manager.setBrewerPalette(network, settings.get("brewerPalette"), group);
				}
			} 
		}
	}


	// Update our settings in the network table

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
		EnrichmentUtils.updateEnrichmentSettingsTableGroup(manager, network, group, groupSettings);
	}

	public Map<String, List<Annotation>> getAnnotations() { return annotations; }
	

	public Map<String, List<Annotation>> getAnnotations(StringManager manager, Species species, final String terms, 
	                                                    final String useDATABASE, boolean includeViruses) throws ConnectionException {
		String encTerms = terms.trim();

		// make list of terms unique
		Set<String> termsSet = new HashSet<String>(Arrays.asList(encTerms.split("\n")));
		Object[] termsArray = termsSet.toArray();
		
		// Split the terms up into groups of 5000
		annotations = new HashMap<>();
		for (int i = 0; i < termsArray.length; i = i + 5000) {
			String termsBatch = getTerms(termsArray, i, i + 5000, termsArray.length);
			annotations = getAnnotationBatch(species, termsBatch, useDATABASE, includeViruses);
		}
		// check which identifiers could not be mapped
		termsSet.removeAll(annotations.keySet());
		manager.info("List of nodes that could not be mapped to any STRING identifier: " + termsSet);
		return annotations;
	}

	private Map<String, List<Annotation>> getAnnotationBatch(Species species, final String encTerms, 
	                                                         String useDATABASE, boolean includeViruses) throws ConnectionException {
		// always call the string API first to resolve all potential protein IDs
		// new API 

		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/get_string_ids";
		Map<String, String> args = new HashMap<>();
		String speciesId;
		if (species.getTaxId() > 0)
			speciesId = Integer.toString(species.getTaxId());
		else
			speciesId = species.getName();
		args.put("species", speciesId);
		args.put("identifiers", encTerms);
		args.put("csdata", "1");
		args.put("caller_identity", StringManager.CallerIdentity);
		manager.info("URL: "+url+"?species="+speciesId+"&caller_identity="+StringManager.CallerIdentity+"&identifiers="+encTerms);
		// Get the results
		JSONObject results = null;
		try {
			results = HttpUtils.postJSON(url, args, manager);
			if (results != null) {
				annotations = Annotation.parseAnnotations(results, encTerms, annotations, species, Databases.STRING.getAPIName());
			}
		} catch (ConnectionException ex) {
			if (ex instanceof UnknownSpeciesException && manager.isVirusesEnabled() && annotations.size() == 0 && includeViruses) {
				url = manager.getEntityQueryURL();
				args = new HashMap<>();
				args.put("limit", "5");
				args.put("types", speciesId);
				args.put("format", "json");
				args.put("query", encTerms);
				manager.info("URL: "+url+"?types=" + speciesId + "&limit=5&format=json"+"&identifiers="+HttpUtils.truncate(encTerms));
				
				// Get the results
				results = HttpUtils.postJSON(url, args, manager);
				if (results != null) {
					annotations = Annotation.parseAnnotations(results, encTerms, annotations, species, Databases.JENSENLAB.getAPIName());
				}
			}
		}
		
		results = null;
		
		// then, call other APIs to get resolve them
		// resolve compounds 
		if (useDATABASE.equals(Databases.STITCH.getAPIName())) {
			
			url = manager.getEntityQueryURL();
			args = new HashMap<>();
			// we ask for 10 since half of them are CIDs, which we ignore
			args.put("limit", "10");
			args.put("types", "-1");
			args.put("format", "json");
			args.put("query", encTerms);
			manager.info("URL: "+url+"?types=-1&limit=10&format=json"+"&identifiers="+HttpUtils.truncate(encTerms));
			
			// Get the results
			results = HttpUtils.postJSON(url, args, manager);

			if (results != null) {
				updateAnnotations(results, encTerms, species, Databases.JENSENLAB.getAPIName());
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
					// TODO: [Custom] figure out how to add the ones that resolve to multiple ids and the user chose one of them (if needed) 
					for (Annotation a: annotations.get(key)) {a.setResolved(true);}
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
	
	private void updateAnnotations(JSONObject results, String terms, Species species, String useDATABASE) {
		Map<String, List<Annotation>> newAnnotations = Annotation.getAnnotations(results,
				terms, species, useDATABASE);
		for (String newAnn : newAnnotations.keySet()) {
			List<Annotation> allAnn = new ArrayList<Annotation>(newAnnotations.get(newAnn));
			if (annotations.containsKey(newAnn)) {
				allAnn.addAll(annotations.get(newAnn));
			}
			annotations.put(newAnn, allAnn);
		}
	}
	
}
