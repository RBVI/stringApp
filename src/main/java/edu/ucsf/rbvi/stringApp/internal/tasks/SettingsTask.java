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
import edu.ucsf.rbvi.stringApp.internal.model.NetworkType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringChannelPaletteProvider;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;

public class SettingsTask extends AbstractTask implements ObservableTask, ActionListener, RequestsUIHelper {

	private StringManager manager;
	private CyNetwork network;
	private String group;
	private Palette channelPalette = null;
	private Component parent;
	private PaletteProvider stringProvider = null;

	@Tunable(description="Species", 
			longDescription="Default species for network queries.",
			exampleStringValue = "Homo Sapiens",
			params="lookup=begins", groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=9.0)
	public ListSingleSelection<Species> species;

	@Tunable(description="Network type", 
			longDescription="Default type of edges for network queries.",
			exampleStringValue = "Functional associations",
			groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=10.0)
	public ListSingleSelection<NetworkType> networkType;

	@Tunable(description="Confidence (score) cutoff", 
			longDescription="Default confidence (score) cutoff.",
			exampleStringValue = "0.4",
			groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=11.0,
	         params="slider=true")
	public BoundedDouble defaultConfidence = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	@Tunable(description="Maximum additional interactors (protein and compound query)", 
			longDescription="Default number of additional interactors for the protein and compound query.",
			exampleStringValue = "0",
			groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=12.0,
	         params="slider=true")
	public BoundedInteger additionalProteins = new BoundedInteger(0, 0, 100, false, false);

	@Tunable(description="Maximum proteins (disease and PubMed query)",
			longDescription="Default number of proteins for the disease and PubMed query.",
			exampleStringValue = "100",
			groups={"Query Defaults (take effect after restarting Cytoscape)"}, gravity=13.0, params="slider=true") 
	public BoundedInteger maxProteins = new BoundedInteger(1, 100, 2000, false, false);

	@Tunable(description="Show structure images",
			longDescription="Show structure images by default.",
			exampleStringValue = "true",
			groups={"View Defaults"}, gravity=14.0)
	public boolean showImage = true;

	@Tunable(description="Show STRING style labels", 
			longDescription="Show STRING style labels by default.",
			exampleStringValue = "true",
			groups={"View Defaults"}, gravity=15.0)
	public boolean showEnhancedLabels = true;

	@Tunable(description="Enable STRING glass ball effect", 
			longDescription="Enable STRING glass ball effect by default.",
			exampleStringValue = "true",
			groups={"View Defaults"}, gravity=16.0)
	public boolean showGlassBallEffect = true;

	@Tunable(description="Change edge channel color palette", 
			longDescription="Set the palette to use for the channel colors.",
			exampleStringValue = "STRING channel colors", 
			groups={"View Defaults"}, gravity=17.0, 
			context="gui")
	public UserAction paletteChooserChannels = new UserAction(this);

	@Tunable(description = "Default palette for edge colors",
	         longDescription = "Set the default palette for edge channel colors.",
	         exampleStringValue = "STRING channel colors", 
	         groups={"View Defaults"}, gravity=18.0,
	         context="nogui")
	public ListSingleSelection<Palette> defaultChannelPalette;

	@ContainsTunables
	public EnrichmentSettings enrichmentSettings;

	public SettingsTask(StringManager manager) {
		this.manager = manager;
		this.network = manager.getCurrentNetwork();
		// TODO: [N] decide what to do here, currently using the default group (all)
		this.group = TermCategory.ALL.getTable();
		
		enrichmentSettings = new EnrichmentSettings(manager, network, group);
		species = new ListSingleSelection<Species>(Species.getGUISpecies());
		species.setSelectedValue(Species.getSpecies(manager.getDefaultSpecies()));
		networkType = new ListSingleSelection<NetworkType>(NetworkType.values());
		networkType.setSelectedValue(manager.getDefaultNetworkType());
		defaultConfidence.setValue(manager.getDefaultConfidence());
		additionalProteins.setValue(manager.getDefaultAdditionalProteins());
		maxProteins.setValue(manager.getDefaultMaxProteins());
		showImage = manager.showImage();
		showEnhancedLabels = manager.showEnhancedLabels();
		showGlassBallEffect = manager.showGlassBallEffect();

		// Set our custom palette provider
		stringProvider = new StringChannelPaletteProvider();

		// Get a default palette
		channelPalette = stringProvider.getPalette("default");
		List<PaletteProvider> providers = manager.getService(PaletteProviderManager.class).getPaletteProviders(BrewerType.QUALITATIVE, false);
		List<Palette> colors = new ArrayList<>();
		colors.add(channelPalette);
		for (PaletteProvider provider: providers) {
			for (String pName: provider.listPaletteNames(BrewerType.QUALITATIVE, false)) {
				colors.add(provider.getPalette(pName, 8));
			}
		}

		defaultChannelPalette = new ListSingleSelection<Palette>(colors);
		defaultChannelPalette.setSelectedValue(channelPalette);
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		arg0.setTitle("stringApp settings");;
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
		manager.setDefaultNetworkType(networkType.getSelectedValue());
		manager.setDefaultConfidence(defaultConfidence.getValue());
		manager.setDefaultAdditionalProteins(additionalProteins.getValue());
		manager.setDefaultMaxProteins(maxProteins.getValue());
		manager.setChannelColors(getChannelColorMap());

		// TODO: [N] is it ok to use null here?
		manager.setTopTerms(null,enrichmentSettings.nTerms.getValue(), null);
		manager.setOverlapCutoff(null,enrichmentSettings.overlapCutoff.getValue(), null);
		manager.setEnrichmentPalette(null,enrichmentSettings.defaultEnrichmentPalette.getSelectedValue(), null);
		manager.setChartType(null,enrichmentSettings.chartType.getSelectedValue(), null);
		manager.updateSettings();

		if (network != null) {
			manager.setTopTerms(network,enrichmentSettings.nTerms.getValue(), group);
			manager.setOverlapCutoff(network,enrichmentSettings.overlapCutoff.getValue(), group);
			manager.setEnrichmentPalette(network,enrichmentSettings.defaultEnrichmentPalette.getSelectedValue(), group);
			manager.setChartType(network,enrichmentSettings.chartType.getSelectedValue(), group);			
		}
		
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
		pm.addPaletteProvider(stringProvider);

		CyColorPaletteChooser paletteChooser = 
			manager.getService(CyColorPaletteChooserFactory.class).getColorPaletteChooser(BrewerType.QUALITATIVE, true);
		channelPalette = paletteChooser.showDialog(parent, "Palette for Channel Colors", channelPalette, manager.channels.length);
		if (channelPalette != null) {
			defaultChannelPalette.setSelectedValue(channelPalette);
			manager.setChannelColors(getChannelColorMap());
		}

		pm.removePaletteProvider(stringProvider);
	}

	public void setUIHelper(TunableUIHelper helper) {
		parent = helper.getParent();
	}

	public Map<String, Color> getChannelColorMap() {
		Map<String, Color> colorMap = new HashMap<>();
		Color[] colors = defaultChannelPalette.getSelectedValue().getColors(manager.channels.length);
		for (int i = 0; i < manager.channels.length; i++) {
			colorMap.put(manager.channels[i], colors[i]);
		}
		return colorMap;
	}

}
