package edu.ucsf.rbvi.stringApp.internal.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

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

	public String getDisplayName() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.DISPLAY);
	}

	public String getSpecies() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.SPECIES);
	}

	public String getStringID() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.STRINGID);
	}

	public String getStructureImageURL() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.IMAGE);
	}

	public boolean isStringNode() {
		if (ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.STRINGID) == null)
			return false;
		return true;
	}

	public boolean haveUniprot() {
		return (getUniprot() != null && !getUniprot().equals(""));
	}
	
	public String getUniprot() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.CANONICAL);
	}

	public String getUniprotURL() {
		String uniprot = getUniprot();
		if (uniprot == null) return null;
		return "http://www.uniprot.org/uniprot/"+uniprot;
	}

	public boolean haveGeneCard() {
		return (haveUniprot() && getSpecies().equals("Homo sapiens"));
	}
	
	public String getGeneCardURL() {
		String uniprot = getUniprot();
		if (uniprot == null) return null;
		// GeneCards only supports human proteins
		if (getSpecies().equals("Homo sapiens"))
			return "http://www.genecards.org/cgi-bin/carddisp.pl?gene="+uniprot;
		return null;
	}

	public boolean haveCompartments() {
		return haveData("compartment",null);
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
		return haveData("tissue",null);
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
		// return haveData("pharos ", 4);
		// pharos* columns were renamed to target*
		// every human protein is in pharos as of now
		return (haveUniprot() && getSpecies().equals("Homo sapiens"));
	}

	public String getPharos() {
		return getUniprot();
	}

	public String getPharosURL() {
		String id = getPharos();
		if (id == null) return null;
		return "http://pharos.nih.gov/idg/targets/"+id;
	}

	public boolean haveDisease() {
		return haveData("stringdb","disease score");
	}

	public String getDisease() {
		return getStringID();
	}

	public String getDiseaseURL() {
		String id = getDisease();
		if (id == null) return null;
		return "http://diseases.jensenlab.org/"+id;
	}

	public String getNodeType() {
		return ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.TYPE);
	}
	
	public boolean havePubChem() {
		if (getNodeType() != null && getNodeType().equals("compound")) 
			return true;
		return false;
	}

	public String getPubChem() {
		String dbID = getStringID();
		Matcher m = ModelUtils.cidmPattern.matcher(dbID);
		if (m.lookingAt())
			return m.replaceAll("");
		return null;
	}
	
	public String getPubChemURL() {
		String id = getPubChem();
		if (id == null || id.equals("")) return null;
		return "https://pubchem.ncbi.nlm.nih.gov/compound/"+id;
	}

	
	public BufferedImage getStructureImage() {
		BufferedImage bi = null;

		String input = ModelUtils.getString(stringNetwork.getNetwork(), stringNode, ModelUtils.STYLE);
		if (input == null) return null;
		if (input.equals("string:")) {
			String url = getStructureImageURL();
			if (url == null || url == "") return null;
			try {
				bi = ImageIO.read(new URL(url));
				return bi;
			} catch (Exception e) {
				// ignore
				bi = null;
				e.printStackTrace();
			}
		} else if (input.startsWith("string:data:")) {
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

	public boolean haveData(String namespace, String columnMatch) {
		CyNetwork net = stringNetwork.getNetwork();
		List<String> matchingColumns = new ArrayList<>();
		for (CyColumn column: net.getDefaultNodeTable().getColumns()) {
			if (namespace != null && column.getNamespace() != null && column.getNamespace().equals(namespace)) {
				if (columnMatch != null && column.getNameOnly().equals(columnMatch)) {
					matchingColumns.add(column.getName());
				} else if (columnMatch == null) {
					matchingColumns.add(column.getName());
				}
			} else if (namespace == null && column.getNameOnly().equals(columnMatch)) {
				matchingColumns.add(column.getName());
			}
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
