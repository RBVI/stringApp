package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.OpenBrowser;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public abstract class AbstractStringPanel extends JPanel {

	protected final StringManager manager;
	protected final OpenBrowser openBrowser;
	protected final Font iconFont;
	protected final Font labelFont;
	protected final Font textFont;
	protected CyNetwork currentNetwork;
	protected Map<CyNetwork, Map<String,Map<String, Long>>> filters;

	public AbstractStringPanel(final StringManager manager) {
		this.manager = manager;
		this.openBrowser = manager.getService(OpenBrowser.class);
		this.currentNetwork = manager.getCurrentNetwork();
		IconManager iconManager = manager.getService(IconManager.class);
		iconFont = iconManager.getIconFont(17.0f);
		labelFont = new Font("SansSerif", Font.BOLD, 10);
		textFont = new Font("SansSerif", Font.PLAIN, 10);
		filters = new HashMap<>();
		filters.put(currentNetwork, new HashMap<>());
	}

	abstract void doFilter(String type);
	
	protected JComponent createFilterSlider(String type, String text, CyNetwork network, boolean labels, Class clzz) {
		long value = 0;
		if (filters.containsKey(network) && 
		    filters.get(network).containsKey(type) && 
		    filters.get(network).get(type).containsKey(text)) {
			value = filters.get(network).get(type).get(text);
		}
		Box box = Box.createHorizontalBox();
		if (labels) {
			JLabel label = new JLabel(text);
			label.setFont(labelFont);
			label.setPreferredSize(new Dimension(100,20));
			box.add(Box.createRigidArea(new Dimension(10,0)));
			box.add(label);
			box.add(Box.createHorizontalGlue());
		}
		JSlider slider;
		if (clzz.equals(Double.class))
			slider = new JSlider(0,100,(int)value);
		else
			slider = new JSlider(0,5,(int)value);

		slider.setPreferredSize(new Dimension(100,20));
		box.add(slider);
		box.add(Box.createHorizontalGlue());
		JTextField textField;
		if (clzz.equals(Double.class)) {
			textField = new JTextField(Long.toString(value),4);
			textField.setPreferredSize(new Dimension(30,20));
			textField.setMaximumSize(new Dimension(30,20));
		} else {
			textField = new JTextField(Long.toString(value),1);
			textField.setPreferredSize(new Dimension(10,20));
			textField.setMaximumSize(new Dimension(10,20));
		}
		textField.setFont(textFont);
		box.add(textField);
		// Hook it up
		addChangeListeners(type, text, slider, textField, clzz);
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		return box;
	}

	protected void addChangeListeners(String type, String label, JSlider slider, 
	                                  JTextField textField, Class clzz) {
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider sl = (JSlider)e.getSource();
				int value = sl.getValue();
				if (clzz.equals(Double.class)) {
					double v = ((double)value)/100.0;
					textField.setText(String.format("%.2f",v));
				} else {
					textField.setText(Integer.toString(value));
				}
				addFilter(type, label, value);
				doFilter(type);
			}
		});

		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JTextField field = (JTextField)e.getSource();
				try {
					if (clzz.equals(Double.class)) {
						int value = (int)Double.parseDouble(field.getText())*100;
						slider.setValue(value);
					} else {
						int value = Integer.parseInt(field.getText());
						slider.setValue(value);
					}
				} catch (Exception ex) {
					// not an int?
					if (clzz.equals(Double.class)) {
						double v = ((double)slider.getValue())/100.0;
						field.setText(String.format("%.2f",v));
					} else {
						field.setText(Integer.toString(slider.getValue()));
					}
				}
			}
		});
	}

	protected void addFilter(String type, String label, int value) {
		Map<String,Long> filter = filters.get(currentNetwork).get(type);
		filter.put(label, (long) value);

		if (value == 0)
			filter.remove(label);
	}
}
