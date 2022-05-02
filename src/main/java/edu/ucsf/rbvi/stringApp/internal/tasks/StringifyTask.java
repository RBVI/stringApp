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
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
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
	private String netName;
	private String useDatabase;
	private CyNetwork loadedNetwork = null;
	private int additionalNodes = 0;
	private SearchOptionsPanel optionsPanel = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);
	private final Map<String, CyNode> nodeMap;
	private TaskMonitor monitor;

	@Tunable(description="Network to set as a STRING network", 
	         longDescription=StringToModel.CY_NETWORK_LONG_DESCRIPTION,
	         exampleStringValue=StringToModel.CY_NETWORK_EXAMPLE_STRING,
	         context="nogui", required=true)
	public CyNetwork networkNoGui = null;

	@Tunable(description="Column to use for STRING query", 
	         longDescription="Select the column to use to query for STRING nodes.",
	         exampleStringValue="name",
	         context="gui", required=true)
	public ListSingleSelection<CyColumn> tableColumn = null;

	@Tunable(description="Include unmappable nodes", 
	         longDescription="Option for choosing whether nodes that cannot be mapped to "
	         		+ "STRING identifiers should be included in the new network or not.",
	         exampleStringValue="true")
	public boolean includeNotMapped = true;

	@Tunable(description="Map nodes to compounds", 
	         longDescription="Option for considering compounds when resolving the node "
	         		+ "identifiers and consequently querying STITCH instead of STRING.",
	         exampleStringValue="false")
	public boolean compoundQuery = false;

	@Tunable(description="Column to use for STRING query", 
	         longDescription="Select the column to use to query for STRING nodes",
	         exampleStringValue="name",
	         context="nogui", required=true)
	public String column = null;

	@Tunable(description="Species for the query", 
	         longDescription="Species to use for the query.",
	         exampleStringValue="name",
	         params="lookup=begins",
	         required=true)
	public ListSingleSelection<Species> species;

	@Tunable(description = "Confidence cutoff",
	         longDescription="The confidence score reflects the cumulated evidence that this "+
					                 "interaction exists.  Only interactions with scores greater than "+
													 "this cutoff will be returned.",
	         exampleStringValue="0.4",
	         context="nogui")
	public BoundedDouble cutoff = new BoundedDouble(0.0, 1.0, 1.0, false, false);

	@Tunable(description = "Network type",
	         longDescription="Type of the STRING interactions (edges) to be included in the network, either functional associations or physical interactions.",
	         exampleStringValue="full STRING network",
	         context="nogui")
	public ListSingleSelection<NetworkType> networkType = new ListSingleSelection<>(NetworkType.values());

	
	public StringifyTask(final StringManager manager, final CyNetwork net) {
		this.manager = manager;
		this.net = net;
		this.netName = "";
		this.useDatabase = Databases.STRING.getAPIName();
		species = new ListSingleSelection<Species>(Species.getGUISpecies());
		species.setSelectedValue(Species.getHumanSpecies());
		if (net != null) {
			List<CyColumn> colList = new ArrayList<>(net.getDefaultNodeTable().getColumns());
			tableColumn = new ListSingleSelection<CyColumn>(colList);
			tableColumn.setSelectedValue(net.getDefaultNodeTable().getColumn("name"));
		} else {
			tableColumn = null;
		}
		networkType.setSelectedValue(manager.getDefaultNetworkType());
		nodeMap = new HashMap<>();
	}

	public StringifyTask(final StringManager manager, final CyNetwork net, double confidence, Species sp, String nodeColumn, NetworkType type) {
		this.manager = manager;
		this.net = net;
		this.netName = "";
		this.useDatabase = Databases.STRING.getAPIName();
		species = new ListSingleSelection<Species>(Species.getGUISpecies());
		species.setSelectedValue(sp);
		if (net != null) {
			List<CyColumn> colList = new ArrayList<>(net.getDefaultNodeTable().getColumns());
			tableColumn = new ListSingleSelection<CyColumn>(colList);
			tableColumn.setSelectedValue(net.getDefaultNodeTable().getColumn(nodeColumn));
		} else {
			tableColumn = null;
		}
		cutoff.setValue(confidence);
		networkType.setSelectedValue(type);
		nodeMap = new HashMap<>();
	}

	public void run(TaskMonitor monitor) {
		this.monitor = monitor;
		monitor.setTitle("Stringify network");

		if (networkNoGui != null) {
			net = networkNoGui;
			tableColumn = null;
		} else if (net == null) {
			net = manager.getService(CyApplicationManager.class).getCurrentNetwork();
		}

		// Do a little sanity checking
		if (net == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "No network specified");
			return;
		}

		if (ModelUtils.isStringNetwork(net) && ModelUtils.isCurrentDataVersion(net))  {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Network '"+net+"' is already a STRING network");
			return;
		}

		netName = net.getDefaultNetworkTable().getRow(net.getSUID()).get(CyNetwork.NAME, String.class);
		
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

		// Set to STICH network
		if (compoundQuery) useDatabase = Databases.STITCH.getAPIName();
		
		// Are we command or GUI based?
		if (tableColumn != null) {
			optionsPanel = new SearchOptionsPanel(manager);
			optionsPanel.setConfidence((int)(cutoff.getValue()*100));
			optionsPanel.setNetworkType(networkType.getSelectedValue());
			optionsPanel.setAdditionalNodes(additionalNodes);
			optionsPanel.setSpecies(species.getSelectedValue());

			// GUI based
			TaskIterator ti = new TaskIterator(new GetAnnotationsTask(stringNetwork, taxon, terms, useDatabase));
			manager.execute(ti, this);
			return;
		}

		// Get the annotations
		Map<String, List<Annotation>> annotations;
		try {
			annotations = stringNetwork.getAnnotations(manager, taxon, terms, useDatabase, false);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(TaskMonitor.Level.ERROR,
					"Cannot connect to " + useDatabase);
			throw new RuntimeException("Cannot connect to " + useDatabase);
		}

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
						(int)(cutoff.getValue()*100), additionalNodes, stringIds, queryTermMap, netName, useDatabase, networkType.getSelectedValue());
		manager.execute(new TaskIterator(load), true);
		loadedNetwork = stringNetwork.getNetwork();
		if (loadedNetwork == null) {
			throw new RuntimeException("Query '"+terms+"' returned no results");
		}

		CopyTask copyTask = new CopyTask(manager, column, net, stringNetwork, includeNotMapped);
		copyTask.run(monitor);
	}

	private String trunc(String str) {
		if (str.length() > 1000)
			return str.substring(0,1000)+"...";
		return str;
	}

	@Override
	public void taskFinished(ObservableTask task) {
		if (!(task instanceof GetAnnotationsTask)) {
			return;
		}

		GetAnnotationsTask annTask = (GetAnnotationsTask)task;

		final int taxon = annTask.getTaxon();
		if (stringNetwork.getAnnotations() == null || stringNetwork.getAnnotations().size() == 0) {
			if (annTask.getErrorMessage() != "") {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null,
								"<html>Your query returned no results due to an error. <br />"
										+ annTask.getErrorMessage() + "</html>",
								"No results", JOptionPane.ERROR_MESSAGE);
					}
				});					
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, "Your query returned no results",
								"No results", JOptionPane.ERROR_MESSAGE);
					}
				});
			}
			return;
		}
		boolean noAmbiguity = stringNetwork.resolveAnnotations();
		if (noAmbiguity) {
			// System.out.println("Calling importNetwork");
			importNetwork(taxon, (int)(cutoff.getValue()*100), additionalNodes, useDatabase, networkType.getSelectedValue());

			// Creating the copyTask
			CopyTask copyTask = new CopyTask(manager, column, net, stringNetwork, includeNotMapped);
			copyTask.run(monitor);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JDialog d = new JDialog();
					d.setTitle("Resolve Ambiguous Terms");
					d.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
					// GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, Databases.STRING.getAPIName(), 
					//                                         getSpecies(), false, getConfidence(), getAdditionalNodes());
					CopyTask copyTask = new CopyTask(manager, column, net, stringNetwork, includeNotMapped);
					GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, 
					                                        useDatabase, false, 
					                                        optionsPanel, netName, copyTask);
					panel.createResolutionPanel();
					d.setContentPane(panel);
					d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					d.pack();
					d.setVisible(true);
				}
			});
		}

	}

	public String getSpecies() {
		// This will eventually come from the OptionsComponent...
		if (optionsPanel.getSpecies() != null)
			return optionsPanel.getSpecies().toString();
		return "Homo sapiens"; // Homo sapiens
	}

	void importNetwork(int taxon, int confidence, int additionalNodes, String useDatabase, NetworkType netType) {
		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = stringNetwork.combineIds(queryTermMap);
		TaskFactory factory = new ImportNetworkTaskFactory(stringNetwork, getSpecies(), 
		                                                   taxon, confidence, additionalNodes, stringIds,
		                                                   queryTermMap, netName, useDatabase, netType);
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

	private class CopyTask extends AbstractTask {
		String column;
		CyNetwork network;
		StringNetwork stringNetwork;
		StringManager manager;
		boolean copyNotMappedNodes;

		CopyTask(StringManager manager, String col, CyNetwork network, StringNetwork stringNetwork, boolean includeNotMapped) {
			this.manager = manager;
			this.column = col;
			this.network = network;
			this.stringNetwork = stringNetwork;
			this.copyNotMappedNodes = includeNotMapped;
		}

		public void run(TaskMonitor monitor) {
			CyNetwork loadedNetwork = stringNetwork.getNetwork();

			// Get all of the nodes in the network
			ModelUtils.createNodeMap(loadedNetwork, nodeMap, ModelUtils.QUERYTERM);

			List<String> cols = new ArrayList<String>();
			cols.add(ModelUtils.QUERYTERM);
			cols.add(ModelUtils.DISPLAY);

			// Copy over any missing nodes that we didn't find in STRING
			// column is the node attribute column chosen by the user to be used as IDs
			// also, for all nodes to be copied, set query term and display column to the ID value chosen by the user  
			if (copyNotMappedNodes)
				ModelUtils.copyNodes(network, loadedNetwork, nodeMap, column, cols);

			// TODO: think about that once more
			// we could also check for string network -> !ModelUtils.isStringNetwork(net) 
			if (cutoff.getValue() == 1.0)
				ModelUtils.copyEdges(network, loadedNetwork, nodeMap, column);

			ModelUtils.copyNodeAttributes(network, loadedNetwork, nodeMap, column);

			ModelUtils.copyNodePositions(manager, network, loadedNetwork, nodeMap, column);
		}

	}

}
