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

import java.util.Properties;

import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowNewNodeEffectTaskFactory extends AbstractNetworkViewTaskFactory implements TaskFactory {

	final StringManager manager;
	final boolean show;

	public ShowNewNodeEffectTaskFactory(final StringManager manager) {
		this.manager = manager;
		this.show = false;
	}

	public ShowNewNodeEffectTaskFactory(final StringManager manager, final boolean show) {
		this.manager = manager;
		this.show = show;
	}

	public boolean isReady(CyNetworkView netView) {
		if (netView == null)
			return false;
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	@Override
	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new ShowNewNodeEffectTask(manager, netView, this));
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowNewNodeEffectTask(manager, show, this));
	}


//	public void reregister() {
//		manager.unregisterService(this, NetworkViewTaskFactory.class);
//		Properties props = new Properties();
//		props.setProperty(PREFERRED_MENU, "Apps.STRING");
//		props.setProperty(COMMAND_NAMESPACE, "string");
//		if (manager.showGlassBallEffect()) {
//			props.setProperty(TITLE, "Disable STRING glass balls effect");
//			props.setProperty(COMMAND, "hide glass");
//			props.setProperty(COMMAND_DESCRIPTION, 
//			                  "Hide the STRING glass ball effect on the nodes");
//			props.setProperty(COMMAND_LONG_DESCRIPTION, 
//			                  "Hide the STRING glass ball effect on the nodes.");
//		} else {
//			props.setProperty(TITLE, "Enable STRING glass balls effect");
//			props.setProperty(COMMAND, "show glass");
//			props.setProperty(COMMAND_DESCRIPTION, 
//			                  "Show the STRING glass ball effect on the nodes");
//			props.setProperty(COMMAND_LONG_DESCRIPTION, 
//			                  "Show the STRING glass ball effect on the nodes.");
//
//		}
//		props.setProperty(MENU_GRAVITY, "9.0");
//		props.setProperty(IN_MENU_BAR, "true");
//		props.setProperty(COMMAND_SUPPORTS_JSON, "true");
//		props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
//		manager.registerService(this, NetworkViewTaskFactory.class, props);
//	}

}
