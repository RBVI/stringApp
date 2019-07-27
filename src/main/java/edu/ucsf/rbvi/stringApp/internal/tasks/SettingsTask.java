package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.Palette;
import org.cytoscape.util.color.PaletteProvider;
import org.cytoscape.util.color.PaletteProviderManager;
import org.cytoscape.util.color.PaletteType;
import org.cytoscape.util.swing.CyColorPaletteChooser;
import org.cytoscape.util.swing.CyColorPaletteChooserFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.swing.RequestsUIHelper;
import org.cytoscape.work.swing.TunableUIHelper;
import org.cytoscape.work.swing.util.UserAction;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.ChartType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringChannelPaletteProvider;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class SettingsTask extends AbstractTask implements ObservableTask, ActionListener, RequestsUIHelper {

	private StringManager manager;
	private CyNetwork network;
	private Palette channelPalette = null;
	private Component parent;

	@Tunable(description="Species", 
			longDescription="Default species",
			exampleStringValue = "Homo Sapiens",
			params="lookup=begin", groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=10.0)
	public ListSingleSelection<Species> species;

	@Tunable(description="Confidence (score) cutoff", 
			longDescription="Default confidence (score) cutoff",
			exampleStringValue = "0.4",
			groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=11.0,
	         params="slider=true")
	public BoundedDouble defaultConfidence = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	@Tunable(description="Maximum additional interactors (protein and compound query)", 
			longDescription="Default number of additional interactors for the protein and compound query",
			exampleStringValue = "0",
			groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=12.0,
	         params="slider=true")
	public BoundedInteger additionalProteins = new BoundedInteger(0, 0, 100, false, false);

	@Tunable(description="Maximum proteins (disease and PubMed query)",
			longDescription="Default number of proteins for the disease and PubMed query",
			exampleStringValue = "100",
			groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=13.0, params="slider=true") public BoundedInteger maxProteins = new BoundedInteger(1, 100, 2000, false, false);

	@Tunable(description="Show structure images",
			longDescription="Show structure images by default",
			exampleStringValue = "true",
			groups={"View Defaults"}, gravity=14.0)
	public boolean showImage = true;

	@Tunable(description="Show STRING style labels", 
			longDescription="Show STRING style labels by default",
			exampleStringValue = "true",
			groups={"View Defaults"}, gravity=15.0)
	public boolean showEnhancedLabels = true;

	@Tunable(description="Enable STRING glass ball effect", 
			longDescription="Enable STRING glass ball effect by default",
			exampleStringValue = "true",
			groups={"View Defaults"}, gravity=16.0)
	public boolean showGlassBallEffect = true;

	@Tunable(description="Edge channel color palettes", 
			longDescription="Set the palette to use for the channel colors",
			exampleStringValue = "STRING channel colors", groups={"View Defaults"}, gravity=17.0)
	public UserAction paletteChooser = new UserAction(this);

	@ContainsTunables
	public EnrichmentSettings enrichmentSettings;

	public SettingsTask(StringManager manager) {
		this.manager = manager;
		this.network = manager.getCurrentNetwork();
		enrichmentSettings = new EnrichmentSettings(manager, network);
		species = new ListSingleSelection<Species>(Species.getSpecies());
		species.setSelectedValue(manager.getDefaultSpecies());
		defaultConfidence.setValue(manager.getDefaultConfidence());
		additionalProteins.setValue(manager.getDefaultAdditionalProteins());
		maxProteins.setValue(manager.getDefaultMaxProteins());
		showImage = manager.showImage();
		showEnhancedLabels = manager.showEnhancedLabels();
		showGlassBallEffect = manager.showGlassBallEffect();


		/*
		List<PaletteProvider> providers = manager.getService(PaletteProviderManager.class).getPaletteProviders(BrewerType.QUALITATIVE, false);
		for (PaletteProvider provider: providers) {
			for (String pName: provider.listPaletteNames(BrewerType.QUALITATIVE, false)) {
				colors.add(provider.getPalette(pName, 7));
			}
		}

		channelColors = new ListSingleSelection<Palette>(colors);
		*/
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		// manager.setDefaultSpecies(species.getSelectedValue());
		// System.out.println(species.getSelectedValue());
		// System.out.println(defaultConfidence.getValue());

		TaskManager<?, ?> tm = (TaskManager<?, ?>) manager.getService(TaskManager.class);
		CyNetworkView currentView = manager.getCurrentNetworkView();

		if (manager.showEnhancedLabels() != showEnhancedLabels) {
			if (currentView != null)
				tm.execute(manager.getShowEnhancedLabelsTaskFactory().createTaskIterator(currentView));
			else
				manager.setShowEnhancedLabels(showEnhancedLabels);
		}
		if (manager.showImage() != showImage) {
			if (currentView != null)
				tm.execute(manager.getShowImagesTaskFactory().createTaskIterator(currentView));
			else
				manager.setShowImage(showImage);
		}
		
		if (manager.showGlassBallEffect() != showGlassBallEffect) {
			if (currentView != null)
				tm.execute(manager.getShowGlassBallEffectTaskFactory().createTaskIterator(currentView));
			else
				manager.setShowGlassBallEffect(showGlassBallEffect);
		}

		manager.setDefaultSpecies(species.getSelectedValue());
		manager.setDefaultConfidence(defaultConfidence.getValue());
		manager.setDefaultAdditionalProteins(additionalProteins.getValue());
		manager.setDefaultMaxProteins(maxProteins.getValue());
		manager.setChannelColors(getChannelColorMap());

		manager.setTopTerms(null,enrichmentSettings.nTerms.getValue());
		manager.setOverlapCutoff(null,enrichmentSettings.overlapCutoff.getValue());
		manager.setEnrichmentPalette(null,enrichmentSettings.defaultPalette.getSelectedValue());
		manager.setChartType(null,enrichmentSettings.chartType.getSelectedValue());
		manager.updateSettings();
		
	}

	@ProvidesTitle
	public String getTitle() {
		return "Default settings for stringApp";
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(String.class)) {
			return (R)"";
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				return "{}";
			};
			return (R)res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class);
	}

	public void actionPerformed(ActionEvent ae) {
		PaletteProviderManager pm = manager.getService(PaletteProviderManager.class);
		PaletteProvider stringProvider = new StringChannelPaletteProvider();
		pm.addPaletteProvider(stringProvider);

		CyColorPaletteChooser paletteChooser = 
			manager.getService(CyColorPaletteChooserFactory.class).getColorPaletteChooser(BrewerType.QUALITATIVE, true);
		Palette initial = stringProvider.getPalette("default");
		channelPalette = paletteChooser.showDialog(parent, "Palette for Channel Colors", initial, 7);

		pm.removePaletteProvider(stringProvider);
	}

	public void setUIHelper(TunableUIHelper helper) {
		parent = helper.getParent();
	}

	public Map<String, Color> getChannelColorMap() {
		Map<String, Color> colorMap = new HashMap<>();
		System.out.println("Selected palette = "+channelPalette);
		Color[] colors = channelPalette.getColors(7);
		for (int i = 0; i < 7; i++) {
			System.out.println(manager.channels[i]+" = "+colors[i]);
			colorMap.put(manager.channels[i], colors[i]);
		}
		return colorMap;
	}

}
