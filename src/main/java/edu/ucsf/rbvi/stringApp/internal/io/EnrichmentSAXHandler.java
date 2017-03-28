package edu.ucsf.rbvi.stringApp.internal.io;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm;

public class EnrichmentSAXHandler extends DefaultHandler {
	private Hashtable<String, Integer> tags;

	private CyNetwork network;
	private Map<String, Long> stringNodesMap;
	private double enrichmentCutoff;

	private List<EnrichmentTerm> enrichmentTerms;
	private EnrichmentTerm currTerm;
	private List<String> currGeneList;
	private List<Long> currNodeList;
	private StringBuilder content;
	private String warning;
	private String status;
	private String status_code;

	private boolean in_status = false;
	private boolean in_code = false;
	private boolean in_warning = false;

	private boolean in_term = false;
	private boolean in_name = false;
	private boolean in_description = false;
	private boolean in_numberOfGenes = false;
	private boolean in_pvalue = false;
	private boolean in_bonferroni = false;
	private boolean in_fdr = false;

	private boolean in_genes = false;
	private boolean in_gene = false;

	private final String tag_status = "status";
	private final String tag_code = "code";
	private final String tag_warning = "warning";

	private final String tag_term = "term";
	private final String tag_name = "name";
	private final String tag_description = "description";
	private final String tag_numberOfGenes = "numberOfGenes";
	private final String tag_pvalue = "pvalue";
	private final String tag_bonferroni = "bonferroni";
	private final String tag_fdr = "fdr";

	private final String tag_genes = "genes";
	private final String tag_gene = "gene";

	// <term>
	// <name>GO:0008585</name>
	// <description>female gonad development</description>
	// <numberOfGenes>1</numberOfGenes>
	// <pvalue>1E0</pvalue>
	// <bonferroni>1E0</bonferroni>
	// <fdr>1E0</fdr>
	// <genes><gene>9606.ENSP00000269260</gene></genes>
	// </term>

	public EnrichmentSAXHandler(CyNetwork network, Map<String, Long> stringNodesMap,
			double enrichmentCutoff) {
		this.network = network;
		this.stringNodesMap = stringNodesMap;
		this.enrichmentCutoff = enrichmentCutoff;
		status = null;
		warning = null;
	}

	public void startDocument() throws SAXException {
		tags = new Hashtable<String, Integer>();
		enrichmentTerms = new ArrayList<EnrichmentTerm>();
		content = new StringBuilder();
	}

	public void endDocument() throws SAXException {
		// do something on endDocument?
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
			throws SAXException {
		content = new StringBuilder();
		String key = localName;
		Object value = tags.get(key);
		if (value == null) {
			tags.put(key, new Integer(1));
		} else {
			int count = ((Integer) value).intValue();
			count++;
			tags.put(key, new Integer(count));
		}

		if (key.equals(tag_status)) {
			in_status = true;
		} else if (key.equals(tag_code)) {
			in_code = true;
		} else if (key.equals(tag_warning)) {
			in_warning = true;
		} else if (key.equals(tag_term)) {
			in_term = true;
			currTerm = new EnrichmentTerm();
		} else if (key.equals(tag_name)) {
			in_name = true;
		} else if (key.equals(tag_description)) {
			in_description = true;
		} else if (key.equals(tag_pvalue)) {
			in_pvalue = true;
		} else if (key.equals(tag_bonferroni)) {
			in_bonferroni = true;
		} else if (key.equals(tag_fdr)) {
			in_fdr = true;
		} else if (key.equals(tag_numberOfGenes)) {
			in_numberOfGenes = true;
		} else if (key.equals(tag_genes)) {
			in_genes = true;
			currGeneList = new ArrayList<String>();
			currNodeList = new ArrayList<Long>();
		} else if (key.equals(tag_gene)) {
			in_gene = true;
		}
	}

	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		String key = localName;
		if (key.equals(tag_status)) {
			status = content.toString();
			in_status = false;
		} else if (key.equals(tag_code)) {
			status_code = content.toString();
			in_code = false;
		} else if (key.equals(tag_warning)) {
			warning = content.toString();
			in_warning = false;
		} else if (key.equals(tag_term)) {
			in_term = false;
			if (currTerm.getFDRPValue() <= enrichmentCutoff)
				enrichmentTerms.add(currTerm);
		} else if (key.equals(tag_name)) {
			in_name = false;
			if (in_term)
				currTerm.setName(content.toString());
		} else if (key.equals(tag_description)) {
			in_description = false;
			currTerm.setDescription(content.toString());
		} else if (key.equals(tag_pvalue)) {
			in_pvalue = false;
			double pvalue = Double.parseDouble(content.toString());
			currTerm.setPValue(pvalue);
		} else if (key.equals(tag_bonferroni)) {
			in_bonferroni = false;
			double pvalueB = Double.parseDouble(content.toString());
			currTerm.setBonfPValue(pvalueB);
		} else if (key.equals(tag_fdr)) {
			in_fdr = false;
			double pvalueFDR = Double.parseDouble(content.toString());
			currTerm.setFDRPValue(pvalueFDR);
		} else if (key.equals(tag_numberOfGenes)) {
			in_numberOfGenes = false;
		} else if (key.equals(tag_genes)) {
			in_genes = false;
			currTerm.setGenes(currGeneList);
			currTerm.setNodesSUID(currNodeList);
		} else if (key.equals(tag_gene)) {
			in_gene = false;
			// ... add gene to list
			String enrGeneEnsemblID = content.toString();
			String enrGeneNodeName = enrGeneEnsemblID;
			if (stringNodesMap.containsKey(enrGeneEnsemblID)) {
				final Long nodeSUID = stringNodesMap.get(enrGeneEnsemblID);
				currNodeList.add(nodeSUID);
				if (network.getDefaultNodeTable().getColumn(CyNetwork.NAME) != null) {
					enrGeneNodeName = network.getDefaultNodeTable().getRow(nodeSUID)
							.get(CyNetwork.NAME, String.class);
				}
			}
			currGeneList.add(enrGeneNodeName);
		}

	}

	public void characters(char ch[], int start, int length) {
		content.append(ch, start, length);
	}

	public boolean isStatusOK() {
		if (status != null && status.equals("ok")) {
			return true;
		}
		return false;
	}
	
	public String getStatusCode() {
		return status_code;
	}

	public String getWarning() {
		return warning;
	}

	public List<EnrichmentTerm> getParsedData() {
		return enrichmentTerms;
	}
}

