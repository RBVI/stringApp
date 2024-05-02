package edu.ucsf.rbvi.stringApp.internal.ui;

import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;

class ResolveTableModel extends AbstractTableModel {
	private GetTermsPanel wsClient;
	private List<Annotation> annotations;
	private String term;
	private boolean[] selections;

	public ResolveTableModel(GetTermsPanel wsClient, String term, List<Annotation> annotations) {
		this.annotations = annotations;
		this.term = term;
		this.wsClient = wsClient;
		this.selections = new boolean[annotations.size()];
		/*
		if (annotations.size() == 1) {
			this.selections[0] = true;
			wsClient.addResolvedStringID(term, annotations.get(0).getStringId());
		} else {
			Arrays.fill(selections, false);
		}
		*/
		Arrays.fill(selections, false);
		this.selections[0] = true;
		wsClient.addResolvedStringID(term, annotations.get(0).getStringId());
	}

	@Override
	public int getColumnCount() { return 4; }

	@Override
	public int getRowCount() { 
		if (annotations == null) return 0;
		return annotations.size(); 
	}

	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return "Select";
		case 1:
			return "Name";
		case 2:
			return "ID";
		case 3:
			return "Description";
		}
		return "";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return Boolean.class;
		case 1:
			return String.class;
		case 2:
			return String.class;
		case 3:
			return String.class;
		}
		return null;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Annotation ann = annotations.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return selections[rowIndex];
		case 1:
			return ann.getPreferredName();
		case 2:
			return ann.getStringId();
		case 3:
			return ann.getAnnotation();
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0) return true;
		return false;
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (columnIndex != 0) return;
		boolean v = ((Boolean)value).booleanValue();
		selections[rowIndex] = v;
		fireTableCellUpdated(rowIndex, columnIndex);
		Annotation ann = annotations.get(rowIndex);
		if (v) {
			wsClient.addResolvedStringID(term, ann.getStringId());
			// System.out.println("Selected "+ann.getPreferredName()+": "+ann.getStringId());
		} else {
			wsClient.removeResolvedStringID(term, ann.getStringId());
			// System.out.println("Deselected "+ann.getPreferredName()+": "+ann.getStringId());
		}
	}
}
