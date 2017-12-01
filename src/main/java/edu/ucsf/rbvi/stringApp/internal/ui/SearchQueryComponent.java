package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.util.swing.LookAndFeelUtil;

public class SearchQueryComponent extends JTextField {
	private static final long serialVersionUID = 1L;
	private JTextArea queryTextArea = null;
	private JScrollPane queryScroll = null;
	private JPopupMenu popup = null;
	private static final String DEF_SEARCH_TEXT = "Enter one term per line...";
	final int vgap = 1;
	final int hgap = 5;
	final String tooltip;
	Color msgColor;

	public SearchQueryComponent() {
		super();
		init();
		tooltip = "Press " + (LookAndFeelUtil.isMac() ? "Command" : "Ctrl") + "+ENTER to run the search";
	}

	void init() {
		msgColor = UIManager.getColor("Label.disabledForeground");
		setEditable(false);
		setMinimumSize(getPreferredSize());
		setBorder(BorderFactory.createEmptyBorder(vgap, hgap, vgap, hgap));
		setFont(getFont().deriveFont(LookAndFeelUtil.getSmallFontSize()));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				showQueryPopup();
			}
		});

		// Since we provide our own search component, it should let Cytoscape know
		// when it has been updated by the user, so Cytoscape can give a better
		// feedback to the user of whether or not the whole search component is ready
		// (e.g. Cytoscape may enable or disable the search button)
		getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				fireQueryChanged();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				fireQueryChanged();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				// Nothing to do here...
			}
		});
    setToolTipText(tooltip);

		return;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (getText() == null || getText().trim().isEmpty()) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHints(
				new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
			// Set the font
			g2.setFont(getFont());
			// Get the FontMetrics
			FontMetrics metrics = g2.getFontMetrics(getFont());
			// Determine the X coordinate for the text
			int x = hgap;
			// Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
			int y = (metrics.getHeight() / 2) + metrics.getAscent() + vgap;
			// Draw
			g2.setColor(msgColor);
			g2.drawString(DEF_SEARCH_TEXT, x, y);
			g2.dispose();
		}
	}

	public String getQueryText() {
		if (queryTextArea == null) return null;

		return queryTextArea.getText();
	}

	private void showQueryPopup() {
		popup = new JPopupMenu();
		if (queryScroll == null) {
			createQueryScroll();
		}
		popup.setBackground(getBackground());
		popup.setLayout(new BorderLayout());
		popup.add(queryScroll, BorderLayout.CENTER);

		popup.addPropertyChangeListener("visible", evt -> {
		if (evt.getNewValue() == Boolean.FALSE)
			updateQueryTextField();
		});

		queryScroll.setPreferredSize(new Dimension(getSize().width, 200));
		popup.setPreferredSize(queryScroll.getPreferredSize());

		popup.show(this, 0, 0);
		popup.requestFocus();
		queryTextArea.requestFocusInWindow();
    queryTextArea.setToolTipText(tooltip);

	}

	private void updateQueryTextField() {
		// Set<String> query = getAreaText();
		// String text = query.stream().collect(Collectors.joining(" "));
		// TODO: truncate the text -- no need for this to be the entire string
		String text = queryTextArea.getText();
		if (text.length() > 30)
			text = text.substring(0, 30)+"...";
		setText(text);
  }

	private Set<String> getAreaText() {
		Set<String> query = new HashSet<>();
		String text = queryTextArea.getText();
		if (text.length() > 30) {
			text = text.substring(0, 30)+"...";
			System.out.println("Text: "+text);
		}
		String[] split = text.split("\n");

		for (String s: split) {
			s = s.trim();

			if (!s.isEmpty())
				query.add(s);
		}

		return query;

	}

	private void fireQueryChanged() {
		firePropertyChange(NetworkSearchTaskFactory.QUERY_PROPERTY, null, null);
	}

	private void createQueryScroll() {
		queryTextArea = new JTextArea();
		LookAndFeelUtil.makeSmall(queryTextArea);

		// When Ctrl+ENTER (command+ENTER on macOS) is pressed, ask Cytoscape to perform the query
		String ENTER_ACTION_KEY = "ENTER_ACTION_KEY";
		KeyStroke enterKey = KeyStroke.getKeyStroke(
		                               KeyEvent.VK_ENTER,
		                               LookAndFeelUtil.isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK,
		                               false);
		InputMap inputMap = queryTextArea.getInputMap(JComponent.WHEN_FOCUSED);
		inputMap.put(enterKey, ENTER_ACTION_KEY);

		queryTextArea.getActionMap().put(ENTER_ACTION_KEY, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// System.out.println("\n\nENTER");
				SearchQueryComponent.this.firePropertyChange(NetworkSearchTaskFactory.SEARCH_REQUESTED_PROPERTY, null, null);
				popup.setVisible(false);
			}
		});

		queryScroll = new JScrollPane(queryTextArea);
		queryScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		queryScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		LookAndFeelUtil.makeSmall(queryScroll);

	}

}
