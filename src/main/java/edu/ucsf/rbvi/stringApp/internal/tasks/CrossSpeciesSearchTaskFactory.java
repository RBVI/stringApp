package edu.ucsf.rbvi.stringApp.internal.tasks;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.CROSS_SPECIES_LAYERS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.getIconFont;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetAnnotationsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.ImportNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.ui.PubMedQueryPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchOptionsPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.TextIcon;

public class CrossSpeciesSearchTaskFactory extends AbstractNetworkSearchTaskFactory {
	StringManager manager;
	static String CROSS_SPECIES_ID = "edu.ucsf.rbvi.x-species";
	static String CROSS_SPECIES_URL = "http://string-db.org";
	static String CROSS_SPECIES_NAME = "STRING Cross Species query";
	static String CROSS_SPECIES_DESC = "Search STRING for protein-protein interactions across species";
	static String CROSS_SPECIES_DESC_LONG =  "<html>The PubMed query retrieves a STRING network pertaining to any topic of interest <br />"
											+ "based on text mining of PubMed abstracts. STRING is a database of known and <br />"
											+ "predicted protein interactions for thousands of organisms, which are integrated <br />"
											+ "from several sources, scored, and transferred across orthologs. The network <br />"
											+ "includes both physical interactions and functional associations.</html>";

	private StringNetwork stringNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private JComboBox<Species> queryComponent = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	private static final Icon icon = new TextIcon(CROSS_SPECIES_LAYERS, getIconFont(32.0f), STRING_COLORS, 36, 36);

	private static URL stringURL() {
		try {
			return new URL(CROSS_SPECIES_URL);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public CrossSpeciesSearchTaskFactory(StringManager manager) {
		super(CROSS_SPECIES_ID, CROSS_SPECIES_NAME, CROSS_SPECIES_DESC, icon, CrossSpeciesSearchTaskFactory.stringURL());
		this.manager = manager;
	}

	public boolean isReady() { 
		if (manager.haveURIs() && queryComponent.getSelectedItem() != null)
			return true; 
		return false;
	}

	public TaskIterator createTaskIterator() {
		final Species species2 = (Species)queryComponent.getSelectedItem();

		return new TaskIterator(new AbstractTask() {
			@Override
			public void run(TaskMonitor m) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run () {
						Species species;
						try {
							species = optionsPanel.getSpecies();
						} catch (ClassCastException e) {
							String speciesText = optionsPanel.getSpeciesText();
							m.showMessage(TaskMonitor.Level.ERROR, "Unknown species: '"+speciesText+"'");
							return;
						}
						JDialog d = new JDialog();
						d.setTitle("Resolve Ambiguous Terms");
						d.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
						// CrossSpeciesQueryPanel panel = new CrossSpeciesQueryPanel(manager, stringNetwork, species2, optionsPanel);
						// d.setContentPane(panel);
						d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						d.pack();
						// d.setVisible(true);
						// panel.doImport();
					}
				});
			}
		});

	}

	@Override
	public String getName() { return CROSS_SPECIES_NAME; }

	@Override
	public String getId() { return CROSS_SPECIES_ID; }

	@Override
	public String getDescription() {
		return CROSS_SPECIES_DESC_LONG;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() { 
		return CrossSpeciesSearchTaskFactory.stringURL();
	}

	// Create a JPanel that provides the species, confidence interval, and number of interactions
	// NOTE: we need to use reasonable defaults since it's likely the user won't actually change it...
	@Override
	public JComponent getOptionsComponent() {
		optionsPanel = new SearchOptionsPanel(manager, true, false);
		return optionsPanel;
	}

	@Override
	public JComponent getQueryComponent() {
		if (queryComponent == null) {
			queryComponent = new MySearchComponent();
		}
		return queryComponent;
	}


	// We want a private class so we can add our helper text
	private class MySearchComponent extends JComboBox {
		Color msgColor;
		final int vgap = 1;
		final int hgap = 5;
		private boolean haveFocus = false;

		public MySearchComponent() {
			super();
			init();
		}

		private void init() {
			msgColor = UIManager.getColor("Label.disabledForeground");
			setMinimumSize(getPreferredSize());
			setBorder(BorderFactory.createEmptyBorder(vgap, hgap, vgap, hgap));
			setFont(getFont().deriveFont(LookAndFeelUtil.getSmallFontSize()));
			return;
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
		}


		private void fireQueryChanged() {
			MySearchComponent.this.firePropertyChange(NetworkSearchTaskFactory.QUERY_PROPERTY, null, null);
		}
	}

	@Override
	public TaskObserver getTaskObserver() { return null; }

}
