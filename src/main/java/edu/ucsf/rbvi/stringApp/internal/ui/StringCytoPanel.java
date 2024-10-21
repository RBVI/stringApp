package edu.ucsf.rbvi.stringApp.internal.ui;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.LAYERED_STRING_ICON;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.getIconFont;

import java.awt.BorderLayout;
import java.awt.Component;

import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.OpenBrowser;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.TextIcon;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public class StringCytoPanel extends JPanel 
                             implements CytoPanelComponent2, 
                                        SetCurrentNetworkListener, 
                                        SelectedNodesAndEdgesListener {

	final StringManager manager;

	private JTabbedPane tabs;
	private StringNodePanel nodePanel;
	private StringEdgePanel edgePanel;
	private boolean registered = false;
	private static final Icon icon = new TextIcon(LAYERED_STRING_ICON, getIconFont(20.0f), STRING_COLORS, 16, 16);

	public StringCytoPanel(final StringManager manager) {
		this.manager = manager;
		this.setLayout(new BorderLayout());
		tabs = new JTabbedPane(JTabbedPane.BOTTOM);
		nodePanel = new StringNodePanel(manager);
		tabs.add("Nodes", nodePanel);
		edgePanel = new StringEdgePanel(manager);
		tabs.add("Edges", edgePanel);
		this.add(tabs, BorderLayout.CENTER);
		manager.setCytoPanel(this);
		manager.registerService(this, SetCurrentNetworkListener.class, new Properties());
		manager.registerService(this, SelectedNodesAndEdgesListener.class, new Properties());
		registered = true;
		revalidate();
		repaint();
	}


	public void showCytoPanel() {
		// System.out.println("show panel");
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
		if (!registered) {
			manager.registerService(this, CytoPanelComponent.class, new Properties());
			registered = true;
		}
		if (cytoPanel.getState() == CytoPanelState.HIDE)
			cytoPanel.setState(CytoPanelState.DOCK);

		// Tell tabs
		nodePanel.networkChanged(manager.getCurrentNetwork());
		edgePanel.networkChanged(manager.getCurrentNetwork());
	}

	public void reinitCytoPanel() {
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
		if (!registered) {
			manager.registerService(this, CytoPanelComponent.class, new Properties());
			registered = true;
		}
		if (cytoPanel.getState() == CytoPanelState.HIDE)
			cytoPanel.setState(CytoPanelState.DOCK);

		// Tell tabs & remove/undo filters
		CyNetwork current = manager.getCurrentNetwork();
		nodePanel.removeFilters(current);
		nodePanel.undoFilters();
		nodePanel.networkChanged(current);
		edgePanel.removeFilters(current);
		edgePanel.undoFilters();
		edgePanel.networkChanged(current);
	}

	public void hideCytoPanel() {
		manager.unregisterService(this, CytoPanelComponent.class);
		registered = false;
	}

	public String getIdentifier() {
		return "edu.ucsf.rbvi.stringApp.String";
	}

	public Component getComponent() {
		// TODO Auto-generated method stub
		return this;
	}

	public CytoPanelName getCytoPanelName() {
		// TODO Auto-generated method stub
		return CytoPanelName.EAST;
	}

	public Icon getIcon() {
		return icon;
	}

	public String getTitle() {
		return "STRING";
	}

	public void updateControls() {
		nodePanel.updateControls();
		edgePanel.updateScore();
		edgePanel.updateSubPanel();
	}

	@Override
	public void handleEvent(SelectedNodesAndEdgesEvent event) {
		if (!registered) return;
		// Pass selected nodes to nodeTab
		nodePanel.selectedNodes(event.getSelectedNodes());
		// Pass selected edges to edgeTab
		edgePanel.selectedEdges(event.getSelectedEdges());
		// small hack to switch to the Edge tab if only edges are selected 
		// or to the Node tab if only nodes are selected
		if (event.getSelectedNodes().size() > 0 && event.getSelectedEdges().size() == 0)
			tabs.setSelectedIndex(0);
		else if (event.getSelectedNodes().size() == 0 && event.getSelectedEdges().size() > 0)
			tabs.setSelectedIndex(1);
	}

	@Override
	public void handleEvent(SetCurrentNetworkEvent event) {
		CyNetwork network = event.getNetwork();
		if (ModelUtils.ifHaveStringNS(network)) {
			if (!registered) {
				showCytoPanel();
			}

			// Tell tabs
			nodePanel.networkChanged(network);
			edgePanel.networkChanged(network);
		} else {
			hideCytoPanel();
		}
	}

}
