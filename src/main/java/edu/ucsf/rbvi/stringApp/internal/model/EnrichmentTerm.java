package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EnrichmentTerm implements Comparable<EnrichmentTerm> {
	String name;
	String description;
	String category;
	double pvalue;
	double bonfPValue;
	double fdrPValue;
	List<String> genes;
	List<Long> nodes;

	public static final String enrichmentURLTest = "http://gamma.string-db.org/cgi/webservices/enrichmentWrapper.pl";
	public static final String enrichmentURL = "http://version-10.string-db.org/cgi/webservices/enrichmentWrapper.pl";

	// Change to an enum?
	public static enum TermCategory {
		GOPROCESS("Process", "GO Process", "STRING Enrichment: GO Biological Process"),
		GOCOMPONENT("Component", "GO Component", "STRING Enrichment: GO Cellular Component"),
		GOFUNCTION("Function", "GO Function", "STRING Enrichment: GO Molecular Function"),
		INTERPRO("InterPro", "InterPro", "STRING Enrichment: InterPro"),
		KEGG("KEGG", "KEGG Pathways", "STRING Enrichment: KEGG"),
		PFAM("PFAM", "PFAM", "STRING Enrichment: Pfam"),
		ALL("All", "All", "STRING Enrichment: All");

		String key, name, table;
		TermCategory(String key, String name, String table) {
			this.key = key;
			this.name = name;
			this.table = table;
		}

		public String getKey() { return key; }
		public String getName() { return name; }
		public String getTable() { return table; }
		public String toString() { return name; }
		static public List<String> getCategories() {
			List<String> cats = new ArrayList<String>();
			for (TermCategory tc: values()) {
				cats.add(tc.getKey());
			}
			return cats;
		}
		static public List<TermCategory> getValues() {
			List<TermCategory> cats = new ArrayList<TermCategory>();
			for (TermCategory tc: values()) {
				if (tc != ALL)
					cats.add(tc);
			}
			return cats;
		}
		static public List<String> getTables() {
			List<String> tables = new ArrayList<String>();
			for (TermCategory tc: values()) {
				tables.add(tc.getTable());
			}
			return tables;
		}
		static public boolean containsKey(String key) {
			for (TermCategory tc: values()) {
				if (tc.getKey().equals(key))
					return true;
			}
			return false;
		}
		static public String getName(String key) {
			for (TermCategory tc: values()) {
				if (tc.getKey().equals(key))
					return tc.getName();
			}
			return null;
		}
	}

	public static final String colID = "term id";
	public static final String colName = "term name";
	public static final String colDescription = "description";
	public static final String colCategory = "category";
	public static final String colPvalue = "p-value";
	public static final String colBonferroni = "bonferroni p-value";
	public static final String colFDR = "FDR p-value";
	public static final String colGenes = "enriched genes";
	public static final String colGenesSUID = "nodes.SUID";
	public static final String colGenesCount = "# enriched genes";
	public static final String colNetworkSUID = "network.SUID";
	// public static final String colShowChart = "showInPieChart";
	public static final String colChartColor = "chart color";

	public static final String colEnrichmentTermsNames = "enrichmentTermsNames";
	public static final String colEnrichmentTermsIntegers = "enrichmentTermsIntegers";
	public static final String colEnrichmentPassthrough = "enrichmentPassthrough";

	
	public static final String[] swingColumns = new String[] { colCategory, colChartColor, colName, colDescription, colFDR,
			colGenesCount, colGenes, colGenesSUID };
	public static final int nodeSUIDColumn = 7;
	public static final int fdrColumn = 4;
	// public static final int chartColumnSel = 1;
	public static final int chartColumnCol = 1;
	public static final int nameColumn = 2;
	
	public EnrichmentTerm() {
		this.name = "";
		this.description = "";
		this.category = "";
		this.pvalue = -1.0;
		this.bonfPValue = -1.0;
		this.fdrPValue = -1.0;
		this.genes = new ArrayList<String>();
		this.nodes = new ArrayList<Long>();

	}

	public EnrichmentTerm(String enrichmentCategory) {
		this.name = "";
		this.description = "";
		this.category = enrichmentCategory;
		this.pvalue = -1.0;
		this.bonfPValue = -1.0;
		this.fdrPValue = -1.0;
		this.genes = new ArrayList<String>();
		this.nodes = new ArrayList<Long>();

	}

	public EnrichmentTerm(String name, String description, String category, double pvalue, double bonfPValue,
			double fdrPValue) {
		this.name = name;
		this.description = description;
		this.category = category;
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

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
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
