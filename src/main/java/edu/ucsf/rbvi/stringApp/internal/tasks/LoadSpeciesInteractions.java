package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.TunableSetter;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.JSONUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class LoadSpeciesInteractions extends AbstractTask {

	final StringNetwork stringNet;
	final String speciesName;
	final Species species;
	Species species2 = null;
	final int confidence;
	// final int additionalNodes;
	// final List<String> stringIds;
	// final Map<String, String> queryTermMap;
	final String netName;
	final String useDATABASE;
	NetworkType netType;
	String errorMsg;

	public LoadSpeciesInteractions(final StringNetwork stringNet, final Species species1,
	                               final Species species2, final int confidence, final NetworkType netType, final String netName) {
		this(stringNet, species1.toString(), species1, confidence, netName, Databases.STRING.getAPIName());
		this.species2 = species2;
		this.netType = netType;
	}
	
	public LoadSpeciesInteractions(final StringNetwork stringNet, final String speciesName,
			final Species species, final int confidence, final String netName,
			final String useDATABASE, final NetworkType netType) {
		this(stringNet, speciesName, species, confidence, netName, useDATABASE);
		this.netType = netType;
	}

	public LoadSpeciesInteractions(final StringNetwork stringNet, final String speciesName,
			final Species species, final int confidence, final String netName,
			final String useDATABASE) {

		this.stringNet = stringNet;
		this.species = species;
		this.confidence = confidence;
		this.speciesName = speciesName;
		this.netName = netName;
		this.useDATABASE = useDATABASE;
		this.errorMsg = null;
	}

	public void run(TaskMonitor monitor) {
		StringManager manager = stringNet.getManager();
		Map<String, String> args = new HashMap<>();
		String networkURL = "";

		if (useDATABASE.equals(Databases.STRING.getAPIName()) && (species2 != null || Species.isViral(species))){
			monitor.setTitle("Loading interactions from Jensenlab for " + species + " and " + species2);
			monitor.setStatusMessage("Please be patient, this might take several minutes. In case of server timeout, consider increasing the confidence cutoff.");
			
			networkURL = manager.getNetworkURL();
			String conf = "0." + confidence;
			if (confidence == 100)
				conf = "1.0";
			args.put("score", conf);	
			args.put("database", netType.getAPIName());
			args.put("organism", String.valueOf(species.getTaxId()));
			if (species2 != null)
				args.put("organism2", String.valueOf(species2.getTaxId()));
			args.put("caller_identity", StringManager.CallerIdentity);
		} else {
			// TODO: [move] test with custom species
			// if useDATABASE.equals(Databases.STRINGDB.getAPIName()) || species.isCustom() go to STRING-DB for the whole organism query
			if (Species.isViral(species))
				monitor.setTitle("Loading interactions from Jensenlab for " + species);
			else
				monitor.setTitle("Loading interactions from STRING-DB for " + species);
			monitor.setStatusMessage("Please be patient, this might take several minutes. In case of server timeout, consider increasing the confidence cutoff.");
			networkURL = manager.getStringNetworkURL();
			args.put("identifiers","__ALL_PROTEINS__");
			args.put("required_score",String.valueOf(confidence*10));
			args.put("network_type", netType.getAPIName());
			args.put("species", species.getName());
			args.put("caller_identity", StringManager.CallerIdentity);
		} 
		
		// double time = System.currentTimeMillis();
		JSONObject results;
		try {
			results = HttpUtils.postJSON(networkURL, args, manager);
		} catch (ConnectionException e) {
			this.errorMsg = e.getMessage();
			if (this.errorMsg.equals("cloudflare server timeout")) {
				String customErrorMsg = "This query is too large and caused a server timeout. Try to rerun the same query or consider increasing the confidence cutoff.";
				monitor.showMessage(Level.ERROR, customErrorMsg);
				throw new RuntimeException(customErrorMsg);
			} else { 
				monitor.showMessage(Level.ERROR, "Network error: " + this.errorMsg);
				throw new RuntimeException("Network error: " + this.errorMsg);
			}
		}
		// System.out.println(
		// "postJSON method " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		// time = System.currentTimeMillis();

		Map<String, CyNode> nodeMap = new HashMap<>();
		CyNetwork network = JSONUtils.createNetworkFromJSON(stringNet, speciesName, results, null, nodeMap,
		                                                    null, netName, useDATABASE, netType.getAPIName());
		// System.out.println("createNetworkFromJSON method "
		// + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		// time = System.currentTimeMillis();

		if (network == null) {
			this.errorMsg = "STRING returned no results";
			monitor.showMessage(TaskMonitor.Level.ERROR, "STRING returned no results");
			return;
		} else {
			monitor.setStatusMessage("Added " + network.getEdgeCount() + " edges");
		}

		if (species2 != null && !Species.isViral(species2)) {
			// TODO: [move] here we fetch intra-species interactions for species2 and species1
			networkURL = manager.getStringNetworkURL();
			args = new HashMap<>();
			args.put("required_score", String.valueOf((int)(confidence*10)));
			args.put("network_type", netType.getAPIName());				
			args.put("species", species2.toString());
			args.put("identifiers", ModelUtils.getExisting(network, species2.toString()).trim());
			JSONObject resultsSTRINGDB = null;
			try {
				monitor.setStatusMessage("Fetching data from: "+manager.getStringNetworkURL() + " for species " + species2.toString());
				resultsSTRINGDB = HttpUtils.postJSON(manager.getStringNetworkURL(), args, manager);
			} catch (ConnectionException e) {
				monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			}
			if (resultsSTRINGDB != null) {
				List<CyEdge> newEdges = new ArrayList<>();
				JSONUtils.augmentNetworkFromJSON(manager.getStringNetwork(network), network, newEdges, resultsSTRINGDB, null, Databases.STRINGDB.getAPIName(), netType.getAPIName());
				monitor.setStatusMessage("Adding " + newEdges.size() + " edges from STRING-DB");
			}
			if (!Species.isViral(species)) {
				// TODO: [move] here we also fetch the intra-species interactions for species1 unless it is a virus
				// overwrite identifiers and species to fetch interactions for the first species
				args.put("species", species.toString());
				args.put("identifiers", ModelUtils.getExisting(network, species.toString()).trim());
				resultsSTRINGDB = null;
				try {
					monitor.setStatusMessage("Fetching data from: "+manager.getStringNetworkURL() + " for species " + species.toString());
					resultsSTRINGDB = HttpUtils.postJSON(manager.getStringNetworkURL(), args, manager);
				} catch (ConnectionException e) {
					monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
				}
				if (resultsSTRINGDB != null) {
					List<CyEdge> newEdges = new ArrayList<>();
					JSONUtils.augmentNetworkFromJSON(manager.getStringNetwork(network), network, newEdges, resultsSTRINGDB, null, Databases.STRINGDB.getAPIName(), netType.getAPIName());
					monitor.setStatusMessage("Adding " + newEdges.size() + " edges from STRING-DB");
				}	
			}
		}
		
		// Set our confidence score
		ModelUtils.setConfidence(network, ((double) confidence) / 100.0);
		ModelUtils.setNetworkType(network, netType.toString());
		ModelUtils.setDatabase(network, useDATABASE);
		ModelUtils.setNetSpecies(network, species.toString());
		ModelUtils.setDataVersion(network, manager.getDataVersion());
		ModelUtils.setNetURI(network, manager.getNetworkURL());
		stringNet.setNetwork(network);

		int viewThreshold = ModelUtils.getViewThreshold(manager);
		int networkSize = network.getNodeList().size() + network.getEdgeList().size();
		if (networkSize < viewThreshold) {
			// Now style the network
			CyNetworkView networkView = manager.createNetworkView(network);
			ViewUtils.styleNetwork(manager, network, networkView);
			if (species2 != null) {
				ViewUtils.updateNodeColors(manager, network, networkView, Arrays.asList(species2.toString(), species.toString()));
			}
			// And lay it out
			CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class)
					.getLayout("force-directed");
			Object context = alg.createLayoutContext();
			TunableSetter setter = manager.getService(TunableSetter.class);
			Map<String, Object> layoutArgs = new HashMap<>();
			layoutArgs.put("defaultNodeMass", 10.0);
			setter.applyTunables(context, layoutArgs);
			Set<View<CyNode>> nodeViews = new HashSet<>(networkView.getNodeViews());
			insertTasksAfterCurrentTask(
					alg.createTaskIterator(networkView, context, nodeViews, ColumnNames.SCORE));

		} else {
			ViewUtils.styleNetwork(manager, network, null);
			if (species2 != null) {
				ViewUtils.updateNodeColors(manager, network, null, Arrays.asList(species2.toString(), species.toString()));
			}
		}
		manager.updateControls();
	}

	public boolean hasError() {
		return this.errorMsg != null;
	}
	
	public String getErrorMessage() {
		return this.errorMsg;
	}

	@ProvidesTitle
	public String getTitle() {
		return "Loading whole species interactions";
	}
}
