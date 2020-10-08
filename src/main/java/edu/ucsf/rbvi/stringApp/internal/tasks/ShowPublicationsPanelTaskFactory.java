package edu.ucsf.rbvi.stringApp.internal.tasks;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
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

public class ShowPublicationsPanelTaskFactory extends AbstractTaskFactory {
	final StringManager manager;
	boolean show = false;
	boolean noSignificant = false;

	public ShowPublicationsPanelTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowPublicationsPanelTask(manager, this, show, noSignificant));
	}

	public TaskIterator createTaskIterator(boolean show, boolean noSignificant) {
		return new TaskIterator(new ShowPublicationsPanelTask(manager, this, show, noSignificant));
	}

	public void reregister() {
		manager.unregisterService(this, TaskFactory.class);
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.STRING Enrichment");
		props.setProperty(COMMAND_NAMESPACE, "string");
		if (ShowPublicationsPanelTask.isPanelRegistered(manager)) {
			props.setProperty(TITLE, "Hide publications panel");
			props.setProperty(COMMAND, "hide publications");
			props.setProperty(COMMAND_DESCRIPTION, "Hide the publications panel");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Hide the publications panel");
			show = false;
		} else {
			props.setProperty(TITLE, "Show publications panel");
			props.setProperty(COMMAND, "show publications");
			props.setProperty(COMMAND_DESCRIPTION, "Show the publications panel");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Show the publications panel");
			show = true;
		}
		props.setProperty(COMMAND_SUPPORTS_JSON, "true");
		props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
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
