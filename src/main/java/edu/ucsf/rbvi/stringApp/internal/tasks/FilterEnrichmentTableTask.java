package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
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

public class FilterEnrichmentTableTask extends AbstractTask implements ObservableTask {

	private StringManager manager;
	private EnrichmentCytoPanel enrichmentPanel;
	private CyNetwork network;
	
	// @Tunable(description = "Enrichment cutoff", gravity = 1.0)
	// public double cutoff = 0.05;

	/*
	@Tunable(description = "GO Biological Process", gravity = 2.0)
	public boolean goProcess = false;

	@Tunable(description = "GO Molecular Function", gravity = 3.0)
	public boolean goFunction = false;

	@Tunable(description = "GO Cellular Compartment", gravity = 4.0)
	public boolean goCompartment = false;

	@Tunable(description = "KEGG Pathways", gravity = 5.0)
	public boolean kegg = false;

	@Tunable(description = "Pfam domains", gravity = 6.0)
	public boolean pfam = false;

	@Tunable(description = "InterPro domains", gravity = 7.0)
	public boolean interPro = false;
	*/

	@Tunable(description = "Select categories", 
	         tooltip = "Select the enrichment categories to show in the table",
	         longDescription = "Select the enrichment categories to show in the table",
	         exampleStringValue = "GO Process",
	         gravity = 1.0)
	public ListMultipleSelection<TermCategory> categories = new ListMultipleSelection<>(TermCategory.getValues());

	@Tunable(description = "Remove redundant terms", 
	         tooltip = "Removes terms whose enriched genes significantly overlap with already selected terms",
	         longDescription = "Removes terms whose enriched genes significantly overlap with already selected terms",
	         exampleStringValue = "true",
	         gravity = 8.0)
	public boolean removeOverlapping = false;

	@Tunable(description = "Redundancy cutoff", 
	         tooltip = "<html>This is the maximum Jaccard similarity that will be allowed.<br/>"+
	                   "Values larger than this cutoff will be excluded.</html>",
	         longDescription = "This is the maximum Jaccard similarity that will be allowed. "
	         		+ "Values larger than this cutoff will be excluded.",
	         exampleStringValue="0.5",
	         params="slider=true", dependsOn="removeOverlapping=true", gravity = 9.0)
	public BoundedDouble overlapCutoff = new BoundedDouble(0.0, 0.5, 1.0, false, false);
	
	public FilterEnrichmentTableTask(StringManager manager, EnrichmentCytoPanel panel) {
		this.manager = manager;
		network = manager.getCurrentNetwork();
		this.enrichmentPanel = panel;
		overlapCutoff.setValue(manager.getOverlapCutoff(network));
		categories.setSelectedValues(manager.getCategoryFilter(network));
		removeOverlapping = manager.getRemoveOverlap(network);
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Filter STRING Enrichment table");
	
		List<TermCategory> categoryList = categories.getSelectedValues();
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
				tableModel.filter(categoryList, removeOverlapping, overlapCutoff.getValue());
				// enrichmentPanel.updateLabelRows();
				manager.setRemoveOverlap(network,removeOverlapping);
				manager.setOverlapCutoff(network,overlapCutoff.getValue());
				manager.setCategoryFilter(network,categories.getSelectedValues());
				manager.updateSettings();
				enrichmentPanel.updateFilteredEnrichmentTable();
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
		return "Filter STRING Enrichment table";
	}
}
