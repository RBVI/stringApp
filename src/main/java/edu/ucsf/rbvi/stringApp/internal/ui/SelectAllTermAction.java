package edu.ucsf.rbvi.stringApp.internal.ui;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

import java.util.Map;

/**
 * A Swing action, used on a button in {@link GetTermsPanel}'s ambiguous-term
 * resolution table, that selects all annotation choices for a single query
 * term across its {@link ResolveTableModel}.
 */
class SelectAllTermAction extends AbstractAction {
	final String term;
	final Map<String, ResolveTableModel> tableModelMap;
	

	public SelectAllTermAction(final String term, final Map<String, ResolveTableModel> tableModelMap) {
		super("Select Everything for "+term);
		this.tableModelMap = tableModelMap;
		this.term = term;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ResolveTableModel rtm = tableModelMap.get(term);
		for (int i = 0; i < rtm.getRowCount(); i++) {
			rtm.setValueAt(true, i, 0);
		}
	}
}
