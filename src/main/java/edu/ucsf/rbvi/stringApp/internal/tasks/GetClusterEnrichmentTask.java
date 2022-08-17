package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.command.StringToModel;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.task.analyze.AnalyzeNetworkCollectionTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.EnrichmentSAXHandler;
import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetClusterEnrichmentTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	final CyNetwork network;
	final CyNetworkView netView;
	final boolean publOnly;
	Map<String, List<EnrichmentTerm>> enrichmentResult;
	final Map<String, Long> stringNodesMap;
	final Map<String, CyNetwork> stringNetworkMap;
	final ShowEnrichmentPanelTaskFactory showFactoryEnrich;
	final ShowPublicationsPanelTaskFactory showFactoryPubl;
	List<CyNode> analyzedNodes;
	TaskMonitor monitor;
	private List<CyTable> enrichmentTables = new ArrayList<CyTable>();
	// private static int limitUniqueAttributes = 50; 

	// [N] limit this to the top X with a default -> done, list of values is sorted and only the top X groups are considered
	// [N] have that as an advanced option -> advanced together with a bunch of others
	// [N] check if the number of unique values == number of values and don't show such columns -> 
	// [N] take suggested column name from network attribute __clusterSomething -> done
		
	@Tunable(description = "Column for groups", gravity = 2.0)
	public ListSingleSelection<CyColumn> groupColumn = new ListSingleSelection<CyColumn>();

	@Tunable(description = "Retrieve for species", gravity = 3.0)
	public ListSingleSelection<String> allNetSpecies = new ListSingleSelection<String>();
	
	@Tunable(description = "Number of groups ", groups={"Advanced"}, params="displayState=collapsed", gravity = 5.0)
	public int limitGroupNumber = 12;	

	@Tunable(description = "Group size limit", groups={"Advanced"}, params="displayState=collapsed", gravity = 6.0)
	public int limitGroupSize = 4;	
	
	@Tunable(description="Network to be used as background", groups={"Advanced"}, params="displayState=collapsed", gravity = 7.0
			// longDescription = StringToModel.CY_NETWORK_VIEW_LONG_DESCRIPTION,
			// exampleStringValue = StringToModel.CY_NETWORK_VIEW_EXAMPLE_STRING,
		        )
		public ListSingleSelection<String> background = null;

	public GetClusterEnrichmentTask(StringManager manager, CyNetwork network, CyNetworkView netView,
			ShowEnrichmentPanelTaskFactory showEnrichmentFactory, ShowPublicationsPanelTaskFactory showFactoryPubl, boolean publOnly) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;

		this.showFactoryEnrich = showEnrichmentFactory;
		this.showFactoryPubl = showFactoryPubl;
		this.publOnly = publOnly;
		enrichmentResult = new HashMap<>();
		stringNodesMap = new HashMap<>();
		monitor = null;
		initGroupColumn();
		allNetSpecies = new ListSingleSelection<String>(ModelUtils.getEnrichmentNetSpecies(network));

		stringNetworkMap = new HashMap<>();

		List<String> netList = new ArrayList<>();
		netList.add("genome");
		for (StringNetwork sn: manager.getStringNetworks()) {
			CyNetwork net = sn.getNetwork();
			String name = ModelUtils.getName(net, net);
			netList.add(name);
			stringNetworkMap.put(name, net);
		}

		background = new ListSingleSelection<String>(netList);
		// genome is always the default
		background.setSelectedValue("genome");

	}

	public void run(TaskMonitor monitor) throws Exception {
		this.monitor = monitor;
		monitor.setTitle(this.getTitle());

		if (!ModelUtils.isCurrentDataVersion(network)) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. Network appears to be an old STRING network.");
			return;			
		}
		
		// get background nodes
		String bgNodes = null;
		if (!background.getSelectedValue().equals("genome")) {
			bgNodes = getBackground(stringNetworkMap.get(background.getSelectedValue()), network);
			if (bgNodes.equals("")) {
				monitor.showMessage(Level.ERROR,
						"Task cannot be performed. Nodes from the foreground are missing in the background.");
				showError("Task cannot be performed. Nodes from the foreground are missing in the background.");
				return;
			}
		}

		// get species
		String species = String.valueOf(Species.getSpeciesTaxId(allNetSpecies.getSelectedValue()));

		// map of STRING ID to CyNodes
		CyTable nodeTable = network.getDefaultNodeTable();
		for (final CyNode node : network.getNodeList()) {
			if (nodeTable.getColumn(ModelUtils.STRINGID) != null) {
				String stringid = nodeTable.getRow(node.getSUID()).get(ModelUtils.STRINGID,
						String.class);
				if (stringid != null) {
					stringNodesMap.put(stringid, node.getSUID());
				}
			}
		}

		// get groups 
		CyColumn colGroups = groupColumn.getSelectedValue();
		Class<?> colGroupClass = colGroups.getType();
		// TODO: [N] decide if this is the best sorting, especially given that we will only do enrichment for the top x groups. Should we sort them by size or by name?
		Set<String> groups = new TreeSet<String>(); 
		if (colGroupClass.equals(String.class)) {
			groups.addAll(colGroups.getValues(String.class));
		} else if (colGroupClass.equals(Integer.class)) {
			Set<Integer> colValuesInt = new HashSet<Integer>(colGroups.getValues(Integer.class));
			for (Integer colValue : colValuesInt) {
				if (colValue == null) 
					continue;
				groups.add(colValue.toString());
			}
		}
		monitor.setStatusMessage("Network contains " + groups.size() + " groups.");
		
		int counter = 0;
		for (String group : groups) {
			if (counter > limitGroupNumber) 
				break;

			// define name of enrichment tables
			String groupTableName = EnrichmentTerm.ENRICHMENT_TABLE_PREFIX + colGroups.getName() + " " + group;
			
			// clear old results
			deleteEnrichmentTables(groupTableName);

			// get set of nodes to retrieve enrichment for
			String selected = getGroupNodes(network, colGroups, group).trim(); // also inits the analyzedNodes
			if (analyzedNodes.size() < limitGroupSize) {
				System.out.println("ignore group " + groupTableName);
				continue;
			}
			System.out.println("Retrieving enrichment for group " + groupTableName + " with " + analyzedNodes.size() + " nodes.");
			monitor.showMessage(Level.INFO, "Retrieving enrichment for group " + groupTableName);

			// retrieve enrichment (new API)
			getEnrichmentJSON(selected, species, bgNodes, groupTableName);
			counter += 1;
		}
			
		// show enrichment results
		if (enrichmentResult == null) {
			return;
		} else if (enrichmentResult.size() == 0) {
			// this should not happen anymore
			monitor.showMessage(Level.WARN,
					"Enrichment retrieval returned no results that met the criteria.");
		}

		if (showFactoryPubl != null && publOnly) {
			manager.showPublicationPanel();
		}
		if (showFactoryEnrich != null && !publOnly) {
			manager.showEnrichmentPanel();
		} 
	}

    private void initGroupColumn() {
		Collection<CyColumn> colList = network.getDefaultNodeTable().getColumns();
		colList.remove(network.getDefaultNodeTable().getColumn(CyNetwork.SELECTED));
		colList.remove(network.getDefaultNodeTable().getColumn(CyNetwork.SUID));
		List<CyColumn> showList = new ArrayList<CyColumn>();
		int numValues = network.getNodeCount();
		for (CyColumn col : colList) {
			Set<?> colValues = new HashSet();			 
			if (col.getType().equals(String.class)) {
				colValues = new HashSet<String>(col.getValues(String.class));
			} else if (col.getType().equals(Integer.class)) {
				colValues = new HashSet<Integer>(col.getValues(Integer.class));
			} else if (col.getType().equals(Boolean.class)) {
				colValues = new HashSet<Boolean>(col.getValues(Boolean.class));
			} else if (col.getType().equals(Double.class)) {
				colValues = new HashSet<Double>(col.getValues(Double.class));
			}
			// skip column if it only contains unique values or only one value or unique values for more than half the nodes in the network 
			// filter for empty strings -> maybe enough to put a cutoff here?
			// TODO: [N] is this cutoff OK?
			if (colValues.size() < 2 || colValues.size() == numValues || colValues.size() > numValues/2) {
				// System.out.println("skip: " + col.getName());
				continue;
			}
			showList.add(col);
		}
		// sort attribute list
		Collections.sort(showList, new Comparator<CyColumn>() {
		    public int compare(CyColumn a, CyColumn b) {
		        return a.getName().compareToIgnoreCase(b.getName());
		    }
		});
		groupColumn = new ListSingleSelection<CyColumn>(showList);
		// check if clustering has been done and if find out which one to pre-select the column
		CyColumn mclColNet = network.getDefaultNetworkTable().getColumn("__clusterAttribute");
		if (mclColNet != null) {
			String clusterName = network.getRow(network).get("__clusterAttribute", String.class);
			if (clusterName != null && network.getDefaultNodeTable().getColumn(clusterName) != null) {
				groupColumn.setSelectedValue(network.getDefaultNodeTable().getColumn(clusterName));
			}
		}
    }

	private void getEnrichmentJSON(String selected, String species, String backgroundNodes, String groupTableLabel) {
		Map<String, String> args = new HashMap<String, String>();
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/enrichment";
		args.put("identifiers", selected);
		args.put("species", species);
		args.put("caller_identity", StringManager.CallerIdentity);
		if (backgroundNodes != null) {
			args.put("background_string_identifiers", backgroundNodes);
		}
		JSONObject results;
		try {
			results = HttpUtils.postJSON(url, args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			enrichmentResult = null;
			return;
		}
		if (results == null) {
			monitor.showMessage(Level.ERROR,
					"Enrichment retrieval returned no results, possibly due to an error.");
			enrichmentResult = null;
			return;
			// throw new RuntimeException("Enrichment retrieval returned no results, possibly due to an error.");
		}
		List<EnrichmentTerm> terms = ModelUtils.getEnrichmentFromJSON(manager, results, stringNodesMap, network);
		if (terms == null) {
			String errorMsg = ModelUtils.getErrorMessageFromJSON(manager, results);
			monitor.showMessage(Level.ERROR,
					"Enrichment retrieval returned no results, possibly due to an error. " + errorMsg);
			enrichmentResult = null;
			return;
			// throw new RuntimeException("Enrichment retrieval returned no results, possibly due to an error. " + errorMsg);
		} else {
			Collections.sort(terms);
			// separate terms into all and pmid
			List<EnrichmentTerm> termsAll = new ArrayList<EnrichmentTerm>();
			List<EnrichmentTerm> termsPubl = new ArrayList<EnrichmentTerm>();
			for (EnrichmentTerm term : terms) {
				// System.out.println(term.getCategory());
				if (term.getCategory().equals(TermCategory.PMID.getName())) {
					termsPubl.add(term);
				} else {
					termsAll.add(term);
				}
			}
			System.out.println("all: " + termsAll.size());
			System.out.println("pmid: " + termsPubl.size());
			if (publOnly) {
				//enrichmentResult.put(groupLabel + TermCategory.PMID.getKey(), termsPubl);
				//saveEnrichmentTable(groupLabel + TermCategory.PMID.getTable(), groupLabel + TermCategory.PMID.getKey());				
			} else {
				enrichmentResult.put(groupTableLabel, termsAll);
				saveEnrichmentTable(groupTableLabel, groupTableLabel);
			}
			// info for the user
			if ((publOnly && termsPubl.size() == 0) || (!publOnly && termsAll.size() == 0))
				monitor.showMessage(Level.WARN, "Enrichment retrieval returned no results that met the criteria.");
			else
				monitor.showMessage(Level.INFO, "Enrichment retrieval successful.");
		}
	}

	private void saveEnrichmentTable(String tableName, String enrichmentCategory) {
		CyTableFactory tableFactory = manager.getService(CyTableFactory.class);
		CyTableManager tableManager = manager.getService(CyTableManager.class);

		CyTable enrichmentTable = tableFactory.createTable(tableName, EnrichmentTerm.colID,
				Long.class, false, true);
		enrichmentTable.setSavePolicy(SavePolicy.SESSION_FILE);
		tableManager.addTable(enrichmentTable);
		ModelUtils.setupEnrichmentTable(enrichmentTable);
		enrichmentTables.add(enrichmentTable);
		
		// Step 2: populate the table with some data
		List<EnrichmentTerm> processTerms = enrichmentResult.get(enrichmentCategory);
		if (processTerms == null) {
			return;
		}
		if (processTerms.size() == 0) {
			CyRow row = enrichmentTable.getRow((long) 0);
			row.set(EnrichmentTerm.colNetworkSUID, network.getSUID());
		}
		double maxFDRLogValue = ModelUtils.getMaxFdrLogValue(processTerms);
		for (int i = 0; i < processTerms.size(); i++) {
			EnrichmentTerm term = processTerms.get(i);
			CyRow row = enrichmentTable.getRow((long) i);
			row.set(EnrichmentTerm.colName, term.getName());
			if (term.getName().length() > 4 && term.getName().startsWith("PMID")) {
				row.set(EnrichmentTerm.colIDPubl, term.getName().substring(5));
			} else {
				row.set(EnrichmentTerm.colIDPubl, "");
			}
			row.set(EnrichmentTerm.colYear, term.getYear());
			row.set(EnrichmentTerm.colDescription, term.getDescription());
			row.set(EnrichmentTerm.colCategory, term.getCategory());
			row.set(EnrichmentTerm.colFDR, term.getFDRPValue());
			row.set(EnrichmentTerm.colFDRTransf, -Math.log10(term.getFDRPValue())/maxFDRLogValue);
			row.set(EnrichmentTerm.colPvalue, term.getPValue());
			row.set(EnrichmentTerm.colGenesBG, term.getGenesBG());
			row.set(EnrichmentTerm.colGenesCount, term.getGenes().size());
			row.set(EnrichmentTerm.colGenes, term.getGenes());
			row.set(EnrichmentTerm.colGenesSUID, term.getNodesSUID());
			row.set(EnrichmentTerm.colNetworkSUID, network.getSUID());
			// row.set(EnrichmentTerm.colShowChart, false);
			row.set(EnrichmentTerm.colChartColor, "");
		}
		return;
	}

	protected void showError(String msg) {
	}

	private String getGroupNodes(CyNetwork currentNetwork, CyColumn colGroup, String group) {
		StringBuilder str = new StringBuilder();
		analyzedNodes = new ArrayList<CyNode>();
		for (CyNode node : currentNetwork.getNodeList()) {
			Object groupValue = currentNetwork.getRow(node).get(colGroup.getName(), colGroup.getType());
			if (groupValue == null || !groupValue.toString().equals(group)) 
				continue;
			String stringID = currentNetwork.getRow(node).get(ModelUtils.STRINGID, String.class);
			String type = currentNetwork.getRow(node).get(ModelUtils.TYPE, String.class);
			if (stringID != null && stringID.length() > 0 && type != null && type.equals("protein")) {
				str.append(stringID + "\n");
				analyzedNodes.add(node);
			}
		}
		return str.toString();
	}

	private String getBackground(CyNetwork bgNetwork, CyNetwork fgNetwork) {
		StringBuilder str = new StringBuilder();
		for (CyNode node : bgNetwork.getNodeList()) {
			String stringID = bgNetwork.getRow(node).get(ModelUtils.STRINGID, String.class);
			String type = bgNetwork.getRow(node).get(ModelUtils.TYPE, String.class);
			if (stringID != null && stringID.length() > 0 && type != null
					&& type.equals("protein")) {
				str.append(stringID + "\n");
			}
		}
		// check if foreground is contained in background
		for (CyNode fgNode : analyzedNodes) {
			if (str.indexOf(fgNetwork.getRow(fgNode).get(ModelUtils.STRINGID, String.class)) == -1) {
				System.out.println(fgNode.getSUID());
				return "";
			}
		}
		return str.toString();
	}

	// [N] not working yet? -> seems to work
	private void deleteEnrichmentTables(String groupTableName) {
		CyTableManager tableManager = manager.getService(CyTableManager.class); 
		Set<CyTable> currTables = tableManager.getAllTables(true);
		for (CyTable current : currTables) {
			if (current.getTitle().contains(groupTableName)
					&& current.getColumn(EnrichmentTerm.colNetworkSUID) != null
					&& current.getAllRows().size() > 0) {
				CyRow tempRow = current.getAllRows().get(0);
				if (tempRow.get(EnrichmentTerm.colNetworkSUID, Long.class) != null && tempRow
						.get(EnrichmentTerm.colNetworkSUID, Long.class).equals(network.getSUID())) {
					if (publOnly && !current.getTitle().contains(TermCategory.PMID.getTable())) {
						continue;
					}
					tableManager.deleteTable(current.getSUID());
					manager.flushEvents();
				}
			}
		}
	}
	

	@ProvidesTitle
	public String getTitle() {
		if (publOnly)
			return "Retrieve enriched publications per group";
		return "Retrieve functional enrichment per group";
	}

	public static String EXAMPLE_JSON = 
					"{\"EnrichmentTable\": 101}";

	public static String EXAMPLE_JSON_PUBL = 
			"{\"EnrichmentTable\": 101}";

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		// TODO: [N] See what we need to return here
		if (clzz.equals(CyTable.class)) {
			return (R) enrichmentTables.get(0);
		} else if (clzz.equals(String.class)) {
			if (enrichmentTables == null) 
				return (R)"No results";
			if (enrichmentTables != null)
				return (R)("\"EnrichmentTable\": "+enrichmentTables.get(0).getSUID());
		} else if (clzz.equals(Long.class)) {
			// if (ppiSummary == null) return null; 
			return (R) enrichmentTables.get(0).getSUID();
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				String result = "{";
				if (enrichmentTables != null)
					result += "\"EnrichmentTable\": "+enrichmentTables.get(0).getSUID();
				result += "}";
				// System.out.println(result);
				return result;
      };
      return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class, Long.class, CyTable.class);
	}

}
