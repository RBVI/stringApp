package edu.ucsf.rbvi.stringApp.internal.tasks;

import javax.swing.SwingUtilities;

import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class OpenEvidenceTask extends AbstractTask {
	final StringManager manager;
	final String url;

	public OpenEvidenceTask(final StringManager manager, final String url) {
		this.manager = manager;
		this.url = url;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Linking to text mining URL");
		final OpenBrowser browser = manager.getService(OpenBrowser.class);

		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				browser.openURL(url);
			}
		});

	}
}
