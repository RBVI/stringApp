package edu.ucsf.rbvi.stringApp.internal.model;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringNode {
	final StringNetwork stringNetwork;
	final CyNode stringNode;

	public StringNode(final StringNetwork sNet, final CyNode sNode) {
		stringNetwork = sNet;
		stringNode = sNode;
	}

	public String getName() {
		return ModelUtils.getName(stringNetwork.getNetwork(), stringNode);
	}

	public String getStringID() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.STRINGID);
	}

	public String getUniprot() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.CANONICAL);
	}

	public String getUniprotURL() {
		String uniprot = getUniprot();
		if (uniprot == null) return null;
		return "http://www.uniprot.org/uniprot/"+uniprot;
	}

	public String getGeneCardURL() {
		String uniprot = getUniprot();
		if (uniprot == null) return null;
		return "http://www.genecards.org/cgi-bin/carddisp.pl?gene="+uniprot;
	}

	public boolean haveCompartments() {
		return haveData("compartment ", 5);
	}

	public String getCompartments() {
		return getStringID();
	}

	public String getCompartmentsURL() {
		String id = getCompartments();
		if (id == null) return null;
		return "http://compartments.jensenlab.org/"+id;
	}

	public boolean haveTissues() {
		return haveData("tissue ", 5);
	}

	public String getTissues() {
		return getStringID();
	}

	public String getTissuesURL() {
		String id = getTissues();
		if (id == null) return null;
		return "http://tissues.jensenlab.org/"+id;
	}

	public boolean havePharos() {
		return haveData("pharos ", 4);
	}

	public String getPharos() {
		return getUniprot();
	}

	public String getPharosURL() {
		String id = getPharos();
		if (id == null) return null;
		return "http://pharos.nih.gov/idg/targets/"+id;
	}

	public boolean haveTextMining() {
		return haveData("TextMining", 4);
	}

	public String getTextMining() {
		return getStringID();
	}

	public String getTextMiningURL() {
		String id = getTextMining();
		if (id == null) return null;
		return "http://diseases.jensenlab.org/"+id;
	}

	public BufferedImage getStructureImage() {
		BufferedImage bi = null;

		String input = ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.STYLE);
		if (input != null && input.startsWith("string:data:")) {
			input = input.substring(input.indexOf(","));
		}
		byte[] byteStream = Base64.decodeBase64(input);
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(byteStream);
			bi = ImageIO.read(bis);
			bis.close();
		} catch (Exception e) { 
			bi = null;
			e.printStackTrace(); 
		}
		return bi;
	}

	public String getDescription() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.DESCRIPTION);
	}

	public boolean haveData(String columnMatch, int minimumExtra) {
		CyNetwork net = stringNetwork.getNetwork();
		List<String> matchingColumns = new ArrayList<>();
		for (String column: CyTableUtil.getColumnNames(net.getDefaultNodeTable())) {
			if (column.startsWith(columnMatch) && column.length() >= columnMatch.length()+minimumExtra)
				matchingColumns.add(column);
		}

		if (matchingColumns == null || matchingColumns.size() == 0)
			return false;

		for (String column: matchingColumns) {
			if (net.getRow(stringNode).getRaw(column) != null)
				return true;
		}
		return false;
	}
}
