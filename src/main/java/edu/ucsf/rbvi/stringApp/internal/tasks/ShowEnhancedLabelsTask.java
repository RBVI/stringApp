package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ShowEnhancedLabelsTask extends AbstractTask {
	final StringManager manager;
	CyNetworkView netView;
	final ShowEnhancedLabelsTaskFactory factory;

	public ShowEnhancedLabelsTask(final StringManager manager, final CyNetworkView netView,
			final ShowEnhancedLabelsTaskFactory factory) {
		this.manager = manager;
		this.netView = netView;
		this.factory = factory;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Show STRING style labels");

		if (manager.showEnhancedLabels())
			manager.setShowEnhancedLabels(false);
		else
			manager.setShowEnhancedLabels(true);

		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		CyNetworkViewManager netManager = manager.getService(CyNetworkViewManager.class);
		for (CyNetworkView currNetView : netManager.getNetworkViewSet()) {
			if (vmm.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_NAME) || vmm
					.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_NAME_ORG)) {
				ViewUtils.updateEnhancedLabels(manager, vmm.getVisualStyle(currNetView),
						currNetView, manager.showEnhancedLabels());
			}
		}
		netView.updateView();
		factory.reregister();
	}
}
