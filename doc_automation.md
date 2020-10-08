# stringApp Automation Documentation

stringApp version 1.6.1

Last update: 2020-10-08

## List of commands

### Network query

- [Protein query](#protein-query)
- [Compound query](#compound-query)
- [Disease query](#disease-query)
- [PubMed query](#pubmed-query)
- [Stringify network](#stringify-network)

### Modify existing network

- [Expand network](#expand-network)
- [Change confidence](#change-confidence)
- [Add nodes](#add-nodes)
- [Make STRING network](#make-string-network)

### Visual properties

- [Show structure images](#show-structure-images)
- [Hide structure images](#hide-structure-images)
- [Show glass ball effect](#show-glass-ball-effect)
- [Hide glass ball effect](#hide-glass-ball-effect)
- [Show STRING style labels](#show-string-style-labels)
- [Hide STRING style labels](#hide-string-style-labels)

### Functional enrichment
- [Retrieve functional enrichment](#retrieve-functional-enrichment)
- [Show enrichment panel](#show-enrichment-panel)
- [Hide enrichment panel](#hide-enrichment-panel)
- [Filter functional enrichment](#filter-functional-enrichment )
- [Show enrichment charts](#show-enrichment-charts)
- [Hide enrichment charts](#hide-enrichment-charts)
- [Retrieve enriched publications](#retrieve-enriched-publications)
- [Show publications panel](#show-publications-panel) 
- [Hide publications panel](#hide-publications-panel) 

### Other
- [Settings](#settings)
- [List species](#list-species)
- [Version](#version)

## Protein query

`string protein query`

The protein query retrieves a STRING network for one or more proteins. STRING is a database of known and predicted protein interactions for thousands of organisms, which are integrated from several sources, scored, and transferred across orthologs. The network includes both physical interactions and functional associations. 

### Arguments

- `cutoff` (optional) *Double* Default: `0.4` 
   
   The confidence score reflects the cumulated evidence that this interaction exists. Only interactions with scores greater than this cutoff will be returned. It must be a value between 0.0 and 1.0. 

- `includesViruses` (optional) *boolean* Default: `true`
   
   By default, a query will search for identifiers in both the protein and virus databases. By changing this to `false`, only the protein database will be searched.

- `limit` (optional) *int* Default: `10` 
	
   The maximum number of proteins to return in addition to the query set. It must be a value between 0 and 10000.

- `network` (optional) *String* Default: `current`
   
   Specifies a network by name, or by SUID if the prefix `SUID:` is used. The keyword `CURRENT`, or a blank value can also be used to specify the current network.

- `newNetName` (optional) *String*
	
   Name for the network to be created.

- `query` **required** *String*
   
   Comma separated list of protein names or identifiers.

- `species` (optional) *String* Default: `Homo sapiens`
   
   Species name. This should be the actual taxonomic name (e.g. homo sapiens, not human).

- `taxonID` (optional) *int* Default: `9606`
   
   The species taxonomy ID. See the NCBI taxonomy home page for IDs. If both species and taxonID are set to a different species, the taxonID has priority.

### Example

`string protein query query="cdk1"`

Create a network of cdk1 and its first 10 neighbors in human (limit is equal to 10 and species to human by default). 

[List of commands](#list-of-commands) - [List of Network query commands](#network-query)

## Compound query

`string compound query`

The compound query retrieves a STITCH network for one or more proteins or compounds. STITCH is a resource to explore known and predicted interactions of chemicals and proteins. Chemicals are linked to other chemicals and proteins by evidence derived from experiments, databases and the literature.

### Arguments

- `cutoff` (optional) *Double* Default: `0.4` 
   
   The confidence score reflects the cumulated evidence that this interaction exists. Only interactions with scores greater than this cutoff will be returned. It must be a value between 0.0 and 1.0. 

- `includesViruses` (optional) *boolean* Default: `true`
   
   By default, a query will search for identifiers in both the protein and virus databases. By changing this to `false`, only the protein database will be searched.
   
- `limit` (optional) *int* Default: `10` 
	
   The maximum number of proteins and compounds to return in addition to the query set. It must be a value between 0 and 10000.

- `newNetName` (optional) *String*
	
   Name for the network to be created.

- `query` **required** *String*
   
   Comma separated list of protein or compound names or identifiers.

- `species` (optional) *String* Default: `Homo sapiens`
   
   Species name. This should be the actual taxonomic name (e.g. homo sapiens, not human).

- `taxonID` (optional) *int* Default: `9606`
   
   The species taxonomy ID. See the NCBI taxonomy home page for IDs. If both species and taxonID are set to a different species, the taxonID has priority.

### Example

`string compound query query="aspirin"`


[List of commands](#list-of-commands) - [List of Network query commands](#network-query)

## Disease query

`string disease query`

The disease query retrieves a STRING network for the top-N human proteins associated with the queried disease in the DISEASES database. DISEASES is a weekly updated web resource that integrates evidence on disease-gene associations from automatic text mining, manually curated literature, cancer mutation data, and genome-wide association studies. STRING is a database of known and predicted protein interactions for thousands of organisms, which are integrated from several sources, scored, and transferred across orthologs. The network includes both physical interactions and functional associations. 

### Arguments

- `cutoff` (optional) *Double* Default: `0.4` 
   
   The confidence score reflects the cumulated evidence that this interaction exists. Only interactions with scores greater than this cutoff will be returned. It must be a value between 0.0 and 1.0. 

- `disease` **required** *String*
   
   The name (or partial name) of a disease.

- `limit` (optional) *int* Default: `100` 
	
   The maximum number of proteins to return in addition to the query set. It must be a value between 0 and 10000.

- `species` (optional) *String* Default: `Homo sapiens`
   
   Species name. This should be the actual taxonomic name (e.g. homo sapiens, not human).

- `taxonID` (optional) *int* Default: `9606`
   
   The species taxonomy ID. See the NCBI taxonomy home page for IDs. If both species and taxonID are set to a different species, the taxonID has priority.

### Example 

`string disease query disease="Alzheimer's disease"`

[List of commands](#list-of-commands) - [List of Network query commands](#network-query)

## PubMed query 

`string pubmed query`

The PubMed query retrieves a STRING network pertaining to any topic of interest based on text mining of PubMed abstracts. STRING is a database of known and predicted protein interactions for thousands of organisms, which are integrated from several sources, scored, and transferred across orthologs. The network includes both physical interactions and functional associations.

### Arguments

- `cutoff` (optional) *Double* Default: `0.4` 
   
   The confidence score reflects the cumulated evidence that this interaction exists. Only interactions with scores greater than this cutoff will be returned. It must be a value between 0.0 and 1.0. 

- `limit` (optional) *int* Default: `100` 
	
   The maximum number of proteins to return in addition to the query set. It must be a value between 0 and 10000.

- `pubmed` **required** *String*
   
   Enter a pubmed query (see NCBI tutorials for information about pubmed query syntax).

- `species` (optional) *String* Default: `Homo sapiens`
   
   Species name. This should be the actual taxonomic name (e.g. homo sapiens, not human).

- `taxonID` (optional) *int* Default: `9606`
   
   The species taxonomy ID. See the NCBI taxonomy home page for IDs. If both species and taxonID are set to a different species, the taxonID has priority.

### Example 

`string pubmed query pubmed="Alzheimer's disease"`

[List of commands](#list-of-commands) - [List of Network query commands](#network-query)

## Stringify network

`string stringify`

Creates a new network from the nodes and edges of the specified network,by querying STRING for all of the nodes and then copying over the edges from the original network.

### Arguments

- `column` **required** *String* Default: `name`
   
   Select the column to use to query for STRING nodes.

- `cutoff` (optional) *Double* Default: `1.0`
   
   The confidence score reflects the cumulated evidence that this interaction exists. Only interactions with scores greater than this cutoff will be returned. It must be a value betwene 0.0 and 1.0.

- `includeNotMapped` (optional) *boolean* Default: `true`
   
   Option for choosing whether nodes that cannot be mapped to STRING identifiers should be included in the new network or not.

- `compoundQuery` (optional) *boolean* Default: `false`

   Option for considering compounds when resolving the node identifiers and consequently querying STITCH instead of STRING.

- `networkNoGui` **required** *String* Default: `current`
   
   Specifies a network by name, or by SUID if the prefix `SUID:` is used. The keyword `CURRENT`, or a blank value can also be used to specify the current network.

- `species` **required** *String* Default: `Homo sapiens`

### Example

`string stringify column="name" networkNoGui="current" species="Homo sapiens"`

[List of commands](#list-of-commands) - [List of Network query commands](#network-query)





## Expand network

`string expand`

Expands an already existing STRING network by more interactors such as STITCH compounds, proteins of the network species as well as proteins interacting with available viruses or host species proteins.

### Arguments

- `additionalNodes` (optional) *int* Default: `10`

   The maximum number of proteins to return in addition to the nodes in the existing network

- `network` **required** *String* Default: `current`
   
   Specifies a network by name, or by SUID if the prefix `SUID:` is used. The keyword `CURRENT`, or a blank value can also be used to specify the current network.
   
- `nodeTypes` (optional) *String* Default: `STITCH compounds`

   Type of interactors to expand the network by, including `STITCH compounds` (default choice), proteins of the same species as the network's one or other species for which host-virus interactions are available. Proteins are specified by the species name, for example, `Homo sapiens` for human proteins or `Influenza A virus` for influenza A proteins.
   
- `selectivityAlpha` (optional) *Double* Default: `0.5`
   
   The selectivity parameter provides a tradeoff between the specificity and the confidence of new interactors and must be a value between 0.0 and 1.0. Low selectivity will retrieve more hub proteins, which may have many high-confidence interactions to the current network but also many other interactions. High selectivity will retrieve proteins that primarily interact with the current network but with lower confidence, since the higher-confidence hubs have been filtered out.

### Example

`string expand network=current nodeTypes="Homo sapiens"`

[List of commands](#list-of-commands) - [List of Modify existing network commands](#modify-existing-network)

## Change confidence

`string change confidence`

Changes the confidence of the network. If increased, some edges will disappear. If decreased, new edges might be added to the network.

### Arguments

- `confidence`**required** *Double* Default: `0.4`
   
   Confidence score for the STRING interactions to be included in this network. It must be a value between 0.0 and 1.0.

- `network` **required** *String* Default: `current`
   
   Specifies a network by name, or by SUID if the prefix `SUID:` is used. The keyword `CURRENT`, or a blank value can also be used to specify the current network.

### Example

`string change confidence network="current" confidence=0.7`

[List of commands](#list-of-commands) - [List of Modify existing network commands](#modify-existing-network)

## Add nodes

`string add nodes`

Adds a new set of query nodes to an existing STRING network as well as their interactions with the nodes in the existing network.

### Arguments

- `cutoff` (optional) *Double* Default: `0.4` 
   
   The confidence score reflects the cumulated evidence that this interaction exists. Only interactions with scores greater than this cutoff will be returned. It must be a value between 0.0 and 1.0. 

- `includesViruses` (optional) *boolean* Default: `true`
   
   By default, a query will search for identifiers in both the protein and virus databases. By changing this to `false`, only the protein database will be searched.

- `limit` (optional) *int* Default: `10` 
	
   The maximum number of proteins to return in addition to the query set. It must be a value between 0 and 10000.

- `network` (optional) *String* Default: `current`
   
   Specifies a network by name, or by SUID if the prefix `SUID:` is used. The keyword `CURRENT`, or a blank value can also be used to specify the current network.

- `query` **required** *String*
   
   Comma separated list of protein names or identifiers.

- `species` (optional) *String* Default: `Homo sapiens`
   
   Species name. This should be the actual taxonomic name (e.g. homo sapiens, not human).

- `taxonID` (optional) *int* Default: `9606`
   
   The species taxonomy ID. See the NCBI taxonomy home page for IDs. If both species and taxonID are set to a different species, the taxonID has priority.

### Example

`string add nodes query="cdk1"`

[List of commands](#list-of-commands) - [List of Modify existing network commands](#modify-existing-network)

## Make STRING network

`string make string`

Sets the network as a STRING network. This assumes that the network was originally derived from STRING and has all of the necessary STRING columns.

### Arguments 

- `network` (optional) *String* Default: `current`
   
   Specifies a network by name, or by SUID if the prefix `SUID:` is used. The keyword `CURRENT`, or a blank value can also be used to specify the current network.

### Example

`string make string network="current"`

[List of commands](#list-of-commands) - [List of Modify existing network commands](#modify-existing-network)





## Show structure images

`string show images`

Shows the structure images on the nodes.

[List of commands](#list-of-commands) - [List of Visual properties commands](#visual-properties)

## Hide structure images

`string hide images`

Hides the structure images on the nodes.

[List of commands](#list-of-commands) - [List of Visual properties commands](#visual-properties)

## Show glass ball effect

`string show glass`

Shows the glass ball effect on the nodes.

[List of commands](#list-of-commands) - [List of Visual properties commands](#visual-properties)

## Hide glass ball effect

`string hide glass`

Hides the glass ball effect on the nodes.

[List of commands](#list-of-commands) - [List of Visual properties commands](#visual-properties)

## Show STRING style labels

`string show labels`

Shows the STRING style labels on the nodes.

[List of commands](#list-of-commands) - [List of Visual properties commands](#visual-properties)

## Hide STRING style labels

`string hide labels`

Hides the STRING style labels on the nodes.

[List of commands](#list-of-commands) - [List of Visual properties commands](#visual-properties)





## Retrieve functional enrichment

`string retrieve enrichment`

Retrieves the functional enrichment for the current String network. This includes enrichment for GO Process, GO Component, GO Function, InterPro, KEGG Pathways, and PFAM. Note that in order to view the results, the command [Show enrichment panel](#show-enrichment-panel) should be used.

### Arguments

- `allNetSpecies` (optional) *String*
   
   Specify the species, for which enrichment should be run in case of several species in the same network (e.g. virus-host networks). 

- `background` (optional) *String* Default: `genome`
   
   Another STRING network to be used as the background set for the functional ernichment. By default, the whole genome of the chosen species is considered.  

- `selectedNodesOnly` (optional) *boolean* Default: `false`

   Setting this to `true` and selecting a subset of the nodes will retrieve enrichment only for the selected nodes. If this is `false` enrichment will be retrieved for all nodes in the network

### Example

`string retrieve enrichment selectedNodesOnly=true`

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)

## Show enrichment panel

`string show enrichment`

Shows the enrichment panel.

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)

## Hide enrichment panel

`string hide enrichment`

Hides the enrichment panel.

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)

## Filter functional enrichment

`string filter enrichment`

Filters the terms in the enrichment table. 

### Arguments

- `categories` (optional) *String* 

Select the enrichment categories to show in the table. Possible values are:
   - `GO Process`: Gene Ontology Biological Process
   - `GO Component`: Gene Ontology Cellular Component
   - `GO Function`: Gene Ontology Molecular Function
   - `InterPro Domains`: Protein domains from InterPro
   - `KEGG Pathways`: KEGG pathways
   - `PFAM Domains`: Protein domains from PFAM
   - `Reactome Pathways`: Reactome pathways
   - `STRING Clusters`: CLusters of proteins in the STRING network with a specific functional annotation
   - `SMART Domains`: Protein domains from SMART
   - `UniProt Keywords`: Functional annotations from UniProt 

- `overlapCutoff` (optional) *Double* Default: 0.5

This is the maximum Jaccard similarity between annotation terms that will be allowed to keep a term with a lower FDR value than an already selected one. For paier of term with Jaccard similarity larger than this cutoff, the term from the pair with lower FDR will be excluded.

- `removeOverlapping` (optional) *boolean* Default: `false`

Removes terms, whose enriched genes significantly overlap with already selected terms. Terms are ranked based on their FDR value.  

### Example

`string filter enrichment categories="GO Process,KEGG Pathways" removeOverlapping=true`

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)

## Show enrichment charts

`string show charts`

Shows the enrichment charts using the default settings (see [Settings](#settings)).

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)

## Hide enrichment charts

`string hide charts`

Hides the enrichment charts. 

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)


##Retrieve enriched publications

`string retrieve publications`

Retrieves the enriched PubMed publications for the current String network. Note that in order to view the results, the command [Show publications panel](#show-publications-panel) should be used.

### Arguments

- `allNetSpecies` (optional) *String*
   
   Specify the species, for which enrichment should be run in case of several species in the same network (e.g. virus-host networks). 

- `background` (optional) *String* Default: `genome`
   
   Another STRING network to be used as the background set for the publication ernichment. By default, the whole genome of the chosen species is considered.  

- `selectedNodesOnly` (optional) *boolean* Default: `false`

   Setting this to `true` and selecting a subset of the nodes will retrieve enrichment only for the selected nodes. If this is `false` enrichment will be retrieved for all nodes in the network

### Example

`string retrieve publications selectedNodesOnly=true`

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)

## Show publications panel

`string show publications`

Shows the enriched publications panel.

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)

## Hide publications panel

`string hide publications`

Hides the enriched publications panel.

[List of commands](#list-of-commands) - [List of Functional enrichment commands](#functional-enrichment)



## Settings

`string settings`

Adjusts various default settings of the stringApp for network queries, enrichment and visual properties.

### Arguments 

- `additionalProteins` (optional) *int* Default: `0` 

   Default number of additional interactors for the protein and compound query. It must be a number between 0 and 100. 

- `chartType` (optional) *String* Default: `Split donut`

   Set the desired chart type for enrichment visualization. Each chosen term is represented by a color if it annotates the node and by white if it doesnt. Possible values:
   - `Split donut`  
   - `Full donut` 
   - `Donut slices only`
   - `Split Pie Chart`
   - `Pie Chart`


- `defaultConfidence` (optional) *Double* Default: `0.4`

   Default confidence (score) cutoff. It must be a value between 0.0 and 1.0.

- `defaultEnrichmentPalette` (optional) *String* Default: `ColorBrewer Paired colors`

   Set the default Brewer palette for enrichment charts. Possible values are the available Brewer palettes: `ColorBrewer Set1 colors`, `ColorBrewer Pastel1 colors`, `ColorBrewer Set2 colors`, `ColorBrewer Paired colors`, `ColorBrewer Dark colors`, `ColorBrewer Pastel2 colors`, `ColorBrewer Set3 colors`, `ColorBrewer Accents`, `Rainbow OSC`, `Random`, `Rainbow`.

- `maxProteins` (optional) *int* Default: `100`

   Default number of proteins for the disease and PubMed query. It must be a number between 1 and 2000. 

- `nTerms` (optional) *int* Default: `5`

   Set the default number of terms to use for charts. It must be a number between 1 and 2000.

- `overlapCutoff` (optional) *Double* Default: `0.5`

   This is the maximum Jaccard similarity that will be allowed. Values larger than this cutoff will be excluded. It must be a value between 0.0 and 1.0.

- `defaultChannelPalette` (optional) *String* Default: `default channel colors`

   Set the palette to use for the edge colors of the STRING channels. Possible values are the default `default channel colors` and the available Brewer palettes: `ColorBrewer Set1 colors`, `ColorBrewer Pastel1 colors`, `ColorBrewer Set2 colors`, `ColorBrewer Paired colors`, `ColorBrewer Dark colors`, `ColorBrewer Pastel2 colors`, `ColorBrewer Set3 colors`, `ColorBrewer Accents`, `Rainbow OSC`, `Random`, `Rainbow`.

- `showEnhancedLabels` (optional) *boolean* Default: `true`

   Shows STRING style labels by default.

- `showGlassBallEffect` (optional) *boolean* Default: `true`

   Enables STRING glass ball effect by default.

- `showImage` (optional) *boolean* Default: `true`

   Shows structure images by default.

- `species` (optional) *String* Default: `Homo sapiens`

   Default species for network queries. It needs to be one of the species in the list available for the STRING database. See also [List species](#list-species).

### Example 

`string settings species="Mus musculus" defaultConfidence=0.7`

[List of Other commands](#other)

## List species

`string list species`

Retrieves the list of species known to STRING, including the taxonomy ID.

[List of Other commands](#other)

## Version

`string version`

Returns the current version of the app.

[List of Other commands](#other)
