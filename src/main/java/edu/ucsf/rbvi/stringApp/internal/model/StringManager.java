package edu.ucsf.rbvi.stringApp.internal.model;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.apache.log4j.Logger;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringManager implements NetworkAddedListener {
	final CyServiceRegistrar registrar;
	final CyEventHelper cyEventHelper;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	final TaskManager<?,?> dialogTaskManager;
	final SynchronousTaskManager<?> synchronousTaskManager;
	private boolean showImage = true;
	private boolean ignore = false;
	private Map<CyNetwork, StringNetwork> stringNetworkMap;

	public static String ResolveURI = "http://string-db.org/api/";
	public static String URI = "http://api.jensenlab.org/";
	public static String CallerIdentity = "string_app";

	public StringManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;
		// Get our task managers
		dialogTaskManager = registrar.getService(TaskManager.class);
		synchronousTaskManager = registrar.getService(SynchronousTaskManager.class);
		cyEventHelper = registrar.getService(CyEventHelper.class);
		stringNetworkMap = new HashMap<>();
	}

	public String getNetworkName(String ids) {
		String name = "String Network";

		// If we have a single query term, use that as the network name
		if (ids != null) {
			if (ids.split("\n").length == 1) {
				return name+" - "+ids;
			}
		}

		// Use simple name, but make sure we're unique
		Set<CyNetwork> allNetworks = registrar.getService(CyNetworkManager.class).getNetworkSet();
		if (allNetworks == null || allNetworks.size() == 0)
			return name;

		int max = 0;
		for (CyNetwork network: allNetworks) {
			String netName = getNetworkName(network);
			if (netName.startsWith("String Network")) {
				String [] parts = netName.split("_");
				if (parts.length == 1)
					max = 1;
				else {
					max = Integer.parseInt(parts[1].trim());
				}
			}
		}

		if (max > 0)
			return name+"_"+max;

		return name;
	}

	public CyNetwork createNetwork(String name) {
		CyNetwork network = registrar.getService(CyNetworkFactory.class).createNetwork();
		network.getRow(network).set(CyNetwork.NAME, name);
		return network;
	}

	public CyNetwork createStringNetwork(String name, StringNetwork stringNet) {
		CyNetwork network = createNetwork(name);
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
		CyNetworkView view = registrar.getService(CyNetworkViewFactory.class).createNetworkView(network);
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
	public void setShowImage(boolean set) { showImage = set; }

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

	public String getResolveURL() {
		return ResolveURI;
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

	public <T> T getService(Class<? extends T> clazz) {
		return registrar.getService(clazz);
	}

	public <T> T getService(Class<? extends T> clazz, String filter) {
		return registrar.getService(clazz, filter);
	}

	public void registerService(Object service, Class<?> clazz, Properties props) {
		registrar.registerService(service, clazz, props);
	}

	public void unregisterService(Object service, Class<?> clazz) {
		registrar.unregisterService(service, clazz);
	}

	public void setVersion(String version) {
		String v = version.replace('.', '_');
		StringManager.CallerIdentity = "string_app_v"+v;
	}

}
