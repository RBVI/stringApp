package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.Component;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;

/**
 * Task that registers or unregisters the {@link EnrichmentCytoPanel} as a
 * {@code CytoPanelComponent} in the south {@code CytoPanel}: if the panel is
 * already present it is unregistered and cleared from the
 * {@link StringManager}, otherwise a new panel is created, registered, docked,
 * and selected. Also provides a static helper to check whether the panel is
 * currently registered.
 */
public class ShowEnrichmentPanelTask extends AbstractTask {
	final StringManager manager;
	final boolean show;
	final boolean noSignificant;
	final ShowEnrichmentPanelTaskFactory factory;

	public ShowEnrichmentPanelTask(final StringManager manager,
			ShowEnrichmentPanelTaskFactory factory, boolean show, boolean noSignificant) {
		this.manager = manager;
		this.factory = factory;
		this.show = show;
		this.noSignificant = noSignificant;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Show/hide enrichment panel");

		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);

		// If the panel is already registered, but should not be shown, unregister it
		if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0) {
			int compIndex = cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment");
			Component panel = cytoPanel.getComponentAt(compIndex);
			if (panel instanceof CytoPanelComponent2) {
				// Unregister it
				manager.unregisterService(panel, CytoPanelComponent.class);
				manager.setEnrichPanel(null);
			}
		} else {
			// If the panel is not already registered, create it
			CytoPanelComponent2 panel = new EnrichmentCytoPanel(manager, noSignificant);

			// Register it
			manager.registerService(panel, CytoPanelComponent.class, new Properties());

			if (cytoPanel.getState() == CytoPanelState.HIDE)
				cytoPanel.setState(CytoPanelState.DOCK);

			cytoPanel.setSelectedIndex(
					cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
		}
	}

	public static boolean isPanelRegistered(StringManager sman) {
		CySwingApplication swingApplication = sman.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);

		if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0)
			return true;

		return false;
	}
}
