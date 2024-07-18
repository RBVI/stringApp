package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.edit.EditNetworkTitleTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
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

/**
 * This version of LoadInteractions will query the jensenlab database first, then
 * query string-db.  This is used for STITCH, and Cross-species queries.
 */

public class LoadInteractions2 extends AbstractTask {
	final StringNetwork stringNet;
	final String speciesName;
	final Species species;
	final int confidence;
	final int additionalNodes;
	final List<String> stringIds;
	final Map<String, String> queryTermMap;
	final String netName;
	final String useDATABASE;
	NetworkType netType;

	public LoadInteractions2(final StringNetwork stringNet, final String speciesName, final Species species, 
                           final int confidence, final int additionalNodes,
									         final List<String>stringIds,
									         final Map<String, String> queryTermMap,
									         final String netName,
									         final String useDATABASE,
									         final NetworkType netType) {
		this(stringNet, speciesName, species, confidence, additionalNodes, stringIds, queryTermMap, netName, useDATABASE);
		this.netType = netType;
	}
	
	public LoadInteractions2(final StringNetwork stringNet, final String speciesName, final Species species, 
	                         final int confidence, final int additionalNodes,
													 final List<String>stringIds,
													 final Map<String, String> queryTermMap,
													 final String netName,
													 final String useDATABASE) {
		this.stringNet = stringNet;
		this.species = species;
		this.additionalNodes = additionalNodes;
		this.confidence = confidence;
		this.stringIds = stringIds;
		this.speciesName = speciesName;
		this.queryTermMap = queryTermMap;
		this.netName = netName;
		this.useDATABASE = useDATABASE;
	}

	public void run(TaskMonitor monitor) {
		// make sure the list of resolved IDs is unique
		Set<String> uniqueIds = new HashSet<String>(stringIds);
		if (useDATABASE.equals(Databases.STRING.getAPIName()))
			monitor.setTitle("Loading data from STRING for " + uniqueIds.size() + " identifier(s).");
		else if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			monitor.setTitle("Loading data from STITCH for " + uniqueIds.size() + " identifier(s).");

		// System.out.println("Using database: "+useDATABASE);
		StringManager manager = stringNet.getManager();
		String ids = null;
		for (String id: uniqueIds) {
			if (ids == null)
				ids = id;
			else
				ids += "\n"+id;
		}


		// String url = "http://api.jensenlab.org/network?entities="+URLEncoder.encode(ids.trim())+"&score="+conf;
		Map<String, String> args = new HashMap<>();
		String networkURL = manager.getNetworkURL();

		// FIXME: we need to be a bit careful with the "additional nodes" here since we don't really know how to get
		// additional protein nodes at this point if we've queried with a compound
		String conf = "0."+confidence;
		if (confidence == 100) 
			conf = "1.0";
		args.put("database", netType.getAPIName());
		args.put("entities",ids.trim());
		args.put("score", conf);
		args.put("caller_identity", StringManager.CallerIdentity);
		if (additionalNodes > 0) {
			args.put("additional", Integer.toString(additionalNodes));
			if (useDATABASE.equals(Databases.STRING.getAPIName())) {
				args.put("filter", String.valueOf(species.getTaxId()) + ".%");
			} else {
				args.put("filter", String.valueOf(species.getTaxId()) + ".%|CIDm%");
			}
		}

		JSONObject results;
		// System.out.println("URL: "+networkURL);
		try {
			results = HttpUtils.postJSON(networkURL, args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}

		// System.out.println("results: "+results.toString());

		Map<String, CyNode> nodeMap = new HashMap<>();
		CyNetwork network = JSONUtils.createNetworkFromJSON(stringNet, speciesName, results, 
		                                                    queryTermMap, nodeMap, ids.trim(), 
		                                                    netName, useDATABASE, netType.getAPIName());

		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"STRING returned no results");
			return;
		}

		// Set our confidence score
		ModelUtils.setConfidence(network, ((double)confidence)/100.0);
		ModelUtils.setNetworkType(network, netType.toString());
		ModelUtils.setDatabase(network, useDATABASE);
		ModelUtils.setNetSpecies(network, speciesName);
		ModelUtils.setDataVersion(network, manager.getDataVersion());
		ModelUtils.setNetURI(network, manager.getNetworkURL());
		stringNet.setNetwork(network);

		/**
		 * At this point, we have the interactions from the jensenlab servers -- specifically,
		 * either inter-species interactions or compound-compound and compound-protein interactions.
		 * The next step is to augment those with the intra-species protein-protein interactions.
		 */

		// Find all of the protein node identifiers
		List<String> proteins = ModelUtils.getProteinNodes(network);
		ids = null;
		for (String protein: proteins) {
			if (ids == null)
				ids = protein;
			else
				ids += "\n"+protein;
		}

		// Query string
		args.clear();
		// System.out.println("Identifiers: "+proteins.trim());
		networkURL = manager.getStringNetworkURL();
		args.put("identifiers",ids.trim());
		args.put("required_score",String.valueOf(confidence*10));
		args.put("network_type", netType.getAPIName());
		if (additionalNodes > 0) {
			args.put("additional_network_nodes", Integer.toString(additionalNodes));
		}
		args.put("caller_identity", StringManager.CallerIdentity);

		try {
			results = HttpUtils.postJSON(networkURL, args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}

		// Add to network
		List<CyEdge> newEdges = new ArrayList<>();
		List<CyNode> newNodes = JSONUtils.augmentNetworkFromJSON(stringNet, network, newEdges, 
						                                                 results, null, useDATABASE, netType.getAPIName());

		// System.out.println("Results: "+results.toString());
		int viewThreshold = ModelUtils.getViewThreshold(manager);
		int networkSize = network.getNodeList().size() + network.getEdgeList().size();
		if (networkSize < viewThreshold) {
			// Now style the network
			// TODO:  change style to accomodate STITCH
			CyNetworkView networkView = manager.createNetworkView(network);
			ViewUtils.styleNetwork(manager, network, networkView);
	
			// cutoff for max number of nodes with structure displayed is currently 300, the same as on the STRING page
			if (network.getNodeCount() <= ModelUtils.MAX_NODES_STRUCTURE_DISPLAY) {
				ModelUtils.fetchImages(network);
			} else {
				manager.setShowImage(false);
				ModelUtils.setNetworkHasImages(network, false);
			}
			
			// And lay it out
			CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
			Object context = alg.createLayoutContext();
			TunableSetter setter = manager.getService(TunableSetter.class);
			Map<String, Object> layoutArgs = new HashMap<>();
			layoutArgs.put("defaultNodeMass", 10.0);
			setter.applyTunables(context, layoutArgs);
			Set<View<CyNode>> nodeViews = new HashSet<>(networkView.getNodeViews());
			insertTasksAfterCurrentTask(alg.createTaskIterator(networkView, context, nodeViews, ColumnNames.SCORE));
		} else {
			ViewUtils.styleNetwork(manager, network, null);
		}
		manager.updateControls();
	}

	@ProvidesTitle
	public String getTitle() { return "Loading interactions"; }
}
