package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
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
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class LoadTermsTask extends AbstractTask {
	final StringNetwork stringNet;
	final String species;
	final int taxonId;
	final int confidence;
	final int additionalNodes;
	final List<String> stringIds;
	final Map<String, String> queryTermMap;

	@Tunable(description="Re-layout network?")
	public boolean relayout = false;

	public LoadTermsTask(final StringNetwork stringNet, final String species, final int taxonId, 
	                     final int confidence, final int additionalNodes,
								     	 final List<String>stringIds,
								    	 final Map<String, String> queryTermMap) {
		this.stringNet = stringNet;
		this.taxonId = taxonId;
		this.additionalNodes = additionalNodes;
		this.confidence = confidence;
		this.stringIds = stringIds;
		this.species = species;
		this.queryTermMap = queryTermMap;
	}

	public void run(TaskMonitor monitor) {
		StringManager manager = stringNet.getManager();
		CyNetwork network = stringNet.getNetwork();

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
		args.put("entities",ids.trim());
		args.put("score", conf);
		if (additionalNodes > 0)
			args.put("additional", Integer.toString(additionalNodes));
		args.put("existing", ModelUtils.getExisting(network).trim());

		monitor.setStatusMessage("Getting additional terms from "+manager.getNetworkURL());

		Object results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);

		monitor.setStatusMessage("Augmenting network");

		List<CyEdge> newEdges = new ArrayList<>();
		List<CyNode> newNodes = ModelUtils.augmentNetworkFromJSON(manager, network, newEdges,
		                                                          results, queryTermMap);

		monitor.setStatusMessage("Adding "+newNodes.size()+" nodes and "+newEdges.size()+" edges");

		// Set our confidence score
		ModelUtils.setConfidence(network, ((double)confidence)/100.0);

		// Get our view
		CyNetworkView netView = null;
		Collection<CyNetworkView> views = 
		          manager.getService(CyNetworkViewManager.class).getNetworkViews(network);
		for (CyNetworkView view: views) {
			if (view.getRendererId().equals("org.cytoscape.ding")) {
				netView = view;
				break;
			}
		}

		// If we have a view, re-apply the style and layout
		if (netView != null) {
			monitor.setStatusMessage("Updating style");
			// monitor.setStatusMessage("Laying out network");
			ViewUtils.updateNodeStyle(manager, netView, newNodes);
			ViewUtils.updateEdgeStyle(manager, netView, newEdges);
			netView.updateView();

			// And lay it out
			if (relayout) {
				monitor.setStatusMessage("Updating layout");
				CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
				Object context = alg.createLayoutContext();
				TunableSetter setter = manager.getService(TunableSetter.class);
				Map<String, Object> layoutArgs = new HashMap<>();
				layoutArgs.put("defaultNodeMass", 10.0);
				setter.applyTunables(context, layoutArgs);
				Set<View<CyNode>> nodeViews = new HashSet<>(netView.getNodeViews());
				insertTasksAfterCurrentTask(alg.createTaskIterator(netView, context, nodeViews, "score"));
			}
		}
	}

	@ProvidesTitle
	public String getTitle() {
		return "Adding Terms to Network";
	}
}
