package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.BoundedFloat;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ChangeConfidenceTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	CyNetworkView netView;
	float currentConfidence = 0.4f;

	@Tunable (description="New confidence cutoff", 
					longDescription="Confidence score for the STRING interactions to be included in this network. ", 
					exampleStringValue="0.4", 
					gravity=1.0, params="slider=true", required=true)
	public BoundedFloat confidence = new BoundedFloat(0.0f, currentConfidence, 1.0f, false, false);

	@Tunable(description="Network to change the confidence cutoff for", 
	         longDescription=StringToModel.CY_NETWORK_LONG_DESCRIPTION,
	         exampleStringValue=StringToModel.CY_NETWORK_EXAMPLE_STRING,
	         context="nogui", required=true)
	public CyNetwork network = null;
	
	public ChangeConfidenceTask(final StringManager manager, final CyNetwork network, CyNetworkView netView) {
		this.manager = manager;
		if (network != null)
			this.network = network;
		this.netView = netView;
		if (this.network != null) {
			Double current = ModelUtils.getConfidence(network);
			if (current == null)
				throw new RuntimeException("Network doesn't appear to be a STRING network");
			currentConfidence = current.floatValue();
			confidence.setValue(current.floatValue());
		}
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Change confidence");
		
		if (network == null) {
			network = manager.getCurrentNetwork();
		}
		
		if (!ModelUtils.isCurrentDataVersion(network)) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. Network appears to be an old STRING network.");
			// showError("Task cannot be performed. Network appears to be an old STRING network.");
			return;			
		}

		// Always set the currentConfidence after the network is set
		Double current = ModelUtils.getConfidence(network);
		if (current == null)
			throw new RuntimeException("Network doesn't appear to be a STRING network");
		currentConfidence = current.floatValue();
		
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

		// convert confidence to an integer to avoid issues with number precision
		int newConfidence = (int)(confidence.getValue()*1000);
		List<CyEdge> newEdges = new ArrayList<>();

		// See if we're actually increasing the cutoff
		if (confidence.getValue() > currentConfidence) {
			monitor.setStatusMessage("Increased confidence: trimming edges");
			// Yes, just trim the network
			List<CyEdge> removeEdges = new ArrayList<>();
			for (CyEdge edge: network.getEdgeList()) {
				Double score = network.getRow(edge).get(ModelUtils.SCORE, Double.class);
				if (score != null && (int)(score*1000) < newConfidence) {
					removeEdges.add(edge);
				}
			}
			monitor.setStatusMessage("Removing "+removeEdges.size()+" edges");
			network.removeEdges(removeEdges);
			// And set the new value
			ModelUtils.setConfidence(network, (double)Math.round(confidence.getValue()*1000)/1000);
		} else if (confidence.getValue() < currentConfidence) {
			monitor.setStatusMessage("Decreased confidence: fetching new edges");
			// We're decreasing the confidence, so we need to get new edges
			// Get all of the current nodes for our "existing" list
			String existing = ModelUtils.getExisting(network);
			String species = ModelUtils.getNetSpecies(network);
			if (species == null) {
				species = ModelUtils.getMostCommonNetSpecies(network);
				ModelUtils.setNetSpecies(network, species);
			}
			Species selSpecies = Species.getSpecies(species);
			String currentType = NetworkType.getType(ModelUtils.getNetworkType(network)).getAPIName();
			String database = ModelUtils.getDatabase(network);
			Map<String, String> args = new HashMap<>();
			if (selSpecies.isCustom()) {
				database = Databases.STRINGDB.getAPIName();
				args.put("identifiers", existing.trim());
				args.put("required_score", String.valueOf((int)(confidence.getValue()*1000)));
				args.put("network_type", currentType);				
				args.put("species", selSpecies.toString());
			} else {
				args.put("existing", existing.trim());
				args.put("score", confidence.getValue().toString());
				args.put("maxscore", Float.toString(currentConfidence));
				args.put("database", currentType);
			}
			// set network type
			JSONObject results;
			try {
				if (database.equals(Databases.STRINGDB.getAPIName())) {
					monitor.setStatusMessage("Fetching data from: "+manager.getStringNetworkURL());
					results = HttpUtils.postJSON(manager.getStringNetworkURL(), args, manager);
				} else {
					monitor.setStatusMessage("Fetching data from: "+manager.getNetworkURL());
					results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
				}
			} catch (ConnectionException e) {
				e.printStackTrace();
				monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
				return;
			}

			if (results != null) {
				// This may change...
				List<CyNode> newNodes = ModelUtils.augmentNetworkFromJSON(manager.getStringNetwork(network), network, newEdges, 
						                                                      results, null, database, currentType);

				// TODO: newEdges appears empty when this status message is printed
				//monitor.setStatusMessage("Adding "+newEdges.size()+" edges");
				monitor.setStatusMessage("Adding new edges");
	
				ModelUtils.setConfidence(network, (double)Math.round(confidence.getValue()*1000)/1000);
			}
		}

		// If we have a view, re-apply the style and layout
		if (netView != null) {
			// monitor.setStatusMessage("Laying out network");
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
		// reset filters in the results panel
		manager.reinitResultsPanel(network);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Change Confidence";
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
