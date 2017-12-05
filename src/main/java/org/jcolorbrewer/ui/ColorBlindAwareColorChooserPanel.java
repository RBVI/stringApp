package org.jcolorbrewer.ui;

import java.awt.Container;
import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andreas on 7/19/15.
 */
public abstract class ColorBlindAwareColorChooserPanel extends AbstractColorChooserPanel  {

    boolean showColorBlindSave = false;
		ColorBlindAwareColorChooserPanel chooserPanel;

    List<Container> currentButtons = new ArrayList<Container>();

    public boolean isShowColorBlindSave() {
        return showColorBlindSave;
    }

    public void setShowColorBlindSave(boolean showColorBlindSave) {
				if (this.showColorBlindSave == showColorBlindSave)
					return;
        this.showColorBlindSave = showColorBlindSave;
				chooserPanel = null;
        this.repaint();
    }

    public void updateChooser() {

        if ( currentButtons == null)
            currentButtons = new ArrayList<Container>();

				if (chooserPanel == null) {
        	for (Container c: currentButtons) {
         	   remove(c);
        	}
        	currentButtons.clear();

        	buildChooser();
					chooserPanel = this;
				}

    }

}
