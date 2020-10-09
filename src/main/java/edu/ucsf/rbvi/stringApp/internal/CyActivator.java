package edu.ucsf.rbvi.stringApp.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_EXAMPLE_JSON;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND_SUPPORTS_JSON;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;

import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.tasks.AddTermsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ChangeConfidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.DiseaseSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ExpandNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ExportEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ExportPublicationsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.FilterEnrichmentTableTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetPublicationsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetSpeciesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.HideChartsTaskFactory;
// import edu.ucsf.rbvi.stringApp.internal.tasks.FindProteinsTaskFactory;
// import edu.ucsf.rbvi.stringApp.internal.tasks.OpenEvidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.PubmedSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.SetConfidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.SetLabelAttributeTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.SettingsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowChartsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnhancedLabelsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowGlassBallEffectTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowImagesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowPublicationsPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowResultsPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.StitchSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.StringifyTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.StringSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.VersionTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.ui.DiseaseNetworkWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.StitchWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.StringWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.TextMiningWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.view.StringCustomGraphicsFactory;
import edu.ucsf.rbvi.stringApp.internal.view.StringLayer;

// TODO: [Optional] Improve non-gui mode
public class CyActivator extends AbstractCyActivator {
	String JSON_EXAMPLE = "{\"SUID\":1234}";

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		// See if we have a graphics console or not
		boolean haveGUI = true;
		ServiceReference ref = bc.getServiceReference(CySwingApplication.class.getName());

		if (ref == null) {
			haveGUI = false;
			// Issue error and return
		}

		// Get a handle on the CyServiceRegistrar
		CyServiceRegistrar registrar = getService(bc, CyServiceRegistrar.class);
		StringManager manager = new StringManager(registrar);

		// Get our version number
		Version v = bc.getBundle().getVersion();
		String version = v.toString(); // The full version

		// Only look at the .0 version for our internal purposes
		String minorVersion = new Version(v.getMajor(),v.getMinor(), 0).toString();
		manager.setVersion(minorVersion);
		
		// Get configuration and messages for user from server 
		manager.updateURIsFromConfig();
		
		{
			// Register our network added listener and session loaded listener
			registerService(bc, manager, NetworkAddedListener.class, new Properties());
			registerService(bc, manager, SessionLoadedListener.class, new Properties());
			registerService(bc, manager, NetworkAboutToBeDestroyedListener.class, new Properties());
		}

		{
			// Register our web service client
			StringWebServiceClient client = new StringWebServiceClient(manager);
			registerAllServices(bc, client, new Properties());
		}
		
		{
			// Register our text mining web service client
			TextMiningWebServiceClient client = new TextMiningWebServiceClient(manager);
			registerAllServices(bc, client, new Properties());
		}
		
		{
			// Register our disease network web service client
			DiseaseNetworkWebServiceClient client = new DiseaseNetworkWebServiceClient(manager);
			registerAllServices(bc, client, new Properties());
		}
		
		{
			// Register our stitch network web service client
			StitchWebServiceClient client = new StitchWebServiceClient(manager);
			registerAllServices(bc, client, new Properties());
		}

		{
			GetNetworkTaskFactory getNetwork = new GetNetworkTaskFactory(manager, "protein");
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "protein query");
			props.setProperty(COMMAND_DESCRIPTION, 
										    "Create a STRING network from multiple protein names/identifiers");
			props.setProperty(COMMAND_LONG_DESCRIPTION,
								"<html>The protein query retrieves a STRING network for one or more proteins. <br />"
								+ "STRING is a database of known and predicted protein interactions for <br />"
								+ "thousands of organisms, which are integrated from several sources, <br />"
								+ "scored, and transferred across orthologs. The network includes both <br />"
								+ "physical interactions and functional associations.</html>");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);

			registerService(bc, getNetwork, TaskFactory.class, props);
		}

		{
			GetNetworkTaskFactory getNetwork = new GetNetworkTaskFactory(manager, "disease");
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "disease query");
			props.setProperty(COMMAND_DESCRIPTION, 
										    "Create a STRING network by finding proteins associated with a disease");
			props.setProperty(COMMAND_LONG_DESCRIPTION,
							  "<html>The disease query retrieves a STRING network for the top-N human proteins associated <br />"
							  + "with the queried disease in the DISEASES database. DISEASES is a weekly updated web <br />"
							  + "resource that integrates evidence on disease-gene associations from automatic text <br />"
							  + "mining, manually curated literature, cancer mutation data, and genome-wide association <br />"
							  + "studies. STRING is a database of known and predicted protein interactions for thousands <br />"
							  + "of organisms, which are integrated from several sources, scored, and transferred across <br />"
							  + "orthologs. The network  includes both physical interactions and functional associations.</html>");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);
			registerService(bc, getNetwork, TaskFactory.class, props);
		}
		
		{
			GetNetworkTaskFactory getNetwork = new GetNetworkTaskFactory(manager, "pubmed");
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "pubmed query");
			props.setProperty(COMMAND_DESCRIPTION, 
										    "Create a STRING network by entering a pubmed query");
			props.setProperty(COMMAND_LONG_DESCRIPTION, 
					"<html>The PubMed query retrieves a STRING network pertaining to any topic of interest <br />"
							+ "based on text mining of PubMed abstracts. STRING is a database of known and <br />"
							+ "predicted protein interactions for thousands of organisms, which are integrated <br />"
							+ "from several sources, scored, and transferred across orthologs. The network <br />"
							+ "includes both physical interactions and functional associations.</html>");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);
			registerService(bc, getNetwork, TaskFactory.class, props);
		}
		
		{
			ExpandNetworkTaskFactory expandFactory = new ExpandNetworkTaskFactory(manager);
			Properties expandProps = new Properties();
			expandProps.setProperty(COMMAND_NAMESPACE, "string");
			expandProps.setProperty(COMMAND, "expand");
			expandProps.setProperty(COMMAND_DESCRIPTION, "Expand a STRING network by more interactors");
			expandProps.setProperty(COMMAND_LONG_DESCRIPTION, 
					"Expand an already existing STRING network by more interactors such as STITCH compounds, "
					+ "proteins of the network species as well as proteins interacting with available viruses or host species proteins.");
			expandProps.setProperty(COMMAND_SUPPORTS_JSON, "true");
			expandProps.setProperty(COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);			
			registerService(bc, expandFactory, TaskFactory.class, expandProps);
		}

    {
      GetNetworkTaskFactory getNetwork = new GetNetworkTaskFactory(manager, "compound");
      Properties props = new Properties();
      props.setProperty(COMMAND_NAMESPACE, "string");
      props.setProperty(COMMAND, "compound query");
			props.setProperty(COMMAND_DESCRIPTION, 
										    "Create a STITCH network from multiple protein and compound names/identifiers");
			props.setProperty(COMMAND_LONG_DESCRIPTION, 
					"<html>The compound query retrieves a STITCH network for one or more proteins or compounds. <br />"
							+ "STITCH is a resource to explore known and predicted interactions of chemicals and <br />"
							+ "proteins. Chemicals are linked to other chemicals and proteins by evidence derived <br />"
							+ "from experiments, databases and the literature.</html>");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);
      registerService(bc, getNetwork, TaskFactory.class, props);
    }


		{
			GetNetworkTaskFactory getNetwork = new GetNetworkTaskFactory(manager, "additional");
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "add nodes");
			props.setProperty(COMMAND_DESCRIPTION, "Add query nodes to an existing STRING network");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Add a new set of query nodes to an existing STRING network as well as "
					+ "their interactions with the nodes in the existing network.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props.setProperty(COMMAND_EXAMPLE_JSON, JSON_EXAMPLE);

			registerService(bc, getNetwork, TaskFactory.class, props);
		}

		{
			VersionTaskFactory versionFactory = new VersionTaskFactory(version);
			Properties versionProps = new Properties();
			versionProps.setProperty(COMMAND_NAMESPACE, "string");
			versionProps.setProperty(COMMAND, "version");
			versionProps.setProperty(COMMAND_DESCRIPTION, "Returns the version of StringApp");
			versionProps.setProperty(COMMAND_LONG_DESCRIPTION, "Returns the version of StringApp.");
			versionProps.setProperty(COMMAND_SUPPORTS_JSON, "true");
			versionProps.setProperty(COMMAND_EXAMPLE_JSON, "{\"version\":\"2.1.0\"}");
			registerService(bc, versionFactory, TaskFactory.class, versionProps);
		}

		{
			FilterEnrichmentTableTaskFactory filterFactory = 
							new FilterEnrichmentTableTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "filter enrichment");
			props.setProperty(COMMAND_DESCRIPTION, "Filter the terms in the enrichment table");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Filter the terms in the enrichment table.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
			registerService(bc, filterFactory, TaskFactory.class, props);
		}

		{
			ShowChartsTaskFactory showChartsFactory = 
							new ShowChartsTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "show charts");
			props.setProperty(COMMAND_DESCRIPTION, "Show the enrichment charts");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Show the enrichment charts using the default settings.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
			registerService(bc, showChartsFactory, TaskFactory.class, props);
		}
		
		{
			HideChartsTaskFactory hideChartsFactory = 
							new HideChartsTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "hide charts");
			props.setProperty(COMMAND_DESCRIPTION, "Hide the enrichment charts");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Hide the enrichment charts.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
			registerService(bc, hideChartsFactory, TaskFactory.class, props);
		}
		
		{
			SettingsTaskFactory settingsFactory = 
							new SettingsTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "Settings");
			props.setProperty(MENU_GRAVITY, "100.0");
			props.setProperty(IN_MENU_BAR, "true");
			props.setProperty(INSERT_SEPARATOR_BEFORE, "true");
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "settings");
			props.setProperty(COMMAND_DESCRIPTION, "Adjust various settings");
			props.setProperty(COMMAND_LONG_DESCRIPTION, "Adjust various default settings of "
					+ "the stringApp for network queries, enrichment and visual properties.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, "{}");
			registerService(bc, settingsFactory, TaskFactory.class, props);
		}

		{
			// Register our "Add Nodes" factory
			ExpandNetworkTaskFactory addNodes = new ExpandNetworkTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "Expand network");
			props.setProperty(MENU_GRAVITY, "1.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, addNodes, NetworkTaskFactory.class, props);

			Properties props2 = new Properties();
			props2.setProperty(PREFERRED_MENU, "Apps.STRING");
			props2.setProperty(TITLE, "Expand network");
			props2.setProperty(MENU_GRAVITY, "1.0");
			props2.setProperty(IN_MENU_BAR, "false");
			registerService(bc, addNodes, NetworkViewTaskFactory.class, props2);

			Properties props3 = new Properties();
			props3.setProperty(PREFERRED_MENU, "Apps.STRING");
			props3.setProperty(TITLE, "Expand network");
			props3.setProperty(MENU_GRAVITY, "1.0");
			props3.setProperty(IN_MENU_BAR, "false");
			registerService(bc, addNodes, NodeViewTaskFactory.class, props3);
		}

		{
			ChangeConfidenceTaskFactory changeConfidence = new ChangeConfidenceTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "Change confidence");
			props.setProperty(MENU_GRAVITY, "2.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, changeConfidence, NetworkTaskFactory.class, props);
			
			Properties props2 = new Properties();
			props2.setProperty(COMMAND_NAMESPACE, "string");
			props2.setProperty(COMMAND, "change confidence");
			props2.setProperty(COMMAND_DESCRIPTION, 
			                            "Change confidence of the network");
			props2.setProperty(COMMAND_LONG_DESCRIPTION,
					"Changes the confidence of the network. If increased, some edges will disapear. "
					+ "If decreased, new edges might be added to the network.");
			props2.setProperty(COMMAND_SUPPORTS_JSON, "true");
			props2.setProperty(COMMAND_EXAMPLE_JSON, "{}");
			registerService(bc, changeConfidence, TaskFactory.class, props2);
		}

		{
			AddTermsTaskFactory addTerms = new AddTermsTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "Query for additional nodes");
			props.setProperty(MENU_GRAVITY, "3.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, addTerms, NetworkTaskFactory.class, props);
		}

		{
			SetLabelAttributeTaskFactory setLabel = new SetLabelAttributeTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "Set STRING label attribute");
			props.setProperty(MENU_GRAVITY, "10.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, setLabel, NetworkTaskFactory.class, props);
		}

		{
			SetConfidenceTaskFactory setConfidence = new SetConfidenceTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "Set as STRING network");
			props.setProperty(MENU_GRAVITY, "6.0");
			props.setProperty(IN_MENU_BAR, "true");
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "make string");
			props.setProperty(COMMAND_DESCRIPTION, 
			                            "Set the network as a STRING network");
			props.setProperty(COMMAND_LONG_DESCRIPTION, 
			                            "Sets the network as a STRING network.  This assumes that the network "+
			                            "was originally derived from STRING and has all of the necessary STRING "+
			                            "columns.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, "{\"network\": 123}");
			registerService(bc, setConfidence, NetworkTaskFactory.class, props);
		}

		{
			StringifyTaskFactory stringify = new StringifyTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "STRINGify network");
			props.setProperty(MENU_GRAVITY, "7.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, stringify, NetworkTaskFactory.class, props);

			props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "stringify");
			props.setProperty(COMMAND_DESCRIPTION, 
			                            "Create a new STRING network from the current network");
			props.setProperty(COMMAND_LONG_DESCRIPTION, 
			                            "Creates a new network from the nodes and edges of the specified network,"+
			                            "by querying STRING for all of the nodes and then copying over the edges "+
			                            "from the original network.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, "{\"network\": 123}");
			registerService(bc, stringify, TaskFactory.class, props);
		}

		{
			ExportEnrichmentTaskFactory exportEnrichment = new ExportEnrichmentTaskFactory(manager);
			// Properties props = new Properties();
			// props.setProperty(PREFERRED_MENU, "File.Export");
			// props.setProperty(TITLE, "STRING Enrichment");
			// props.setProperty(MENU_GRAVITY, "4.0");
			// props.setProperty(IN_MENU_BAR, "true");
			// registerService(bc, exportEnrichment, NetworkTaskFactory.class, props);

			Properties props2 = new Properties();
			props2.setProperty(PREFERRED_MENU, "Apps.STRING Enrichment");
			props2.setProperty(TITLE, "Export enrichment results");
			props2.setProperty(MENU_GRAVITY, "3.0");
			props2.setProperty(IN_MENU_BAR, "true");
			registerService(bc, exportEnrichment, NetworkTaskFactory.class, props2);
		}

		{
			ExportPublicationsTaskFactory exportPublications = new ExportPublicationsTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING Enrichment");
			props.setProperty(TITLE, "Export publications results");
			props.setProperty(MENU_GRAVITY, "6.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, exportPublications, NetworkTaskFactory.class, props);
		}

		if (haveGUI) {
			GetEnrichmentTaskFactory getEnrichment = new GetEnrichmentTaskFactory(manager, true);
			{
				Properties propsEnrichment = new Properties();
				propsEnrichment.setProperty(PREFERRED_MENU, "Apps.STRING Enrichment");
				propsEnrichment.setProperty(TITLE, "Retrieve functional enrichment");
				propsEnrichment.setProperty(MENU_GRAVITY, "1.0");
				propsEnrichment.setProperty(IN_MENU_BAR, "true");
				// propsEnrichment.setProperty(INSERT_SEPARATOR_BEFORE, "true");
				registerService(bc, getEnrichment, NetworkTaskFactory.class, propsEnrichment);

				ShowEnrichmentPanelTaskFactory showEnrichment = new ShowEnrichmentPanelTaskFactory(manager);
				showEnrichment.reregister();
				getEnrichment.setShowEnrichmentPanelFactory(showEnrichment);
				manager.setShowEnrichmentPanelTaskFactory(showEnrichment);
			}
			
			GetPublicationsTaskFactory getPublications = new GetPublicationsTaskFactory(manager, true);
			{
				Properties propsPublications = new Properties();
				propsPublications.setProperty(PREFERRED_MENU, "Apps.STRING Enrichment");
				propsPublications.setProperty(TITLE, "Retrieve enriched publications");
				propsPublications.setProperty(MENU_GRAVITY, "4.0");
				propsPublications.setProperty(IN_MENU_BAR, "true");
				propsPublications.setProperty(INSERT_SEPARATOR_BEFORE, "true");
				registerService(bc, getPublications, NetworkTaskFactory.class, propsPublications);

				ShowPublicationsPanelTaskFactory showPublications = new ShowPublicationsPanelTaskFactory(manager);
				showPublications.reregister();
				getPublications.setShowPublicationsPanelFactory(showPublications);
				manager.setShowPublicationsPanelTaskFactory(showPublications);
			}

			{
				ShowResultsPanelTaskFactory showResults = new ShowResultsPanelTaskFactory(manager);
				showResults.reregister();
				manager.setShowResultsPanelTaskFactory(showResults);

				// Now bring up the side panel if the current network is a STRING network
				CyNetwork current = manager.getCurrentNetwork();
				if (ModelUtils.ifHaveStringNS(current)) {
					// It's the current network.  Bring up the results panel
					manager.execute(showResults.createTaskIterator(), true);
				}
			}	
		}
		
		GetEnrichmentTaskFactory getCommandEnrichment = new GetEnrichmentTaskFactory(manager, false);
		{
			Properties propsEnrichment = new Properties();
			propsEnrichment.setProperty(COMMAND_NAMESPACE, "string");
			propsEnrichment.setProperty(COMMAND, "retrieve enrichment");
			propsEnrichment.setProperty(COMMAND_DESCRIPTION, 
			                            "Retrieve functional enrichment for the current String network");
			propsEnrichment.setProperty(COMMAND_LONG_DESCRIPTION, 
			                            "Retrieve the functional enrichment for the current String network."+
			                            "This includes enrichment for GO Process, GO Component, GO Function, "+
			                            "InterPro, KEGG Pathways, and PFAM.");
			propsEnrichment.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	propsEnrichment.setProperty(COMMAND_EXAMPLE_JSON, GetEnrichmentTaskFactory.EXAMPLE_JSON);
			// propsEnrichment.setProperty(INSERT_SEPARATOR_BEFORE, "true");
			registerService(bc, getCommandEnrichment, NetworkTaskFactory.class, propsEnrichment);
		}

		GetPublicationsTaskFactory getCommandPublications = new GetPublicationsTaskFactory(manager, false);
		{
			Properties propsPubl = new Properties();
			propsPubl.setProperty(COMMAND_NAMESPACE, "string");
			propsPubl.setProperty(COMMAND, "retrieve publications");
			propsPubl.setProperty(COMMAND_DESCRIPTION, 
			                            "Retrieve enriched publications for the current String network");
			propsPubl.setProperty(COMMAND_LONG_DESCRIPTION, 
			                            "Retrieve the enriched PubMed publications for the current String network.");
			propsPubl.setProperty(COMMAND_SUPPORTS_JSON, "true");
			propsPubl.setProperty(COMMAND_EXAMPLE_JSON, GetPublicationsTaskFactory.EXAMPLE_JSON);
			registerService(bc, getCommandPublications, NetworkTaskFactory.class, propsPubl);
		}

		GetSpeciesTaskFactory getSpecies = new GetSpeciesTaskFactory(manager);
		{
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "list species");
			props.setProperty(COMMAND_DESCRIPTION, 
			                            "Retrieve a list of the species available in STRING.");
			props.setProperty(COMMAND_LONG_DESCRIPTION, 
			                            "Retrieve the list of species known to the STRING database, including the taxonomy ID.");
			props.setProperty(COMMAND_SUPPORTS_JSON, "true");
    	props.setProperty(COMMAND_EXAMPLE_JSON, "[{\"taxonomyId\": 9606, \"scientificName\": \"Homo sapiens\", \"abbreviatedName\":\"Homo sapiens\"}]");
			// propsEnrichment.setProperty(INSERT_SEPARATOR_BEFORE, "true");
			registerService(bc, getSpecies, TaskFactory.class, props);
		}

		/*
		{
			OpenEvidenceTaskFactory openEvidence = new OpenEvidenceTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.String");
			props.setProperty(TITLE, "Show evidence for association (if available)");
			props.setProperty(MENU_GRAVITY, "2.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, openEvidence, NodeViewTaskFactory.class, props);
		}
		*/
		
		/*
		{
			FindProteinsTaskFactory findProteins = new FindProteinsTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.String");
			props.setProperty(TITLE, "Find proteins using text mining");
			props.setProperty(MENU_GRAVITY, "4.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, findProteins, TaskFactory.class, props);
		}
		*/

		{
			// Register our "show image" toggle
			ShowImagesTaskFactory showImagesTF = new ShowImagesTaskFactory(manager);
			showImagesTF.reregister();
			manager.setShowImagesTaskFactory(showImagesTF);
		}

		{
			// Register our "show enhanced labels" toggle
			ShowEnhancedLabelsTaskFactory showEnhancedLabelsTF = new ShowEnhancedLabelsTaskFactory(manager);
			showEnhancedLabelsTF.reregister();
			manager.setShowEnhancedLabelsTaskFactory(showEnhancedLabelsTF);
		}
		
		{
			// Register our "show glass ball effect" toggle
			ShowGlassBallEffectTaskFactory showGlassBallEffectTF = new ShowGlassBallEffectTaskFactory(manager);
			showGlassBallEffectTF.reregister();
			manager.setShowGlassBallEffectTaskFactory(showGlassBallEffectTF);
		}

		{
			// Register our custom graphics
			CyCustomGraphicsFactory<StringLayer> stringLookFactory = new StringCustomGraphicsFactory(manager);
			Properties stringProps = new Properties();
			registerService(bc, stringLookFactory, CyCustomGraphicsFactory.class, stringProps);
		}

		    // Register our Network search factories
    {
      StringSearchTaskFactory stringSearch = new StringSearchTaskFactory(manager);
      Properties propsSearch = new Properties();
      registerService(bc, stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
    {
      StitchSearchTaskFactory stringSearch = new StitchSearchTaskFactory(manager);
      Properties propsSearch = new Properties();
      registerService(bc, stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
    {
      PubmedSearchTaskFactory stringSearch = new PubmedSearchTaskFactory(manager);
      Properties propsSearch = new Properties();
      registerService(bc, stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
    {
      DiseaseSearchTaskFactory stringSearch = new DiseaseSearchTaskFactory(manager);
      Properties propsSearch = new Properties();
      registerService(bc, stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }

		manager.info("stringApp " + version + " initialized.");
		System.out.println("stringApp " + version + " initialized.");
	}

}
