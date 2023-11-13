package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Collection;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ShowGlassBallEffectTask extends AbstractTask {
	final StringManager manager;
	CyNetworkView netView;
	final ShowGlassBallEffectTaskFactory factory;
	boolean show;

	@Tunable(description="Network view to set STRING node effect on",
	         // longDescription = StringToModel.CY_NETWORK_VIEW_LONG_DESCRIPTION,
	         // exampleStringValue = StringToModel.CY_NETWORK_VIEW_EXAMPLE_STRING,
	         context = "nogui")
  public CyNetworkView view = null;

	public ShowGlassBallEffectTask(final StringManager manager, final boolean show, 
            final ShowGlassBallEffectTaskFactory factory) {
		this.manager = manager;
		if (view != null)
			this.netView = view;
		else
			this.netView = null;
		this.factory = factory;
		this.show = show;
	}

	public ShowGlassBallEffectTask(final StringManager manager, final CyNetworkView netView,
			final ShowGlassBallEffectTaskFactory factory) {
		this.manager = manager;
		if (view != null) 
			this.netView = view;
		else
			this.netView = netView;
		this.factory = factory;
		this.show = false;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Enable/disable STRING glass ball effect");

		boolean reregister = true;
		if (netView == null) {
			// Command version
			reregister = false;
			manager.setShowGlassBallEffect(show);
			CyNetwork network = manager.getCurrentNetwork();
			Collection<CyNetworkView> views = 
			          manager.getService(CyNetworkViewManager.class).getNetworkViews(network);
			for (CyNetworkView view: views) {
				if (view.getRendererId().equals("org.cytoscape.ding")) {
					netView = view;
					break;
				}
			}
		} else {
			if (manager.showGlassBallEffect()) {
				manager.setShowGlassBallEffect(false);
				show = false;
			} else {
				manager.setShowGlassBallEffect(true);
				manager.setShowFlatNodeDesign(false);
				show = true;
			}
		}

		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		CyNetworkViewManager netManager = manager.getService(CyNetworkViewManager.class);
		for (CyNetworkView currNetView : netManager.getNetworkViewSet()) {
			if (vmm.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_NAME_SIMPLE) || vmm
					.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_ORG)) {
				ViewUtils.updateNodeStyle(manager, vmm.getVisualStyle(currNetView),
						currNetView.getModel(), show);
			}
		}
		if (netView != null)
			netView.updateView();
		// if (reregister) factory.reregister();
		manager.updateControls();
	}
}
