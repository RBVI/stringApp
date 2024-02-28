package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

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
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class LoadInteractions extends AbstractTask {
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

	public LoadInteractions(final StringNetwork stringNet, final String speciesName, final Species species, 
            final int confidence, final int additionalNodes,
									final List<String>stringIds,
									final Map<String, String> queryTermMap,
									final String netName,
									final String useDATABASE,
									final NetworkType netType) {
		this(stringNet, speciesName, species, confidence, additionalNodes, stringIds, queryTermMap, netName, useDATABASE);
		this.netType = netType;
	}
	
	public LoadInteractions(final StringNetwork stringNet, final String speciesName, final Species species, 
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
		else if (useDATABASE.equals(Databases.STRINGDB.getAPIName()))
			monitor.setTitle("Loading data from STRING-DB for " + uniqueIds.size() + " identifier(s).");

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

		// We use different arguments depending on the database
		if (useDATABASE.equals(Databases.STRINGDB.getAPIName())) {
			// System.out.println("Identifiers: "+ids.trim());
			args.put("identifiers",ids.trim());
			args.put("required_score",String.valueOf(confidence*10));
			args.put("network_type", netType.getAPIName());
			networkURL = manager.getStringNetworkURL();
			if (additionalNodes > 0) {
				args.put("add_color_nodes", Integer.toString(additionalNodes));
			}
			args.put("caller_identity", StringManager.CallerIdentity);
			args.put("species", species.getName());
		} else {
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
					args.put("filter", String.valueOf(species.getTaxId()) + ".%%");
				} else {
					args.put("filter", String.valueOf(species.getTaxId()) + ".%%|CIDm%%");
				}
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
		// This may change...
		CyNetwork network = ModelUtils.createNetworkFromJSON(stringNet, speciesName, results, 
		                                                     queryTermMap, nodeMap, ids.trim(), 
		                                                     netName, useDATABASE, netType.getAPIName());

		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"STRING returned no results");
			return;
		}

		// Rename network collection to have the same name as network
		// EditNetworkTitleTaskFactory editNetworkTitle = (EditNetworkTitleTaskFactory) manager
		//		.getService(EditNetworkTitleTaskFactory.class);
		//insertTasksAfterCurrentTask(editNetworkTitle.createTaskIterator(network,
		//		network.getRow(network).get(CyNetwork.NAME, String.class)));
		
		// Set our confidence score
		ModelUtils.setConfidence(network, ((double)confidence)/100.0);
		ModelUtils.setNetworkType(network, netType.toString());
		ModelUtils.setDatabase(network, useDATABASE);
		ModelUtils.setNetSpecies(network, speciesName);
		ModelUtils.setDataVersion(network, manager.getDataVersion());
		ModelUtils.setNetURI(network, manager.getNetworkURL());
		stringNet.setNetwork(network);

		// Finally, update any node information if we're using from STRING
		// Only do this when we asked for additional nodes
		if (useDATABASE.equals(Databases.STRINGDB.getAPIName()) && additionalNodes > 0) {
			String terms = "";
			for (String term: nodeMap.keySet()) {
				terms += term+"\n";
			}
			try {
				Map<String, List<Annotation>> annotations = stringNet.getAnnotations(stringNet.getManager(), species, terms, useDATABASE, true);
				// TODO: [Custom] do we need to resolve or just take the first annotation or last one, which is currently the case...?
				for (String s: annotations.keySet()) {
					CyNode node = nodeMap.get(s);
					for (Annotation a: annotations.get(s)) {
						ModelUtils.updateNodeAttributes(network.getRow(node), a, false);
					}
				}
			} catch (ConnectionException ce) {
				monitor.showMessage(TaskMonitor.Level.ERROR, "Unable to get additional node annotations");
			}
		}

		// System.out.println("Results: "+results.toString());
		int viewThreshold = ModelUtils.getViewThreshold(manager);
		int networkSize = network.getNodeList().size() + network.getEdgeList().size();
		if (networkSize < viewThreshold) {
			// Now style the network
			// TODO:  change style to accomodate STITCH
			CyNetworkView networkView = manager.createNetworkView(network);
			ViewUtils.styleNetwork(manager, network, networkView);
	
			// And lay it out
			CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
			Object context = alg.createLayoutContext();
			TunableSetter setter = manager.getService(TunableSetter.class);
			Map<String, Object> layoutArgs = new HashMap<>();
			layoutArgs.put("defaultNodeMass", 10.0);
			setter.applyTunables(context, layoutArgs);
			Set<View<CyNode>> nodeViews = new HashSet<>(networkView.getNodeViews());
			insertTasksAfterCurrentTask(alg.createTaskIterator(networkView, context, nodeViews, ModelUtils.SCORE));
		} else {
			ViewUtils.styleNetwork(manager, network, null);
		}
		manager.updateControls();
	}

	@ProvidesTitle
	public String getTitle() { return "Loading interactions"; }
}
