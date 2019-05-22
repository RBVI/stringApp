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
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class LoadInteractions extends AbstractTask {
	final StringNetwork stringNet;
	final String species;
	final int taxonId;
	final int confidence;
	final int additionalNodes;
	final List<String> stringIds;
	final Map<String, String> queryTermMap;
	final String netName;
	final String useDATABASE;

	public LoadInteractions(final StringNetwork stringNet, final String species, final int taxonId, 
	                        final int confidence, final int additionalNodes,
													final List<String>stringIds,
													final Map<String, String> queryTermMap,
													final String netName,
													final String useDATABASE) {
		this.stringNet = stringNet;
		this.taxonId = taxonId;
		this.additionalNodes = additionalNodes;
		this.confidence = confidence;
		this.stringIds = stringIds;
		this.species = species;
		this.queryTermMap = queryTermMap;
		this.netName = netName;
		this.useDATABASE = useDATABASE;
	}

	public void run(TaskMonitor monitor) {
		if (useDATABASE.equals(Databases.STRING.getAPIName()))
			monitor.setTitle("Loading interactions from string-db");
		else if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			monitor.setTitle("Loading interactions from STITCH");
		StringManager manager = stringNet.getManager();
		String ids = null;
		for (String id: stringIds) {
			if (ids == null)
				ids = id;
			else
				ids += "\n"+id;
		}

		String conf = "0."+confidence;
		if (confidence == 100) 
			conf = "1.0";

		// String url = "http://api.jensenlab.org/network?entities="+URLEncoder.encode(ids.trim())+"&score="+conf;
		Map<String, String> args = new HashMap<>();
		// args.put("database", useDATABASE);
		// TODO: Is it OK to always use stitch?
		args.put("database", Databases.STITCH.getAPIName());
		args.put("entities",ids.trim());
		args.put("score", conf);
		args.put("caller_identity", StringManager.CallerIdentity);
		if (additionalNodes > 0) {
			args.put("additional", Integer.toString(additionalNodes));
			if (useDATABASE.equals(Databases.STRING.getAPIName())) {
				args.put("filter", taxonId + ".%%");
			} else {
				args.put("filter", taxonId + ".%%|CIDm%%");
			}
		}
		JSONObject results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);

		// This may change...
		CyNetwork network = ModelUtils.createNetworkFromJSON(stringNet, species, results, 
		                                                     queryTermMap, ids.trim(), netName, useDATABASE);

		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}

		// Rename network collection to have the same name as network
		EditNetworkTitleTaskFactory editNetworkTitle = (EditNetworkTitleTaskFactory) manager
				.getService(EditNetworkTitleTaskFactory.class);
		insertTasksAfterCurrentTask(editNetworkTitle.createTaskIterator(network,
				network.getRow(network).get(CyNetwork.NAME, String.class)));
		
		// Set our confidence score
		ModelUtils.setConfidence(network, ((double)confidence)/100.0);
		ModelUtils.setDatabase(network, useDATABASE);
		ModelUtils.setNetSpecies(network, species);
		stringNet.setNetwork(network);

		// System.out.println("Results: "+results.toString());
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
		insertTasksAfterCurrentTask(alg.createTaskIterator(networkView, context, nodeViews, "score"));
	}

	@ProvidesTitle
	public String getTitle() { return "Loading interactions"; }
}
