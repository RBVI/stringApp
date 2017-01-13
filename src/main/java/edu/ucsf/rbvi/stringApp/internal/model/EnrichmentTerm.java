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

	public static final String enrichmentURL = "http://gamma.string-db.org/cgi/webservices/enrichmentWrapper.pl";

	public static final String[] termCategories = new String[] { "Process", "Function", "Component",
			"KEGG", "Pfam", "InterPro" };
	public static final String[] termTables = new String[] { "STRING Enrichment Table: Process",
			"STRING Enrichment Table: Function", "STRING Enrichment Table: Component",
			"STRING Enrichment Table: KEGG", "STRING Enrichment Table: Pfam",
			"STRING Enrichment Table: InterPro" };

	public static final String colID = "id";
	public static final String colName = "name";
	public static final String colDescription = "pathwayDescription";
	public static final String colPvalue = "pValue";
	public static final String colBonferroni = "bonferroni";
	public static final String colFDR = "falseDiscoveryRate";
	public static final String colGenes = "enrichedGenes";
	public static final String colGenesSUID = "nodes.SUID";
	public static final String colGenesCount = "countInGeneSet";

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

	public double getFDRPValue() {
		return fdrPValue;
	}

	public String getDescription() {
		return description;
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
