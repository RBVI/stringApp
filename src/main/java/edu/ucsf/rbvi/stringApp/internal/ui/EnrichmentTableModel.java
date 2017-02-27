package edu.ucsf.rbvi.stringApp.internal.ui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;

public class EnrichmentTableModel extends AbstractTableModel {
	private String[] columnNames;
	private CyTable cyTable;
	private Long[] rowNames;

	public EnrichmentTableModel(CyTable cyTable, String[] columnNames) {
		this.columnNames = columnNames;
		this.cyTable = cyTable;
		initData();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return cyTable.getRowCount();
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		final String colName = columnNames[col];
		final Long rowName = rowNames[row];
		// swingColumns = new String[] { colShownChart, colName, colDescription, colFDR,
		// colGenesCount, colGenes, colGenesSUID };
		if (colName.equals(EnrichmentTerm.colShowChart)) {
			return cyTable.getRow(rowName).get(colName, Boolean.class);
		} else if (colName.equals(EnrichmentTerm.colFDR)) {
			return cyTable.getRow(rowName).get(colName, Double.class);
		} else if (colName.equals(EnrichmentTerm.colGenesCount)) {
			return cyTable.getRow(rowName).get(colName, Integer.class);
		} else if (colName.equals(EnrichmentTerm.colGenes)) {
			return cyTable.getRow(rowName).getList(colName, String.class);
		} else if (colName.equals(EnrichmentTerm.colGenesSUID)) {
			return cyTable.getRow(rowName).getList(colName, Long.class);
		} else {
			return cyTable.getRow(rowName).get(colName, String.class);
		}
	}

	public Object getValueAt(int row, String colName) {
		// final String colName = columnNames[col];
		final Long rowName = rowNames[row];
		// swingColumns = new String[] { colShownChart, colName, colDescription, colFDR,
		// colGenesCount, colGenes, colGenesSUID };
		if (colName.equals(EnrichmentTerm.colShowChart)) {
			return cyTable.getRow(rowName).get(colName, Boolean.class);
		} else if (colName.equals(EnrichmentTerm.colFDR)) {
			return cyTable.getRow(rowName).get(colName, Double.class);
		} else if (colName.equals(EnrichmentTerm.colGenesCount)) {
			return cyTable.getRow(rowName).get(colName, Integer.class);
		} else if (colName.equals(EnrichmentTerm.colGenes)) {
			return cyTable.getRow(rowName).getList(colName, String.class);
		} else if (colName.equals(EnrichmentTerm.colGenesSUID)) {
			return cyTable.getRow(rowName).getList(colName, Long.class);
		} else {
			return cyTable.getRow(rowName).get(colName, String.class);
		}
	}

	public Class<?> getColumnClass(int c) {
		final String colName = columnNames[c];
		// return cyTable.getColumn(colName).getClass();
		if (colName.equals(EnrichmentTerm.colShowChart)) {
			return Boolean.class;
		} else if (colName.equals(EnrichmentTerm.colFDR)) {
			return Double.class;
		} else if (colName.equals(EnrichmentTerm.colGenesCount)) {
			return Integer.class;
		} else if (colName.equals(EnrichmentTerm.colGenes)) {
			return List.class;
		} else if (colName.equals(EnrichmentTerm.colGenesSUID)) {
			return List.class;
		} else {
			return String.class;
		}
	}

	public boolean isCellEditable(int row, int col) {
		if (columnNames[col].equals(EnrichmentTerm.colShowChart)) {
			return true;
		} else {
			return false;
		}
	}

	public void setValueAt(Object value, int row, int col) {
		final String colName = columnNames[col];
		final Long rowName = rowNames[row];
		if (cyTable.getColumn(colName) == null) {
			cyTable.createColumn(EnrichmentTerm.colShowChart, Boolean.class, false);
		}
		cyTable.getRow(rowName).set(colName, value);
		fireTableCellUpdated(row, col);
	}

	private void initData() {
		List<CyRow> rows = cyTable.getAllRows();
		// Object[][] data = new Object[rows.size()][EnrichmentTerm.swingColumns.length];
		rowNames = new Long[rows.size()];
		int i = 0;
		for (CyRow row : rows) {
			rowNames[i] = row.get(EnrichmentTerm.colID, Long.class);
			i++;
		}

	}
}