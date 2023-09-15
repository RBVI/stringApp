package edu.ucsf.rbvi.stringApp.internal.tasks;


import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class FetchStructureImagesTask extends AbstractTask {
	final StringManager manager;
	final CyNetwork network;

	public FetchStructureImagesTask(StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
	}

	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle(this.getTitle());

		System.out.println("Fetch images task");
		ModelUtils.fetchImages(manager, network);
	}

	@ProvidesTitle
	public String getTitle() {
		return "Fetch structure images";
	}

}
