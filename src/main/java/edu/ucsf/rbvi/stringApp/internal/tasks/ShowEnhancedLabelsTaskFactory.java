package edu.ucsf.rbvi.stringApp.internal.tasks;

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

public class ShowEnhancedLabelsTaskFactory extends AbstractNetworkViewTaskFactory implements TaskFactory {

	final StringManager manager;
	final boolean show;

	public ShowEnhancedLabelsTaskFactory(final StringManager manager) {
		this.manager = manager;
		this.show = false;
	}

	public ShowEnhancedLabelsTaskFactory(final StringManager manager, final boolean show) {
		this.manager = manager;
		this.show = show;
	}

	public boolean isReady(CyNetworkView netView) {
		if (netView == null)
			return false;
		return ModelUtils.isStringNetwork(netView.getModel()) && manager.haveEnhancedGraphics();
	}

	@Override
	public boolean isReady() {
		return true;
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new ShowEnhancedLabelsTask(manager, netView, this));
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowEnhancedLabelsTask(manager, show, this));
	}

	public void reregister() {
		manager.unregisterService(this, NetworkViewTaskFactory.class);
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.STRING");
		if (manager.showEnhancedLabels() && manager.haveEnhancedGraphics()) {
			props.setProperty(TITLE, "Don't show STRING style labels");
		} else if (manager.haveEnhancedGraphics()) {
			props.setProperty(TITLE, "Show STRING style labels");
		}
		props.setProperty(MENU_GRAVITY, "8.0");
		props.setProperty(IN_MENU_BAR, "true");
		manager.registerService(this, NetworkViewTaskFactory.class, props);
	}

}
