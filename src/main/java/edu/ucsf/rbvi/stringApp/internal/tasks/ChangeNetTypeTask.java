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
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.JSONUtils;
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
			List<CyEdge> stringEdges = ModelUtils.getStringNetEdges(network);
			for (CyEdge edge: stringEdges) {
				// get score of the interaction and check if the edge should be removed or not
				Double score = network.getRow(edge).get(ColumnNames.SCORE, Double.class);					
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

			// We're changing the network type or confidence, so we need to get new edges and remove the old ones
			List<CyEdge> newEdges = new ArrayList<>();
			// Get all of the current nodes for our "existing" list
			String existing = ModelUtils.getExisting(network);
			// Get current database & confidence
			String database = ModelUtils.getDatabase(network);
			// Get species
			String species = ModelUtils.getNetSpecies(network);
			if (species == null) {
				species = ModelUtils.getMostCommonNetSpecies(network);
				ModelUtils.setNetSpecies(network, species);
			}
			Species selSpecies = Species.getSpecies(species);
			
			Map<String, String> argsSTRINGDB = new HashMap<>();
			argsSTRINGDB.put("identifiers", existing.trim());
			argsSTRINGDB.put("required_score", String.valueOf((int)(confidence.getValue()*1000)));
			argsSTRINGDB.put("network_type", newType.getAPIName());				
			argsSTRINGDB.put("species", selSpecies.toString());

			Map<String, String> argsJensenlab = new HashMap<>();			
			argsJensenlab.put("existing", existing.trim());
			argsJensenlab.put("score", confidence.getValue().toString());
			argsJensenlab.put("database", newType.getAPIName());
			
			JSONObject resultsSTRINGDB = null;
			JSONObject resultsJensenlab = null;
			// TODO: [move] revise if needed and test further
			try {
				if (database.equals(Databases.STITCH.getAPIName()) || (database.equals(Databases.STRING.getAPIName()) && !Species.isViral(selSpecies))) {
					//System.out.println("Call both APIs");
					monitor.setStatusMessage("Fetching data from: "+manager.getStringNetworkURL());
					resultsSTRINGDB = HttpUtils.postJSON(manager.getStringNetworkURL(), argsSTRINGDB, manager);
					monitor.setStatusMessage("Fetching data from: "+manager.getNetworkURL());
					resultsJensenlab = HttpUtils.postJSON(manager.getNetworkURL(), argsJensenlab, manager);	
				} else if (Species.isViral(selSpecies)) {
					//System.out.println("Call Jensenlab API only");
					monitor.setStatusMessage("Fetching data from: "+manager.getNetworkURL());
					resultsJensenlab = HttpUtils.postJSON(manager.getNetworkURL(), argsJensenlab, manager);
				} else if (database.equals(Databases.STRINGDB.getAPIName())) {
					//System.out.println("Call STRING-DB API only");
					monitor.setStatusMessage("Fetching data from: "+manager.getStringNetworkURL());
					resultsSTRINGDB = HttpUtils.postJSON(manager.getStringNetworkURL(), argsSTRINGDB, manager);
				} else {
					//System.out.println("What do we have here?!");
				}
			} catch (ConnectionException e) {
				e.printStackTrace();
				monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
				return;
			}
	
			if (resultsJensenlab != null || resultsSTRINGDB != null) {
				// remove old edges
				List<CyEdge> removeEdges = ModelUtils.getStringNetEdges(network);;
				// monitor.setStatusMessage("Removing "+removeEdges.size()+" edges");
				network.removeEdges(removeEdges);
	
				// add new edges
				if (resultsJensenlab != null) {
					JSONUtils.augmentNetworkFromJSON(manager.getStringNetwork(network), network, newEdges, resultsJensenlab, null, database, newType.getAPIName());
					monitor.setStatusMessage("Adding edges from Jensenlab");
				}
				
				if (resultsSTRINGDB != null) {
					JSONUtils.augmentNetworkFromJSON(manager.getStringNetwork(network), network, newEdges, resultsSTRINGDB, null, Databases.STRINGDB.getAPIName(), newType.getAPIName());
					monitor.setStatusMessage("Adding edges from STRING-DB");
				}
				monitor.setStatusMessage((newEdges.size() - removeEdges.size()) + " edges added to the network");

				// change network attributes
				ModelUtils.setConfidence(network, (double)Math.round(confidence.getValue()*1000)/1000);
				ModelUtils.setNetworkType(network, networkType.getSelectedValue().toString());
				
				// change network name in the special case of changing from physical to functional or the other way around
				if (!newType.equals(currentType)) {
					monitor.setStatusMessage("Updating network name");
					String currentName = manager.getNetworkName(network);
					String newName = currentName;
					if (newType.equals(NetworkType.FUNCTIONAL) && currentName.contains(ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL)) {
						// remove (physical) from the name
						String[] currentNameParts = currentName.split(ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL_REGEXP);
						if (currentNameParts.length > 1)
							newName = currentNameParts[0] + currentNameParts[currentNameParts.length-1];
						else
							newName = currentNameParts[0];
					} else if (newType.equals(NetworkType.PHYSICAL)) {
						// add (physical) to the name
						if (currentName.startsWith(ModelUtils.DEFAULT_NAME_STRING)) {
							if (currentName.split(ModelUtils.DEFAULT_NAME_STRING).length > 1)
								newName = ModelUtils.DEFAULT_NAME_STRING + " " + ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL + currentName.split(ModelUtils.DEFAULT_NAME_STRING)[1];
							else 
								newName = ModelUtils.DEFAULT_NAME_STRING + " " + ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL;
						} else if (currentName.startsWith(ModelUtils.DEFAULT_NAME_STITCH )) {
							if (currentName.split(ModelUtils.DEFAULT_NAME_STITCH)[1].length() > 1)
								newName = ModelUtils.DEFAULT_NAME_STITCH + " " + ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL + currentName.split(ModelUtils.DEFAULT_NAME_STITCH)[1];
							else 
								newName = ModelUtils.DEFAULT_NAME_STITCH + " " + ModelUtils.DEFAULT_NAME_ADDON_PHYSICAL;
						}
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
