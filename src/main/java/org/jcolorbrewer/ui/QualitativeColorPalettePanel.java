/*
 * BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence. This should
 * be distributed with the code. If you do not have a copy,
 * see:
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors. These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 * http://www.biojava.org/
 *
 * This code was contributed from the Molecular Biology Toolkit
 * (MBT) project at the University of California San Diego.
 *
 * Please reference J.L. Moreland, A.Gramada, O.V. Buzko, Qing
 * Zhang and P.E. Bourne 2005 The Molecular Biology Toolkit (MBT):
 * A Modular Platform for Developing Molecular Visualization
 * Applications. BMC Bioinformatics, 6:21.
 *
 * The MBT project was funded as part of the National Institutes
 * of Health PPG grant number 1-P01-GM63208 and its National
 * Institute of General Medical Sciences (NIGMS) division. Ongoing
 * development for the MBT project is managed by the RCSB
 * Protein Data Bank(http://www.pdb.org) and supported by funds
 * from the National Science Foundation (NSF), the National
 * Institute of General Medical Sciences (NIGMS), the Office of
 * Science, Department of Energy (DOE), the National Library of
 * Medicine (NLM), the National Cancer Institute (NCI), the
 * National Center for Research Resources (NCRR), the National
 * Institute of Biomedical Imaging and Bioengineering (NIBIB),
 * the National Institute of Neurological Disorders and Stroke
 * (NINDS), and the National Institute of Diabetes and Digestive
 * and Kidney Diseases (NIDDK).
 *
 * Created on 2011/11/08
 *
 */
package org.jcolorbrewer.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;

import org.jcolorbrewer.ColorBrewer;


/**
 * Creates a color palette of qualitative colors defined by ColorBrewer
 * 
 * @author Peter Rose
 */
public class QualitativeColorPalettePanel extends ColorBlindAwareColorChooserPanel
                               implements ActionListener {
	private static final long serialVersionUID = 1L;
	// protected String selectedPalette = null;

	protected JToggleButton createPalette(ColorBrewer brewer, Border normalBorder) {
		JToggleButton palette = new JToggleButton();
		palette.setActionCommand(brewer.name());
		palette.addActionListener(this);
		Icon icon = new PaletteIcon(brewer, 8, 15, 15);
		palette.setIcon(icon);
		palette.setToolTipText(brewer.getPaletteDescription());
		palette.setBorder(normalBorder);
		return palette;
	}

	protected void buildChooser() {
		setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));

		//ButtonGroup boxOfPalettes = new ButtonGroup();
		Border border = BorderFactory.createEmptyBorder(2,4,2,4);
		Border selectedBorder = BorderFactory.createLineBorder(Color.blue, 2);

		for (ColorBrewer palette: ColorBrewer.getQualitativeColorPalettes(isShowColorBlindSave())) {
			if ( isShowColorBlindSave() ){
				if (!  palette.isColorBlindSave()) {
					continue;
				}
			}

			JToggleButton button = createPalette(palette, border);
			add(button);
			currentButtons.add(button);
		}
	}

	@Override

	public void actionPerformed(ActionEvent e) {
		ColorSelectionModel model = getColorSelectionModel();

		String command;
		if (e.getSource() instanceof JButton) {
			command = ((JButton)e.getSource()).getActionCommand();
		} else if (e.getSource() instanceof JToggleButton) {
			command = ((JToggleButton)e.getSource()).getActionCommand();
		} else {
			return; // Shouldn't happen
		}
		String[] colorSplit = command.split(":");
		selectedPalette = colorSplit[0];
		if (colorSplit.length == 2) {
			Color color = new Color(Integer.parseInt(colorSplit[1]));
			model.setSelectedColor(color);
		}

		for (ColorBrewer palette: ColorBrewer.getQualitativeColorPalettes(isShowColorBlindSave())) {
			if (palette.name().equals(selectedPalette)) {
				((ColorPanelSelectionModel) model).setColorBrewer(palette);
				break;
			}
		}
	}

	public String getDisplayName() {return "Qualitative";}
	
	public void stateChanged(ChangeEvent ce) {
		getColorSelectionModel().setSelectedColor(new Color(1));
	}

	@Override
	public void setSelectedPalette(String palette) {
		ColorSelectionModel model = getColorSelectionModel();
		for (ColorBrewer plt: ColorBrewer.getQualitativeColorPalettes(isShowColorBlindSave())) {
			if (plt.name().equals(selectedPalette)) {
				((ColorPanelSelectionModel) model).setColorBrewer(plt);
				break;
			}
		}
	}

	@Override
	public Icon getLargeDisplayIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Icon getSmallDisplayIcon() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
