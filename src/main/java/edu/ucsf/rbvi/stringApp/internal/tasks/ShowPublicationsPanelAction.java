package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;

import javax.swing.event.MenuEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskManager;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

@SuppressWarnings("serial")
/**
 * Checkbox menu action, under Apps &gt; STRING Enrichment, that shows or hides
 * the publications {@link org.cytoscape.application.swing.CytoPanel} by
 * running the {@link ShowPublicationsPanelTaskFactory}'s task iterator; it is
 * enabled only for STRING networks and reflects whether the panel is
 * currently registered.
 */
public class ShowPublicationsPanelAction extends AbstractCyAction {

	final StringManager manager;
		
	public ShowPublicationsPanelAction(String name, StringManager manager) {
		super(name);

		this.manager = manager;
		setPreferredMenu("Apps.STRING Enrichment");
		setMenuGravity(5.0f);
		useCheckBoxMenuItem = true;
		insertSeparatorBefore = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		ShowPublicationsPanelTaskFactory factory = manager.getShowPublicationsPanelTaskFactory();
		tm.execute(factory.createTaskIterator());
	}

	@Override
	public void menuSelected(MenuEvent evt) {
		updateEnableState();
		putValue(SELECTED_KEY, ShowPublicationsPanelTask.isPanelRegistered(manager));
	}
	
	@Override
	public void updateEnableState() {
		setEnabled(ModelUtils.isStringNetwork(manager.getCurrentNetwork()));
	}
	
}