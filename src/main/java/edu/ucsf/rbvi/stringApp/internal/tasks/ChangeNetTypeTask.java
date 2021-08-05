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
import org.cytoscape.work.util.BoundedFloat;
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
	float currentConfidence = 0.4f;

	@Tunable (description="Confidence cutoff", 
			longDescription="Confidence score for the STRING interactions to be included in this network. ", 
			exampleStringValue="0.4", 
			gravity=1.0, params="slider=true", required=false)
	public BoundedFloat confidence = new BoundedFloat(0.0f, currentConfidence, 1.0f, false, false);

	@Tunable(description = "Network type",
	         longDescription="Type of the STRING interactions (edges) to be included in the network, either functional associations or physical interactions.",
	         exampleStringValue="full STRING network",
	         gravity=2.0, required=true)
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
			String currentNetType = ModelUtils.getNetworkType(network); 
			if (currentNetType == null)
				currentNetType = NetworkType.FUNCTIONAL.toString();
			currentType = NetworkType.getType(currentNetType);
			networkType.setSelectedValue(currentType);
			
			Double currentNetConf = ModelUtils.getConfidence(network);
			if (currentNetConf == null)
				throw new RuntimeException("Network doesn't appear to be a STRING network");
			currentConfidence = currentNetConf.floatValue();
			confidence.setValue(currentNetConf.floatValue());
	
		}
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Change confidence or type");
		
		if (network == null) {
			network = manager.getCurrentNetwork();
		}
		
		if (!ModelUtils.isCurrentDataVersion(network)) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. Network appears to be an old STRING network.");
			// showError("Task cannot be performed. Network appears to be an old STRING network.");
			return;			
		}

		// Always set the currentType and confidence after the network is set
		String currentNetType = ModelUtils.getNetworkType(network); 
		if (currentNetType == null) {
			monitor.showMessage(Level.WARN, "The network appears to not have a network type. stringApp will assume it is a functional network.");
			currentNetType = NetworkType.FUNCTIONAL.toString();
		}
		currentType = NetworkType.getType(currentNetType);
		
		Double currentNetConf = ModelUtils.getConfidence(network);
		if (currentNetConf == null)
			throw new RuntimeException("Network doesn't appear to be a STRING network due to a missing confidence attribute.");
		currentConfidence = currentNetConf.floatValue();

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
		
		NetworkType newType = networkType.getSelectedValue();
		// Check if we change the type or only the confidence
		if (newType.equals(currentType) && confidence.getValue().floatValue() == currentConfidence) {
			// everything stays the same, just ignore
			return;
		} else if (newType.equals(currentType) && confidence.getValue() > currentConfidence) {
			monitor.setStatusMessage("Increased confidence: trimming edges");
			// convert confidence to an integer to avoid issues with number precision
			int newConfidence = (int)(confidence.getValue()*1000);
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
		} else {		
			// choose proper message for the user
			if (newType.equals(currentType) && confidence.getValue() < currentConfidence)
				monitor.setStatusMessage("Decreased confidence: fetching new edges");
			else if (!newType.equals(currentType))
				monitor.setStatusMessage("Changing network type to " + networkType.getSelectedValue().getAPIName());

			// We're changing the network type or confidence, so we need to get new edges  and remove the old ones
			List<CyEdge> newEdges = new ArrayList<>();
			// Get all of the current nodes for our "existing" list
			String existing = ModelUtils.getExisting(network);
			// Get current database & confidence
			String database = ModelUtils.getDatabase(network);
			// Double confidence = ModelUtils.getConfidence(network);
			Map<String, String> args = new HashMap<>();
			args.put("existing", existing.trim());
			// Get chosen network type
			args.put("database", newType.getAPIName());
			args.put("score", confidence.getValue().toString());
			// args.put("maxscore", Float.toString(currentConfidence));
			JSONObject results;
			try {
				results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
			} catch (ConnectionException e) {
				e.printStackTrace();
				monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
				return;
			}
	
			if (results != null) {
				// remove old edges
				List<CyEdge> removeEdges = network.getEdgeList();
				monitor.setStatusMessage("Removing "+removeEdges.size()+" edges");
				network.removeEdges(removeEdges);
	
				// add new edges
				ModelUtils.augmentNetworkFromJSON(manager, network, newEdges, results, null, database, newType.getAPIName());
				monitor.setStatusMessage("Adding "+newEdges.size()+" edges");
	
				// change network attributes
				ModelUtils.setConfidence(network, (double)Math.round(confidence.getValue()*1000)/1000);
				ModelUtils.setNetworkType(network, networkType.getSelectedValue().toString());
				
				// change network name in the special case of changing from physical to functional or the other way around 
				if (!newType.equals(currentType)) {
					String currentName = manager.getNetworkName(network);
					String newName = currentName;
					if (newType.equals(NetworkType.FUNCTIONAL) && currentName.contains(ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL)) {
						// remove (physical) from the name
						String[] currentNameParts = currentName.split(ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL_REGEXP);
						newName = currentNameParts[0] + currentNameParts[currentNameParts.length-1];
					} else if (newType.equals(NetworkType.PHYSICAL)) {
						// add (physical) to the name
						if (currentName.startsWith(ModelUtils.DEFAULT_NAME_STRING))
							newName = ModelUtils.DEFAULT_NAME_STRING + " " + ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL + currentName.split(ModelUtils.DEFAULT_NAME_STRING)[1];							
						else if (currentName.startsWith(ModelUtils.DEFAULT_NAME_STITCH ))
							newName = ModelUtils.DEFAULT_NAME_STITCH + " " + ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL + currentName.split(ModelUtils.DEFAULT_NAME_STITCH)[1];
						}
					network.getRow(network).set(CyNetwork.NAME, manager.adaptNetworkName(newName));
				}
			}

			// If we have a view, re-apply the style and layout
			if (netView != null) {
				ViewUtils.updateEdgeStyle(manager, netView, newEdges);
				netView.updateView();
			}
		}

		// reset filters in the results panel
		manager.reinitResultsPanel(network);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Change Network Confidence or Type";
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
