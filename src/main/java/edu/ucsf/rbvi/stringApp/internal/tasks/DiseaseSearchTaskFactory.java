package edu.ucsf.rbvi.stringApp.internal.tasks;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.DISEASE_LAYERS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.getIconFont;

import java.awt.Dialog;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.TunableSetter;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.ui.DiseaseQueryPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchOptionsPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.TextIcon;

public class DiseaseSearchTaskFactory extends AbstractNetworkSearchTaskFactory {
	StringManager manager;
	static String DISEASE_ID = "edu.ucsf.rbvi.disease";
	static String DISEASE_URL = "http://string-db.org";
	static String DISEASE_NAME = "STRING disease query";
	static String DISEASE_DESC = "Search STRING for protein-protein interactions";
	static String DISEASE_DESC_LONG = "<html>The disease query retrieves a STRING network for the top-N human proteins associated <br />"
										  + "with the queried disease in the DISEASES database. DISEASES is a weekly updated web <br />"
										  + "resource that integrates evidence on disease-gene associations from automatic text <br />"
										  + "mining, manually curated literature, cancer mutation data, and genome-wide association <br />"
										  + "studies. STRING is a database of known and predicted protein interactions for thousands <br />"
										  + "of organisms, which are integrated from several sources, scored, and transferred across <br />"
										  + "orthologs. The network  includes both physical interactions and functional associations.</html>";

	private StringNetwork stringNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	private static final Icon icon = new TextIcon(DISEASE_LAYERS, getIconFont(32.0f), STRING_COLORS, 36, 36);

	private static URL stringURL() {
		try {
			return new URL(DISEASE_URL);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public DiseaseSearchTaskFactory(StringManager manager) {
		super(DISEASE_ID, DISEASE_NAME, DISEASE_DESC, icon, DiseaseSearchTaskFactory.stringURL());
		this.manager = manager;
	}

	public boolean isReady() { 
		if (!manager.haveURIs()) return false;
		if (getQuery() != null && getQuery().length() > 0)
			return true; 
		return false;
	}

	public TaskIterator createTaskIterator() {
		final String terms = getQuery();

		return new TaskIterator(new AbstractTask() {
			@Override
			public void run(TaskMonitor m) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run () {
						JDialog d = new JDialog();
						d.setTitle("Resolve Ambiguous Terms");
						d.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
						// DiseaseQueryPanel panel = new DiseaseQueryPanel(manager, stringNetwork, terms);
						DiseaseQueryPanel panel = new DiseaseQueryPanel(manager, stringNetwork, terms, optionsPanel);
						panel.doImport();
						d.setContentPane(panel);
						d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						d.pack();
						d.setVisible(true);
					}
				});
			}
		});

	}

	@Override
	public String getName() { return DISEASE_NAME; }

	@Override
	public String getId() { return DISEASE_ID; }

	@Override
	public String getDescription() {
		return DISEASE_DESC_LONG;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() { 
		return DiseaseSearchTaskFactory.stringURL();
	}

	// Create a JPanel that provides the species, confidence interval, and number of interactions
	// NOTE: we need to use reasonable defaults since it's likely the user won't actually change it...
	@Override
	public JComponent getOptionsComponent() {
		optionsPanel = new SearchOptionsPanel(manager, false, true);
		return optionsPanel;
	}

	@Override
	public JComponent getQueryComponent() {
		return null;
	}

	@Override
	public TaskObserver getTaskObserver() { return null; }

}
