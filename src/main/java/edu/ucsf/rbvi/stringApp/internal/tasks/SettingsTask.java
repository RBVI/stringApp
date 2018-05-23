package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stringApp.internal.model.ChartType;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class SettingsTask extends AbstractTask implements ObservableTask {

	private StringManager manager;
	private CyNetwork network;

	@Tunable(description="Species", 
			longDescription="Default species",
			exampleStringValue = "Homo Sapiens",
			params="lookup=begin", groups={"Query Defaults"}, gravity=10.0)
	public ListSingleSelection<Species> species;

	@Tunable(description="Confidence (score) cutoff", 
			longDescription="Default confidence (score) cutoff",
			exampleStringValue = "0.4",
			groups={"Query Defaults"}, gravity=11.0,
	         params="slider=true")
	public BoundedDouble defaultConfidence = new BoundedDouble(0.0, 0.4, 1.0, false, false);

	@Tunable(description="Maximum additional interactors (protein and compound query)", 
			longDescription="Default number of additional interactors for the protein and compound query",
			exampleStringValue = "0",
			groups={"Query Defaults"}, gravity=12.0,
	         params="slider=true")
	public BoundedInteger additionalProteins = new BoundedInteger(0, 0, 100, false, false);

	@Tunable(description="Maximum proteins (disease and PubMed query)",
			longDescription="Default number of proteins for the disease and PubMed query",
			exampleStringValue = "100",
			groups={"Query Defaults"}, gravity=13.0,
	         params="slider=true")
	public BoundedInteger maxProteins = new BoundedInteger(1, 100, 1000, false, false);

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
	}

	@Override
	public void run(TaskMonitor arg0) throws Exception {
		// manager.setDefaultSpecies(species.getSelectedValue());
		System.out.println(species.getSelectedValue());
		System.out.println(defaultConfidence);
		
		manager.setShowEnhancedLabels(showEnhancedLabels);
		manager.setShowImage(showImage);
		manager.setShowGlassBallEffect(showGlassBallEffect);

		manager.setDefaultSpecies(species.getSelectedValue());
		manager.setDefaultConfidence(defaultConfidence.getValue());
		manager.setDefaultAdditionalProteins(additionalProteins.getValue());
		manager.setDefaultMaxProteins(maxProteins.getValue());

		manager.setTopTerms(null,enrichmentSettings.nTerms.getValue());
		manager.setOverlapCutoff(null,enrichmentSettings.overlapCutoff.getValue());
		manager.setBrewerPalette(null,enrichmentSettings.defaultPalette.getSelectedValue());
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

}
