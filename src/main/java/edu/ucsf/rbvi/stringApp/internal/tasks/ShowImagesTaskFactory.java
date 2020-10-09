package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Properties;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowImagesTaskFactory extends AbstractNetworkViewTaskFactory implements TaskFactory {
	final StringManager manager;
	final boolean show;

	public ShowImagesTaskFactory(final StringManager manager) {
		this.manager = manager;
		this.show = false;
	}

	public ShowImagesTaskFactory(final StringManager manager, final boolean show) {
		this.manager = manager;
		this.show = show;
	}

	@Override
	public boolean isReady(CyNetworkView netView) {
		if (netView == null) return false;
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	@Override
	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new ShowImagesTask(manager, netView, this));
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowImagesTask(manager, show, this));
	}

	public void reregister() {
		manager.unregisterService(this, NetworkViewTaskFactory.class);
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.STRING");
		props.setProperty(COMMAND_NAMESPACE, "string");
		if (manager.showImage()) {
			props.setProperty(TITLE, "Don't show structure images");
			props.setProperty(COMMAND, "hide images");
			props.setProperty(COMMAND_DESCRIPTION, 
			                  "Hide the structure images on the nodes");
			props.setProperty(COMMAND_LONG_DESCRIPTION, 
			                  "Hide the structure images on the nodes.");
		} else {
			props.setProperty(TITLE, "Show structure images");
			props.setProperty(COMMAND, "show images");
			props.setProperty(COMMAND_DESCRIPTION, 
			                  "Show the structure images on the nodes");
			props.setProperty(COMMAND_LONG_DESCRIPTION, 
			                  "Show the structure images on the nodes.");

		}
		props.setProperty(MENU_GRAVITY, "7.0");
		props.setProperty(IN_MENU_BAR, "true");
		props.setProperty(INSERT_SEPARATOR_BEFORE, "true");
		props.setProperty(COMMAND_SUPPORTS_JSON, "true");
		props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
		manager.registerService(this, NetworkViewTaskFactory.class, props);
	}
}

