package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

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
		else if (type.equals("disease"))
			return new TaskIterator(new DiseaseQueryTask(manager));
		else if (type.equals("pubmed"))
			return new TaskIterator(new PubmedQueryTask(manager));
		return null;
	}
}

