package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ImportNetworkTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	int taxon;
	int confidence;
	int additionalNodes;
	List<String> stringIds;
	

	public ImportNetworkTaskFactory(final StringManager manager, int taxon, int confidence, int additional_nodes, 
	                                 final List<String> stringIds) {
		this.manager = manager;
		this.taxon = taxon;
		this.confidence = confidence;
		this.additionalNodes = additional_nodes;
		this.stringIds = stringIds;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new LoadInteractions(manager, taxon, confidence, additionalNodes, stringIds));
	}
}
