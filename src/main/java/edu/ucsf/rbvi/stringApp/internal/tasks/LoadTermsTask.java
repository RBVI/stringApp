package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

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
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
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
	String useDATABASE;
	NetworkType netType;

	@Tunable(description="Re-layout network?")
	public boolean relayout = false;

	public LoadTermsTask(final StringNetwork stringNet, final String species, final int taxonId, 
            final int confidence, final int additionalNodes,
					     	 final List<String>stringIds,
					    	 final Map<String, String> queryTermMap, final String useDATABASE,
					    	 final NetworkType netType) {
		this(stringNet, species, taxonId, confidence, additionalNodes, stringIds,
						    	 queryTermMap, useDATABASE);
		this.netType = netType;
	}
	
	public LoadTermsTask(final StringNetwork stringNet, final String species, final int taxonId, 
	                     final int confidence, final int additionalNodes,
								     	 final List<String>stringIds,
								    	 final Map<String, String> queryTermMap, final String useDATABASE) {
		this.stringNet = stringNet;
		this.taxonId = taxonId;
		this.additionalNodes = additionalNodes;
		this.confidence = confidence;
		this.stringIds = stringIds;
		this.species = species;
		this.queryTermMap = queryTermMap;
		this.useDATABASE = useDATABASE;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Adding " + stringIds.size() + " terms to network");
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
		// args.put("database", useDATABASE);
		// TODO: Is it OK to always use stitch?
		args.put("database", netType.getAPIName());
		args.put("entities",ids.trim());
		args.put("score", conf);
		if (additionalNodes > 0) {
			args.put("additional", Integer.toString(additionalNodes));
			if (useDATABASE.equals(Databases.STRING.getAPIName())) {
				args.put("filter", taxonId + ".%%");
			} else {
				args.put("filter", taxonId + ".%%|CIDm%%");
			}
		}
		args.put("existing", ModelUtils.getExisting(network).trim());

		monitor.setStatusMessage("Getting additional terms from "+manager.getNetworkURL());

		JSONObject results;
		try {
			results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}

		if (results == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results");
			return;
		}

		monitor.setStatusMessage("Augmenting network");

		List<CyEdge> newEdges = new ArrayList<>();
		List<CyNode> newNodes = ModelUtils.augmentNetworkFromJSON(manager, network, newEdges,
		                                                          results, queryTermMap, useDATABASE, netType.getAPIName());

		if (newEdges.size() > 0 || newNodes.size() > 0) {
			monitor.setStatusMessage("Adding "+newNodes.size()+" nodes and "+newEdges.size()+" edges");
		} else {
			// monitor.showMessage(Level.WARN, "Adding "+newNodes.size()+" nodes and "+newEdges.size()+" edges");
			throw new RuntimeException("This query will not add any new nodes or edges to the existing network.");
			// SwingUtilities.invokeLater(new Runnable() {
			// public void run() {
			// JOptionPane.showMessageDialog(null,
			// "This query will not add any new nodes or edges to the existing network.",
			// "Warning", JOptionPane.WARNING_MESSAGE);
			// }
			// });
			// return;
		}

		// Set our confidence score
		ModelUtils.setConfidence(network, ((double)confidence)/100.0);
		ModelUtils.setNetworkType(network, netType.toString());

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
