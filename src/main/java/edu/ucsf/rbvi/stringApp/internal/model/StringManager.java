package edu.ucsf.rbvi.stringApp.internal.model;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.command.AvailableCommands;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeLoadedEvent;
import org.cytoscape.session.events.SessionAboutToBeLoadedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.apache.log4j.Logger;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnhancedLabelsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowImagesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringManager implements NetworkAddedListener, SessionLoadedListener {
	final CyServiceRegistrar registrar;
	final CyEventHelper cyEventHelper;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	final TaskManager<?,?> dialogTaskManager;
	final SynchronousTaskManager<?> synchronousTaskManager;
	final AvailableCommands availableCommands;
	private ShowImagesTaskFactory imagesTaskFactory;
	private ShowEnhancedLabelsTaskFactory labelsTaskFactory;
	private boolean showImage = true;
	private boolean showEnhancedLabels = true;
	private boolean ignore = false;
	private Boolean haveChemViz = null;
	private Map<CyNetwork, StringNetwork> stringNetworkMap;

	public static String STRINGResolveURI = "http://string-db.org/api/";
	public static String STITCHResolveURI = "http://stitch.embl.de/api/";
	//public static String STITCHResolveURI = "http://beta.stitch-db.org/api/";
	public static String URI = "http://api.jensenlab.org/";
	public static String CallerIdentity = "string_app";
	public static String APIVERSION = "String-api-version";
	public static String RESULT = "QueryResult";

	public StringManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;
		// Get our task managers
		dialogTaskManager = registrar.getService(TaskManager.class);
		synchronousTaskManager = registrar.getService(SynchronousTaskManager.class);
		availableCommands = registrar.getService(AvailableCommands.class);
		cyEventHelper = registrar.getService(CyEventHelper.class);
		stringNetworkMap = new HashMap<>();
		if (haveEnhancedGraphics())
			showEnhancedLabels = true;
		else
			showEnhancedLabels = false;
	}

	public CyNetwork createNetwork(String name) {
		CyNetwork network = registrar.getService(CyNetworkFactory.class).createNetwork();
		CyNetworkManager netMgr = registrar.getService(CyNetworkManager.class);

		// See if this name is already taken
		int index = -1;
		boolean match = false;
		for (CyNetwork net: netMgr.getNetworkSet()) {
			String netName = net.getRow(net).get(CyNetwork.NAME, String.class);
			if (netName.equals(name)) {
				match = true;
			} else if (netName.startsWith(name)) {
				String subname = netName.substring(name.length());
				if (subname.startsWith(" - ")) {
					try {
						int v = Integer.parseInt(subname.substring(3));
						if (v >= index)
							index = v+1;
					} catch (NumberFormatException e) {}
				}
			}
		}
		if (match && index < 0) {
			name = name + " - 1";
		} else if (index > 0) {
			name = name + " - " + index;
		}
		network.getRow(network).set(CyNetwork.NAME, name);

		return network;
	}

	public CyNetwork createStringNetwork(String name, StringNetwork stringNet, String useDATABASE, String species) {
		CyNetwork network = createNetwork(name);
		ModelUtils.setDatabase(network, useDATABASE);
		ModelUtils.setNetSpecies(network, species);
		addStringNetwork(stringNet, network);
		return network;
	}

	public void addStringNetwork(StringNetwork stringNet, CyNetwork network) {
		stringNetworkMap.put(network, stringNet);
		stringNet.setNetwork(network);
	}

	public StringNetwork getStringNetwork(CyNetwork network) {
		if (stringNetworkMap.containsKey(network))
			return stringNetworkMap.get(network);
		return null;
	}

	public String getNetworkName(CyNetwork net) {
		return net.getRow(net).get(CyNetwork.NAME, String.class);
	}

	public CyNetworkView createNetworkView(CyNetwork network) {
		CyNetworkView view = registrar.getService(CyNetworkViewFactory.class)
		                                          .createNetworkView(network);
		return view;
	}

	public void addNetwork(CyNetwork network) {
		registrar.getService(CyNetworkManager.class).addNetwork(network);
		registrar.getService(CyApplicationManager.class).setCurrentNetwork(network);
	}
	
	public CyNetwork getCurrentNetwork() {
		return registrar.getService(CyApplicationManager.class).getCurrentNetwork();
	}

	public boolean showImage() { return showImage; }

	public void setShowImage(boolean set) { 
		showImage = set; 
		ModelUtils.setStringProperty(this, ModelUtils.showStructureImagesFlag, new Boolean(set));
	}

	public boolean showEnhancedLabels() { return showEnhancedLabels; }
	
	public void setShowEnhancedLabels(boolean set) { 
		showEnhancedLabels = set; 
		ModelUtils.setStringProperty(this, ModelUtils.showEnhancedLabelsFlag, new Boolean(set));
	}

	public void flushEvents() {
		cyEventHelper.flushPayloadEvents();
	}

	public void execute(TaskIterator iterator) {
		execute(iterator, false);
	}

	public void execute(TaskIterator iterator, TaskObserver observer) {
		execute(iterator, observer, false);
	}

	public void execute(TaskIterator iterator, boolean synchronous) {
		if (synchronous) {
			synchronousTaskManager.execute(iterator);
		} else {
			dialogTaskManager.execute(iterator);
		}
	}

	public void execute(TaskIterator iterator, TaskObserver observer, boolean synchronous) {
		if (synchronous) {
			synchronousTaskManager.execute(iterator, observer);
		} else {
			dialogTaskManager.execute(iterator, observer);
		}
	}

	public String getNetworkURL() {
		return URI+"network";
	}

	public String getTextMiningURL() {
		return URI+"Textmining";
	}

	public String getEntityQueryURL() {
		return URI+"EntityQuery";
	}

	public String getIntegrationURL() {
		return URI+"Integration";
	}

	public String getResolveURL(String useDATABASE) {
		if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			return STITCHResolveURI;

		return STRINGResolveURI;
	}

	public void info(String info) {
		logger.info(info);
	}

	public void error(String error) {
		logger.error(error);
	}

	public void warn(String warn) {
		logger.warn(warn);
	}

	public void ignoreAdd() {
		ignore = true;
	}

	public void listenToAdd() {
		ignore = false;
	}

	public void handleEvent(NetworkAddedEvent nae) {
		CyNetwork network = nae.getNetwork();
		if (ignore) return;

		// This is a string network only if we have a confidence score in the network table,
		// "@id", "species", "canonical name", and "sequence" columns in the node table, and 
		// a "score" column in the edge table
		if (ModelUtils.isStringNetwork(network)) {
			StringNetwork stringNet = new StringNetwork(this);
			stringNet.setNetwork(network);
			addStringNetwork(stringNet, network);
		}
	}

	public void handleEvent(SessionLoadedEvent arg0) {
		Boolean sessionValueLabels = ModelUtils.getStringProperty(this,
				ModelUtils.showEnhancedLabelsFlag);
		// System.out.println("show labels: " + sessionValueLabels);
		if (sessionValueLabels != null) {
			showEnhancedLabels = sessionValueLabels;
		} else {
			ModelUtils.setStringProperty(this, ModelUtils.showEnhancedLabelsFlag,
					new Boolean(showEnhancedLabels));
		}
		labelsTaskFactory.reregister();
		
		Boolean sessionValueImage = ModelUtils.getStringProperty(this, ModelUtils.showStructureImagesFlag);
		// System.out.println("show image: " + sessionValueImage);
		if (sessionValueImage != null) {
			showImage = sessionValueImage;
		} else {
			ModelUtils.setStringProperty(this, ModelUtils.showStructureImagesFlag,
					new Boolean(showImage));
		}
		imagesTaskFactory.reregister();
	}

	public void setShowImagesTaskFactory(ShowImagesTaskFactory factory) {
		imagesTaskFactory = factory;		
	}
	
	public void setShowEnhancedLabelsTaskFactory(ShowEnhancedLabelsTaskFactory factory) {
		labelsTaskFactory = factory;		
	}

	public <T> T getService(Class<? extends T> clazz) {
		return registrar.getService(clazz);
	}

	public <T> T getService(Class<? extends T> clazz, String filter) {
		return registrar.getService(clazz, filter);
	}

	public void registerService(Object service, Class<?> clazz, Properties props) {
		registrar.registerService(service, clazz, props);
	}

	public void registerAllServices(CyProperty<Properties> service, Properties props) {
		registrar.registerAllServices(service, props);
	}

	public void unregisterService(Object service, Class<?> clazz) {
		registrar.unregisterService(service, clazz);
	}

	public void setVersion(String version) {
		String v = version.replace('.', '_');
		StringManager.CallerIdentity = "string_app_v"+v;
	}

	public boolean haveEnhancedGraphics() {
		return availableCommands.getNamespaces().contains("enhancedGraphics");
	}

	public boolean haveChemViz() {
		if (haveChemViz == null)
			haveChemViz = availableCommands.getNamespaces().contains("chemviz");
		return haveChemViz;
	}

	
}
