package edu.ucsf.rbvi.stringApp.internal.tasks;

import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.List;
import java.util.Properties;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowEnrichmentPanelTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	boolean show = false;

	public ShowEnrichmentPanelTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowEnrichmentPanelTask(manager, this, show));
	}

	public TaskIterator createTaskIterator(boolean show) {
		return new TaskIterator(new ShowEnrichmentPanelTask(manager, this, show));
	}

	public void reregister() {
		manager.unregisterService(this, TaskFactory.class);
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.STRING");
		if (ShowEnrichmentPanelTask.isPanelRegistered(manager)) {
			props.setProperty(TITLE, "Hide enrichment panel");
			show = false;
		} else {
			props.setProperty(TITLE, "Show enrichment panel");
			show = true;
		}
		props.setProperty(MENU_GRAVITY, "5.0");
		props.setProperty(IN_MENU_BAR, "true");
		manager.registerService(this, TaskFactory.class, props);
	}

	public boolean isReady() {
		// We always want to be able to shut it off
		if (!show)
			return true;

		CyNetwork net = manager.getCurrentNetwork();
		if (net == null)
			return false;

		if (ModelUtils.isStringNetwork(net)) 
			return true;

		return false;
	}
}
