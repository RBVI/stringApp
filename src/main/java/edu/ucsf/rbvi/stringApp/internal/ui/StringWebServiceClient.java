package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.util.ArrayList;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

import edu.ucsf.rbvi.stringApp.internal.tasks.ImportNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ResolveNamesTaskFactory;

// TODO: [Optional] Improve non-gui mode
public class StringWebServiceClient extends AbstractWebServiceGUIClient 
                                    implements NetworkImportWebServiceClient, SearchWebServiceClient {
	JTextArea searchTerms;
	JTextField additionalNodesText;
	StringManager manager;
	JPanel mainPanel;
	JPanel mainSearchPanel;
	JComboBox speciesCombo;
	JSlider confidenceSlider;
	JButton importButton;

	public StringWebServiceClient(StringManager manager) {
		super(manager.getURL(), "String DB", "The String Database");
		this.manager = manager;
		init();
	}

	public TaskIterator createTaskIterator(Object query) {
		if (query == null)
			throw new NullPointerException("null query");
		return new TaskIterator();
	}

	private void init() {
		// Create the surrounding panel
		mainPanel = new JPanel(new GridBagLayout());
		super.gui = mainPanel;
		EasyGBC c = new EasyGBC();

		// Create the database options
		//createDatabasePanel(manager.getDatabases());

		// Create the species panel
		List<Species> speciesList = Species.getSpecies();
		if (speciesList == null) {
			try {
				speciesList = Species.readSpecies(manager);
			} catch (Exception e) {
				manager.error("Unable to get species: "+e.getMessage());
				e.printStackTrace();
				return;
			}
		}
		JPanel speciesBox = createSpeciesComboBox(speciesList);
		super.gui.add(speciesBox, c.expandHoriz().insets(0,5,0,5));
		

		// Create the search list panel
		mainSearchPanel = createSearchPanel();
		super.gui.add(mainSearchPanel, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel confidenceSlider = createConfidenceSlider();
		super.gui.add(confidenceSlider, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel additionalNodesPanel = createAdditionalNodesPanel();
		super.gui.add(additionalNodesPanel, c.down().expandBoth().insets(5,5,0,5));

		// Create the evidence types buttons
		// createEvidenceButtons(manager.getEvidenceTypes());

		// Add Query/Cancel buttons
		JPanel buttonPanel =  createControlButtons();
		super.gui.add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
	}

	JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel(new GridBagLayout());
		searchPanel.setPreferredSize(new Dimension(500,500));
		EasyGBC c = new EasyGBC();

		JLabel searchLabel = new JLabel("Enter protein or compound names:");
		c.noExpand().anchor("northwest").insets(0,5,0,5);
		searchPanel.add(searchLabel, c);
		searchTerms = new JTextArea();
		JScrollPane jsp = new JScrollPane(searchTerms);
		c.down().expandBoth().insets(5,10,5,10);
		searchPanel.add(jsp, c);
		return searchPanel;
	}

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox(speciesList.toArray());

		// Set Human as the default
		for (Species s: speciesList) {
			if (s.toString().equals("Homo sapiens")) {
				speciesCombo.setSelectedItem(s);
				break;
			}
		}
		c.right().expandHoriz().insets(0,5,0,5);
		speciesPanel.add(speciesCombo, c);
		return speciesPanel;
	}

	JPanel createControlButtons() {
		JPanel buttonPanel = new JPanel();
		BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS);
		buttonPanel.setLayout(layout);
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent e) {
          ((Window)mainPanel.getRootPane().getParent()).dispose();
        }
      });

		importButton = new JButton(new AbstractAction("Import") {
        @Override
        public void actionPerformed(ActionEvent e) {
					// Start our task cascade
					Species species = (Species)speciesCombo.getSelectedItem();

					int additionalNodes = 0;
					String addText = additionalNodesText.getText();
					if (addText != null && addText.length() > 0) {
				 		try {
							additionalNodes	= Integer.parseInt(addText);
							System.out.println("Additional Nodes = "+additionalNodes);
						} catch (NumberFormatException nfe) {
							JOptionPane.showMessageDialog(null, "Additional nodes must be an integer value", 
							                              "Additional Nodes Error", JOptionPane.ERROR_MESSAGE); 
							return;
						}
					}

					int taxon = species.getTaxId();
					String terms = searchTerms.getText();

					Map<String,List<Annotation>> annotations = manager.getAnnotations(taxon, terms);
					List<String> stringIds = new ArrayList<>();
					boolean noAmbiguity = resolveAnnotations(annotations, stringIds);
					if (noAmbiguity) {
						importNetwork(taxon, confidenceSlider.getValue(), additionalNodes, stringIds);
					} else {
						createResolutionPanel(annotations);
					}

					//TaskFactory factory = new ResolveNamesTaskFactory(manager, species.getTaxId(), 
					//                                                  confidenceSlider.getValue(),
					//                                                  additionalNodes,
					//                                                  searchTerms.getText());
					// manager.execute(factory.createTaskIterator());
        }

			});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(importButton);
		return buttonPanel;
	}

	JPanel createConfidenceSlider() {
		JPanel confidencePanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel confidenceLabel = new JLabel("Required confidence (score):");
		Font labelFont = confidenceLabel.getFont();
		confidenceLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
		c.anchor("west").noExpand().insets(0,5,0,5);
		confidencePanel.add(confidenceLabel, c);
		confidenceSlider = new JSlider();
		Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
		NumberFormat formatter = new DecimalFormat("#0.00");
		for (int value = 0; value <= 100; value += 10) {
			double labelValue = (double)value/100.0;
			JLabel label = new JLabel(formatter.format(labelValue));
			label.setFont(valueFont);
			labels.put(value, label);
		}
		confidenceSlider.setLabelTable(labels);
		confidenceSlider.setPaintLabels(true);
		confidenceSlider.setValue(0);
		c.down().expandBoth().insets(0,5,10,5);
		confidencePanel.add(confidenceSlider, c);
		return confidencePanel;
	}

	JPanel createAdditionalNodesPanel() {
		JPanel nodePanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel nodeLabel = new JLabel("Additional network nodes:");
		Font labelFont = nodeLabel.getFont();
		nodeLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
		c.anchor("west").noExpand().insets(0,5,0,5);
		nodePanel.add(nodeLabel, c);
		additionalNodesText = new JTextField();
		c.right().expandHoriz().insets(0,5,0,5);
		nodePanel.add(additionalNodesText, c);
		return nodePanel;
	}

	boolean resolveAnnotations(final Map<String, List<Annotation>> annotations,
	                           List<String> stringIds) {
		boolean noAmbiguity = true;
		for (String key: annotations.keySet()) {
			if (annotations.get(key).size() > 1) {
				noAmbiguity = false;
				break;
			} else {
				stringIds.add(annotations.get(key).get(0).getStringId());
			}
		}
		return noAmbiguity;
	}

	void importNetwork(int taxon, int confidence, int additional_nodes, List<String> stringIds) {
		TaskFactory factory = new ImportNetworkTaskFactory(manager, taxon, confidence, additional_nodes, stringIds);
		manager.execute(factory.createTaskIterator());
	}

	void createResolutionPanel(final Map<String, List<Annotation>> annotations) {
		mainSearchPanel.removeAll();
		mainPanel.revalidate();

		mainSearchPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		{	
			String label = "<html><b>Multiple possible matches for some terms:</b> ";
			label += "Select the term in the left column to see the possibilities, then select the correct term from the table";
			label += "</html>";
	
			JLabel lbl = new JLabel(label);
			c.anchor("northeast").expandHoriz();
			mainSearchPanel.add(lbl, c);
		}


		{
			JPanel annPanel = new JPanel(new GridBagLayout());
			EasyGBC ac = new EasyGBC();
	
			final JTable table = new JTable();

			final JList termList = new JList(annotations.keySet().toArray());
			termList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			termList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					String term = (String)termList.getSelectedValue();
					table.setModel(new ResolveTableModel(annotations.get(term)));
					table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
					table.getColumnModel().getColumn(2).setCellRenderer(new MyCellRenderer());
					table.getColumnModel().getColumn(0).setPreferredWidth(50);
					table.getColumnModel().getColumn(1).setPreferredWidth(50);
					table.getColumnModel().getColumn(2).setPreferredWidth(350);
				}
			});

			JScrollPane termScroller = new JScrollPane(termList);
			termScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			ac.anchor("east").expandVert();
			annPanel.add(termScroller, ac);
	
			JScrollPane tableScroller = new JScrollPane(table);
			ac.right().expandBoth().insets(0,5,0,5);
			annPanel.add(tableScroller, ac);

			c.down().expandBoth().insets(5,0,5,0);
			mainSearchPanel.add(annPanel, c);
		}

		importButton.setEnabled(false);

		mainPanel.revalidate();
	}

	class ResolveTableModel extends AbstractTableModel {
		private List<Annotation> annotations;

		public ResolveTableModel(List<Annotation> annotations) {
			this.annotations = annotations;
		}

		public int getColumnCount() { return 3; }

		public int getRowCount() { 
			if (annotations == null) return 0;
			return annotations.size(); 
		}

		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return "Select";
			case 1:
				return "Name";
			case 2:
				return "Description";
			}
			return "";
		}

		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return Boolean.class;
			case 1:
				return String.class;
			case 2:
				return String.class;
			}
			return null;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Annotation ann = annotations.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return false;
			case 1:
				return ann.getPreferredName();
			case 2:
				return ann.getAnnotation();
			}
			return null;
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 0) return true;
			return false;
		}

	}

	public class MyCellRenderer extends JTextArea implements TableCellRenderer {
		public MyCellRenderer() {
			setLineWrap(true);
			setWrapStyleWord(true);
 		}

		public Component getTableCellRendererComponent(JTable table, 
		                                               Object value, 
											                             boolean isSelected, 
																									 boolean hasFocus, int row, int column) {
			setText((String)value);//or something in value, like value.getNote()...
			setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
			if (table.getRowHeight(row) != getPreferredSize().height) {
				table.setRowHeight(row, getPreferredSize().height);
			}
			return this;
		}
	} 
}
