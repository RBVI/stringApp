package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.stringApp.internal.model.EntityIdentifier;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;

import edu.ucsf.rbvi.stringApp.internal.tasks.GetDiseaseTermsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetStringIDsFromDiseasesTask;

// TODO: [Optional] Improve non-gui mode
public class DiseaseQueryPanel extends JPanel { 
	StringNetwork stringNetwork = null;
	StringNetwork initialStringNetwork = null;
	final StringManager manager;

	JTextField searchTerms;
	JPanel mainSearchPanel;
	// JComboBox<Species> speciesCombo;
	JSlider confidenceSlider;
	JTextField confidenceValue;
	JSlider limitSlider;
	JTextField limitValue;
	JButton importButton;
	JButton backButton;
	NumberFormat formatter = new DecimalFormat("#0.00");
	NumberFormat intFormatter = new DecimalFormat("#0");

	EntityIdentifier diseaseEntity = null;
	List<EntityIdentifier> entityList;
	Species species = Species.getSpecies("Homo sapiens");

	private int confidence = 40;
	private int additionalNodes = 100;

	private boolean ignore = false;

	public DiseaseQueryPanel(final StringManager manager) {
		super(new GridBagLayout());
		this.manager = manager;
		init();
	}

	public DiseaseQueryPanel(final StringManager manager, StringNetwork stringNetwork) {
		this(manager, stringNetwork, null, 40, 100);
	}


	public DiseaseQueryPanel(final StringManager manager, StringNetwork stringNetwork, String query) {
		this(manager, stringNetwork,query, 40, 100);
	}

	public DiseaseQueryPanel(final StringManager manager, StringNetwork stringNetwork, String query, int confidence, int additionalNodes) {
		super(new GridBagLayout());
		this.manager = manager;
		this.stringNetwork = stringNetwork;
		this.initialStringNetwork = stringNetwork;
		this.confidence = confidence;
		this.additionalNodes = additionalNodes;
		init();
		if (query != null)
			searchTerms.setText(query);
	}

	public void doImport() {
		importButton.doClick();
	}

	private void init() {
		// Create the surrounding panel
		setPreferredSize(new Dimension(800,600));
		EasyGBC c = new EasyGBC();

		// Create the search list panel
		mainSearchPanel = createSearchPanel();
		add(mainSearchPanel, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the limit 
		JPanel limitSlider = createLimitSlider();
		add(limitSlider, c.down().expandHoriz().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel confidenceSlider = createConfidenceSlider();
		add(confidenceSlider, c.down().expandHoriz().insets(5,5,0,5));

		// Add Query/Cancel buttons
		JPanel buttonPanel =  createControlButtons();
		add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
	}

	JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel(new GridBagLayout());
		searchPanel.setPreferredSize(new Dimension(600,400));
		fillSearchPanel(searchPanel);
		return searchPanel;
	}

	void fillSearchPanel(JPanel searchPanel) {
		String ttText = "<html>Enter disease name or partial name e.g.:"+
		                "<dl><dd>cancer (matches various cancers)</dd>"+
		                "<dd>demen (matches several forms of dementia)</dd>"+
		                "<dd>als (matches various forms of amyotrophic lateral sclerosis)</dd></dl></html>";
		EasyGBC c = new EasyGBC();

		JLabel searchLabel = new JLabel("Enter disease term:");
		searchLabel.setToolTipText(ttText);
		c.noExpand().anchor("northwest").insets(0,5,0,5);
		searchPanel.add(searchLabel, c);
		searchTerms = new JTextField();
		searchTerms.setToolTipText(ttText);
		searchTerms.addActionListener(new InitialAction());
		c.down().expandHoriz().insets(5,10,5,10);
		searchPanel.add(searchTerms, c);
		JLabel filler = new JLabel();
		c.down().expandBoth().insets(5,10,5,10);
		searchPanel.add(filler, c);
	}

	void replaceSearchPanel() {
		mainSearchPanel.removeAll();
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
		mainSearchPanel.setLayout(new GridBagLayout());
		fillSearchPanel(mainSearchPanel);
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
	}

	JPanel createControlButtons() {
		JPanel buttonPanel = new JPanel();
		BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS);
		buttonPanel.setLayout(layout);
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent e) {
          cancel();
        }
      });

		backButton = new JButton(new AbstractAction("Back") {
        @Override
        public void actionPerformed(ActionEvent e) {
					stringNetwork.reset();
					replaceSearchPanel();
					importButton.setEnabled(true);
					backButton.setEnabled(false);
					importButton.setAction(new InitialAction());
					getParent().revalidate();
        }
			});
		backButton.setEnabled(false);

		importButton = new JButton(new InitialAction());

		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createHorizontalGlue());
		// buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(backButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(importButton);
		return buttonPanel;
	}

	JPanel createLimitSlider() {
		JPanel limitPanel = new JPanel(new GridBagLayout());
		limitPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();

		Font labelFont;
		{
			c.anchor("west").noExpand().insets(0,5,0,5);
			JLabel limitLabel = new JLabel("Maximum number of proteins:");
			labelFont = limitLabel.getFont();
			limitLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
			limitPanel.add(limitLabel, c);
		}

		{
			limitSlider = new JSlider(0, 2000, additionalNodes);
			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
			for (int value = 0; value <= 2000; value += 400) {
				JLabel label = new JLabel(intFormatter.format(value));
				label.setFont(valueFont);
				labels.put(value, label);
			}
			limitSlider.setLabelTable(labels);
			limitSlider.setPaintLabels(true);

			limitSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (ignore) return;
					ignore = true;
					int value = limitSlider.getValue();
					limitValue.setText(intFormatter.format(value));
					ignore = false;
				}
			});
			// c.anchor("southwest").expandHoriz().insets(0,5,0,5);
			c.right().expandHoriz().insets(0,5,0,5);
			limitPanel.add(limitSlider, c);
		}

		{
			limitValue = new JTextField(4);
			limitValue.setHorizontalAlignment(JTextField.RIGHT);
			limitValue.setText(""+additionalNodes);
			c.right().noExpand().insets(0,5,0,5);
			limitPanel.add(limitValue, c);

			limitValue.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					limitValueChanged();
				}
			});

			limitValue.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					limitValueChanged();
				}
			});

		}
		return limitPanel;
	}

	JPanel createConfidenceSlider() {
		JPanel confidencePanel = new JPanel(new GridBagLayout());
		confidencePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		EasyGBC c = new EasyGBC();

		Font labelFont;
		{
			c.anchor("west").noExpand().insets(0,5,0,5);
			JLabel confidenceLabel = new JLabel("Confidence (score) cutoff:");
			labelFont = confidenceLabel.getFont();
			confidenceLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
			confidencePanel.add(confidenceLabel, c);
		}

		{
			confidenceSlider = new JSlider(0,100,confidence);
			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
			for (int value = 0; value <= 100; value += 10) {
				double labelValue = (double)value/100.0;
				JLabel label = new JLabel(formatter.format(labelValue));
				label.setFont(valueFont);
				labels.put(value, label);
			}
			confidenceSlider.setLabelTable(labels);
			confidenceSlider.setPaintLabels(true);
			confidenceSlider.setValue(40);

			confidenceSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (ignore) return;
					ignore = true;
					int value = confidenceSlider.getValue();
					confidenceValue.setText(formatter.format(((double)value)/100.0));
					ignore = false;
				}
			});
			// c.anchor("southwest").expandHoriz().insets(0,5,0,5);
			c.right().expandHoriz().insets(0,5,0,5);
			confidencePanel.add(confidenceSlider, c);
		}

		{
			confidenceValue = new JTextField(4);
			confidenceValue.setHorizontalAlignment(JTextField.RIGHT);
			confidenceValue.setText(formatter.format(((double)confidence)/100.0));
			c.right().noExpand().insets(0,5,0,5);
			confidencePanel.add(confidenceValue, c);

			confidenceValue.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					confidenceValueChanged();
				}
			});

			confidenceValue.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					confidenceValueChanged();
				}
			});

		}
		return confidencePanel;
	}

	
	private void confidenceValueChanged() {
		if (ignore) return;
		ignore = true;
		String text = confidenceValue.getText();
		Number n = formatter.parse(text, new ParsePosition(0));
		double val = 0.0;
		if (n == null) {
			try {
				val = Double.valueOf(confidenceValue.getText());
			} catch (NumberFormatException nfe) {
				val = confidenceInputError();
			}
		} else if (n.doubleValue() > 1.0 || n.doubleValue() < 0.0) {
			val = confidenceInputError();
		} else {
			val = n.doubleValue();
		}

		val = val*100.0;
		confidenceSlider.setValue((int)val);
		ignore = false;
	}

	private double confidenceInputError() {
		confidenceValue.setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, 
				                          "Please enter a confence cutoff between 0.0 and 1.0", 
											            "Alert", JOptionPane.ERROR_MESSAGE);
		confidenceValue.setBackground(UIManager.getColor("TextField.background"));

		// Reset the value to correspond to the current slider setting
		double val = ((double)confidenceSlider.getValue())/100.0;
		confidenceValue.setText(formatter.format(val));
		return val;
	}
	
	private void limitValueChanged() {
		if (ignore) return;
		ignore = true;
		String text = limitValue.getText();
		Number n = intFormatter.parse(text, new ParsePosition(0));
		int val = 0;
		if (n == null) {
			try {
				val = Integer.valueOf(limitValue.getText());
			} catch (NumberFormatException nfe) {
				val = limitInputError();
			}
		} else if (n.intValue() > 2000 || n.intValue() < 0) {
			val = limitInputError();
		} else {
			val = n.intValue();
		}

		limitSlider.setValue((int)val);
		ignore = false;
	}

	private int limitInputError() {
		limitValue.setBackground(Color.RED);
		JOptionPane.showMessageDialog(null, 
				                          "Please enter maximum number of proteins between 100 and 10,000", 
											            "Alert", JOptionPane.ERROR_MESSAGE);
		limitValue.setBackground(UIManager.getColor("TextField.background"));

		// Reset the value to correspond to the current slider setting
		int val = limitSlider.getValue();
		limitValue.setText(intFormatter.format(val));
		return val;
	}

	void importNetwork(EntityIdentifier entity) {
		if (stringNetwork == null)
			stringNetwork = new StringNetwork(manager);
		int confidence = confidenceSlider.getValue();
		int limit = limitSlider.getValue();
		manager.execute(new TaskIterator(new GetStringIDsFromDiseasesTask(stringNetwork, species, limit,
		                                                                  confidence, entity.getIdentifier(),
																																			entity.getPrimaryName())));
		cancel();
	}

	public void createResolutionPanel() {
		mainSearchPanel.removeAll();
		revalidate();
		mainSearchPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		{
			String label = "<html><b>Multiple possible matches for term:</b> ";
			label += "Select the best matching disease from the table";
			label += "</html>";

			JLabel lbl = new JLabel(label);
			c.anchor("northeast").expandHoriz();
			mainSearchPanel.add(lbl, c);
		}

		ResolveDiseaseTermsTableModel tableModel;
		{
			JPanel annPanel = new JPanel(new GridBagLayout());
			EasyGBC ac = new EasyGBC();

			tableModel = new ResolveDiseaseTermsTableModel(entityList);
			final JTable table = new JTable(tableModel);
			
			table.setRowSelectionAllowed(false);

			JScrollPane tableScroller = new JScrollPane(table);
			ac.right().expandBoth().insets(0,5,0,5);
			annPanel.add(tableScroller, ac);

			c.down().expandBoth().insets(5,0,5,0);
			mainSearchPanel.add(annPanel, c);
		}

		importButton.setAction(new ResolvedAction(tableModel));
		backButton.setEnabled(true);

		revalidate();
		importButton.setEnabled(true);
	}

	public void cancel() {
		stringNetwork = initialStringNetwork;
		if (stringNetwork != null) stringNetwork.reset();
		replaceSearchPanel();
		importButton.setEnabled(true);
		backButton.setEnabled(false);
		importButton.setAction(new InitialAction());
		((Window)getRootPane().getParent()).dispose();
	}


	class InitialAction extends AbstractAction implements TaskObserver {
		public InitialAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			// Start our task cascade
			if (stringNetwork == null)
				stringNetwork = new StringNetwork(manager);

			int taxon = species.getTaxId(); // Only supported for human right now;
			String terms = searchTerms.getText();
			if (terms == null || terms.length() == 0) {
				JOptionPane.showMessageDialog(null, "No terms were entered -- nothing to search for",
							                        "Nothing entered", JOptionPane.ERROR_MESSAGE); 
				return;
			}
			manager.info("Getting disease identifiers for terms: "+terms);

			// Launch a task to get the annotations. 
			manager.execute(new TaskIterator(new GetDiseaseTermsTask(manager, taxon, terms)), this);
		}

		@Override
		public void allFinished(FinishStatus finishStatus) {
		}

		@Override
		public void taskFinished(ObservableTask task) {
			if (!(task instanceof GetDiseaseTermsTask)) {
				return;
			}
			GetDiseaseTermsTask annTask = (GetDiseaseTermsTask)task;

			entityList = annTask.getMatchedTerms(); 

			final int taxon = annTask.getTaxon();
			if (entityList == null || entityList.size() == 0) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, "Your query returned no results",
									                        "No results", JOptionPane.ERROR_MESSAGE); 
					}
				});
				return;
			}
			// Always create the resolution panel
			if (entityList.size() == 1) {
				diseaseEntity = entityList.get(0);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						importNetwork(diseaseEntity);
					}
				});
			} else {
				createResolutionPanel();
			}
		}
	}

	class ResolvedAction extends AbstractAction {
		final ResolveDiseaseTermsTableModel model;
		public ResolvedAction(final ResolveDiseaseTermsTableModel model) {
			super("Import");
			this.model = model;
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			int taxon = species.getTaxId();

			diseaseEntity = model.getSelectedEntity();

			importNetwork(diseaseEntity);

		}
	}

	class ResolveDiseaseTermsTableModel extends AbstractTableModel {
		final List<EntityIdentifier> entityList;
		final Boolean[] selected;
		String[] columns = {"Select", "Matched name", "Primary name", "Type", "Identifier"};
		public ResolveDiseaseTermsTableModel(final List<EntityIdentifier> entityList) {
			this.entityList = entityList;
			selected = new Boolean[entityList.size()];
			selected[0] = Boolean.TRUE;
			for (int i = 1; i < entityList.size(); i++)
				selected[i] = Boolean.FALSE;
		}

		public EntityIdentifier getSelectedEntity() {
			for (int i = 0; i < entityList.size(); i++) {
				if (selected[i])
					return entityList.get(i);
			}
			return null;
		}

		@Override
		public int getColumnCount() { return 5; }

		@Override
		public String getColumnName(int column) { return columns[column]; }

		@Override
		public int getRowCount() { return entityList.size(); }

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			EntityIdentifier eid = entityList.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return selected[rowIndex];
			case 1:
				return eid.getMatchedName();
			case 2:
				return eid.getPrimaryName();
			case 3:
				return "Disease";
			case 4:
				return eid.getIdentifier();
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return Boolean.class;
			case 1:
			case 2:
			case 3:
			case 4:
				return String.class;
			}
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 0) 
				return true;
			return false;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex != 0) {
				return;
			}
			Boolean bool = (Boolean) value;
			if (bool.equals(Boolean.FALSE)) {
				selected[rowIndex] = bool;
				return;
			}

			for (int i = 0; i < getRowCount(); i++) 
				if (i != rowIndex)
					setValueAt(Boolean.FALSE, i, 0);
			selected[rowIndex] = Boolean.TRUE;
			fireTableDataChanged();
		}
	}

}
