package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class SetConfidenceTask extends AbstractTask {
	final StringManager manager;
	final CyNetwork net;

	// @Tunable (description="Confidence value to set")
	// public BoundedDouble confidence = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	public SetConfidenceTask(final StringManager manager, final CyNetwork net) {
		this.manager = manager;
		this.net = net;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Set as STRING network");

		double minScore = 1.0;
		for (CyEdge edge : net.getEdgeList()) {
			Double score = net.getRow(edge).get(ModelUtils.SCORE, Double.class);
			if (score == null || score >= minScore)
				continue;
			minScore = score;
		}

		ModelUtils.setConfidence(net, minScore);

		// TODO: Find a better way to set the database, e.g. check certain node column
		ModelUtils.setDatabase(net, Databases.STRING.getAPIName());
		ModelUtils.setNetSpecies(net, ModelUtils.getMostCommonNetSpecies(net));

		StringNetwork stringNet = new StringNetwork(manager);
		stringNet.setNetwork(net);
		manager.addStringNetwork(stringNet, net);
	}
}
