package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class ShowImagesTask extends AbstractTask implements ObservableTask {
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

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(String.class)) {
			return (R)"";
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				return "{}";
			};
			return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class);
	}


}
