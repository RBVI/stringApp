package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Font;

import java.util.Collection;

import javax.swing.JPanel;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.OpenBrowser;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public class StringEdgePanel extends JPanel {

	final StringManager manager;
	final OpenBrowser openBrowser;
	final Font iconFont;

	public StringEdgePanel(final StringManager manager) {
		this.manager = manager;
		this.openBrowser = manager.getService(OpenBrowser.class);
		IconManager iconManager = manager.getService(IconManager.class);
		iconFont = iconManager.getIconFont(17.0f);
	}

	public void networkChanged(CyNetwork newNetwork) {
	}

	public void selectedEdges(Collection<CyEdge> edges) {
	}
}
