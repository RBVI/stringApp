package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.ArrayList;
import java.util.List;

public class EnrichmentTerm implements Comparable<EnrichmentTerm> {
	String name;
	String description;
	double pvalue;
	double bonfPValue;
	double fdrPValue;
	List<String> genes;
	List<Long> nodes;

	public static final String enrichmentURLTest = "http://gamma.string-db.org/cgi/webservices/enrichmentWrapper.pl";
	public static final String enrichmentURL = "http://version-10.string-db.org/cgi/webservices/enrichmentWrapper.pl";

	public static final String[] termCategories = new String[] { "Process", "Component", "Function",
			"InterPro", "KEGG", "Pfam" };
	public static final String[] termTables = new String[] {
			"STRING Enrichment: GO Biological Process", "STRING Enrichment: GO Cellular Component",
			"STRING Enrichment: GO Molecular Function", "STRING Enrichment: InterPro",
			"STRING Enrichment: KEGG", "STRING Enrichment: Pfam" };

	public static final String colID = "term id";
	public static final String colName = "term name";
	public static final String colDescription = "termDescription";
	public static final String colPvalue = "pValue";
	public static final String colBonferroni = "bonferroni";
	public static final String colFDR = "falseDiscoveryRate";
	public static final String colGenes = "enrichedGenes";
	public static final String colGenesSUID = "nodes.SUID";
	public static final String colGenesCount = "countInGeneSet";
	public static final String colNetworkSUID = "network.SUID";
	public static final String colShowChart = "showInPieChart";

	public static final String[] swingColumns = new String[] { colName, colDescription, colFDR,
			colGenesCount, colGenes, colGenesSUID };
	public static final int nodeSUIDColumn = 5;
	public static final int fdrColumn = 2;

	public EnrichmentTerm() {
		this.name = "";
		this.description = "";
		this.pvalue = -1.0;
		this.bonfPValue = -1.0;
		this.fdrPValue = -1.0;
		this.genes = new ArrayList<String>();
		this.nodes = new ArrayList<Long>();

	}

	public EnrichmentTerm(String name, String description, double pvalue, double bonfPValue,
			double fdrPValue) {
		this.name = name;
		this.description = description;
		this.pvalue = pvalue;
		this.bonfPValue = bonfPValue;
		this.fdrPValue = fdrPValue;
		this.genes = new ArrayList<String>();
		this.nodes = new ArrayList<Long>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public double getPValue() {
		return pvalue;
	}

	public void setPValue(double pvalue) {
		this.pvalue = pvalue;
	}

	public double getBonfPValue() {
		return bonfPValue;
	}

	public void setBonfPValue(double bonfPValue) {
		this.bonfPValue = bonfPValue;
	}

	public double getFDRPValue() {
		return fdrPValue;
	}

	public void setFDRPValue(double fdrPValue) {
		this.fdrPValue = fdrPValue;
	}

	public int getNumberGenes() {
		return genes.size();
	}

	public List<String> getGenes() {
		return genes;
	}

	public void setGenes(List<String> genes) {
		this.genes = genes;
	}

	public List<Long> getNodesSUID() {
		return nodes;
	}

	public void setNodesSUID(List<Long> nodes) {
		this.nodes = nodes;
	}

	public String toString() {
		return name + "\t" + getNumberGenes() + "\t" + fdrPValue;
	}

	public int compareTo(EnrichmentTerm et) {
		// if (t.toString() == null) return 1;
		if (this.fdrPValue < et.getFDRPValue()) {
			return -1;
		} else if (this.fdrPValue == et.getFDRPValue()) {
			return 0;
		}
		return 1;
	}

}
