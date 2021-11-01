package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.Component;
import java.util.List;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;

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
		if (!show && cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0) {
			int compIndex = cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment");
			Component panel = cytoPanel.getComponentAt(compIndex);
			if (panel instanceof CytoPanelComponent2) {
				// Unregister it
				manager.unregisterService(panel, CytoPanelComponent.class);
				manager.unregisterService(panel, RowsSetListener.class);
				manager.unregisterService(panel, SelectedNodesAndEdgesListener.class);
			}
		} else if (show && cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0) {
			// Special case...
			EnrichmentCytoPanel panel = (EnrichmentCytoPanel) cytoPanel.getComponentAt(
					cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
			panel.initPanel(noSignificant);
			cytoPanel.setSelectedIndex(
					cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
		} else {
			// If the panel is not already registered, create it
			CytoPanelComponent2 panel = new EnrichmentCytoPanel(manager, noSignificant);

			// Register it
			manager.registerService(panel, CytoPanelComponent.class, new Properties());
			manager.registerService(panel, RowsSetListener.class, new Properties());
			manager.registerService(panel, SelectedNodesAndEdgesListener.class, new Properties());

			if (cytoPanel.getState() == CytoPanelState.HIDE)
				cytoPanel.setState(CytoPanelState.DOCK);

			cytoPanel.setSelectedIndex(
					cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment"));
		}
		// factory.reregister();
	}

	public static boolean isPanelRegistered(StringManager sman) {
		CySwingApplication swingApplication = sman.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.SOUTH);

		if (cytoPanel.indexOfComponent("edu.ucsf.rbvi.stringApp.Enrichment") >= 0)
			return true;

		return false;
	}
}
