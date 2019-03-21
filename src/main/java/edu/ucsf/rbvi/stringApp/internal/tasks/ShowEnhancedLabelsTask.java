package edu.ucsf.rbvi.stringApp.internal.tasks;

import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class ShowEnhancedLabelsTask extends AbstractTask {
	final StringManager manager;
	CyNetworkView netView;
	final ShowEnhancedLabelsTaskFactory factory;

	@Tunable(description="Network view to set enhanced labels on",
	         // longDescription = StringToModel.CY_NETWORK_VIEW_LONG_DESCRIPTION,
	         // exampleStringValue = StringToModel.CY_NETWORK_VIEW_EXAMPLE_STRING,
	         context = "nogui")
  public CyNetworkView view = null;


	public ShowEnhancedLabelsTask(final StringManager manager, final CyNetworkView netView,
			final ShowEnhancedLabelsTaskFactory factory) {
		this.manager = manager;
		if (view != null) 
			this.netView = view;
		else
			this.netView = netView;
		this.factory = factory;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Show/hide STRING style labels");

		if (manager.showEnhancedLabels())
			manager.setShowEnhancedLabels(false);
		else
			manager.setShowEnhancedLabels(true);

		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);
		CyNetworkViewManager netManager = manager.getService(CyNetworkViewManager.class);
		for (CyNetworkView currNetView : netManager.getNetworkViewSet()) {
			if (vmm.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_NAME) || 
			    vmm.getVisualStyle(currNetView).getTitle().startsWith(ViewUtils.STYLE_NAME_ORG)) {
				ViewUtils.updateEnhancedLabels(manager, vmm.getVisualStyle(currNetView),
						currNetView.getModel(), manager.showEnhancedLabels());
			}
		}
		netView.updateView();
		factory.reregister();
		manager.updateControls();
	}
}
