package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.ucsf.rbvi.stringApp.internal.model.Species;

/**
 * Makes the species combo box searchable.
 */
public class JComboBoxDecorator {

	private List<Species> previousEntries = new ArrayList<Species>();
	private List allEntries;
  private JComboBox jcb;
  private boolean editable;
  private boolean species;

  public JComboBoxDecorator(final JComboBox jcb, final boolean editable, final boolean species, final List entries) {
    this.jcb = jcb;
    this.editable = editable;
    this.species = species;
    this.allEntries = entries;

  }

  public void decorate(final List entries) {
		String selectedEntry = jcb.getSelectedItem().toString();
		jcb.setEditable(editable);
		jcb.setModel(new DefaultComboBoxModel(entries.toArray()));

		final JTextField textField = (JTextField)jcb.getEditor().getEditorComponent();
		textField.setText(selectedEntry.toString());
		jcb.setSelectedItem(selectedEntry);

		textField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
				 	public void run() {
						int currentCaretPosition=textField.getCaretPosition();
						comboFilter(textField.getText(), entries);
						textField.setCaretPosition(currentCaretPosition);
				 	}
				});
			}
		});
  }

  public void updateEntries(final List entries) {
		if (entries != null)
			allEntries = entries;
    previousEntries.clear();
		final JTextField textField = (JTextField)jcb.getEditor().getEditorComponent();
		if (jcb.getSelectedItem() != null)
			textField.setText(jcb.getSelectedItem().toString());
		else {
			textField.setText("");
		}
  }

	/**
	 * Create a list of entries that match the user's entered text.
	 */
	private void comboFilter(String enteredText, List entries) {
		List entriesFiltered = new ArrayList();
		boolean changed = true;
		DefaultComboBoxModel jcbModel = (DefaultComboBoxModel) jcb.getModel();

		if (enteredText == null) {
			return;
		} else if (enteredText.length() == 0) {
      if (species)
			  entriesFiltered = Species.getModelSpecies();
      else
        entriesFiltered = allEntries;
		} else if (enteredText.length() < 4) {
			return;
		} else {
      if (species)
        entriesFiltered = Species.search(enteredText);
      else
        entriesFiltered = entrySearch(enteredText);
		}

		if (previousEntries.size() == entriesFiltered.size()
				&& previousEntries.containsAll(entriesFiltered)) {
			changed = false;
		}

		if (changed && entriesFiltered.size() > 1) {
			previousEntries = entriesFiltered;
			jcb.setModel(new DefaultComboBoxModel(entriesFiltered.toArray()));
			jcb.setSelectedItem(enteredText);
			jcb.showPopup();
		} else if (entriesFiltered.size() == 1) {
			if (entriesFiltered.get(0).toString().equalsIgnoreCase(enteredText)) {
				previousEntries = new ArrayList<>();
				jcb.setSelectedItem(entriesFiltered.get(0));
				jcb.hidePopup();
			} else {
				previousEntries = entriesFiltered;
				jcb.setModel(new DefaultComboBoxModel(entriesFiltered.toArray()));
				jcb.setSelectedItem(enteredText);
				jcb.showPopup();
			}
		} else if (entriesFiltered.size() == 0) {
			previousEntries = new ArrayList<>();
			jcb.hidePopup();
		}
	}

  private List entrySearch(String enteredText) {
    List ret = new ArrayList();
    enteredText = enteredText.toUpperCase();
    for (Object o: allEntries) {
      if (o.toString().toUpperCase().startsWith(enteredText)) {
				Species sp = Species.getSpecies(o.toString());
				if (sp != null)
					ret.add(sp.toString());
			}
    }
    return ret;
  }

}
