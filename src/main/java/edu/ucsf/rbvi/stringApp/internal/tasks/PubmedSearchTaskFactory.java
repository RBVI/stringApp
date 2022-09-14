package edu.ucsf.rbvi.stringApp.internal.tasks;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.PUBMED_LAYERS;
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

public class PubmedSearchTaskFactory extends AbstractNetworkSearchTaskFactory {
	StringManager manager;
	static String PUBMED_ID = "edu.ucsf.rbvi.pubmed";
	static String PUBMED_URL = "http://string-db.org";
	static String PUBMED_NAME = "STRING PubMed query";
	static String PUBMED_DESC = "Search STRING for protein-protein interactions based on PubMed queries";
	static String PUBMED_DESC_LONG =  "<html>The PubMed query retrieves a STRING network pertaining to any topic of interest <br />"
											+ "based on text mining of PubMed abstracts. STRING is a database of known and <br />"
											+ "predicted protein interactions for thousands of organisms, which are integrated <br />"
											+ "from several sources, scored, and transferred across orthologs. The network <br />"
											+ "includes both physical interactions and functional associations.</html>";

	private StringNetwork stringNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private JTextField queryComponent = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);

	private static final Icon icon = new TextIcon(PUBMED_LAYERS, getIconFont(32.0f), STRING_COLORS, 36, 36);

	private static URL stringURL() {
		try {
			return new URL(PUBMED_URL);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public PubmedSearchTaskFactory(StringManager manager) {
		super(PUBMED_ID, PUBMED_NAME, PUBMED_DESC, icon, PubmedSearchTaskFactory.stringURL());
		this.manager = manager;
	}

	public boolean isReady() { 
		if (manager.haveURIs() && queryComponent.getText() != null && queryComponent.getText().length() > 0)
			return true; 
		return false;
	}

	public TaskIterator createTaskIterator() {
		final String terms = queryComponent.getText();

		return new TaskIterator(new AbstractTask() {
			@Override
			public void run(TaskMonitor m) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run () {
						Species species;
						try {
							species = optionsPanel.getSpecies();
						} catch (RuntimeException e) {
							String speciesText = optionsPanel.getSpeciesText();
							m.showMessage(TaskMonitor.Level.ERROR, "Unknown species: '"+speciesText+"'");
              // Reset
              optionsPanel.setSpeciesText(manager.getDefaultSpecies());
							return;
						}
						JDialog d = new JDialog();
						d.setTitle("Resolve Ambiguous Terms");
						d.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
						PubMedQueryPanel panel = new PubMedQueryPanel(manager, stringNetwork, terms, 
						                                              optionsPanel);
						d.setContentPane(panel);
						d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						d.pack();
						// d.setVisible(true);
						panel.doImport();
					}
				});
			}
		});

	}

	@Override
	public String getName() { return PUBMED_NAME; }

	@Override
	public String getId() { return PUBMED_ID; }

	@Override
	public String getDescription() {
		return PUBMED_DESC_LONG;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() { 
		return PubmedSearchTaskFactory.stringURL();
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
	private class MySearchComponent extends JTextField {
		Color msgColor;
		private static final String DEF_SEARCH_TEXT = "Enter one term per line.      Set species →";
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

			getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent e) {
					fireQueryChanged();
				}
				@Override
				public void insertUpdate(DocumentEvent e) {
					fireQueryChanged();
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
					// Nothing to do here...
				}
			});

			addActionListener( new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					MySearchComponent.this.firePropertyChange(NetworkSearchTaskFactory.SEARCH_REQUESTED_PROPERTY, null, null);
				}
			});

			addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) { haveFocus = false; }

				@Override
				public void focusGained(FocusEvent e) { haveFocus = true; }
			});

			return;
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Color msgColor = UIManager.getColor("Label.disabledForeground");

			if ((getText() == null || getText().trim().isEmpty()) && !haveFocus) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHints(
					new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
				// Set the font
				g2.setFont(getFont());
				// Get the FontMetrics
				FontMetrics metrics = g2.getFontMetrics(getFont());
				// Determine the X coordinate for the text
				int x = hgap;
				// Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
				int y = (metrics.getHeight() / 2) + metrics.getAscent() + vgap;
				// Draw
				g2.setColor(msgColor);
				g2.drawString(DEF_SEARCH_TEXT, x, y);
				g2.dispose();
			}
		}


		private void fireQueryChanged() {
			MySearchComponent.this.firePropertyChange(NetworkSearchTaskFactory.QUERY_PROPERTY, null, null);
		}
	}

	@Override
	public TaskObserver getTaskObserver() { return null; }

}
