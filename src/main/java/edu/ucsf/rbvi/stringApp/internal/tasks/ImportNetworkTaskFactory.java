package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ImportNetworkTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	final String species;
	int taxon;
	int confidence;
	int additionalNodes;
	List<String> stringIds;
	

	public ImportNetworkTaskFactory(final StringManager manager, final String species, 
	                                int taxon, int confidence, int additional_nodes, 
	                                final List<String> stringIds) {
		this.manager = manager;
		this.taxon = taxon;
		this.confidence = confidence;
		this.additionalNodes = additional_nodes;
		this.stringIds = stringIds;
		this.species = species;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new LoadInteractions(manager, species, taxon, confidence, additionalNodes, stringIds));
	}
}
