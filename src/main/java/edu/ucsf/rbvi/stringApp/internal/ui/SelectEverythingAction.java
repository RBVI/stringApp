package edu.ucsf.rbvi.stringApp.internal.ui;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

import java.util.Map;

/**
 * A Swing action, used on a button in {@link GetTermsPanel}'s ambiguous-term
 * resolution table, that selects all annotation choices for every query
 * term by setting each term's {@link ResolveTableModel}.
 */
class SelectEverythingAction extends AbstractAction {
	final Map<String, ResolveTableModel> tableModelMap;
	

	public SelectEverythingAction(final Map<String, ResolveTableModel> tableModelMap) {
		super("Select Everything");
		this.tableModelMap = tableModelMap;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		for (String term: tableModelMap.keySet()) {
			ResolveTableModel rtm = tableModelMap.get(term);
			for (int i = 0; i < rtm.getRowCount(); i++) {
				rtm.setValueAt(true, i, 0);
			}
		}
	}
}
