package edu.ucsf.rbvi.stringApp.internal.tasks;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_LAYERS;
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
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.ui.GetTermsPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchOptionsPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchQueryComponent;
import edu.ucsf.rbvi.stringApp.internal.utils.TextIcon;
import edu.ucsf.rbvi.stringApp.internal.utils.TextUtils;

public class StringSearchTaskFactory extends AbstractNetworkSearchTaskFactory implements TaskObserver {
	StringManager manager;
	static String STRING_ID = "edu.ucsf.rbvi.string";
	static String STRING_URL = "http://string-db.org";
	static String STRING_NAME = "STRING protein query";
	static String STRING_DESC = "Search STRING for protein-protein interactions";
	static String STRING_DESC_LONG = "<html>The protein query retrieves a STRING network for one or more proteins. <br />"
										+ "STRING is a database of known and predicted protein interactions for <br />"
										+ "thousands of organisms, which are integrated from several sources, <br />"
										+ "scored, and transferred across orthologs. The network includes both <br />"
										+ "physical interactions and functional associations.</html>";

	private StringNetwork stringNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private SearchQueryComponent queryComponent = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	private static final Icon icon = new TextIcon(STRING_LAYERS, getIconFont(32.0f), STRING_COLORS, 36, 36);

	private static URL stringURL() {
		try {
			return new URL(STRING_URL);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public StringSearchTaskFactory(StringManager manager) {
		super(STRING_ID, STRING_NAME, STRING_DESC, icon, StringSearchTaskFactory.stringURL());
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
		// Strip off any blank lines as well as trailing spaces

		stringNetwork = new StringNetwork(manager);
		int taxon = getTaxId();
		return new TaskIterator(new GetAnnotationsTask(stringNetwork, taxon, terms, Databases.STRING.getAPIName()));
	}

	@Override
	public String getName() { return STRING_NAME; }

	@Override
	public String getId() { return STRING_ID; }

	@Override
	public String getDescription() {
		return STRING_DESC_LONG;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() { 
		return StringSearchTaskFactory.stringURL();
	}

	// Create a JPanel that provides the species, confidence interval, and number of interactions
	// NOTE: we need to use reasonable defaults since it's likely the user won't actually change it...
	@Override
	public JComponent getOptionsComponent() {
		optionsPanel = new SearchOptionsPanel(manager);
		optionsPanel.setUseSmartDelimiters(true);
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
		try {
			if (optionsPanel.getSpecies() != null) {
				return optionsPanel.getSpecies().getTaxId();
			}
			return 9606; // Homo sapiens
		} catch (ClassCastException e) {
			// The user might not have given us a full species name
			String name = optionsPanel.getSpeciesText();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, "Unknown species: '"+name+"'",
								                        "Unknown species", JOptionPane.ERROR_MESSAGE); 
				}
			});
			return -1;
		}
	}

	public String getSpecies() {
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

		final int taxon = annTask.getTaxon();
		if (stringNetwork.getAnnotations() == null || stringNetwork.getAnnotations().size() == 0) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(null, "Your query returned no results",
								                        "No results", JOptionPane.ERROR_MESSAGE); 
				}
			});
			return;
		}
		boolean noAmbiguity = stringNetwork.resolveAnnotations();
		if (noAmbiguity) {
			int additionalNodes = getAdditionalNodes();
			// This mimics the String web site behavior
			if (stringNetwork.getResolvedTerms() == 1 && additionalNodes == 0) {
				additionalNodes = 10;
				logger.warn("STRING Protein: Only one protein was selected -- additional interactions set to 10");
			}

			final int addNodes = additionalNodes;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					importNetwork(taxon, getConfidence(), addNodes);
				}
			});
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JDialog d = new JDialog();
					d.setTitle("Resolve Ambiguous Terms");
					d.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
					// GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, Databases.STRING.getAPIName(), 
					//                                         getSpecies(), false, getConfidence(), getAdditionalNodes());
					GetTermsPanel panel = new GetTermsPanel(manager, stringNetwork, 
					                                        Databases.STRING.getAPIName(), false, optionsPanel);
					panel.createResolutionPanel();
					d.setContentPane(panel);
					d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					d.pack();
					d.setVisible(true);
				}
			});
		}
	}

	void importNetwork(int taxon, int confidence, int additionalNodes) {
		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = stringNetwork.combineIds(queryTermMap);
		// System.out.println("Importing "+stringIds);
		TaskFactory factory = new ImportNetworkTaskFactory(stringNetwork, getSpecies(), 
		                                                   taxon, confidence, additionalNodes, stringIds,
		                                                   queryTermMap, "", Databases.STRING.getAPIName());
		if (optionsPanel.getLoadEnrichment())
			manager.execute(factory.createTaskIterator(), this);
		else
			manager.execute(factory.createTaskIterator());
	}
}
