package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;

import javax.swing.event.MenuEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskManager;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

@SuppressWarnings("serial")
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
