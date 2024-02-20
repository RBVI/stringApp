package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.net.URI;

import javax.swing.JLabel;

import org.cytoscape.util.swing.OpenBrowser;

public class SwingLinkCyBrowser extends JLabel {
  private static final long serialVersionUID = 8273875024682878518L;
  private String text;
  private URI uri;
	private final OpenBrowser openBrowser;

  public SwingLinkCyBrowser(final String text, final URI uri, final OpenBrowser openBrowser){
    super();
		this.openBrowser = openBrowser;
    setup(text,uri);
  }

  public SwingLinkCyBrowser(String text, String uri, final OpenBrowser openBrowser){
    super();
		this.openBrowser = openBrowser;
    setup(text,URI.create(uri));
  }

  public void setup(String t, URI u){
    text = t;
    uri = u;
    setText(text, true);
    setToolTipText(uri.toString());
    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        open(uri);
      }
      public void mouseEntered(MouseEvent e) {
        // setText(text,false);
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
      public void mouseExited(MouseEvent e) {
        // setText(text,true);
				setCursor(Cursor.getDefaultCursor());
      }
    });
  }

  @Override
  public void setText(String text){
    setText(text,true);
  }

  public void setText(String text, boolean ul){
    String link = ul ? "<u>"+text+"</u>" : text;
    super.setText("<html><span style=\"color: #000099;\">"+
    link+"</span></html>");
    this.text = text;
  }

  public String getRawText(){
    return text;
  }

  private void open(URI uri) {
		openBrowser.openURL(uri.toString());
  }
}
