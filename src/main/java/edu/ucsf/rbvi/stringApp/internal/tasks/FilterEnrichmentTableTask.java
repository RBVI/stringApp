package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListMultipleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentTableModel;

public class FilterEnrichmentTableTask extends AbstractTask {

	private StringManager manager;
	private EnrichmentTableModel tableModel;
	
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
					 gravity = 1.0)
	public ListMultipleSelection<TermCategory> categories = new ListMultipleSelection<>(TermCategory.values());

	@Tunable(description = "Remove overlapping", gravity = 8.0)
	public boolean removeOverlapping = false;

	@Tunable(description = "Overlap cutoff", params="slider=true", gravity = 9.0)
	public double overlapCutoff = 0.7;
	// public BoundedDouble ovlCutoff = new BoundedDouble(0.0, 0.7, 1.0, false, false);
	
	public FilterEnrichmentTableTask(StringManager manager, EnrichmentTableModel tableModel) {
		this.manager = manager;
		this.tableModel = tableModel;
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		System.out.println("Run filtering ...");
		List<TermCategory> categoryList = categories.getSelectedValues();
		// Filter the current list
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				tableModel.filter(categoryList, removeOverlapping, overlapCutoff);
			}
		});
	}

}
