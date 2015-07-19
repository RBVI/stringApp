package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;

public class TextAreaRenderer extends JTextArea implements TableCellRenderer {
	static int PADDING = 10;
	public TextAreaRenderer() {
		setLineWrap(true);
		setWrapStyleWord(true);
 	}

	public Component getTableCellRendererComponent(JTable table, 
	                                               Object value, 
										                             boolean isSelected, 
																								 boolean hasFocus, int row, int column) {
		setText((String)value);//or something in value, like value.getNote()...
		setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
		if (table.getRowHeight(row) != getPreferredSize().height+PADDING) {
			table.setRowHeight(row, getPreferredSize().height+PADDING);
		}
		return this;
	}
} 
