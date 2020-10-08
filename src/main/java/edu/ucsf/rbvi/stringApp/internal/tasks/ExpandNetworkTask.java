package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
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
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.StringResults;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ExpandNetworkTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	CyNetworkView netView;
	View<CyNode> nodeView;
	
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
			exampleStringValue = "Homo Sapiens", 
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
		String existing = ModelUtils.getExisting(network);
		String selected = ModelUtils.getSelected(network, nodeView);
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
		// int taxonId = Species.getSpeciesTaxId(species);
		int taxonId = Species.getSpeciesTaxId(selectedType);
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
		// String nodeType = nodeTypes.getSelectedValue().toLowerCase();
		args.put("alpha", selectivityAlpha.getValue().toString());
		String useDatabase = "";
		if (selectedType.equals(ModelUtils.COMPOUND)) {
			useDatabase = Databases.STITCH.getAPIName();
			args.put("filter", "CIDm%%");			
		} else {
			useDatabase = Databases.STRING.getAPIName();
			if (taxonId != -1) 
				args.put("filter", taxonId + ".%%");
		}
		// TODO: Is it OK to always use stitch?
		args.put("database", Databases.STITCH.getAPIName());
		monitor.setStatusMessage("Getting additional nodes from: "+manager.getNetworkURL());

		JSONObject results;
		try {
			results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}

		monitor.setStatusMessage("Augmenting network");

		// This may change...
		List<CyEdge> newEdges = new ArrayList<>();
		List<CyNode> newNodes = ModelUtils.augmentNetworkFromJSON(manager, network, newEdges, results, null, useDatabase);

		if (newNodes.size() == 0 && newEdges.size() == 0) {
			if (conf == 1.0) { 
				monitor.showMessage(TaskMonitor.Level.ERROR,"String returned no results with a confidence larger than 1.0.<br> Consider changing the confidence threshold.");
				// manager.error("String returned no results with a confidence larger than 1.0.<br> Consider changing the confidence threshold.");
				// throw new RuntimeException("String returned no results with a confidence larger than 1.0. Consider changing the confidence threshold.");
				return;
			} else { 
				manager.error("String returned no results");
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
		monitor.setStatusMessage("Adding "+newNodes.size()+" nodes and "+newEdges.size()+" edges");

		// If we have a view, re-apply the style and layout
		monitor.setStatusMessage("Updating style");
		// System.out.println("Updating style");
		if (netView != null)
			ViewUtils.updateEdgeStyle(manager, netView, newEdges);
		if (!selectedType.equals(species) && !selectedType.equals(ModelUtils.COMPOUND)) {
			ViewUtils.updateNodeColors(manager, network, netView, true);
		}
		// System.out.println("Done");
		if (netView != null) {
			netView.updateView();
			monitor.setStatusMessage("Updating layout");
			// layoutAll();
			// experimental, layout only the new nodes
			shiftAndLayoutGridSelectedOnly(newNodes);
		}
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
		insertTasksAfterCurrentTask(alg.createTaskIterator(netView, context, nodeViews, "score"));
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
