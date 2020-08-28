package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ExportPublicationsTaskFactory extends AbstractNetworkTaskFactory {
	// implements ExportTableTaskFactory {

	private StringManager manager;

	public ExportPublicationsTaskFactory(StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetwork network) {
		if (ModelUtils.getEnrichmentTables(manager, network).size() > 0)
			return true;
		else
			return false;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new ExportEnrichmentTableTask(manager, network, null, 
				ModelUtils.getEnrichmentTable(manager, network, TermCategory.PMID.getTable()), false));
	}

}
