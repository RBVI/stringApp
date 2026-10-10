package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

/**
 * Task factory that dispatches to the appropriate STRING/STITCH query task based on
 * a query type string ("protein", "additional", "disease", "pubmed", "compound", or
 * "cross-species"), creating a {@link ProteinQueryTask}, {@link ProteinQueryAdditionalTask},
 * {@code DiseaseQueryTask}, {@link PubmedQueryTask}, {@code CompoundQueryTask}, or
 * {@code CrossSpeciesQueryTask} respectively. Backs the various "Query" menu commands.
 */
public class GetNetworkTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	final String type;

	public GetNetworkTaskFactory(final StringManager manager, final String queryType) {
		this.manager = manager;
		this.type = queryType;
	}

	public TaskIterator createTaskIterator() {
		if (type.equals("protein"))
			return new TaskIterator(new ProteinQueryTask(manager));
		else if (type.equals("additional")) 
			return new TaskIterator(new ProteinQueryAdditionalTask(manager));
		else if (type.equals("disease"))
			return new TaskIterator(new DiseaseQueryTask(manager));
		else if (type.equals("pubmed"))
			return new TaskIterator(new PubmedQueryTask(manager));
		else if (type.equals("compound"))
			return new TaskIterator(new CompoundQueryTask(manager));
		else if (type.equals("cross-species"))
			return new TaskIterator(new CrossSpeciesQueryTask(manager));
		return null;
	}

	public boolean isReady() {
		return manager.haveURIs();
	}
}

