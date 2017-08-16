package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import edu.ucsf.rbvi.stringApp.internal.model.Species;

/**
 * Makes the species combo box searchable.
 */
public class JComboBoxDecorator {

	public static List<Species> previousEntries = new ArrayList<Species>();

	public static void decorate(final JComboBox<Species> jcb, boolean editable, boolean species) {
		List<Species> entries = new ArrayList<Species>();
		for (int i = 0; i < jcb.getItemCount(); i++) {
			if (species) {
				entries.add(jcb.getItemAt(i));
			}
		}
		decorate(jcb, editable, entries);
	}

	public static void decorate(final JComboBox<Species> jcb, boolean editable,
			final List<Species> entries) {
		jcb.setEditable(editable);
		jcb.setModel(new DefaultComboBoxModel(entries.toArray()));

		final JTextField textField = (JTextField) jcb.getEditor().getEditorComponent();

		textField.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						comboFilter(textField.getText(), jcb, entries);
					}
				});
			}
		});
	}

	/**
	 * Create a list of entries that match the user's entered text.
	 */
	private static void comboFilter(String enteredText, JComboBox<Species> jcb,
			List<Species> entries) {
		List<Species> entriesFiltered = new ArrayList<Species>();
		boolean changed = true;
		DefaultComboBoxModel<Species> jcbModel = (DefaultComboBoxModel<Species>) jcb.getModel();

		if (enteredText == null) {
			return;
		}

		for (Species entry : entries) {
			if (entry.getName().toLowerCase().contains(enteredText.toLowerCase())) {
				entriesFiltered.add(entry);
				// System.out.println(jcbModel.getIndexOf(entry));
			}
		}

		if (previousEntries.size() == entriesFiltered.size()
				&& previousEntries.containsAll(entriesFiltered)) {
			changed = false;
		}

		if (changed && entriesFiltered.size() > 0) {
			previousEntries = entriesFiltered;
			jcb.setModel(new DefaultComboBoxModel(entriesFiltered.toArray()));
			jcb.setSelectedItem(enteredText);
			jcb.showPopup();
		} else if (entriesFiltered.size() == 0) {
			previousEntries = new ArrayList<Species>();
			jcb.hidePopup();
		}
	}

}