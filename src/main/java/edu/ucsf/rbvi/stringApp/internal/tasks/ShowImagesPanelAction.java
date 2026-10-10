package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;

import javax.swing.event.MenuEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskManager;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

@SuppressWarnings("serial")
/**
 * Checkbox menu action, under Apps &gt; STRING, that toggles display of STRING
 * structure images on the current network view by running the
 * {@link ShowImagesTaskFactory}'s task iterator; it is enabled only for
 * STRING networks and reflects the current show-image state in its checked
 * state.
 */
public class ShowImagesPanelAction extends AbstractCyAction {


	final StringManager manager;
		
	public ShowImagesPanelAction(String name, StringManager manager) {
		super(name);

		this.manager = manager;
		setPreferredMenu("Apps.STRING");
		setMenuGravity(7.0f);
		useCheckBoxMenuItem = true;
		insertSeparatorBefore = true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		ShowImagesTaskFactory factory = manager.getShowImagesTaskFactory();
		tm.execute(factory.createTaskIterator(manager.getCurrentNetworkView()));
	}

	@Override
	public void menuSelected(MenuEvent evt) {
		updateEnableState();
		putValue(SELECTED_KEY, manager.showImage());
	}
	
	@Override
	public void updateEnableState() {
		setEnabled(ModelUtils.isStringNetwork(manager.getCurrentNetwork()));
	}
	
}
