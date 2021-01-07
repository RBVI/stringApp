package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ChangeNetTypeTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	CyNetworkView netView;
	NetworkType currentType = NetworkType.FUNCTIONAL;

	@Tunable(description = "New network type",
	         longDescription="Change the type of edges of the network between functional associations and physical interactions.",
	         exampleStringValue="Functional associations",
	         required=true)
	public ListSingleSelection<NetworkType> networkType;

	@Tunable(description="Network to change the type for", 
	         longDescription=StringToModel.CY_NETWORK_LONG_DESCRIPTION,
	         exampleStringValue=StringToModel.CY_NETWORK_EXAMPLE_STRING,
	         context="nogui", required=true)
	public CyNetwork network = null;
	
	public ChangeNetTypeTask(final StringManager manager, final CyNetwork network, CyNetworkView netView) {
		this.manager = manager;
		networkType = new ListSingleSelection<>(NetworkType.values());
		if (network != null)
			this.network = network;
		this.netView = netView;
		if (this.network != null) {
			String current = ModelUtils.getNetworkType(network); 
			if (current == null)
				throw new RuntimeException("Network doesn't appear to be a STRING network");
			currentType = NetworkType.getType(current);
			if (currentType.equals(NetworkType.FUNCTIONAL))
				networkType.setSelectedValue(NetworkType.PHYSICAL);
			else
				networkType.setSelectedValue(NetworkType.FUNCTIONAL);
		}
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Change network type");
		
		if (network == null) {
			network = manager.getCurrentNetwork();
		}
		
		if (!ModelUtils.isCurrentDataVersion(network)) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. Network appears to be an old STRING network.");
			// showError("Task cannot be performed. Network appears to be an old STRING network.");
			return;			
		}

//		// Always set the currentType after the network is set
		String current = ModelUtils.getNetworkType(network); 
		if (current == null)
			throw new RuntimeException("Network doesn't appear to be a STRING network");
		currentType = NetworkType.getType(current);
		
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

		// We're changing the network type, so we need to get new edges
		List<CyEdge> newEdges = new ArrayList<>();
		monitor.setStatusMessage("Fetching new edges");
		// Get all of the current nodes for our "existing" list
		String existing = ModelUtils.getExisting(network);
		// Get current database & confidence
		String database = ModelUtils.getDatabase(network);
		Double confidence = ModelUtils.getConfidence(network);
		Map<String, String> args = new HashMap<>();
		args.put("existing", existing.trim());
		args.put("database", database);
		args.put("score", confidence.toString());
		// args.put("maxscore", Float.toString(currentConfidence));
		// Get chosen network type
		args.put("type", networkType.getSelectedValue().getAPIName());
		JSONObject results;
		try {
			results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}

		if (results != null) {
			// This may change...
			ModelUtils.augmentNetworkFromJSON(manager, network, newEdges, results, null, database);

			monitor.setStatusMessage("Adding "+newEdges.size()+" edges");

			ModelUtils.setNetworkType(network, networkType.getSelectedValue().toString());
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
		return "Change Network Type";
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(String.class)) {
			return (R)"";
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				return "{}";
			};
			return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class);
	}
}
