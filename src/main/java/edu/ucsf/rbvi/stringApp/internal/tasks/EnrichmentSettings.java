package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.Palette;
import org.cytoscape.util.color.PaletteProvider;
import org.cytoscape.util.color.PaletteProviderManager;
import org.cytoscape.util.color.PaletteType;
import org.cytoscape.util.swing.CyColorPaletteChooser;
import org.cytoscape.util.swing.CyColorPaletteChooserFactory;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.swing.RequestsUIHelper;
import org.cytoscape.work.swing.TunableUIHelper;
import org.cytoscape.work.swing.util.UserAction;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.ChartType;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
// import org.jcolorbrewer.ColorBrewer;
// import org.jcolorbrewer.ui.ColorPaletteChooserDialog;
// import org.jcolorbrewer.ui.ColorPaletteChooserDialog.PALETTE_TYPE;

public class EnrichmentSettings implements ActionListener, RequestsUIHelper {
	private StringManager manager;
	private CyNetwork network;
	private String group;
	private Component parent;

	@Tunable(description = "Type of chart to draw",
	         tooltip = "Set the desired chart type",
	         longDescription = "Set the desired chart type",
	         exampleStringValue = "Split donut",
					 groups = {"Enrichment Defaults"},
	         gravity = 100.0)
	public ListSingleSelection<ChartType> chartType;
	
	@Tunable(description = "Number of terms to chart",
	         tooltip = "Set the default number of terms to use for charts",
	         longDescription = "Set the default number of terms to use for charts",
	         exampleStringValue = "5",
					 groups = {"Enrichment Defaults"},
	         gravity = 101.0, params="slider=true")
	public BoundedInteger nTerms = new BoundedInteger(1, 5, 8, false, false);
	
	@Tunable(description = "Default Brewer palette for enrichment charts",
	         longDescription = "Set the default Brewer palette for enrichment charts",
	         exampleStringValue = "ColorBrewer Paired colors",
					 groups = {"Enrichment Defaults"},
	         gravity = 102.0, context="nogui")
	public ListSingleSelection<Palette> defaultEnrichmentPalette;

	@Tunable(description = "Change enrichment color palette",
	         tooltip = "Set the default Brewer color palette for enrichment charts",
					 groups = {"Enrichment Defaults"},
	         gravity = 103.0, context="gui")
	public UserAction paletteChooserEnrichment = new UserAction(this);

	@Tunable(description = "Overlap cutoff", 
	         tooltip = "<html>This is the maximum Jaccard similarity that will be allowed.<br/>"+
	                   "Values larger than this cutoff will be excluded.</html>",
	         longDescription = "This is the maximum Jaccard similarity that will be allowed."+
	    	                   "Values larger than this cutoff will be excluded.",
	         exampleStringValue = "0.5",
					 groups = {"Enrichment Defaults"},
	         params="slider=true", gravity = 109.0)
	public BoundedDouble overlapCutoff = new BoundedDouble(0.0, 0.5, 1.0, false, false);

	// TODO: [N] is it ok to just add the group here?
	public EnrichmentSettings(StringManager manager, CyNetwork network, String group) {
		this.manager = manager;
		this.network = network;
		this.group = group;

		PaletteProviderManager pm = manager.getService(PaletteProviderManager.class);
		List<PaletteProvider> providers = pm.getPaletteProviders(BrewerType.QUALITATIVE, false);
		List<Palette> palettes = new ArrayList<>();
		for (PaletteProvider provider: providers) {
			List<String> paletteList = provider.listPaletteNames(BrewerType.QUALITATIVE, false);
			for (String id: paletteList) {
				palettes.add(provider.getPalette(id));
			}

		}
		// ColorBrewer[] palettes = ColorBrewer.getQualitativeColorPalettes(false);
		defaultEnrichmentPalette = new ListSingleSelection<Palette>(palettes);
		defaultEnrichmentPalette.setSelectedValue(manager.getEnrichmentPalette(network, group));
		
		nTerms.setValue(manager.getTopTerms(network, group));
		overlapCutoff.setValue(manager.getOverlapCutoff(network, group));
		chartType = new ListSingleSelection<ChartType>(ChartType.values());
		chartType.setSelectedValue(manager.getChartType(network, group));
	}

	public void actionPerformed(ActionEvent e) {
		CyColorPaletteChooser paletteChooser = 
			manager.getService(CyColorPaletteChooserFactory.class).getColorPaletteChooser(BrewerType.QUALITATIVE, true);
		Palette palette = paletteChooser.showDialog(parent, "Palette for Enrichment Colors", 
				manager.getEnrichmentPalette(network, group), nTerms.getValue());
		if (palette != null) {
			manager.setEnrichmentPalette(network, palette, group);
			defaultEnrichmentPalette.setSelectedValue(palette);
		}

		/*
		ColorPaletteChooserDialog dialog = new ColorPaletteChooserDialog(null, PALETTE_TYPE.QUALITATIVE);
		if (defaultPalette.getSelectedValue() != null)
			dialog.setColorBrewer(defaultPalette.getSelectedValue());
		boolean okPressed = dialog.showDialog();
		if (okPressed) {
			defaultPalette.setSelectedValue(dialog.getColorPalette());
		}
		*/
	}

	public void setUIHelper(TunableUIHelper helper) {
		parent = helper.getParent();
	}
}
