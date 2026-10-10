package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;

import javax.swing.event.MenuEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskManager;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

@SuppressWarnings("serial")
/**
 * Checkbox menu action, under Apps &gt; STRING, that toggles STRING's glass ball
 * node effect on the current network view by running the
 * {@link ShowGlassBallEffectTaskFactory}'s task iterator; it is enabled only
 * for STRING networks and reflects the current glass-ball-effect state in its
 * checked state.
 */
public class ShowGlassBallEffectPanelAction extends AbstractCyAction {


	final StringManager manager;
		
	public ShowGlassBallEffectPanelAction(String name, StringManager manager) {
		super(name);

		this.manager = manager;
		setPreferredMenu("Apps.STRING");
		setMenuGravity(10.0f);
		useCheckBoxMenuItem = true;
		insertSeparatorBefore = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		ShowGlassBallEffectTaskFactory factory = manager.getShowGlassBallEffectTaskFactory();
		tm.execute(factory.createTaskIterator(manager.getCurrentNetworkView()));
	}

	@Override
	public void menuSelected(MenuEvent evt) {
		updateEnableState();
		putValue(SELECTED_KEY, manager.showGlassBallEffect());
	}
	
	@Override
	public void updateEnableState() {
		setEnabled(ModelUtils.isStringNetwork(manager.getCurrentNetwork()));
	}
	
}
