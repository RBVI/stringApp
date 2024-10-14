package edu.ucsf.rbvi.stringApp.internal.utils;

public class ColumnNames {
	// Namespaces
	public static String STRINGDB_NAMESPACE = "stringdb";
	public static String NAMESPACE_SEPARATOR = "::";

	// Node information
	public static String COLOR = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "node color";
	public static String CANONICAL = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "canonical name";
	public static String CV_STYLE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "chemViz Passthrough";
	public static String DESCRIPTION = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "description";
	public static String DISEASE_SCORE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "disease score";
	public static String DISPLAY = "display name";
	public static String ELABEL_STYLE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "enhancedLabel Passthrough";
	public static String FULLNAME = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "full name";
	public static String ID = "@id";
	public static String IMAGE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "imageurl";
	public static String NAMESPACE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "namespace";
	public static String QUERYTERM = "query term";
	public static String SEQUENCE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "sequence";
	public static String SMILES = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "smiles";
	public static String SPECIES = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "species";
	public static String STRINGID = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "database identifier";
	public static String STRUCTURES = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "structures";
	public static String STYLE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "STRING style";
	public static String TYPE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "node type";
	public static String TM_FOREGROUND = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "textmining foreground";
	public static String TM_BACKGROUND = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "textmining background";
	public static String TM_SCORE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "textmining score";

	public static String TARGET_NAMESPACE = "target";
	public static String TISSUE_NAMESPACE = "tissue";
	public static String COMPARTMENT_NAMESPACE = "compartment";

	// Enrichment node information
	public static String ENRICHMENT_NAMESPACE = "enrichment";
	public static String NODE_ENRICHMENT_FDR = ENRICHMENT_NAMESPACE + ColumnNames.NAMESPACE_SEPARATOR + "FDR value";
	public static String NODE_ENRICHMENT_GENES = ENRICHMENT_NAMESPACE + ColumnNames.NAMESPACE_SEPARATOR + "# genes";
	public static String NODE_ENRICHMENT_BG = ENRICHMENT_NAMESPACE + ColumnNames.NAMESPACE_SEPARATOR + "# background genes";
	public static String NODE_ENRICHMENT_CAT = ENRICHMENT_NAMESPACE + ColumnNames.NAMESPACE_SEPARATOR + "category";

	public static String GENESET_DESCRIPTION = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "geneset description";
	public static String GENESET_PRIMARY = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "primary description";
	public static String GENESET_SECONDARY = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "secondary description";
	public static String GENESET_TERTIARY = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "tertiary description";
	
	// Edge information
	public static String SCORE = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "score";
	public static String SCORE_NO_NAMESPACE = "score";
	public static String INTERSPECIES = STRINGDB_NAMESPACE + NAMESPACE_SEPARATOR + "interspecies";
	
	// Network information
	public static String CONFIDENCE = "confidence score";
	public static String NETWORK_TYPE = "network type";
	public static String DATABASE = "database";
	public static String NET_SPECIES = "species";
	public static String NET_DATAVERSION = "data version";
	public static String NET_HAS_IMAGES = "has images";
	public static String NET_URI = "uri";

	public static String NET_ANALYZED_NODES = "analyzedNodes.SUID";
	public static String NET_ANALYZED_NODES_PUBL = "analyzedNodesPubl.SUID";
	public static String NET_PPI_ENRICHMENT = "ppiEnrichment";
	public static String NET_ENRICHMENT_NODES = "enrichmentNodes";
	public static String NET_ENRICHMENT_EXPECTED_EDGES = "enrichmentExpectedEdges";
	public static String NET_ENRICHMENT_EDGES = "enrichmentEdges";
	public static String NET_ENRICHMENT_CLSTR = "enrichmentClusteringCoeff";
	public static String NET_ENRICHMENT_DEGREE = "enrichmentAvgDegree";
	public static String NET_ENRICHMENT_SETTINGS = "enrichmentSettings";
	public static String NET_ENRICHMENT_SETTINGS_TABLE = "Enrichment Settings Table";
	public static String NET_ENRICHMENT_SETTINGS_TABLE_SUID = "enrichmentSettingsTable.SUID";
	public static String NET_ENRICHMENT_GROUP = "enrichmentGroup";
	public static String NET_ENRICHMENT_TABLES = "enrichmentTables";

	public static String NET_ENRICHMENT_VISTEMRS = "visualizedTerms";
	public static String NET_ENRICHMENT_VISCOLORS = "visualizedTermsColors";
	
	public static String STRUCTURE_SOURCE_PDB = "PDB";
	public static String STRUCTURE_SOURCE_AF = "AlphaFold DB";
	public static String STRUCTURE_SOURCE_SM = "SWISS-MODEL";
	public static String USE_ENRICHMENT = "use for enrichment";
}
