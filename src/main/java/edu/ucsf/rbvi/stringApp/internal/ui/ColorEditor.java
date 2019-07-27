package edu.ucsf.rbvi.stringApp.internal.ui;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.Palette;
import org.cytoscape.util.swing.CyColorPaletteChooser;
import org.cytoscape.util.swing.CyColorPaletteChooserFactory;

/* 
 * ColorEditor.java (compiles with releases 1.3 and 1.4) is used by 
 * TableDialogEditDemo.java.
 */

public class ColorEditor extends AbstractCellEditor
                         implements TableCellEditor,
			            ActionListener {
    Color currentColor;
		Palette currentPalette;
    JButton button;
    // JColorChooser colorChooser;
    CyColorPaletteChooserFactory chooserFactory;
		final Component parent;
    protected static final String EDIT = "edit";

    public ColorEditor(Component parent, CyColorPaletteChooserFactory chooserFactory, Palette current) {
        //Set up the editor (from the table's point of view),
        //which is a button.
        //This button brings up the color chooser dialog,
        //which is the editor from the user's point of view.
				this.chooserFactory = chooserFactory;
				this.parent = parent;
				this.currentPalette = current;
        button = new JButton();
        button.setActionCommand(EDIT);
        button.addActionListener(this);
        button.setBorderPainted(false);

    }

    /**
     * Handles events from the editor button and from
     * the dialog's OK button.
     */
    public void actionPerformed(ActionEvent e) {
        if (EDIT.equals(e.getActionCommand())) {
					// button.setBackground(currentColor);
					CyColorPaletteChooser chooser = chooserFactory.getColorPaletteChooser(BrewerType.QUALITATIVE, false);
					chooser.showDialog(parent, "Pick a color", currentPalette, currentColor, 10);
					currentPalette = chooser.getSelectedPalette();
					currentColor = chooser.getSelectedColor();
           fireEditingStopped();

					/*
            //The user has clicked the cell, so
            //bring up the dialog.
            button.setBackground(currentColor);
            dialog.setColor(currentColor);
						dialog.setColorBrewer(currentPalette);
            // colorChooser.showDialog();
            dialog.setVisible(true);

            //Make the renderer reappear.
            fireEditingStopped();
					*/

        } else { //User pressed dialog's "OK" button.
            // currentColor = dialog.getColor();
						// currentPalette = dialog.getColorPalette();
        }
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    public Object getCellEditorValue() {
        return currentColor;
    }

    //Implement the one method defined by TableCellEditor.
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        currentColor = (Color)value;
        return button;
    }
}

