package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowResultsPanelTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	boolean show = false;

	public ShowResultsPanelTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowResultsPanelTask(manager, this, show));
	}

	public void reregister() {
		manager.unregisterService(this, TaskFactory.class);
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.STRING");

		if (ShowResultsPanelTask.isPanelRegistered(manager)) {
			props.setProperty(TITLE, "Hide results panel");
			show = false;
		} else {
			props.setProperty(TITLE, "Show results panel");
			show = true;
		}
		props.setProperty(MENU_GRAVITY, "5.0");
		props.setProperty(IN_MENU_BAR, "true");
		props.setProperty(INSERT_SEPARATOR_BEFORE, "true");
		manager.registerService(this, TaskFactory.class, props);
	}

	public boolean isReady() {
		// We always want to be able to shut it off
		if (!show) return true;

		CyNetwork net = manager.getCurrentNetwork();
		if (net == null) return false;

		if (ModelUtils.isStringNetwork(net)) return true;

		return false;
	}
}

