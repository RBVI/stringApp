package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.Component;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.ui.StringCytoPanel;

public class ShowResultsPanelTask extends AbstractTask {
	final StringManager manager;
	final ShowResultsPanelTaskFactory factory;
	final boolean show;

	public ShowResultsPanelTask(final StringManager manager, 
	                            final ShowResultsPanelTaskFactory factory, boolean show) {
		this.manager = manager;
		this.factory = factory;
		this.show = show;
	}

	public void run(TaskMonitor monitor) {

		if (show)
			monitor.setTitle("Show results panel");
		else
			monitor.setTitle("Hide results panel");

		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);

		// If the panel is not already registered, create it
		if (show && cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.String") < 0) {
			CytoPanelComponent2 panel = new StringCytoPanel(manager);

			// Register it
			manager.registerService(panel, CytoPanelComponent.class, new Properties());
			// manager.registerService(panel, RowsSetListener.class, new Properties());

			if (cytoPanel.getState() == CytoPanelState.HIDE)
				cytoPanel.setState(CytoPanelState.DOCK);

			// manager.registerService(panel, SetCurrentNetworkListener.class, new Properties());
		} else if (!show && cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.String") >= 0) {
			int compIndex = cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.String");
			Component panel = cytoPanel.getComponentAt(compIndex);
			if (panel instanceof CytoPanelComponent2) {
				// Unregister it
				manager.unregisterService(panel, CytoPanelComponent.class);
				// manager.unregisterService(panel, RowsSetListener.class);
			}
		}

		factory.reregister();
	}

	public static boolean isPanelRegistered(StringManager sman) {
		CySwingApplication swingApplication = sman.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);

		if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.String") >= 0) 
			return true;

		return false;
	}
}
