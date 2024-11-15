package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.JSONUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.StringResults;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ExpandNetworkTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	CyNetworkView netView;
	View<CyNode> nodeView;
	
	final static String mixedTypeProteins = "proteins";
	final static String mixedTypeCompounds = "compounds";
	
	@Tunable(description = "Network to expand", 
			longDescription = StringToModel.CY_NETWORK_LONG_DESCRIPTION, 
			exampleStringValue = StringToModel.CY_NETWORK_EXAMPLE_STRING, 
			context = "nogui", required=true)
	public CyNetwork network;

	@Tunable (description="Number of interactors to expand network by", 
			longDescription = "The maximum number of proteins to return in addition to the nodes in the existing network", 
			exampleStringValue = "10", 
			tooltip="", gravity=1.0)
	public int additionalNodes = 10;

	@Tunable (description="Type of interactors to expand network by", 
			longDescription = "Type of interactors to expand the network by, "
					+ "including STITCH compounds (default choice), proteins of the same species "
					+ "as the network's one or other species for which host-virus interactions are "
					+ "available. Proteins are specified by the species name, for example, "
					+ "'Homo Sapiens' for human proteins or 'Influenza A virus' for influenza A proteins.", 
			exampleStringValue = "Homo Sapiens", params="lookup=begins",
			gravity=2.0)
	public ListSingleSelection<String> nodeTypes = new ListSingleSelection<String>();
	
	@Tunable (description="Selectivity of interactors", tooltip="<html>"
			+ "The selectivity parameter provides a tradeoff between the specificity and <br />"
			+ "the confidence of new interactors. Low selectivity will retrieve more hub <br />"
			+ "proteins, which may have many high-confidence interactions to the current <br />"
			+ "network but also many other interactions. High selectivity will retrieve <br />"
			+ "proteins that primarily interact with the current network but with lower <br />"
			+ "confidence, since the higher-confidence hubs have been filtered out." 
			+ "</html>", 
			longDescription = "The selectivity parameter provides a tradeoff between the specificity "
					+ "and the confidence of new interactors. Low selectivity will retrieve more hub "
					+ "proteins, which may have many high-confidence interactions to the current "
					+ "network but also many other interactions. High selectivity will retrieve proteins "
					+ "that primarily interact with the current network but with lower confidence, "
					+ "since the higher-confidence hubs have been filtered out.", 
			exampleStringValue = "0.5", 
			params="slider=true", gravity=3.0)
	public BoundedDouble selectivityAlpha = new BoundedDouble(0.0, 0.5, 1.0, false, false);

	@Tunable(description="Is protein-compound network?", 
			exampleStringValue = "false", 
			gravity=4.0)
	 public boolean mixedTypes = false;
	
	@Tunable (description="Type of interactors to expand network on/from", 
			longDescription = "", 
			exampleStringValue = "proteins",
			dependsOn = "mixedTypes=true",
			gravity=5.0)
	public ListSingleSelection<String> nodeTypesSource = new ListSingleSelection<String>(Arrays.asList(mixedTypeProteins, mixedTypeCompounds));

	// @Tunable (description="Layout new nodes?", gravity=4.0)
	// public boolean relayout = true;

	//@Tunable (description="Expand from database", gravity=3.0, groups = "Advanced options", params = "displayState=collapsed")
	//public ListSingleSelection<String> databases = new ListSingleSelection<String>("string", "stitch");
	
	public ExpandNetworkTask(final StringManager manager, final CyNetwork network, CyNetworkView netView) {
		this.manager = manager;
		if (network != null)
			this.network = network;
		this.netView = netView;
		this.nodeView = null;
		// Make sure we have a network.  This should only happen at this point if we're coming in
		// via a command
		if (this.network == null)
			this.network = manager.getCurrentNetwork();

		if (this.network != null) {
			nodeTypes = new ListSingleSelection<String>(
					ModelUtils.getAvailableInteractionPartners(this.network));
			String netSpecies = ModelUtils.getNetSpecies(this.network);
			if (netSpecies != null) {
				nodeTypes.setSelectedValue(netSpecies);
			} else {
				nodeTypes.setSelectedValue(ModelUtils.COMPOUND);
			}
			nodeTypesSource.setSelectedValue(mixedTypeProteins);
			if (ModelUtils.getCompoundNodes(network).size() > 0 && ModelUtils.getProteinNodes(network).size() > 0) {
				mixedTypes = true;
			} else {
				mixedTypes = false;
			}
		}
	}

	public ExpandNetworkTask(final StringManager manager, final CyNetwork network, CyNetworkView netView, View<CyNode> nodeView) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;
		this.nodeView = nodeView;
		nodeTypes = new ListSingleSelection<String>(ModelUtils.getAvailableInteractionPartners(network));
		String netSpecies = ModelUtils.getNetSpecies(network);
		if (netSpecies != null) {
			nodeTypes.setSelectedValue(netSpecies);
		} else {
			nodeTypes.setSelectedValue(ModelUtils.COMPOUND);
		}
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Expand network");
		// check if we have a network
		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.WARN, "No network to expand");
			return;
		}
		
		if (!ModelUtils.isCurrentDataVersion(network)) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. Network appears to be an old STRING network.");
			// showError("Task cannot be performed. Network appears to be an old STRING network.");
			return;			
		}

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
		String existing = ModelUtils.getExisting(network).trim();
		String selected = ModelUtils.getSelected(network, nodeView, null).trim();
		// if mixed network, replace the existing and selected by either only proteins or only compounds
		if (mixedTypes) {
			if (nodeTypesSource.getSelectedValue().equals(mixedTypeProteins)) {
				existing = ModelUtils.getExistingProteins(network).trim();
				selected = ModelUtils.getSelected(network, nodeView, mixedTypeProteins).trim();
			} else {
				existing = ModelUtils.getExistingCompounds(network).trim();
				selected = ModelUtils.getSelected(network, nodeView, mixedTypeCompounds).trim();
			}
		}
		String species = ModelUtils.getNetSpecies(network);
		if (species == null) {
			species = ModelUtils.getMostCommonNetSpecies(network);
			ModelUtils.setNetSpecies(network, species);
		}
		String selectedType = nodeTypes.getSelectedValue();
		if (selectedType == null || selectedType.equals(ModelUtils.EMPTYLINE)) {
			monitor.showMessage(TaskMonitor.Level.WARN, "No node type to expand by");
			return;
		}

		Double conf = ModelUtils.getConfidence(network);
		String networkType = NetworkType.FUNCTIONAL.getAPIName();
		NetworkType currentType = NetworkType.getType(ModelUtils.getNetworkType(network));
		if (currentType != null)
			networkType = currentType.getAPIName();

		String database = ModelUtils.getDatabase(network);
		// int taxonId = Species.getSpeciesTaxId(species);
		Species selSpecies = Species.getSpecies(selectedType);
		String filterString = "";
		String useDatabase = "";
		Map<String, String> args = new HashMap<>();
		// if expand by compounds, go to jensenlab and ask for compounds
		// TODO: [move] expand still needs some testing
		// if expand by same species that is in STRING
		if (selectedType.equals(species) && !Species.isViral(selSpecies)) {
			// if the network is a stitch network and we expand on the compounds, go to Jensenlab
			// TODO: [move] can we avoid repeating the code here and in the else statement?
			if (database.equals(Databases.STITCH.getAPIName()) && nodeTypesSource.getSelectedValue().equals(mixedTypeCompounds)) {
				useDatabase = Databases.STITCH.getAPIName();
				filterString = String.valueOf(selSpecies.getTaxId());
				args.put("filter", filterString + ".%");
				args.put("existing",existing);
				args.put("score", conf.toString());
				args.put("database", networkType);
				args.put("alpha", selectivityAlpha.getValue().toString());
				if (additionalNodes > 0)
					args.put("additional", Integer.toString(additionalNodes));				
				if (selected != null && selected.length() > 0)
					args.put("selected",selected);
			} else {
				// otherwise go to STRING-DB to expand by proteins from the same species
				useDatabase = Databases.STRINGDB.getAPIName();
				args.put("species",species);
				args.put("existing_string_identifiers",existing.trim());
				// TODO: [move] is that how we should use selected with string?
				if (selected != null && selected.length() > 0)
					args.put("identifiers",selected);
				else 
					args.put("identifiers",existing);
				args.put("required_score",String.valueOf((int)(conf*1000)));
				args.put("network_type", networkType);
				args.put("custom_alpha", selectivityAlpha.getValue().toString());
				if (additionalNodes > 0)
					args.put("additional_network_nodes", Integer.toString(additionalNodes));				
			}
		} else if (selectedType.equals(ModelUtils.COMPOUND)) {
			useDatabase = Databases.STITCH.getAPIName();
			args.put("filter", "CIDm%");			
			args.put("existing",existing);
			args.put("score", conf.toString());
			args.put("database", networkType);
			args.put("alpha", selectivityAlpha.getValue().toString());
			if (additionalNodes > 0)
				args.put("additional", Integer.toString(additionalNodes));
			if (selected != null && selected.length() > 0)
				args.put("selected",selected);
		} else {
			useDatabase = Databases.STRING.getAPIName();
			filterString = String.valueOf(selSpecies.getTaxId());
			args.put("filter", filterString + ".%");
			args.put("existing",existing.trim());
			args.put("score", conf.toString());
			args.put("database", networkType);
			args.put("alpha", selectivityAlpha.getValue().toString());
			if (additionalNodes > 0)
				args.put("additional", Integer.toString(additionalNodes));
			if (selected != null && selected.length() > 0)
				args.put("selected",selected.trim());
		}  
		//else {
		//	monitor.showMessage(TaskMonitor.Level.WARN, "This Expand functionality is not implemented yet");
		//	return;
		//}
  		/*else {
			// if expand by other species, go to jensenlab and ask for more proteins of this species
			// TODO: [move]if expanding a cross-species network, go to STRING for the number of additional nodes of that species, then query Jensenlab for potential cross-species interactions
			// TODO: [move] test this more extensively
			// TODO: [move] if this is a stitch network (i.e. species == null), go to Jensenlab assuming we want to expand the compounds by proteins.
			useDatabase = Databases.STRING.getAPIName();
			filterString = String.valueOf(selSpecies.getTaxId());
			args.put("filter", filterString + ".%");
			args.put("existing",existing.trim());
			args.put("score", conf.toString());
			args.put("database", database);
			args.put("alpha", selectivityAlpha.getValue().toString());
			if (additionalNodes > 0)
				args.put("additional", Integer.toString(additionalNodes));
		}*/

		// String nodeType = nodeTypes.getSelectedValue().toLowerCase();

		JSONObject results;
		try {
			if (useDatabase.equals(Databases.STRINGDB.getAPIName())) {
				monitor.setStatusMessage("Getting additional nodes from: "+manager.getStringNetworkURL());
				results = HttpUtils.postJSON(manager.getStringNetworkURL(), args, manager);
			} else {
				monitor.setStatusMessage("Getting additional nodes from: "+manager.getNetworkURL());
				results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
			}
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}

		monitor.setStatusMessage("Augmenting network");

		// This may change...
		StringNetwork stringNet = manager.getStringNetwork(network);
		List<CyEdge> newEdges = new ArrayList<>();
		List<CyNode> newNodes = JSONUtils.augmentNetworkFromJSON(stringNet, network, newEdges, results, null, useDatabase, networkType);

		// TODO: [move] what do we do if the set of returned proteins/compounds is already the same as what exists in the network? 
		// example: expand an 11 protein network by compounds and after that expand it again by compounds based on the proteins in the network 
		if (newNodes.size() == 0 && newEdges.size() == 0) {
			if (conf == 1.0) { 
				monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results with a confidence larger than 1.0.<br> Consider changing the confidence threshold.");
				// manager.error("String returned no results with a confidence larger than 1.0.<br> Consider changing the confidence threshold.");
				// throw new RuntimeException("String returned no results with a confidence larger than 1.0. Consider changing the confidence threshold.");
				return;
			} else { 
				manager.error("STRING returned no results");
				throw new RuntimeException("This query will not add any new nodes or edges to the existing network.");
			}
			// SwingUtilities.invokeLater(new Runnable() {
			// public void run() {
			// JOptionPane.showMessageDialog(null,
			// "This query will not add any new nodes or edges to the existing network.",
			// "Warning", JOptionPane.WARNING_MESSAGE);
			// }
			// });
			// return;
		}

		// if we got back more than one node, we need to get some extra interactions
		// for example, if we asked for a different species, we need to also get the intra-species interactions of the proteins from this species
		// we might need to add interactions from either STRING-DB or Jensenlab depending on the setup and where we went first
		if (newNodes.size() > 1) {			
			// TODO: [move] do we need to ask for edges for all species in the network? even if we expanded only by one?
			if (!useDatabase.equals(Databases.STRINGDB.getAPIName()) && !Species.isViral(selSpecies) && !selectedType.equals(ModelUtils.COMPOUND)) {
				//if ((useDatabase.equals(Databases.STRING.getAPIName()) && !Species.isViral(selSpecies)) || (useDatabase.equals(Databases.STITCH.getAPIName()) && selectedType.equals(species))) {
				args.clear();
				args.put("required_score",String.valueOf((int)(conf*1000)));
				args.put("network_type", networkType);
				args.put("species", selSpecies.toString());
				args.put("identifiers", ModelUtils.getExisting(network, selSpecies.toString().trim()));
				results = null;
				try {
					results = HttpUtils.postJSON(manager.getStringNetworkURL(), args, manager);
				} catch (ConnectionException e) {
					monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
				}
				if (results != null) {
					JSONUtils.augmentNetworkFromJSON(manager.getStringNetwork(network), network, newEdges, results, null, Databases.STRINGDB.getAPIName(), networkType);
				}
			} else {
				// if ((useDatabase.equals(Databases.STRINGDB.getAPIName()) && (mixedTypes || !selectedType.equals(species))) || (useDatabase.equals(Databases.STITCH.getAPIName()))) {
				// if we went to STRING but the network contains more than one species/node type, we need to also get interactions from Jensenlab 
				args.clear();
				args.put("existing", ModelUtils.getExisting(network).trim());
				args.put("score", conf.toString());
				args.put("database", networkType);
				results = null;
				try {
					results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
				} catch (ConnectionException e) {
					monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
				}
				if (results != null) {
					JSONUtils.augmentNetworkFromJSON(manager.getStringNetwork(network), network, newEdges, results, null, Databases.STRING.getAPIName(), networkType);
				}
			}
		}
		
		monitor.setStatusMessage("Adding "+newNodes.size()+" nodes and "+newEdges.size()+" edges");

		// Finally, update any node information if we got any new protein nodes from either STRING or Jensenlab (except viral ones) 
		if (newNodes.size() > 0 && !Species.isViral(selSpecies) && (useDatabase.equals(Databases.STRINGDB.getAPIName()) || useDatabase.equals(Databases.STRING.getAPIName()))) {
			Map<String, CyNode> nodeMap = new HashMap<>();
			for (CyNode newNode: newNodes) {
				nodeMap.put(ModelUtils.getName(network, newNode), newNode);
			}

			String terms = "";
			for (String term: nodeMap.keySet()) {
				terms += term+"\n";
			}
			try {
				Map<String, List<Annotation>> annotations = stringNet.getAnnotations(stringNet.getManager(), selSpecies, terms, useDatabase, true);
				// TODO: [Custom] do we need to resolve or just take the first annotation or last one, which is currently the case...?
				for (String s: annotations.keySet()) {
					CyNode node = nodeMap.get(s);
					ModelUtils.updateNodeAttributes(network.getRow(node), annotations.get(s).get(0), false);
				}
			} catch (ConnectionException ce) {
				monitor.showMessage(TaskMonitor.Level.ERROR, "Unable to get additional node annotations");
			}
		}
	
		// we need to also retrieve the tissue/compartments scores for any new nodes coming from STRING
		if (newNodes.size() > 0 && useDatabase.equals(Databases.STRINGDB.getAPIName())) {
			args.clear();
			// we need to get all ids, not just the query ids 
			String ids = null;
			for (CyNode node: newNodes) {
				if (ids == null)
					ids = ModelUtils.getName(network, node);
				else
					ids += "\n"+ModelUtils.getName(network, node);
			}
			args.put("entities",ids.trim());
			args.put("caller_identity", StringManager.CallerIdentity);
			try {
				results = HttpUtils.postJSON(manager.getNodeInfoURL(), args, manager);
				JSONUtils.addExtraNodeData(stringNet, results);
			} catch (ConnectionException e) {
				// e.printStackTrace();
				monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			}
		}

		// If we have a view, re-apply the style and layout
		monitor.setStatusMessage("Updating style");
		// System.out.println("Updating style");
		if (netView != null)
			ViewUtils.updateEdgeStyle(manager, netView, newEdges);
		if (!selectedType.equals(species) && !selectedType.equals(ModelUtils.COMPOUND)) {
			ViewUtils.updateNodeColors(manager, network, netView, Arrays.asList(species, selectedType));
		}
		// System.out.println("Done");
		if (netView != null) {
			netView.updateView();
			monitor.setStatusMessage("Updating layout");
			// layoutAll();
			// experimental, layout only the new nodes
			shiftAndLayoutGridSelectedOnly(newNodes);
		}
		// reset filters in the results panel
		manager.reinitResultsPanel(network);
	}

	
	private void layoutAll() {
		CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
		Object context = alg.createLayoutContext();
		TunableSetter setter = manager.getService(TunableSetter.class);
		Map<String, Object> layoutArgs = new HashMap<>();
		layoutArgs.put("defaultNodeMass", 10.0);
		setter.applyTunables(context, layoutArgs);
		Set<View<CyNode>> nodeViews = new HashSet<>(netView.getNodeViews());
		insertTasksAfterCurrentTask(alg.createTaskIterator(netView, context, nodeViews, "score"));
	}
	

	private void layoutSelectedOnly(List<CyNode> nodesToLayout) {
		Set<View<CyNode>> nodeViews = new HashSet<>();
		for (View<CyNode> nodeView : netView.getNodeViews()) {
			if (nodesToLayout.contains(nodeView.getModel())) {
				nodeViews.add(nodeView);
			}
		}
		// get layout and set attributes
		CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("force-directed");
		Object context = alg.createLayoutContext();
		TunableSetter setter = manager.getService(TunableSetter.class);
		Map<String, Object> layoutArgs = new HashMap<>();
		layoutArgs.put("defaultNodeMass", 5.0);
		layoutArgs.put("selectedOnly", true);
		setter.applyTunables(context, layoutArgs);
		insertTasksAfterCurrentTask(alg.createTaskIterator(netView, context, nodeViews, "score"));
	}

	private void shiftAndLayoutGridSelectedOnly(List<CyNode> nodesToLayout) {
		final VisualProperty<Double> xLoc = BasicVisualLexicon.NODE_X_LOCATION;
		final VisualProperty<Double> yLoc = BasicVisualLexicon.NODE_Y_LOCATION;
		Set<Double> xPos = new HashSet<Double>();
		Set<Double> yPos = new HashSet<Double>();
		Set<View<CyNode>> nodeViews = new HashSet<>();
		for (View<CyNode> nodeView : netView.getNodeViews()) {
			if (nodesToLayout.contains(nodeView.getModel())) {
				nodeViews.add(nodeView);
			} else {
				xPos.add(nodeView.getVisualProperty(xLoc));
				yPos.add(nodeView.getVisualProperty(yLoc));
			}
		}
		double xMin = Collections.min(xPos);
		double xMax = Collections.max(xPos);
		double xSpan = xMax - xMin; 
		double scaling = netView.getNodeViews().size()/(double)nodeViews.size();
		for (View<CyNode> nodeView2 : nodeViews) {
			nodeView2.setVisualProperty(xLoc, xMax + xSpan/scaling);
			network.getRow(nodeView2.getModel()).set(CyNetwork.SELECTED, true);
		}

		// get layout and set attributes
		CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("grid");
		Object context = alg.createLayoutContext();
		TunableSetter setter = manager.getService(TunableSetter.class);
		Map<String, Object> layoutArgs = new HashMap<>();
		layoutArgs.put("selectedOnly", true);
		setter.applyTunables(context, layoutArgs);
		insertTasksAfterCurrentTask(alg.createTaskIterator(netView, context, nodeViews, "score"));
	}



	private void layoutSelectedCircular(List<CyNode> nodesToLayout) {
		final VisualProperty<Double> xLoc = BasicVisualLexicon.NODE_X_LOCATION;
		final VisualProperty<Double> yLoc = BasicVisualLexicon.NODE_Y_LOCATION;
		Set<Double> xPos = new HashSet<Double>();
		Set<Double> yPos = new HashSet<Double>();
		Set<View<CyNode>> nodeViews = new HashSet<>();
		for (View<CyNode> nodeView : netView.getNodeViews()) {
			if (nodesToLayout.contains(nodeView.getModel())) {
				nodeViews.add(nodeView);
			} else {
				xPos.add(nodeView.getVisualProperty(xLoc));
				yPos.add(nodeView.getVisualProperty(yLoc));
			}
		}
		double xSpan = Math.abs(Collections.max(xPos)) + Math.abs(Collections.min(xPos));
		// System.out.println(xSpan);
		double ySpan = Math.abs(Collections.max(yPos)) + Math.abs(Collections.min(yPos));
		// System.out.println(ySpan);
		int spacing = (int)Math.max(xSpan, ySpan)/4;
		// System.out.println(spacing);
		// get layout and set attributes
		CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class).getLayout("circular");
		Object context = alg.createLayoutContext();
		TunableSetter setter = manager.getService(TunableSetter.class);
		Map<String, Object> layoutArgs = new HashMap<>();
		layoutArgs.put("defaultNodeMass", 10.0);
		layoutArgs.put("selectedOnly", true);
		layoutArgs.put("spacing", spacing);
		setter.applyTunables(context, layoutArgs);
		// Set<View<CyNode>> nodeViews = new HashSet<>(netView.getNodeViews());
		insertTasksAfterCurrentTask(alg.createTaskIterator(netView, context, nodeViews, ColumnNames.SCORE));
	}
		
	@ProvidesTitle
	public String getTitle() {
		return "Expand Network";
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		return StringResults.getResults(clzz, network);
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return StringResults.getResultClasses();
	}

}
