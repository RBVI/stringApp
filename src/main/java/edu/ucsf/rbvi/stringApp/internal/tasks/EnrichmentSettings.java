package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
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

public class EnrichmentSettings implements ActionListener {
	private StringManager manager;
	private CyNetwork network;

	@Tunable(description = "Type of chart to draw",
	         tooltip = "Set the desired chart type",
					 groups = {"Enrichment Defaults"},
	         gravity = 100.0)
	public ListSingleSelection<ChartType> chartType;
	
	@Tunable(description = "Number of terms to chart",
	         tooltip = "Set the default number of terms to use for charts",
					 groups = {"Enrichment Defaults"},
	         gravity = 101.0, params="slider=true")
	public BoundedInteger nTerms = new BoundedInteger(1, 8, 8, false, false);
	
	@Tunable(description = "Default Brewer palette",
	         tooltip = "Set the default Brewer palette for charts",
					 groups = {"Enrichment Defaults"},
	         gravity = 102.0, context="nogui")
	public ListSingleSelection<ColorBrewer> defaultPalette;

	@Tunable(description = "Change Color Palette",
	         tooltip = "Set the default Brewer color palette for charts",
					 groups = {"Enrichment Defaults"},
	         gravity = 103.0, context="gui")
	public UserAction paletteChooser = new UserAction(this);

	@Tunable(description = "Overlap cutoff", 
	         tooltip = "<html>This is the maximum Jaccard similarity that will be allowed.<br/>"+
	                   "Values larger than this cutoff will be excluded.</html>",
					 groups = {"Enrichment Defaults"},
	         params="slider=true", gravity = 109.0)
	public BoundedDouble overlapCutoff = new BoundedDouble(0.0, 0.5, 1.0, false, false);

	public EnrichmentSettings(StringManager manager, CyNetwork network) {
		this.manager = manager;
		this.network = network;

		nTerms.setValue(manager.getTopTerms(network));
		overlapCutoff.setValue(manager.getOverlapCutoff(network));
		ColorBrewer[] palettes = ColorBrewer.getQualitativeColorPalettes(false);
		defaultPalette = new ListSingleSelection<ColorBrewer>(Arrays.asList(palettes));
		chartType = new ListSingleSelection<ChartType>(ChartType.values());
		chartType.setSelectedValue(manager.getChartType(network));
		if (manager.getBrewerPalette(network) != null) defaultPalette.setSelectedValue(manager.getBrewerPalette(network));
	}

	public void actionPerformed(ActionEvent e) {
		ColorPaletteChooserDialog dialog = new ColorPaletteChooserDialog(null, PALETTE_TYPE.QUALITATIVE);
		boolean okPressed = dialog.showDialog();
		if (okPressed)
			defaultPalette.setSelectedValue(dialog.getColorPalette());

		// System.out.println("Default panel is: "+defaultPalette.getSelectedValue());
	}
}
