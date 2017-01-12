package edu.ucsf.rbvi.stringApp.internal.tasks;

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
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ExpandNetworkTask extends AbstractTask {
	final StringManager manager;
	final CyNetwork network;
	CyNetworkView netView;
	View<CyNode> nodeView;
	
	@Tunable (description="Number of nodes to expand network by", gravity=1.0)
	public int additionalNodes = 10;

	@Tunable (description="Expand from database", gravity=2.0)
	public ListSingleSelection<String> databases = new ListSingleSelection<String>(
			Databases.STRING.toString(), Databases.STITCH.toString());
	
	@Tunable (description="Relayout network?", gravity=3.0)
	public boolean relayout = false;

	//@Tunable (description="Expand from database", gravity=3.0, groups = "Advanced options", params = "displayState=collapsed")
	//public ListSingleSelection<String> databases = new ListSingleSelection<String>("string", "stitch");
	
	public ExpandNetworkTask(final StringManager manager, final CyNetwork network, CyNetworkView netView) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;
		this.nodeView = null;
		databases.setSelectedValue(Databases.STRING.toString());
		if (ModelUtils.getDatabase(network) != null && ModelUtils.getDatabase(network).equals(Databases.STITCH.getAPIName()))
			databases.setSelectedValue(Databases.STITCH.toString());
	}

	public ExpandNetworkTask(final StringManager manager, final CyNetwork network, CyNetworkView netView, View<CyNode> nodeView) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;
		this.nodeView = nodeView;
		databases.setSelectedValue(Databases.STRING.toString());
		if (ModelUtils.getDatabase(network) != null && ModelUtils.getDatabase(network).equals(Databases.STITCH.getAPIName()))
			databases.setSelectedValue(Databases.STITCH.toString());
	}

	public void run(TaskMonitor monitor) {
		// First see if we've got a view
		if (netView == null) {
			Collection<CyNetworkView> views = 
			          manager.getService(CyNetworkViewManager.class).getNetworkViews(network);
			for (CyNetworkView view: views) {
				if (view.getRendererId().equals("org.cytoscape.ding")) {
					netView = view;
					break;
				}
			}
		}


		// Get all of the current nodes for our "existing" list
		String existing = ModelUtils.getExisting(network);
		String selected = ModelUtils.getSelected(network, nodeView);
		String url = "http://api.jensenlab.org/network";
		Map<String, String> args = new HashMap<>();
		args.put("existing",existing.trim());
		if (selected != null && selected.length() > 0)
			args.put("selected",selected.trim());
		Double conf = ModelUtils.getConfidence(network);
		if (conf == null)
			args.put("score", "0.4");
		else
			args.put("score", conf.toString());
		if (additionalNodes > 0)
			args.put("additional", Integer.toString(additionalNodes));
		String useDatabase = databases.getSelectedValue().toLowerCase();
		args.put("database", useDatabase);
		monitor.setStatusMessage("Getting additional nodes from: "+url);

		JSONObject results = HttpUtils.postJSON(url, args, manager);

		monitor.setStatusMessage("Augmenting network");

		// This may change...
		List<CyEdge> newEdges = new ArrayList<>();
		List<CyNode> newNodes = ModelUtils.augmentNetworkFromJSON(manager, network, newEdges, results, null, useDatabase);

		monitor.setStatusMessage("Adding "+newNodes.size()+" nodes and "+newEdges.size()+" edges");
		// System.out.println("Adding "+newNodes.size()+" nodes and "+newEdges.size()+" edges");

		// If we have a view, re-apply the style and layout
		if (netView != null) {
			monitor.setStatusMessage("Updating style");
			// System.out.println("Updating style");
			ViewUtils.updateEdgeStyle(manager, netView, newEdges);
			// System.out.println("Done");
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
		return "Expand Network";
	}
}
