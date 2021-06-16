package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EnrichmentTerm implements Comparable<EnrichmentTerm> {
	String name;
	String description;
	int year;
	String category;
	double pvalue;
	double bonfPValue;
	double fdrPValue;
	int genesBG;
	List<String> genes;
	List<Long> nodes;

	public static final String enrichmentURLTest = "http://gamma.string-db.org/cgi/webservices/enrichmentWrapper.pl";
	public static final String enrichmentURL = "http://version-10.string-db.org/cgi/webservices/enrichmentWrapper.pl";

	// Change to an enum?
	public static enum TermCategory {
		ALL("All", "All", "STRING Enrichment: All"),
		ALLFILTERED("AllFilt", "All Filtered", "STRING Enrichment: All Filtered"),

		GOPROCESS("Process", "GO Process", "STRING Enrichment: GO Biological Process"),
		GOCOMPONENT("Component", "GO Component", "STRING Enrichment: GO Cellular Component"),
		GOFUNCTION("Function", "GO Function", "STRING Enrichment: GO Molecular Function"),
		
		KEGG("KEGG", "KEGG Pathways", "STRING Enrichment: KEGG Pathways"),
		REACTOME("RCTM", "Reactome Pathways", "STRING Enrichment: Reactome Pathways"),
		WIKIPATHWAYS("WikiPathways", "WikiPathways", "STRING Enrichment: WikiPathways"),
		
		STRINGCLUSTER("NetworkNeighborAL", "STRING Clusters", "STRING Enrichment: STRING network clusters"),
		UniProt("Keyword", "UniProt Keywords", "STRING Enrichment: UniProt Keywords"),		

		INTERPRO("InterPro", "InterPro Domains", "STRING Enrichment: InterPro Protein Domains"),
		PFAM("PFAM", "PFAM Domains", "STRING Enrichment: Pfam Protein Domains"),
		SMART("SMART", "SMART Domains", "STRING Enrichment: SMART Protein Domains"),
		
		PMID("PMID", "Reference publications", "STRING Enrichment: PMID"),
		
		COMPARTMENTS("COMPARTMENTS", "COMPARTMENTS", "STRING Enrichment: COMPARTMENTS"),
		TISSUES("TISSUES", "TISSUES", "STRING Enrichment: TISSUES"),
		DISEASES("DISEASES", "DISEASES", "STRING Enrichment: DISEASES");
		
		// AGOTOOL_UNIPROT("UniProt keywords", "UniProt Keywords", "aGO tool Enrichment: "),
		// AGOTOOL_PMID("PMID (PubMed IDentifier)", "Reference publications", "aGO tool Enrichment: "),
		// AGOTOOL_COMPARTMENTS("Gene Ontology cellular component TEXTMINING", "COMPARTMENTS", "aGO tool Enrichment: "),
		// AGOTOOL_TISSUES("Brenda Tissue Ontology", "TISSUES", "aGO tool Enrichment: "), 
		// AGOTOOL_DISEASES("Disease Ontology", "DISEASES", "aGO tool Enrichment: "),
		// AGOTOOL_KEGG("KEGG (Kyoto Encyclopedia of Genes and Genomes)", "KEGG Pathways", "aGO tool Enrichment: "),
		// AGOTOOL_REACTOME("Reactome", "Reactome Pathways", "aGO tool Enrichment: "),
		// AGOTOOL_WIKIPATHWAYS("WikiPathways", "WikiPathways", "aGO tool Enrichment: "),
		// AGOTOOL_GOPROCESS("Gene Ontology biological process", "GO Process", "aGO tool Enrichment: "),
		// AGOTOOL_GOCOMPONENT("Gene Ontology cellular component", "GO Component", "aGO tool Enrichment: "),
		// AGOTOOL_GOFUNCTION("Gene Ontology molecular function", "GO Function", "aGO tool Enrichment: "),
		
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
		// return only the categories that should/could be filtered (exclude publications and all)
		static public List<TermCategory> getValues() {
			List<TermCategory> cats = new ArrayList<TermCategory>();
			for (TermCategory tc: values()) {
				if (tc != ALL && tc != ALLFILTERED && tc!= PMID)
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
	public static final String colIDPubl = "PMID";
	public static final String colName = "term name";
	public static final String colYear = "year";
	public static final String colDescription = "description";
	public static final String colCategory = "category";
	public static final String colPvalue = "p-value";
	public static final String colBonferroni = "bonferroni value";
	public static final String colFDR = "FDR value";
	public static final String colFDRTransf = "transferred FDR value";
	public static final String colGenesBG = "# background genes";
	public static final String colGenes = "genes";
	public static final String colGenesOld = "enriched genes";
	public static final String colGenesSUID = "nodes.SUID";
	public static final String colGenesCount = "# genes";
	public static final String colGenesCountOld = "# enriched genes";
	public static final String colNetworkSUID = "network.SUID";
	// public static final String colShowChart = "showInPieChart";
	public static final String colChartColor = "chart color";

	public static final String colEnrichmentTermsNames = "enrichmentTermsNames";
	public static final String colEnrichmentTermsIntegers = "enrichmentTermsIntegers";
	public static final String colEnrichmentPassthrough = "enrichmentPassthrough";

	
	public static final String[] swingColumnsEnrichment = new String[] { colCategory, colChartColor, colName, colDescription, colFDR,
			colGenesCount, colGenesBG, colGenes, colGenesSUID };
	public static final String[] swingColumnsEnrichmentOld = new String[] { colCategory, colChartColor, colName, colDescription, colFDR,
			colGenesCountOld, colGenesBG, colGenesOld, colGenesSUID };
	public static final int nodeSUIDColumn = 8;
	public static final int fdrColumn = 4;
	// public static final int chartColumnSel = 1;
	public static final int chartColumnCol = 1;
	public static final int nameColumn = 2;
	
	public static final String[] swingColumnsPublications = new String[] { colIDPubl, colYear, colDescription, colFDR,
			colGenesCount, colGenesBG, colGenes, colGenesSUID };
	public static final int nodeSUIDColumnPubl = 7;
	public static final int fdrColumnPubl = 3;
	public static final int idColumnPubl = 0;
	
	public EnrichmentTerm() {
		this.name = "";
		this.year = 0;
		this.description = "";
		this.category = "";
		this.pvalue = -1.0;
		this.bonfPValue = -1.0;
		this.fdrPValue = -1.0;
		this.genesBG = 0;
		this.genes = new ArrayList<String>();
		this.nodes = new ArrayList<Long>();

	}

	public EnrichmentTerm(String enrichmentCategory) {
		this.name = "";
		this.year = 0;
		this.description = "";
		this.category = enrichmentCategory;
		this.pvalue = -1.0;
		this.bonfPValue = -1.0;
		this.fdrPValue = -1.0;
		this.genesBG = 0;
		this.genes = new ArrayList<String>();
		this.nodes = new ArrayList<Long>();

	}

	public EnrichmentTerm(String name, int year, String description, String category, double pvalue, double bonfPValue,
			double fdrPValue, int genesBG) {
		this.name = name;
		this.year = year;
		this.description = description;
		this.category = category;
		this.pvalue = pvalue;
		this.bonfPValue = bonfPValue;
		this.fdrPValue = fdrPValue;
		this.genesBG = genesBG;
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
		if (desc.length() > 5 && desc.substring(1,5).matches("^\\d{4}")) {
			this.description = desc.substring(6);
			this.year = Integer.parseInt(desc.substring(1,5));
		}
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
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

	public int getGenesBG() {
		return genesBG;
	}

	public void setGenesBG(int genesBG) {
		this.genesBG = genesBG;
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
