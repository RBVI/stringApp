package edu.ucsf.rbvi.stringApp.internal.ui;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

import java.util.Map;

/**
 * A Swing action, used on a button in {@link GetTermsPanel}'s ambiguous-term
 * resolution table, that deselects all annotation choices for a single
 * query term across its {@link ResolveTableModel}.
 */
class ClearAllTermAction extends AbstractAction {
	final String term;
	final Map<String, ResolveTableModel> tableModelMap;
	

	public ClearAllTermAction(final String term, final Map<String, ResolveTableModel> tableModelMap) {
		super("Clear Everything for "+term);
		this.tableModelMap = tableModelMap;
		this.term = term;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ResolveTableModel rtm = tableModelMap.get(term);
		for (int i = 0; i < rtm.getRowCount(); i++) {
			rtm.setValueAt(false, i, 0);
		}
	}
}
