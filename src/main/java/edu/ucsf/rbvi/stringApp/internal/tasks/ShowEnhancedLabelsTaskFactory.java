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
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowEnhancedLabelsTaskFactory extends AbstractNetworkViewTaskFactory {

	final StringManager manager;

	public ShowEnhancedLabelsTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetworkView netView) {
		if (netView == null)
			return false;
		return ModelUtils.isStringNetwork(netView.getModel()) && manager.haveEnhancedGraphics();
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new ShowEnhancedLabelsTask(manager, netView, this));
	}

	public void reregister() {
		manager.unregisterService(this, NetworkViewTaskFactory.class);
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.STRING");
		if (manager.showEnhancedLabels() && manager.haveEnhancedGraphics()) {
			props.setProperty(TITLE, "Don't show STRING style labels");
		} else {
			props.setProperty(TITLE, "Show STRING style labels");
		}
		props.setProperty(MENU_GRAVITY, "8.0");
		props.setProperty(IN_MENU_BAR, "true");
		manager.registerService(this, NetworkViewTaskFactory.class, props);
	}
}
