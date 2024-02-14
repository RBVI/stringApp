package edu.ucsf.rbvi.stringApp.internal.tasks;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STITCH_LAYERS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.getIconFont;

import java.awt.Dialog;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.ui.GetTermsPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchOptionsPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchQueryComponent;
import edu.ucsf.rbvi.stringApp.internal.utils.TextIcon;
import edu.ucsf.rbvi.stringApp.internal.utils.TextUtils;

public class StitchSearchTaskFactory extends AbstractNetworkSearchTaskFactory implements TaskObserver {
	StringManager manager;
	static String STITCH_ID = "edu.ucsf.rbvi.stitch";
	static String STITCH_URL = "http://stitch-db.org";
	static String STITCH_NAME = "STITCH: compound query";
	static String STITCH_DESC = "Search STITCH for protein-compound interactions";
	static String STITCH_DESC_LONG = "<html>The compound query retrieves a STITCH network for one or more proteins or compounds. <br />"
										+ "STITCH is a resource to explore known and predicted interactions of chemicals and <br />"
										+ "proteins. Chemicals are linked to other chemicals and proteins by evidence derived <br />"
										+ "from experiments, databases and the literature.</html>";

	private StringNetwork stringNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private SearchQueryComponent queryComponent = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	private static final Icon icon = new TextIcon(STITCH_LAYERS, getIconFont(32.0f), STRING_COLORS, 36, 36);

	private static URL stitchURL() {
		try {
			return new URL(STITCH_URL);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public StitchSearchTaskFactory(StringManager manager) {
		super(STITCH_ID, STITCH_NAME, STITCH_DESC, icon, StitchSearchTaskFactory.stitchURL());
		this.manager = manager;
	}

	public boolean isReady() { 
		if (manager.haveURIs() &&
		    queryComponent.getQueryText() != null && queryComponent.getQueryText().length() > 0 && getTaxId() != -1)
			return true; 
		return false;
	}

	public TaskIterator createTaskIterator() {
		String terms = queryComponent.getQueryText();
		if (optionsPanel.getUseSmartDelimiters())
			terms = TextUtils.smartDelimit(terms);

		stringNetwork = new StringNetwork(manager);
		return new TaskIterator(new GetAnnotationsTask(stringNetwork, getSpecies(), terms, Databases.STITCH.getAPIName()));
	}

	@Override
	public String getName() { return STITCH_NAME; }

	@Override
	public String getId() { return STITCH_ID; }

	@Override
	public String getDescription() {
		return STITCH_DESC_LONG;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() { 
		return StitchSearchTaskFactory.stitchURL();
	}

	// Create a JPanel that provides the species, confidence interval, and number of interactions
	// NOTE: we need to use reasonable defaults since it's likely the user won't actually change it...
	@Override
	public JComponent getOptionsComponent() {
		optionsPanel = new SearchOptionsPanel(manager);
		return optionsPanel;
	}

	@Override
	public JComponent getQueryComponent() {
		if (queryComponent == null)
			queryComponent = new SearchQueryComponent();
		return queryComponent;
	}

	@Override
	public TaskObserver getTaskObserver() { return this; }

	public int getTaxId() {
		return getSpecies().getTaxId();
	}

	public Species getSpecies() {
		try {
			if (optionsPanel.getSpecies() != null) {
				return optionsPanel.getSpecies();
			}
			return Species.getHumanSpecies(); // Homo sapiens
		} catch (RuntimeException e) {
			// The user might not have given us a full species name
			String name = optionsPanel.getSpeciesText();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, "Unknown species: '"+name+"'",
								                        "Unknown species", JOptionPane.ERROR_MESSAGE); 
				}
			});
      // Reset
      optionsPanel.setSpeciesText(manager.getDefaultSpecies());
			return null;
		}
	}

	public String getSpeciesName() {
		// This will eventually come from the OptionsComponent...
		if (optionsPanel.getSpecies() != null)
			return optionsPanel.getSpecies().toString();
		return "Homo sapiens"; // Homo sapiens
	}

	public int getAdditionalNodes() {
		// This will eventually come from the OptionsComponent...
		return optionsPanel.getAdditionalNodes();
	}

	public int getConfidence() {
		// This will eventually come from the OptionsComponent...
		return optionsPanel.getConfidence();
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
    if (optionsPanel.getLoadEnrichment()) {
      GetEnrichmentTaskFactory tf = new GetEnrichmentTaskFactory(manager, true);
      ShowEnrichmentPanelTaskFactory showTf = manager.getShowEnrichmentPanelTaskFactory();
      tf.setShowEnrichmentPanelFactory(showTf);
      TunableSetter setter = manager.getService(TunableSetter.class);
      Map<String, Object> valueMap = new HashMap<>();
      valueMap.put("cutoff", 0.05);
      TaskIterator newIterator =
              setter.createTaskIterator(tf.createTaskIterator(manager.getCurrentNetwork()), valueMap);
      // System.out.println("stringNetwork network = "+stringNetwork.getNetwork());
      manager.execute(newIterator);
    }
	}

	@Override
	public void taskFinished(ObservableTask task) {
		if (!(task instanceof GetAnnotationsTask)) {
			return;
		}
		GetAnnotationsTask annTask = (GetAnnotationsTask)task;

		final Species species = annTask.getSpecies();
		if (stringNetwork.getAnnotations() == null || stringNetwork.getAnnotations().size() == 0) {
			if (annTask.getErrorMessage() != "") {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null,
								"<html>Your query returned no results due to an error. <br />"
										+ annTask.getErrorMessage() + "</html>",
								"No results", JOptionPane.ERROR_MESSAGE);
					}
				});					
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, "Your query returned no results",
								"No results", JOptionPane.ERROR_MESSAGE);
					}
				});
			}
			return;
		}
		boolean noAmbiguity = stringNetwork.resolveAnnotations();
		if (noAmbiguity) {
			int additionalNodes = getAdditionalNodes();
			// This mimics the String web site behavior
			if (stringNetwork.getResolvedTerms() == 1 && additionalNodes == 0) {
				/*
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, 
												"This will return only one node (Hint: increase maximum interactors slider?)",
									       "Hint", JOptionPane.WARNING_MESSAGE); 
					}
				});
				*/
				additionalNodes = 10;
				logger.warn("STRING Compound: Only one protein or compound was selected -- additional interactions set to 10");
			}
			//	additionalNodes = 10;

			final int addNodes = additionalNodes;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					importNetwork(species, getConfidence(), addNodes);
				}
			});
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JDialog d = new JDialog();
					d.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
					d.setTitle("Resolve Ambiguous Terms");
					// GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, Databases.STITCH.getAPIName(), 
					//                                         getSpecies(), false, getConfidence(), getAdditionalNodes());
					GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, 
					                                        Databases.STITCH.getAPIName(), false, optionsPanel);
					panel.createResolutionPanel();
					d.setContentPane(panel);
					d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					d.pack();
					d.setVisible(true);
				}
			});
		}
	}

	void importNetwork(Species species, int confidence, int additionalNodes) {
		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = stringNetwork.combineIds(queryTermMap);
		// System.out.println("Importing "+stringIds);
		TaskFactory factory = new ImportNetworkTaskFactory(stringNetwork, getSpeciesName(), 
		                                                   species, confidence, additionalNodes, stringIds,
		                                                   queryTermMap, "", Databases.STITCH.getAPIName(), 
		                                                   optionsPanel.getNetworkType());
		manager.execute(factory.createTaskIterator());
	}

}
