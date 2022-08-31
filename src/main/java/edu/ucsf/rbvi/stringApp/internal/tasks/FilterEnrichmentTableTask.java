package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
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

	// TODO: [N] Make filtering table specific 
	
	private StringManager manager;
	private EnrichmentCytoPanel enrichmentPanel;
	private CyNetwork network;
	private String group;
	private CyTable filteredEnrichmentTable;
	
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
	         longDescription = "Select the enrichment categories to show in the table. Use \"All\" to remove the filtering.",
	         exampleStringValue = "GO Process",
	         gravity = 1.0)
	public ListMultipleSelection<TermCategory> categories = new ListMultipleSelection<>(TermCategory.getValues());

	@Tunable(description = "Remove redundant terms", 
	         tooltip = "Removes terms whose enriched genes significantly overlap with already selected terms.",
	         longDescription = "Removes terms whose enriched genes significantly overlap with already selected terms.",
	         exampleStringValue = "true",
	         gravity = 8.0)
	public boolean removeOverlapping = false;

	@Tunable(description = "Redundancy cutoff", 
	         tooltip = "<html>This is the maximum Jaccard similarity that will be allowed <br/>"
	                   + "between a less significant term and a more significant term such that <br/>"
	                   + "the less significant term is kept in the list.</html>",
	         longDescription = "This is the maximum Jaccard similarity that will be allowed "
	         		+ "between a less significant term and a more significant term such that "
	         		+ "the less significant term is kept in the list.",
	         exampleStringValue="0.5",
	         params="slider=true", dependsOn="removeOverlapping=true", gravity = 9.0)
	public BoundedDouble overlapCutoff = new BoundedDouble(0.0, 0.5, 1.0, false, false);
	
	public FilterEnrichmentTableTask(StringManager manager, EnrichmentCytoPanel panel) {
		this.manager = manager;
		network = manager.getCurrentNetwork();
		this.enrichmentPanel = panel;
		// TODO: [N] is it ok to just add the group here?
		this.group = panel.getTable();
		overlapCutoff.setValue(manager.getOverlapCutoff(network, group));
		categories.setSelectedValues(manager.getCategoryFilter(network, group));
		removeOverlapping = manager.getRemoveOverlap(network, group);
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Filter STRING Enrichment table");
	
		// Filter the current list
		List<TermCategory> categoryList = categories.getSelectedValues();
		//SwingUtilities.invokeLater(new Runnable() {
			//public void run() {
				// when using commands, we need to get the enrichment panel again
				if (enrichmentPanel == null) { 
					CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
					CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);
					if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") != -1)
						enrichmentPanel = (EnrichmentCytoPanel) cytoPanel.getComponentAt(
								cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
					else return;
				}
				EnrichmentTableModel tableModel = enrichmentPanel.getTableModel();
				// TODO: [N] FIgure out filtering 
				System.out.println("filter table model");
				tableModel.filter(categoryList, removeOverlapping, overlapCutoff.getValue());
				// enrichmentPanel.updateLabelRows();
				manager.setRemoveOverlap(network,removeOverlapping, group);
				manager.setOverlapCutoff(network,overlapCutoff.getValue(), group);
				manager.setCategoryFilter(network,categories.getSelectedValues(), group);
				manager.updateSettings();
				enrichmentPanel.getFilteredTable();
				// enrichmentPanel.updateFilteredEnrichmentTable();
			//}
		//});
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(String.class)) {
			if (filteredEnrichmentTable != null)
				return (R)("\"EnrichmentTable\": "+filteredEnrichmentTable.getSUID());
			return (R)"";
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				if (filteredEnrichmentTable != null)
					return "{\"EnrichmentTable\": "+filteredEnrichmentTable.getSUID()+"}";
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
