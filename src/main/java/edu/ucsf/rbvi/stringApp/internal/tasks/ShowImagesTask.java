package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ShowImagesTask extends AbstractTask {
	final StringManager manager;
	final CyNetworkView netView;
	final ShowImagesTaskFactory factory;
	final boolean show;

	public ShowImagesTask(final StringManager manager, final boolean show) {
		this.manager = manager;
		this.netView = null;
		this.factory = null;
		this.show = show;
	}

	public ShowImagesTask(final StringManager manager, final CyNetworkView netView, 
	                      final ShowImagesTaskFactory factory) {
		this.manager = manager;
		this.netView = netView;
		this.factory = factory;
		this.show = false;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Show images");

		if (factory == null) {
			// Command version
			manager.setShowImage(show);
			return;
		}

		if (manager.showImage())
			manager.setShowImage(false);
		else
			manager.setShowImage(true);
		netView.updateView();
		factory.reregister();
	}
}
