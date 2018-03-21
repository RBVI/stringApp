package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.util.UserAction;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.ChartType;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import org.jcolorbrewer.ColorBrewer;
import org.jcolorbrewer.ui.ColorPaletteChooserDialog;
import org.jcolorbrewer.ui.ColorPaletteChooserDialog.PALETTE_TYPE;

public class SettingsTask extends AbstractTask implements ActionListener {

	private StringManager manager;
	private CyNetwork network;

	@Tunable(description = "Type of chart to draw",
	         tooltip = "Set the desired chart type",
	         gravity = 1.0)
	public ListSingleSelection<ChartType> chartType;
	
	@Tunable(description = "Number of terms to chart",
	         tooltip = "Set the default number of terms to use for charts",
	         gravity = 2.0, params="slider=true")
	public BoundedInteger nTerms = new BoundedInteger(1, 8, 8, false, false);
	
	@Tunable(description = "Default Brewer palette",
	         tooltip = "Set the default Brewer palette for charts",
	         gravity = 3.0, context="nogui")
	public ListSingleSelection<ColorBrewer> defaultPalette;

	@Tunable(description = "Change Color Palette",
	         tooltip = "Set the default Brewer color palette for charts",
	         gravity = 3.0, context="gui")
	public UserAction paletteChooser = new UserAction(this);

	@Tunable(description = "Overlap cutoff", 
	         tooltip = "<html>This is the maximum Jaccard similarity that will be allowed.<br/>"+
	                   "Values larger than this cutoff will be excluded.</html>",
	         params="slider=true", gravity = 9.0)
	public BoundedDouble overlapCutoff = new BoundedDouble(0.0, 0.5, 1.0, false, false);

	@Tunable(description = "Make these settings the default",
	         longDescription = "Unless this is set to true, these settings only apply to the current network",
	         tooltip = "<html>Unless this is set to true, these settings only apply to the current network.</html>")
	public boolean makeDefault = false;

	public SettingsTask(StringManager manager) {
		this.manager = manager;
		this.network = manager.getCurrentNetwork();

		nTerms.setValue(manager.getTopTerms(network));
		overlapCutoff.setValue(manager.getOverlapCutoff(network));
		ColorBrewer[] palettes = ColorBrewer.getQualitativeColorPalettes(false);
		defaultPalette = new ListSingleSelection<ColorBrewer>(Arrays.asList(palettes));
		chartType = new ListSingleSelection<ChartType>(ChartType.values());
		chartType.setSelectedValue(manager.getChartType(network));
		if (manager.getBrewerPalette(network) != null) defaultPalette.setSelectedValue(manager.getBrewerPalette(network));
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		if (makeDefault) {
			manager.setTopTerms(null,nTerms.getValue());
			manager.setOverlapCutoff(null,overlapCutoff.getValue());
			manager.setBrewerPalette(null,defaultPalette.getSelectedValue());
			manager.setChartType(null,chartType.getSelectedValue());
			manager.updateSettings();
		} else {
			manager.setTopTerms(network,nTerms.getValue());
			manager.setOverlapCutoff(network,overlapCutoff.getValue());
			manager.setBrewerPalette(network,defaultPalette.getSelectedValue());
			manager.setChartType(network,chartType.getSelectedValue());
		}
		// TODO: maybe this is a way to automatically apply settings?
		TaskManager<?, ?> tm = manager.getService(TaskManager.class);
		tm.execute(new ShowChartsTaskFactory(manager).createTaskIterator());
	}

	public void actionPerformed(ActionEvent e) {
		ColorPaletteChooserDialog dialog = new ColorPaletteChooserDialog(null, PALETTE_TYPE.QUALITATIVE);
		boolean okPressed = dialog.showDialog();
		if (okPressed)
			defaultPalette.setSelectedValue(dialog.getColorPalette());

		// System.out.println("Default panel is: "+defaultPalette.getSelectedValue());
	}

	
	@ProvidesTitle
	public String getTitle() {
		return "Settings for STRING Enrichment table";
	}	
}
