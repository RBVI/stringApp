package edu.ucsf.rbvi.stringApp.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.tasks.AddNodesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ChangeConfidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.AddTermsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.ui.StringWebServiceClient;
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
		
		{
			// Register our web service client
			StringWebServiceClient client = new StringWebServiceClient(manager);
			registerAllServices(bc, client, new Properties());
		}

		{
			// Register our "Add Nodes" factory
			AddNodesTaskFactory addNodes = new AddNodesTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.String");
			props.setProperty(TITLE, "Expand network");
			props.setProperty(MENU_GRAVITY, "1.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, addNodes, NetworkTaskFactory.class, props);

			Properties props2 = new Properties();
			props2.setProperty(PREFERRED_MENU, "Apps.String");
			props2.setProperty(TITLE, "Expand network");
			props2.setProperty(MENU_GRAVITY, "1.0");
			props2.setProperty(IN_MENU_BAR, "false");
			registerService(bc, addNodes, NetworkViewTaskFactory.class, props2);
		}

		{
			ChangeConfidenceTaskFactory changeConfidence = new ChangeConfidenceTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.String");
			props.setProperty(TITLE, "Change confidence");
			props.setProperty(MENU_GRAVITY, "2.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, changeConfidence, NetworkTaskFactory.class, props);
		}

		{
			AddTermsTaskFactory addTerms = new AddTermsTaskFactory(manager);
			Properties props = new Properties();
			props.setProperty(PREFERRED_MENU, "Apps.String");
			props.setProperty(TITLE, "Add Terms to Network");
			props.setProperty(MENU_GRAVITY, "3.0");
			props.setProperty(IN_MENU_BAR, "true");
			registerService(bc, addTerms, NetworkTaskFactory.class, props);
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
