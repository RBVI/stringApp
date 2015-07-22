package edu.ucsf.rbvi.stringApp.internal.model;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
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

public class StringManager {
	final CyServiceRegistrar registrar;
	final CyEventHelper cyEventHelper;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	final TaskManager dialogTaskManager;
	final SynchronousTaskManager synchronousTaskManager;
	public static String URI = "http://string-db.org/api/";

	public StringManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;
		// Get our task managers
		dialogTaskManager = registrar.getService(TaskManager.class);
		synchronousTaskManager = registrar.getService(SynchronousTaskManager.class);
		cyEventHelper = registrar.getService(CyEventHelper.class);
	}

	public Map<String, List<Annotation>> getAnnotations(int taxon, final String terms) {
		String encTerms;
		try {
			encTerms = URLEncoder.encode(terms.trim(), "UTF-8");
		} catch (Exception e) {
			return new HashMap<String, List<Annotation>>();
		}

		String url = getURL()+"json/resolveList";
		Map<String, String> args = new HashMap<>();
		args.put("species", Integer.toString(taxon));
		args.put("identifiers", encTerms);
		System.out.println("URL: "+url);
		// Get the results
		Object results = HttpUtils.postJSON(url, args, this);
		return Annotation.getAnnotations(results, terms);
	}

	public CyNetwork createNetwork(String name) {
		CyNetwork network = registrar.getService(CyNetworkFactory.class).createNetwork();
		network.getRow(network).set(CyNetwork.NAME, name);
		return network;
	}

	public CyNetworkView createNetworkView(CyNetwork network) {
		return registrar.getService(CyNetworkViewFactory.class).createNetworkView(network);
	}

	public void addNetwork(CyNetwork network) {
		registrar.getService(CyNetworkManager.class).addNetwork(network);
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

	public String getURL() {
		return URI;
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

}
