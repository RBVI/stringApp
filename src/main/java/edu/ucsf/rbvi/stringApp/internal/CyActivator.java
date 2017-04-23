package edu.ucsf.rbvi.stringApp.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;

import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.tasks.AddTermsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ChangeConfidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ExpandNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetEnrichmentTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetNetworkTaskFactory;
// import edu.ucsf.rbvi.stringApp.internal.tasks.FindProteinsTaskFactory;
// import edu.ucsf.rbvi.stringApp.internal.tasks.OpenEvidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.SetConfidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnhancedLabelsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowImagesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowResultsPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.VersionTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.ui.DiseaseNetworkWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.StitchWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.StringWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.TextMiningWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.view.StringCustomGraphicsFactory;
import edu.ucsf.rbvi.stringApp.internal.view.StringLayer;

// TODO: [Optional] Improve non-gui mode
public class CyActivator extends AbstractCyActivator {
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
		String version = bc.getBundle().getVersion().toString();
		manager.setVersion(version);

		{
			// Register our network added listener
			registerService(bc, manager, NetworkAddedListener.class, new Properties());
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
			registerService(bc, getNetwork, TaskFactory.class, props);
		}

		{
			GetNetworkTaskFactory getNetwork = new GetNetworkTaskFactory(manager, "disease");
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "disease query");
			registerService(bc, getNetwork, TaskFactory.class, props);
		}
		
		{
			GetNetworkTaskFactory getNetwork = new GetNetworkTaskFactory(manager, "pubmed");
			Properties props = new Properties();
			props.setProperty(COMMAND_NAMESPACE, "string");
			props.setProperty(COMMAND, "pubmed query");
			registerService(bc, getNetwork, TaskFactory.class, props);
		}

		{
			VersionTaskFactory versionFactory = new VersionTaskFactory(version);
			Properties versionProps = new Properties();
			versionProps.setProperty(COMMAND_NAMESPACE, "string");
			versionProps.setProperty(COMMAND, "version");
			registerService(bc, versionFactory, TaskFactory.class, versionProps);
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
			SetConfidenceTaskFactory setConfidence = new SetConfidenceTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.STRING");
			props.setProperty(TITLE, "Set as STRING network");
			props.setProperty(MENU_GRAVITY, "10.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, setConfidence, NetworkTaskFactory.class, props);
		}

		
		GetEnrichmentTaskFactory getEnrichment = new GetEnrichmentTaskFactory(manager);
		Properties propsEnrichment = new Properties();
		propsEnrichment.setProperty(PREFERRED_MENU, "Apps.STRING");
		propsEnrichment.setProperty(TITLE, "Retrieve functional enrichment");
		propsEnrichment.setProperty(MENU_GRAVITY, "4.0");
		propsEnrichment.setProperty(IN_MENU_BAR, "true");
		propsEnrichment.setProperty(INSERT_SEPARATOR_BEFORE, "true");
		registerService(bc, getEnrichment, NetworkTaskFactory.class, propsEnrichment);

		if (haveGUI) {
			ShowEnrichmentPanelTaskFactory showEnrichment = new ShowEnrichmentPanelTaskFactory(manager);
			showEnrichment.reregister();
			getEnrichment.setShowEnrichmentPanelFactory(showEnrichment);

			ShowResultsPanelTaskFactory showResults = new ShowResultsPanelTaskFactory(manager);
			showResults.reregister();

			
			/*
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.String");
			props.setProperty(TITLE, "Show results panel");
			props.setProperty(MENU_GRAVITY, "4.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, showResults, TaskFactory.class, props);
			*/

			/*
			ShowResultsPanelTaskFactory hideResults = new ShowResultsPanelTaskFactory(manager, false);
			Properties hideProps = new Properties();
			hideProps.setProperty(PREFERRED_MENU, "Apps.String");
			hideProps.setProperty(TITLE, "Show results panel");
			hideProps.setProperty(MENU_GRAVITY, "4.0");
			hideProps.setProperty(IN_MENU_BAR, "true");
			registerService(bc, hideResults, TaskFactory.class, hideProps);
			*/
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
		}

		{
			// Register our "show enhanced labels" toggle
			ShowEnhancedLabelsTaskFactory showEnhancedLabelsTF = new ShowEnhancedLabelsTaskFactory(manager);
			showEnhancedLabelsTF.reregister();
		}

		{
			// Register our custom graphics
			CyCustomGraphicsFactory<StringLayer> stringLookFactory = new StringCustomGraphicsFactory(manager);
			Properties stringProps = new Properties();
			registerService(bc, stringLookFactory, CyCustomGraphicsFactory.class, stringProps);
		}

		manager.info("String APP initialized");
	}

}
