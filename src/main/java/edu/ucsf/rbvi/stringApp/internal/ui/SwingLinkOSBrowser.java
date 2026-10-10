package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;

import javax.swing.JLabel;


/**
 * A {@link JLabel} styled and behaving like a hyperlink: it renders its text
 * underlined, shows a hand cursor on hover, and opens the associated URI in
 * the system's default browser via {@link java.awt.Desktop}, rather than
 * through a Cytoscape service. Equivalent to {@link SwingLinkCyBrowser} for
 * contexts where no {@code OpenBrowser} service is available.
 */
public class SwingLinkOSBrowser extends JLabel {
	private static final long serialVersionUID = 8273875024682878518L;
	private String text;
	private URI uri;

	public SwingLinkOSBrowser(final String text, final URI uri) {
		super();
		setup(text, uri);
	}

	public SwingLinkOSBrowser(String text, String uri) {
		super();
		setup(text, URI.create(uri));
	}

	public void setup(String t, URI u) {
		text = t;
		uri = u;
		setText(text, true);
		setToolTipText(uri.toString());
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				open(uri);
			}

			public void mouseEntered(MouseEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			public void mouseExited(MouseEvent e) {
				setCursor(Cursor.getDefaultCursor());
			}
		});
	}

	@Override
	public void setText(String text) {
		setText(text, true);
	}

	public void setText(String text, boolean ul) {
		String link = ul ? "<u>" + text + "</u>" : text;
		super.setText("<html><span style=\"color: #000099;\">" + link + "</span></html>");
		this.text = text;
	}

	public String getRawText() {
		return text;
	}

	private void open(URI uri) {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(uri);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
