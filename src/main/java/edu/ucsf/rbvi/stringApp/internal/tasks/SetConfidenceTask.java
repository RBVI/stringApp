package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedDouble;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
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
		for (CyEdge edge: net.getEdgeList()) {
			Double score = net.getRow(edge).get(ModelUtils.SCORE, Double.class);
			if (score == null || score >= minScore)
				continue;
			minScore = score;
		}

		ModelUtils.setConfidence(net, minScore);
		StringNetwork stringNet = new StringNetwork(manager);
		stringNet.setNetwork(net);
		manager.addStringNetwork(stringNet, net);
	}
}
