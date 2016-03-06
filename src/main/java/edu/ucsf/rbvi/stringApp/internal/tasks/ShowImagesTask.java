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
import org.cytoscape.work.util.BoundedFloat;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ShowImagesTask extends AbstractTask {
	final StringManager manager;
	final CyNetworkView netView;
	final ShowImagesTaskFactory factory;

	public ShowImagesTask(final StringManager manager, final CyNetworkView netView, 
	                      final ShowImagesTaskFactory factory) {
		this.manager = manager;
		this.netView = netView;
		this.factory = factory;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Show images");

		if (manager.showImage())
			manager.setShowImage(false);
		else
			manager.setShowImage(true);
		netView.updateView();
		factory.reregister();
	}
}
