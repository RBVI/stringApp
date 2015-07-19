package edu.ucsf.rbvi.stringApp.internal.ui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;

class ResolveTableModel extends AbstractTableModel {
	private StringWebServiceClient wsClient;
	private List<Annotation> annotations;
	private String term;
	private int selection = -1;

	public ResolveTableModel(StringWebServiceClient wsClient, String term, List<Annotation> annotations) {
		this.annotations = annotations;
		this.term = term;
		this.wsClient = wsClient;
	}

	@Override
	public int getColumnCount() { return 3; }

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
		}
		return null;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Annotation ann = annotations.get(rowIndex);
		switch (columnIndex) {
		case 0:
			if (rowIndex == selection)
				return true;
			return false;
		case 1:
			return ann.getPreferredName();
		case 2:
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
		if (selection >= 0)
			fireTableCellUpdated(selection, columnIndex);
		selection = rowIndex;
		// fireTableDataChanged();
		fireTableCellUpdated(rowIndex, columnIndex);
		wsClient.addResolvedStringID(term, annotations.get(rowIndex).getStringId());
		System.out.println("Selected "+annotations.get(rowIndex).getPreferredName());
	}
}
