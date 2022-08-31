package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListMultipleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentTableModel;

public class NodeFilterEnrichmentTableTask extends AbstractTask implements ObservableTask {

	private StringManager manager;
	private EnrichmentCytoPanel enrichmentPanel;
	private CyNetwork network;
	
	@Tunable(description = "Select nodes", 
			context = "nogui", 
	         gravity = 1.0)
	public ListMultipleSelection<CyNode> nodesToFilterBy = null;

	@Tunable(description = "Term should annotate all selected",
			context = "nogui", 
	         gravity = 1.0)
	public boolean annotateAllNodes = false;

	public NodeFilterEnrichmentTableTask(StringManager manager, EnrichmentCytoPanel panel) {
		this.manager = manager;
		network = manager.getCurrentNetwork();
		this.enrichmentPanel = panel;
		nodesToFilterBy = new ListMultipleSelection<CyNode>(network.getNodeList());
		nodesToFilterBy.setSelectedValues(CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true));
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Filter STRING Enrichment table by node");
	
		List<CyNode> selectedNodesList = nodesToFilterBy.getSelectedValues();
		List<Long> nodesToFilter = new ArrayList<Long>();
		for (CyNode node : selectedNodesList) {
			nodesToFilter.add(node.getSUID());
		}
		// Filter the current list
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// when using commands, we need to get the enrichment panel again
				if (enrichmentPanel == null) { 
					CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
					CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
					enrichmentPanel = (EnrichmentCytoPanel) cytoPanel.getComponentAt(
							cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
				}
				EnrichmentTableModel tableModel = enrichmentPanel.getTableModel();
				// tableModel.filterByNodeSUID(nodesToFilter, annotateAllNodes, manager.getCategoryFilter(network), manager.getRemoveOverlap(network), manager.getOverlapCutoff(network));
				enrichmentPanel.updateLabelRows();
				// manager.setCategoryFilter(network,categories.getSelectedValues());
				// manager.updateSettings();
				// enrichmentPanel.updateFilteredEnrichmentTable();
			}
		});
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

	@ProvidesTitle
	public String getTitle() {
		return "Filter STRING Enrichment table by node";
	}
}
