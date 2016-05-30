package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.swing.BasicCollapsiblePanel;
import org.cytoscape.util.swing.OpenBrowser;

import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.model.StringNode;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Displays string information
 * @author Scooter Morris
 *
 */
public class StringPanel extends JPanel 
{
	final StringNetwork stringNetwork;
	final StringNode sNode;
	final OpenBrowser openBrowser;

	public StringPanel(final OpenBrowser openBrowser, final StringNetwork stringNetwork, final CyNode node) {
		this.stringNetwork = stringNetwork;
		this.sNode = new StringNode(stringNetwork, node);
		this.openBrowser = openBrowser;

		setLayout(new GridBagLayout());

		EasyGBC c = new EasyGBC();

		{
			// JEditorPane textArea = new JEditorPane("text/html", null);
			// textArea.setBackground(getBackground());
			// textArea.setEditable(false);
			// String message = "<html><div style=\"margin-left: 5px; margin-right: 5px; width=100px;\">"+sNode.getDescription()+"</div></html>";
			// JLabel label = new JLabel(message);
			JTextArea textArea = new JTextArea(sNode.getDescription(), 2, 20);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			textArea.setEditable(false);
			textArea.setBackground(getBackground());
			// textArea.setText(message);
			// textArea.setPreferredSize(new Dimension(100,100));
			add(textArea, c.anchor("west").noExpand());
		}

		// Now add our image
		Image img = sNode.getStructureImage();
		if (img != null) {
			Image scaledImage = img.getScaledInstance(200,200,Image.SCALE_SMOOTH);
			JLabel label = new JLabel(new ImageIcon(scaledImage));
			// label.setPreferredSize(new Dimension(100,100));
			// label.setMinimumSize(new Dimension(100,100));
			add(label, c.down().anchor("west").noExpand());
		}

		// Now, add all of our crosslinks
		{
			JEditorPane textArea = new JEditorPane("text/html", null);
			textArea.addHyperlinkListener(new MyHLinkListener());
			textArea.setBackground(getBackground());
			textArea.setEditable(false);
			String message = "<h3 style=\"margin-left: 5px;margin-bottom: 0px;\">CrossLinks</h3>";
			message += "<table style=\"margin-left: 10px;margin-top: 0px;\">";
			message += "<tr><td>Uniprot: </td>";
			message += "<td><a href=\""+sNode.getUniprotURL()+"\">"+sNode.getUniprot()+"</a></td></tr>";
			message += "<tr><td>GeneCard: </td>";
			message += "<td><a href=\""+sNode.getGeneCardURL()+"\">"+sNode.getUniprot()+"</a></td></tr>";

			if (sNode.haveCompartments()) {
				message += "<tr><td>Compartments: </td>";
				message += "<td><a href=\""+sNode.getCompartmentsURL()+"\">"+sNode.getCompartments()+"</a></td></tr>";
			}

			if (sNode.haveTissues()) {
				message += "<tr><td>Tissues: </td>";
				message += "<td><a href=\""+sNode.getTissuesURL()+"\">"+sNode.getTissues()+"</a></td></tr>";
			}

			if (sNode.haveDisease()) {
				message += "<tr><td>Diseases: </td>";
				message += "<td><a href=\""+sNode.getDiseaseURL()+"\">"+sNode.getDisease()+"</a></td></tr>";
			}

			if (sNode.havePharos()) {
				message += "<tr><td>Pharos: </td>";
				message += "<td><a href=\""+sNode.getPharosURL()+"\">"+sNode.getPharos()+"</a></td></tr>";
			}

			message += "</table>";
			textArea.setText(message);
			add(textArea, c.down().anchor("west").noExpand());
			add(new JPanel(), c.down().anchor("west").expandVert());
		}

	}

	// JEditorPane textArea = new JEditorPane("text/html", null);
	// textArea.setEditable(false);
	// textArea.addHyperlinkListener(new HyperlinkListener() {

	class MyHLinkListener implements HyperlinkListener {
		public void hyperlinkUpdate(HyperlinkEvent e) {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				openBrowser.openURL(e.getURL().toString());
			}
		}
	}

}
