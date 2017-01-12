package edu.ucsf.rbvi.stringApp.internal.tasks;

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
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedFloat;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ChangeConfidenceTask extends AbstractTask {
	final StringManager manager;
	final CyNetwork network;
	CyNetworkView netView;
	float currentConfidence = 0.4f;

	@Tunable (description="New confidence cutoff", gravity=1.0, params="slider=true")
	public BoundedFloat confidence = null;

	public ChangeConfidenceTask(final StringManager manager, final CyNetwork network, CyNetworkView netView) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;
		Double current = ModelUtils.getConfidence(network);
		if (current == null)
			throw new RuntimeException("Network doesn't appear to be a STRING network");
		currentConfidence = current.floatValue();
		confidence = new BoundedFloat(0.0f, currentConfidence, 1.0f, false, false);
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Change confidence");
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

		double newConfidence = confidence.getValue().doubleValue();
		List<CyEdge> newEdges = new ArrayList<>();

		// See if we're actually increasing the cutoff
		if (confidence.getValue() > currentConfidence) {
			monitor.setStatusMessage("Increased confidence: trimming edges");
			// Yes, just trim the network
			List<CyEdge> removeEdges = new ArrayList<>();
			for (CyEdge edge: network.getEdgeList()) {
				Double score = network.getRow(edge).get(ModelUtils.SCORE, Double.class);
				if (score != null && score < newConfidence)
					removeEdges.add(edge);
			}
			monitor.setStatusMessage("Removing "+removeEdges.size()+" edges");
			network.removeEdges(removeEdges);
			// And set the new value
			ModelUtils.setConfidence(network, confidence.getValue());
		} else if (confidence.getValue() < currentConfidence) {
			monitor.setStatusMessage("Decreased confidence: fetching new edges");
			// We're decreasing the confidence, so we need to get new edges
			// Get all of the current nodes for our "existing" list
			String existing = ModelUtils.getExisting(network);
			String database = ModelUtils.getDatabase(network);
			Map<String, String> args = new HashMap<>();
			args.put("existing", existing.trim());
			args.put("database", database.trim());
			args.put("score", confidence.getValue().toString());
			args.put("maxscore", Float.toString(currentConfidence));
			JSONObject results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);

			// This may change...
			List<CyNode> newNodes = ModelUtils.augmentNetworkFromJSON(manager, network, newEdges, results, null, database);

			monitor.setStatusMessage("Adding "+newEdges.size()+" edges");

			ModelUtils.setConfidence(network, confidence.getValue());
		}

		// If we have a view, re-apply the style and layout
		if (netView != null) {
			monitor.setStatusMessage("Laying out network");
			ViewUtils.updateEdgeStyle(manager, netView, newEdges);
			netView.updateView();

			// At some point, we want to change this to only restyle the edges
			/* ViewUtils.reapplyStyle(manager, netView);
			CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
			Object context = alg.createLayoutContext();
			Set<View<CyNode>> nodeViews = new HashSet<>(netView.getNodeViews());
			insertTasksAfterCurrentTask(alg.createTaskIterator(netView, context, nodeViews, "score"));
			*/
		}
	}

	@ProvidesTitle
	public String getTitle() {
		return "Change Confidence";
	}
}
