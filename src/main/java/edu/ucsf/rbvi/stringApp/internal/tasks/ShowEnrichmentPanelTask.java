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
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;

public class ShowEnrichmentPanelTask extends AbstractTask {
	final StringManager manager;
	final boolean show;
	final ShowEnrichmentPanelTaskFactory factory;

	public ShowEnrichmentPanelTask(final StringManager manager,
			ShowEnrichmentPanelTaskFactory factory, boolean show) {
		this.manager = manager;
		this.factory = factory;
		this.show = show;
	}

	public void run(TaskMonitor monitor) {
		System.out.println("");
		if (show)
			monitor.setTitle("Show enrichment panel");
		else
			monitor.setTitle("Hide enrichment panel");

		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);

		// If the panel is not already registered, create it
		if (show && cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") < 0) {
			System.out.println("create panel");
			CytoPanelComponent2 panel = new EnrichmentCytoPanel(manager);

			// Register it
			manager.registerService(panel, CytoPanelComponent.class, new Properties());
			manager.registerService(panel, RowsSetListener.class, new Properties());

			if (cytoPanel.getState() == CytoPanelState.HIDE)
				cytoPanel.setState(CytoPanelState.DOCK);

		} else if (show && cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0) {
			EnrichmentCytoPanel panel = new EnrichmentCytoPanel(manager);
			System.out.println("update panel");
			panel.initPanel();
		} else if (!show && cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0) {
			System.out.println("unregister panel");
			int compIndex = cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment");
			Component panel = cytoPanel.getComponentAt(compIndex);
			if (panel instanceof CytoPanelComponent2) {
				// Unregister it
				manager.unregisterService(panel, CytoPanelComponent.class);
				manager.unregisterService(panel, RowsSetListener.class);
			}
		}

		factory.reregister();
	}

	public static boolean isPanelRegistered(StringManager sman) {
		CySwingApplication swingApplication = sman.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);

		if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0)
			return true;

		return false;
	}
}
