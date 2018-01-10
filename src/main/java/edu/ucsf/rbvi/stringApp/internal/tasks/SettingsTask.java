package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.util.UserAction;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import org.jcolorbrewer.ColorBrewer;
import org.jcolorbrewer.ui.ColorPaletteChooserDialog;
import org.jcolorbrewer.ui.ColorPaletteChooserDialog.PALETTE_TYPE;

public class SettingsTask extends AbstractTask implements ActionListener {

	private StringManager manager;
	
	@Tunable(description = "Number of terms to chart",
	         tooltip = "Set the default number of terms to use for charts",
	         gravity = 1.0, params="slider=true")
	public BoundedInteger nTerms = new BoundedInteger(1, 8, 8, false, false);
	
	@Tunable(description = "Default Brewer palette",
	         tooltip = "Set the default Brewer palette for charts",
	         gravity = 2.0, context="nogui")
	public ListSingleSelection<ColorBrewer> defaultPalette;

	@Tunable(description = "Change Color Palette",
	         tooltip = "Set the default Brewer color palette for charts",
	         gravity = 2.0, context="gui")
	public UserAction paletteChooser = new UserAction(this);

	@Tunable(description = "Default overlap cutoff", 
	         tooltip = "<html>This is the maximum Jaccard similarity that will be allowed.<br/>"+
	                   "Values larger than this cutoff will be excluded.</html>",
	         params="slider=true", gravity = 9.0)
	public BoundedDouble overlapCutoff = new BoundedDouble(0.0, 0.5, 1.0, false, false);

	public SettingsTask(StringManager manager) {
		this.manager = manager;
		nTerms.setValue(manager.topTerms);
		overlapCutoff.setValue(manager.overlapCutoff);
		ColorBrewer[] palettes = ColorBrewer.getQualitativeColorPalettes(false);
		defaultPalette = new ListSingleSelection<ColorBrewer>(Arrays.asList(palettes));
		if (manager.brewerPalette != null) defaultPalette.setSelectedValue(manager.brewerPalette);
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		manager.topTerms = nTerms.getValue();
		manager.overlapCutoff = overlapCutoff.getValue();
		manager.brewerPalette = defaultPalette.getSelectedValue();
		manager.updateSettings();
	}

	public void actionPerformed(ActionEvent e) {
		ColorPaletteChooserDialog dialog = new ColorPaletteChooserDialog(null, PALETTE_TYPE.QUALITATIVE);
		boolean okPressed = dialog.showDialog();
		if (okPressed)
			defaultPalette.setSelectedValue(dialog.getColorPalette());

		// System.out.println("Default panel is: "+defaultPalette.getSelectedValue());
	}

}
