package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;

import javax.swing.event.MenuEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskManager;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

@SuppressWarnings("serial")
/**
 * Checkbox menu action, under Apps &gt; STRING, that shows or hides the STRING
 * results {@link org.cytoscape.application.swing.CytoPanel} by running the
 * {@link ShowResultsPanelTaskFactory}'s task iterator; it is enabled only for
 * STRING networks and reflects whether the panel is currently registered.
 */
public class ShowResultsPanelAction extends AbstractCyAction {

	final StringManager manager;
		
	public ShowResultsPanelAction(String name, StringManager manager) {
		super(name);

		this.manager = manager;
		setPreferredMenu("Apps.STRING");
		setMenuGravity(4.0f);
		useCheckBoxMenuItem = true;
		insertSeparatorBefore = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		ShowResultsPanelTaskFactory factory = manager.getShowResultsPanelTaskFactory();
		tm.execute(factory.createTaskIterator());
	}

	@Override
	public void menuSelected(MenuEvent evt) {
		updateEnableState();
		putValue(SELECTED_KEY, ShowResultsPanelTask.isPanelRegistered(manager));
	}
	
	@Override
	public void updateEnableState() {
		setEnabled(ModelUtils.isStringNetwork(manager.getCurrentNetwork()));
	}
	
}
