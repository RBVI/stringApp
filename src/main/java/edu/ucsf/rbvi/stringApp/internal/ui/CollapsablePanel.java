package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * A generic Swing {@link JPanel} that wraps another panel behind a clickable,
 * labeled header with an expand/collapse arrow, so the wrapped content can be
 * shown or hidden by the user. Used in stringApp dialogs to tuck away
 * secondary content (e.g. advanced options) until the user asks for it.
 */
public class CollapsablePanel extends JPanel {
	private static String RIGHT_ARROW = "\uF0DA";
	private static String DOWN_ARROW = "\uF0D7";
	Font awesomeFont;

	JPanel contentPanel_;
	HeaderPanel headerPanel_;

	private class HeaderPanel extends JPanel implements ActionListener {
		Font font;
		JButton expandButton;
		JLabel label;
		boolean expanded = false;

		public HeaderPanel(Font iconFont, String text, boolean collapsed, int fontSize) {
			font = new Font("sans-serif", Font.BOLD, fontSize);

			this.setLayout(new GridBagLayout());
			this.expanded = !collapsed;

			EasyGBC c = new EasyGBC();

			if (collapsed)
				expandButton = new JButton(RIGHT_ARROW);
			else
				expandButton = new JButton(DOWN_ARROW);
			expandButton.addActionListener(this);
			expandButton.setBorderPainted(false);
			expandButton.setContentAreaFilled(false);
			expandButton.setOpaque(false);
			expandButton.setFocusPainted(false);
			expandButton.setFont(iconFont);
			this.add(expandButton, c.anchor("west").noExpand());
			label = new JLabel(text);
			label.setFont(font);
			this.add(label, c.right().expandHoriz());
			this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
			this.setBackground(Color.LIGHT_GRAY);
			setPreferredSize(new Dimension(200, 20));
		}

		public void setText(String text) {
			label.setText("<html>"+text+"</html>");
		}

		public void actionPerformed(ActionEvent e) {
			toggleSelection();
		}

		public void setButton(String buttonState) {
			expandButton.setText(buttonState);
		}

	}

	public CollapsablePanel(Font iconFont, String text, JPanel panel, boolean collapsed) {
		this(iconFont, text, panel, collapsed, 14);
	}

	public CollapsablePanel(Font iconFont, String text, JPanel panel, boolean collapsed, int fontSize) {
		super();
		setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		headerPanel_ = new HeaderPanel(iconFont, text, collapsed, fontSize);

		setBackground(new Color(200, 200, 220));
		contentPanel_ = panel;

		add(headerPanel_, c.anchor("northwest").down().expandHoriz());
		add(contentPanel_, c.anchor("west").down().expandBoth());
		contentPanel_.setVisible(!collapsed);
	}

	public void setLabel(String label) {
		headerPanel_.setText(label);
	}

	public void addContent(JComponent panel) {
		contentPanel_.add(panel);
	}

	public JPanel getContent() {
		return contentPanel_;
	}

	public void toggleSelection() {
		if (contentPanel_.isShowing()) {
			headerPanel_.setButton(RIGHT_ARROW);
			contentPanel_.setVisible(false);
		} else {
			contentPanel_.setVisible(true);
			headerPanel_.setButton(DOWN_ARROW);
		}

		validate();

		headerPanel_.repaint();
	}

	public void collapse() {
		if (contentPanel_.isShowing())
			toggleSelection();
	}

	public void expand() {
		if (!contentPanel_.isShowing())
			toggleSelection();
	}

}
