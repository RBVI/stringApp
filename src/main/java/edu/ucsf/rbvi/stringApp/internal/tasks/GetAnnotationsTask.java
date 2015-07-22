package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class GetAnnotationsTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	final int taxon;
	final String terms;
	Map<String, List<Annotation>> annotations = null;

	public GetAnnotationsTask(StringManager manager, int taxon, String terms) {
		this.manager = manager;
		this.taxon = taxon;
		this.terms = terms;
	}

	@Override
	public void run(TaskMonitor monitor) {
		monitor.setTitle("Getting annotations");
		annotations = manager.getAnnotations(taxon, terms);
	}

	public Map<String, List<Annotation>> getAnnotations() { return annotations; }

	public int getTaxon() { return taxon; }

	@Override
	public <T> T getResults(Class<? extends T> type) {
		return null;
	}
}
	
