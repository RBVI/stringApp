package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import org.cytoscape.work.Task;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ResolveNamesTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	final int taxon;
	final int confidence;
	final int additionalNodes;
	final String terms;

	public ResolveNamesTaskFactory(StringManager manager, int taxonId, 
	                               int confidence, int additionalNodes, String terms) {
		this.manager = manager;
		this.taxon = taxonId;
		this.confidence = confidence;
		this.additionalNodes = additionalNodes;
		this.terms = terms;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ResolveNamesTask(manager, taxon, confidence, additionalNodes, terms));
	}

}

