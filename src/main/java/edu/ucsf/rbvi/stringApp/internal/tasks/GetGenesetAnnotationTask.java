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

import javax.swing.JTable;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.ListSingleSelection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentTableModel;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ColumnNames;
import edu.ucsf.rbvi.stringApp.internal.utils.EnrichmentUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.JSONUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class GetGenesetAnnotationTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	final CyNetwork network;
	final CyNetworkView netView;
	final Map<String, Long> stringNodesMap;
	final Map<String, CyNetwork> stringNetworkMap;
	List<CyNode> analyzedNodes;
	Map<String, String> annotations;
	TaskMonitor monitor;

	public static String primary = "primary";
	public static String secondary = "secondary";
	public static String tertiary = "tertiary";
	
	@Tunable(description = "Column for groups", 
	         longDescription="Specify the column that contains the node groups to be used for group-wise enrichment",
	         exampleStringValue="",
	         required=true, 
	         gravity = 2.0)
	public ListSingleSelection<CyColumn> groupColumn = new ListSingleSelection<CyColumn>();

	@Tunable(description = "Retrieve for species", 
			gravity = 3.0)
	public ListSingleSelection<String> allNetSpecies = new ListSingleSelection<String>();
	
	@Tunable(description = "Maximum number of groups", 
			groups={"Advanced"}, params="displayState=collapsed",
			exampleStringValue="20",
			gravity = 5.0)
	public int maxGroupNumber = 20;	

	@Tunable(description = "Minimum group size", 
			groups={"Advanced"}, params="displayState=collapsed",
			exampleStringValue="3",
			gravity = 6.0)
	public int minGroupSize = 3;	
	
	public GetGenesetAnnotationTask(StringManager manager, CyNetwork network, CyNetworkView netView) {
		this.manager = manager;
		this.network = network;
		this.netView = netView;

		stringNodesMap = new HashMap<>();
		monitor = null;
		initGroupColumn(); // initializes groupColumn
		allNetSpecies = new ListSingleSelection<String>(EnrichmentUtils.getEnrichmentNetSpecies(network));

		stringNetworkMap = new HashMap<>();

		List<String> netList = new ArrayList<>();
		netList.add("genome");
		for (StringNetwork sn: manager.getStringNetworks()) {
			CyNetwork net = sn.getNetwork();
			String name = ModelUtils.getName(net, net);
			netList.add(name);
			stringNetworkMap.put(name, net);
		}
	}

	public void run(TaskMonitor monitor) throws Exception {
		this.monitor = monitor;
		monitor.setTitle(this.getTitle());

		if (!ModelUtils.isCurrentDataVersion(network)) {
			monitor.showMessage(Level.ERROR,
					"Task cannot be performed. Network appears to be an old STRING network.");
			return;			
		}
		
		// get species
		Species selSpecies = Species.getSpecies(allNetSpecies.getSelectedValue());
		String species = String.valueOf(selSpecies.getTaxId());
		if (selSpecies.isCustom())
			species = selSpecies.toString();
		
		// map of STRING ID to CyNodes
		CyTable nodeTable = network.getDefaultNodeTable();
		for (final CyNode node : network.getNodeList()) {
			if (nodeTable.getColumn(ColumnNames.STRINGID) != null) {
				String stringid = nodeTable.getRow(node.getSUID()).get(ColumnNames.STRINGID, String.class);
				if (stringid != null) {
					stringNodesMap.put(stringid, node.getSUID());
				}
			}
		}

		// get groups 
		CyColumn colGroups = groupColumn.getSelectedValue();
		Class<?> colGroupClass = colGroups.getType();
		// Sort groups by size  	
		List<Group> groups = new ArrayList<Group>();
		List<?> colValues = colGroups.getValues(colGroupClass);
		Set<?> colValuesUnique = new HashSet(colValues);
		for (Object colval : colValuesUnique) {
			if (colval == null || colval.toString().equals(""))
				continue;
			int valfreq = Collections.frequency(colValues, colval);
			groups.add(new Group(colval.toString(), valfreq));
		}
		// monitor.setStatusMessage("Network contains " + groups.size() + " groups.");
		Collections.sort(groups, Collections.reverseOrder());
		// System.out.println(groups);

		// create columns to save annotations in the node table
		ModelUtils.createColumnIfNeeded(nodeTable, String.class, ColumnNames.GENESET_PRIMARY);
		ModelUtils.createColumnIfNeeded(nodeTable, String.class, ColumnNames.GENESET_SECONDARY);
		ModelUtils.createColumnIfNeeded(nodeTable, String.class, ColumnNames.GENESET_TERTIARY);
		
		// get annotations for each group
		int counter = 0;
		for (Group group : groups) {
			// System.out.println("Group: " + group);			
			if (counter >= maxGroupNumber) 
				break;
			counter += 1;

			// get set of nodes to retrieve enrichment for
			String selected = getGroupNodes(network, colGroups, group.getName()).trim(); // also initializes the analyzedNodes
			if (analyzedNodes.size() < minGroupSize) {
				// System.out.println("ignore group " + groupTableName);
				continue;
			}
			String groupName = colGroups.getName() + " " + group.getName();

			// System.out.println("Retrieving annotations for " + groupName + " with " + analyzedNodes.size() + " nodes.");
			monitor.showMessage(Level.INFO, "Retrieving annotataions for group " + groupName + " with " + analyzedNodes.size() + " nodes.");

			// retrieve enrichment (new API)
			getAnnotationJSON(selected, species, groupName);			
			if (annotations != null && annotations.size() > 0) {
				//System.out.println("Annotations: " + annotations.toString());
				if (annotations.containsKey(primary) && annotations.get(primary) != null) {
					addAnnotationToNetwork(annotations.get(primary));
				} else if (annotations.containsKey(secondary) && annotations.get(secondary) != null) {
					addAnnotationToNetwork(annotations.get(secondary));
				} else if (annotations.containsKey(tertiary) && annotations.get(tertiary) != null) {
					addAnnotationToNetwork(annotations.get(tertiary));
				}
			} else {
				monitor.showMessage(Level.WARN, "Gene set annotation returned no results for this set of genes.");
			}
			
			for (CyNode node : analyzedNodes) {
				if (annotations.containsKey(primary) && annotations.get(primary) != null) {
					network.getRow(node).set(ColumnNames.GENESET_PRIMARY, annotations.get(primary));
				}
				if (annotations.containsKey(secondary) && annotations.get(secondary) != null) {
					network.getRow(node).set(ColumnNames.GENESET_SECONDARY, annotations.get(secondary));
				}
				if (annotations.containsKey(tertiary) && annotations.get(tertiary) != null) {
					network.getRow(node).set(ColumnNames.GENESET_TERTIARY, annotations.get(tertiary));
				}
			}
		}
	
	}

	private void addAnnotationToNetwork(String annotation) {
		CyNetwork network = manager.getCurrentNetwork();
		CyNetworkView view = manager.getCurrentNetworkView();
		if (network == null || view == null || annotation == null)
			return;
		
		AnnotationManager annotManager = manager.getService(AnnotationManager.class);
		AnnotationFactory<TextAnnotation> textFactory = 
				(AnnotationFactory<TextAnnotation>) manager.getService(AnnotationFactory.class, "(type=TextAnnotation.class)");
		if (annotManager == null || textFactory == null) {
			System.out.println("AnnotationManager or textFactory is null");
			return;
		}
		
			
		final VisualProperty<Double> xLoc = BasicVisualLexicon.NODE_X_LOCATION;
		final VisualProperty<Double> yLoc = BasicVisualLexicon.NODE_Y_LOCATION;
		Set<Double> xPos = new HashSet<Double>();
		Set<Double> yPos = new HashSet<Double>();
		for (View<CyNode> nodeView : view.getNodeViews()) {
			if (analyzedNodes.contains(nodeView.getModel())) {
				xPos.add(nodeView.getVisualProperty(xLoc));
				yPos.add(nodeView.getVisualProperty(yLoc));
			}
		}
		double xSpan = Collections.max(xPos) - Collections.min(xPos);
		double ySpan = Collections.max(yPos) - Collections.min(yPos);
		// double scaling = view.getNodeViews().size()/(double)termNodes.size();
		
		// create annotation
		Map<String, String> args = new HashMap<>();
		args.put(TextAnnotation.X, String.valueOf(Collections.min(xPos) - xSpan/8.0));
		if (analyzedNodes.size() > 30)
			args.put(TextAnnotation.Y, String.valueOf(Collections.min(yPos) - ySpan/8.0));
		else if (analyzedNodes.size() > 5)
			args.put(TextAnnotation.Y, String.valueOf(Collections.min(yPos) - ySpan/5.0));
		else 
			args.put(TextAnnotation.Y, String.valueOf(Collections.min(yPos) - ySpan/2.0));
		//args.put(TextAnnotation.Z, String.valueOf(-1));
		args.put(TextAnnotation.TEXT, annotation);
		args.put(TextAnnotation.FONTSIZE, String.valueOf(30));
		//args.put(TextAnnotation.FONTFAMILY, this.font.getFamily());
		//args.put(TextAnnotation.FONTSTYLE, String.valueOf(this.font.getStyle()));
		//args.put(TextAnnotation.COLOR, Color.BLACK.toString());
		
		TextAnnotation textAnnotation = textFactory.createAnnotation(TextAnnotation.class, view, args);
		textAnnotation.setName("stringApp_" + annotation);
		annotManager.addAnnotation(textAnnotation);
	}
	
	private void initGroupColumn() {
    	List<CyColumn> showList = ModelUtils.getGroupColumns(network);
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

	private void getAnnotationJSON(String selected, String species, String groupTableLabel) {
		Map<String, String> args = new HashMap<String, String>();
		String url = manager.getResolveURL(Databases.STRING.getAPIName())+"json/geneset_description";
		args.put("identifiers", selected);
		args.put("species", species);
		args.put("caller_identity", StringManager.CallerIdentity);
		JSONObject jsonResults;
		try {
			jsonResults = HttpUtils.postJSON(url, args, manager);
		} catch (ConnectionException e) {
			e.printStackTrace();
			monitor.showMessage(Level.ERROR, "Network error: " + e.getMessage());
			return;
		}
		if (jsonResults == null) {
			monitor.showMessage(Level.ERROR, "Gene set annotation returned no results, possibly due to an error.");
			return;
		}
		// System.out.println(jsonResults);
		JSONArray annotationsArray = JSONUtils.getResultsFromJSON(jsonResults, JSONArray.class);
		// [{"primary_description": "p53 binding", "secondary_description": "Regulation of TP53 Activity through Methylation", 
		// "tertiary_description": "-", "proteins": "CREBBP, DAXX, EP300, MDM2, TP53"}]

		if (annotationsArray == null) {
			String errorMsg = JSONUtils.getErrorMessageFromJSON(manager, jsonResults);
			monitor.showMessage(Level.ERROR, "Gene set annotation returned no results, possibly due to an error. " + errorMsg);
			return;
		}

		JSONObject annotationsObject = (JSONObject)annotationsArray.get(0);
		String primaryAnnot = (String)annotationsObject.get("primary_description");
		String secondaryAnnot = (String)annotationsObject.get("secondary_description");
		String tertiaryAnnot = (String)annotationsObject.get("tertiary_description");
		annotations = new HashMap<String, String>();
		// String proteins = (String)annotationsObject.get("proteins");
		if (primary != null) {
			annotations.put(primary, primaryAnnot);
		} 
		if (secondary != null) {			
			annotations.put(secondary, secondaryAnnot);
		} 
		if (tertiary != null) {			
			annotations.put(tertiary, tertiaryAnnot);
		}
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
			String stringID = currentNetwork.getRow(node).get(ColumnNames.STRINGID, String.class);
			// check the node type and only accept if protein
			String type = currentNetwork.getRow(node).get(ColumnNames.TYPE, String.class);
			// check the node species and only allow if it is the same as the one chosen by the user
			String species = currentNetwork.getRow(node).get(ColumnNames.SPECIES, String.class);
			if (stringID != null && stringID.length() > 0 
					&& type != null && type.equals("protein") 
					&& species != null && species.equals(allNetSpecies.getSelectedValue())) {
				str.append(stringID + "\n");
				analyzedNodes.add(node);
			}
		}
		return str.toString();
	}


	private class Group implements Comparable {
		private String name;
		private int size;

		public Group(String _n, int _m) {
			setName(_n);
			size = _m;
		}

		@Override
		public int compareTo(Object o) {
			Group other = (Group) o;

			if (other.size > size) {
				return -1;
			} else if (other.size < size) {
				return 1;
			}
			return 0;
			// return other.name.compareTo(name);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name + ":" + size;
		}
	}
	
	@ProvidesTitle
	public String getTitle() {
		return "Retrieve gene set annotation";
	}

	public static String EXAMPLE_JSON = 
			"{\"annotation\": anotation term}";

	public static String EXAMPLE_JSON_PUBL = 
			"{\"annotation\": anotation term}";

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		// TODO: [N] Test if this works for all cases
		if (clzz.equals(String.class)) {
			if (annotations == null)
				return (R) "No annotation";
			else {
				String result = annotations.toString();
				return (R) result;
			}
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				String result = "{";
				if (annotations != null) {
					result += "\"annotation\": " + annotations.toString();
				}
				result += "}";
				// System.out.println(result);
				return result;
			};
			return (R) res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class);
	}

}
