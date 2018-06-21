package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.List;
import java.util.Map;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

public class GetAnnotationsTask extends AbstractTask implements ObservableTask {
	final StringNetwork stringNetwork;
	final int taxon;
	final String terms;
	final String useDATABASE;
	Map<String, List<Annotation>> annotations = null;

	public GetAnnotationsTask(StringNetwork stringNetwork, int taxon, String terms, String useDATABASE) {
		this.stringNetwork = stringNetwork;
		this.taxon = taxon;
		this.terms = terms;
		this.useDATABASE = useDATABASE;
	}

	@Override
	public void run(TaskMonitor monitor) {
		monitor.setTitle("Getting annotations");
		annotations = stringNetwork.getAnnotations(taxon, terms, useDATABASE);
		if (annotations == null || annotations.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Query returned no terms");
		}
	}

	public Map<String, List<Annotation>> getAnnotations() { return annotations; }

	public int getTaxon() { return taxon; }

	public StringNetwork getStringNetwork() { return stringNetwork; }

	@Override
	public <T> T getResults(Class<? extends T> type) {
		return null;
	}
}

