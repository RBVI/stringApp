# GIT repository of the stringApp for Cytoscape

*stringApp* imports functional associations or physical interactions between protein-protein and protein-chemical pairs from [STRING](https://string-db.org/), [Viruses.STRING](http://viruses.string-db.org/), [STITCH](http://stitch.embl.de/), [DISEASES](http://diseases.jensenlab.org/) and from PubMed text mining into Cytoscape. Users provide a list of one or more gene, protein, compound, disease, or PubMed queries, the species, the network type, and a confidence score and *stringApp* queries the database to return the matching network. Currently, four different queries are supported:

- STRING: protein query - enter a list of protein names (e.g. gene symbols or UniProt identifiers/accession numbers) to obtain a STRING network for the proteins
- STRING: PubMed query - enter a PubMed query and utilize text mining to get a STRING network for the top N proteins associated with the query
- STRING: disease query - enter a disease name to retrieve a STRING network of the top N proteins associated with the specified disease
- STITCH: protein/compound query - enter a list of protein or compound names to obtain a network for them from STITCH

For each query, the user can choose to retrieve a *full STRING network* of functional associations or a *physical subnetwork*. 

*stringApp* also allows users to change the confidence score and to expand the resulting network by adding an arbitrary number of nodes; this can be either proteins from the same organism, proteins involved in virus-host interactions, or chemical compounds. All STRING networks are visualized using a "String Style" custom graphic, which closely resembles the networks on the STRING web site.

In addition, *stringApp* can retrieve functional enrichment for Gene Ontology terms, KEGG & Reactome Pathways, and protein domains at a user-specified significance threshold and show the results in a new table in the Table Panel. The app provides several different types of charts to show the enriched terms. Note that you need to install the [enhancedGraphics](http://apps.cytoscape.org/apps/enhancedgraphics) app for the charts to show. 

Since version 1.6, *stringApp* is also integrated with the [clusterMaker](http://apps.cytoscape.org/apps/clustermaker2) and [EnrichmentMap](http://apps.cytoscape.org/apps/enrichmentmap) apps. With the former, users can quickly cluster a STRING network using MCL clustering, while with the latter it is possible to create an enrichment map from the STRING erncihemtn results.  

*stringApp* supports automation from both commands and CyREST.

See also:
- [stringApp tutorials](https://jensenlab.org/training/stringapp/)
- [stringApp automation documentation](doc_automation.md)
- [Cite stringApp](https://pubmed.ncbi.nlm.nih.gov/30450911/)
