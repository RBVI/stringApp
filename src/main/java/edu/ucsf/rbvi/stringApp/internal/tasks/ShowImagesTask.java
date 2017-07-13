package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

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
