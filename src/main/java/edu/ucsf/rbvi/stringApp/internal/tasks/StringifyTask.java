package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.Dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.ui.GetTermsPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchOptionsPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringifyTask extends AbstractTask implements ObservableTask, TaskObserver {
	final StringManager manager;
	private StringNetwork stringNetwork;
	private CyNetwork net;
	private CyNetwork loadedNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);
	private Map<String, CyNode> nodeMap;

	@Tunable(description="Network to set as a STRING network", 
	         longDescription=StringToModel.CY_NETWORK_LONG_DESCRIPTION,
	         exampleStringValue=StringToModel.CY_NETWORK_EXAMPLE_STRING,
	         context="nogui", required=true)
	public CyNetwork network = null;

	@Tunable(description="Column to use for STRING query", 
	         longDescription="Select the column to use to query for STRING nodes",
	         exampleStringValue="name",
	         context="gui", required=true)
	public ListSingleSelection<CyColumn> tableColumn = null;

	@Tunable(description="Column to use for STRING query", 
	         longDescription="Select the column to use to query for STRING nodes",
	         exampleStringValue="name",
	         context="nogui", required=true)
	public String column = null;

	@Tunable(description="Species for the query", 
	         longDescription="Species to use for the query",
	         exampleStringValue="name",
	         required=true)
	public ListSingleSelection<Species> species;

	public StringifyTask(final StringManager manager, final CyNetwork net) {
		this.manager = manager;
		this.net = net;
		species = new ListSingleSelection<Species>(Species.getSpecies());
		species.setSelectedValue(Species.getSpecies("Homo sapiens"));
		if (net != null) {
			List<CyColumn> colList = new ArrayList<>(net.getDefaultNodeTable().getColumns());
			tableColumn = new ListSingleSelection<CyColumn>(colList);
			tableColumn.setSelectedValue(net.getDefaultNodeTable().getColumn("name"));
		} else {
			tableColumn = null;
		}
		nodeMap = new HashMap<>();
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Stringify network");

		if (network != null)
			net = network;
		else if (net == null) {
			net = manager.getService(CyApplicationManager.class).getCurrentNetwork();
		}

		CyColumn col = null;
		if (tableColumn != null)
			col = tableColumn.getSelectedValue();

		if (tableColumn == null) {
			if (column == null)
				column = "name";

			col = net.getDefaultNodeTable().getColumn(column);
		}

		List<String> stringList = col.getValues(String.class);
		column = col.getName();

		String terms = ModelUtils.listToString(stringList);

		// We want the query with newlines, so we need to convert
		terms = terms.replace(",", "\n");
		// Now, strip off any blank lines
		terms = terms.replaceAll("(?m)^\\s*", "");

		// Get the network
		stringNetwork = new StringNetwork(manager);
		int taxon = species.getSelectedValue().getTaxId();

		// Are we command or GUI based?
		if (tableColumn != null) {
			optionsPanel = new SearchOptionsPanel(manager);
			// GUI based
			TaskIterator ti = 
						new TaskIterator(new GetAnnotationsTask(stringNetwork, taxon, terms, Databases.STRING.getAPIName()));
			manager.execute(ti, this);
			return;
		}

		// Get the annotations
		Map<String, List<Annotation>> annotations = 
			stringNetwork.getAnnotations(taxon, terms, Databases.STRING.getAPIName(), false);

		if (annotations == null || annotations.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR,
					"Query '" + trunc(terms) + "' returned no results");
			throw new RuntimeException("Query '"+trunc(terms)+"' returned no results");
		}

		boolean resolved = stringNetwork.resolveAnnotations();
		if (!resolved) {
			// Resolve the annotations by choosing the first stringID for each
			for (String term : annotations.keySet()) {
				stringNetwork.addResolvedStringID(term, annotations.get(term).get(0).getStringId());
			}
		}

		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = stringNetwork.combineIds(queryTermMap);
		LoadInteractions load = 
				new LoadInteractions(stringNetwork, species.toString(), taxon, 
				                     100, 0, stringIds, queryTermMap, "", Databases.STRING.getAPIName());
		manager.execute(new TaskIterator(load), true);
		loadedNetwork = stringNetwork.getNetwork();
		if (loadedNetwork == null) {
			throw new RuntimeException("Query '"+terms+"' returned no results");
		}

		// Get all of the nodes in the network
		createNodeMap();

		copyEdges();
		copyNodeAttributes();

		copyNodePositions(net, loadedNetwork);
	}

	private void copyEdges() {
		System.out.println("copyEdges");
		createColumns(net.getDefaultEdgeTable(), loadedNetwork.getDefaultEdgeTable());
		List<CyEdge> edgeList = net.getEdgeList();
		for (CyEdge edge: edgeList) {
			CyNode sourceNode = edge.getSource();
			CyNode targetNode = edge.getTarget();
			boolean isDirected = edge.isDirected();

			String source = net.getRow(sourceNode).get(column, String.class);
			String target = net.getRow(targetNode).get(column, String.class);

			CyNode newSource = nodeMap.get(source);
			CyNode newTarget = nodeMap.get(target);

			CyEdge newEdge = loadedNetwork.addEdge(newSource, newTarget, isDirected);
			copyRow(net.getDefaultEdgeTable(), loadedNetwork.getDefaultEdgeTable(), edge, newEdge);
		}
	}

	private void copyNodeAttributes() {
		System.out.println("copyNodeAttributes");
		createColumns(net.getDefaultNodeTable(), loadedNetwork.getDefaultNodeTable());
		for (CyNode node: net.getNodeList()) {
			String nodeKey = net.getRow(node).get(column, String.class);
			CyNode newNode = nodeMap.get(nodeKey);
			copyRow(net.getDefaultNodeTable(), loadedNetwork.getDefaultNodeTable(), node, newNode);
		}
	}

	private void copyNodePositions(CyNetwork from, CyNetwork to) {
		CyNetworkView fromView = getNetworkView(from);
		CyNetworkView toView = getNetworkView(to);
		for (View<CyNode> nodeView: fromView.getNodeViews()) {
			// Get the to node
			String nodeKey = from.getRow(nodeView.getModel()).get(column, String.class);
			View<CyNode> toNodeView = toView.getNodeView(nodeMap.get(nodeKey));
			// Copy over the positions
			Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
			Double z = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION);
			toNodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
			toNodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			if (z != null && z != 0.0)
				toNodeView.setVisualProperty(BasicVisualLexicon.NODE_Z_LOCATION, z);
		}
	}

	private CyNetworkView getNetworkView(CyNetwork network) {
		Collection<CyNetworkView> views = manager.getService(CyNetworkViewManager.class).getNetworkViews(network);

		// At some point, figure out a better way to do this
		for (CyNetworkView view: views) {
			return view;
		}
		return null;
	}

	private void createColumns(CyTable fromTable, CyTable toTable) {
		for (CyColumn col: fromTable.getColumns()) {
			String fqn = col.getName();
			// Does that column already exist in our target?
			if (toTable.getColumn(fqn) == null) {
				// No, create it.
				if (col.getType().equals(List.class)) {
					// There is no easy way to handle this, unfortunately...
					// toTable.createListColumn(fqn, col.getListElementType(), col.isImmutable(), (List<?>)col.getDefaultValue());
					if (col.getListElementType().equals(String.class))
						toTable.createListColumn(fqn, String.class, col.isImmutable(), (List<String>)col.getDefaultValue());
					else if (col.getListElementType().equals(Long.class))
						toTable.createListColumn(fqn, Long.class, col.isImmutable(), (List<Long>)col.getDefaultValue());
					else if (col.getListElementType().equals(Double.class))
						toTable.createListColumn(fqn, Double.class, col.isImmutable(), (List<Double>)col.getDefaultValue());
					else if (col.getListElementType().equals(Integer.class))
						toTable.createListColumn(fqn, Integer.class, col.isImmutable(), (List<Integer>)col.getDefaultValue());
					else if (col.getListElementType().equals(Boolean.class))
						toTable.createListColumn(fqn, Boolean.class, col.isImmutable(), (List<Boolean>)col.getDefaultValue());
				} else {
					toTable.createColumn(fqn, col.getType(), col.isImmutable(), col.getDefaultValue());
				}
			}
		}
	}

	private void copyRow(CyTable fromTable, CyTable toTable, CyIdentifiable from, CyIdentifiable to) {
		for (CyColumn col: fromTable.getColumns()) {
			if (col.getName().equals(CyNetwork.SUID)) 
				continue;
			if (col.getName().equals(CyNetwork.NAME)) 
				continue;
			if (col.getName().equals(CyNetwork.SELECTED)) 
				continue;
			if (col.getName().equals(CyRootNetwork.SHARED_NAME)) 
				continue;
			if (from.getClass().equals(CyEdge.class) && col.getName().equals(CyRootNetwork.SHARED_INTERACTION)) 
				continue;
			if (from.getClass().equals(CyEdge.class) && col.getName().equals(CyEdge.INTERACTION)) 
				continue;

			Object v = fromTable.getRow(from.getSUID()).getRaw(col.getName());
			toTable.getRow(to.getSUID()).set(col.getName(), v);
		}
	}

	private String trunc(String str) {
		if (str.length() > 1000)
			return str.substring(0,1000)+"...";
		return str;
	}

	private void createNodeMap() {
		// Get all of the nodes in the network
		for (CyNode node: loadedNetwork.getNodeList()) {
			String key = loadedNetwork.getRow(node).get("query term", String.class);
			nodeMap.put(key, node);
		}
	}

	@Override
	public void taskFinished(ObservableTask task) {
		System.out.println("Task: "+task);
		if (!(task instanceof GetAnnotationsTask)) {
			return;
		}

		GetAnnotationsTask annTask = (GetAnnotationsTask)task;

		final int taxon = annTask.getTaxon();
		if (stringNetwork.getAnnotations() == null || stringNetwork.getAnnotations().size() == 0) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, "Your query returned no results",
								                        "No results", JOptionPane.ERROR_MESSAGE); 
				}
			});
			return;
		}
		boolean noAmbiguity = stringNetwork.resolveAnnotations();
		if (noAmbiguity) {
			int additionalNodes = 0;

			final int addNodes = additionalNodes;

			System.out.println("Calling importNetwork");
			importNetwork(taxon, 100, 0);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JDialog d = new JDialog();
					d.setTitle("Resolve Ambiguous Terms");
					d.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
					// GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, Databases.STRING.getAPIName(), 
					//                                         getSpecies(), false, getConfidence(), getAdditionalNodes());
					GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, 
					                                        Databases.STRING.getAPIName(), false, optionsPanel);
					panel.createResolutionPanel();
					d.setContentPane(panel);
					d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					d.pack();
					d.setVisible(true);
				}
			});
		}

		System.out.println("Creating the node map");
		createNodeMap();

		copyEdges();
		copyNodeAttributes();

		copyNodePositions(net, loadedNetwork);
	}

	public String getSpecies() {
		// This will eventually come from the OptionsComponent...
		if (optionsPanel.getSpecies() != null)
			return optionsPanel.getSpecies().toString();
		return "Homo sapiens"; // Homo sapiens
	}

	void importNetwork(int taxon, int confidence, int additionalNodes) {
		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = stringNetwork.combineIds(queryTermMap);
		TaskFactory factory = new ImportNetworkTaskFactory(stringNetwork, getSpecies(), 
		                                                   taxon, 100, 0, stringIds,
		                                                   queryTermMap, Databases.STRING.getAPIName());
		if (optionsPanel.getLoadEnrichment())
			manager.execute(factory.createTaskIterator(), this, true);
		else
			manager.execute(factory.createTaskIterator(), this, true);
		loadedNetwork = stringNetwork.getNetwork();
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
	}


	@Override
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(CyNetwork.class)) {
			return (R) net;
		} else if (clzz.equals(Long.class)) {
			if (net == null)
				return null;
			return (R) net.getSUID();
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				if (net == null) return "{}";
				else return "{\"network\": "+net.getSUID()+"}";
      };
      return (R)res;
		} else if (clzz.equals(String.class)) {
			if (net == null)
				return (R) "No network was set";
			String resp = "Set network '"
					+ net.getRow(net).get(CyNetwork.NAME, String.class);
			resp += " as STRING network";
			return (R) resp;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class, Long.class, CyNetwork.class);
	}

}
