package edu.ucsf.rbvi.stringApp.internal.tasks;


import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Task created by {@link FetchStructureImagesTaskFactory} that fetches (if not already cached)
 * the structure images for the nodes of a STRING network via {@link ModelUtils#fetchImages} and
 * then enables image display in the app's controls.
 */
public class FetchStructureImagesTask extends AbstractTask {
	final CyNetwork network;
	final StringManager manager; 

	public FetchStructureImagesTask(StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;
	}

	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle(this.getTitle());

		// The method only fetches the image if the row doesnt contain the string "image/png;base64"		
		ModelUtils.fetchImages(network);
		
		// make sure to show the images once they are fetched
		manager.setShowImage(true);
		manager.updateControls();
	}

	@ProvidesTitle
	public String getTitle() {
		return "Fetch structure images";
	}

}
