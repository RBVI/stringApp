package edu.ucsf.rbvi.stringApp.internal.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.command.AvailableCommands;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.CyProperty.SavePolicy;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.jcolorbrewer.ColorBrewer;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.tasks.AddNamespacesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnhancedLabelsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowGlassBallEffectTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowImagesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowPublicationsPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowResultsPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.ui.StringCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringManager implements NetworkAddedListener, SessionLoadedListener, NetworkAboutToBeDestroyedListener {
	final CyServiceRegistrar registrar;
	final CyEventHelper cyEventHelper;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	final TaskManager<?,?> dialogTaskManager;
	final SynchronousTaskManager<?> synchronousTaskManager;
	final CommandExecutorTaskFactory commandExecutorTaskFactory;
	final AvailableCommands availableCommands;

	private ShowImagesTaskFactory imagesTaskFactory;
	private ShowEnhancedLabelsTaskFactory labelsTaskFactory;
	private ShowEnrichmentPanelTaskFactory enrichmentTaskFactory;
	private ShowPublicationsPanelTaskFactory publicationsTaskFactory;
	private ShowGlassBallEffectTaskFactory glassBallTaskFactory;
	private ShowResultsPanelTaskFactory resultsPanelTaskFactory;

	private Boolean haveChemViz = null;
	private Boolean haveCyBrowser = null;
	private boolean haveURIs = false;

	private Map<CyNetwork, StringNetwork> stringNetworkMap;

	private StringCytoPanel cytoPanel = null;

	public static String CONFIGURI = "https://jensenlab.org/assets/stringapp/";
	
	public static String STRINGResolveURI = "http://version11.string-db.org/api/";
	public static String STITCHResolveURI = "http://stitch.embl.de/api/";
	public static String VIRUSESResolveURI = "http://viruses.string-db.org/cgi/webservice_handler.pl";
	//public static String STITCHResolveURI = "http://beta.stitch-db.org/api/";
	public static String URI = "https://api11.jensenlab.org/";
	public static String DATAVERSION = "11";
	public static String OLD_DATAVERSION = "10";
	public static String alternativeAPIProperty = "alternativeAPI";
	public static String alternativeCONFIGURIProperty = "alternativeCONFIGURI";
	public static String alternativeCONFIGURI = "";
	public static String CallerIdentity = "string_app";
	public static String APIVERSION = "String-api-version";
	public static String RESULT = "QueryResult";
	
	public static String STRINGDevelopmentURI = "http://string-gamma.org/api/";
	
	public static boolean enableViruses = true;
	public static boolean useSTRINGDevelopmentVersion = false; 

	// These are various default values that are saved and restored from
	// the network table
	// TODO: move all of these to StringNetwork?

	// Settings default values.  Network specific values are stored in StringNetwork
	private boolean showImage = true;
	private boolean showEnhancedLabels = true;
	private boolean showGlassBallEffect = true;
	private boolean showStringColors = true;
	private boolean showSingletons = true;
	private Species species;
	private double defaultConfidence = 0.40;
	private int additionalProteins = 0;
	private int maximumProteins = 100;

	private int topTerms = 5;
	private double overlapCutoff = 0.5;
	private ColorBrewer brewerPalette = ColorBrewer.Paired;
	private List<TermCategory> categoryFilter = TermCategory.getValues();
	private ChartType chartType = ChartType.SPLIT;
	private boolean removeOverlap = false;
	private Map<String, Color> channelColors;

	public static String ShowStructureImages = "showStructureImages";
	public static String ShowEnhancedLabels = "showEnhancedLabels";
	public static String ShowGlassBallEffect = "showGlassBallEffect";
	public static String ShowStringColors = "showStringColors";
	public static String ShowSingletons = "showSingletons";

	public static String[] channels = { "databases", "experiments", "neighborhood", "fusion",
	                                    "cooccurrence", "textmining", // Lime green 
																			"coexpression"
	};


	private CyProperty<Properties> sessionProperties;
	private CyProperty<Properties> configProps;

	private boolean ignore = false;

	public StringManager(CyServiceRegistrar registrar) {
		this.registrar = registrar;
		// Get our task managers
		dialogTaskManager = registrar.getService(TaskManager.class);
		synchronousTaskManager = registrar.getService(SynchronousTaskManager.class);
		availableCommands = registrar.getService(AvailableCommands.class);
		commandExecutorTaskFactory = registrar.getService(CommandExecutorTaskFactory.class);
		cyEventHelper = registrar.getService(CyEventHelper.class);
		stringNetworkMap = new HashMap<>();
		if (!haveEnhancedGraphics())
			showEnhancedLabels = false;

		// Make sure we've read in our species
		if (Species.getSpecies() == null) {
			try {
				Species.readSpecies(this);
			} catch (Exception e) {
				throw new RuntimeException("Can't read species information");
			}
		}

		channelColors = new HashMap<>();
		// Set up our default channel colors
		channelColors.put("databases",Color.CYAN);
		channelColors.put("experiments",Color.MAGENTA);
		channelColors.put("neighborhood",Color.GREEN);
		channelColors.put("fusion",Color.RED);
		channelColors.put("cooccurrence",Color.BLUE);
		channelColors.put("textmining",new Color(199,234,70)); // Lime green
		channelColors.put("coexpression", Color.BLACK);

		// Get our default settings
		configProps = ModelUtils.getPropertyService(this, SavePolicy.CONFIG_DIR);

		// check for an alternative config URI
		if (ModelUtils.hasProperty(configProps, alternativeCONFIGURIProperty)) {
			alternativeCONFIGURI = (String) ModelUtils.getStringProperty(configProps,
					alternativeCONFIGURIProperty);
		} else {
			ModelUtils.setStringProperty(configProps, alternativeCONFIGURIProperty, alternativeCONFIGURI);
		}

		// set all stringApp default proerties
		if (ModelUtils.hasProperty(configProps, ShowStructureImages)) {
			setShowImage(ModelUtils.getBooleanProperty(configProps,ShowStructureImages));
		}
		if (ModelUtils.hasProperty(configProps, ShowEnhancedLabels)) {
			setShowEnhancedLabels(ModelUtils.getBooleanProperty(configProps,ShowEnhancedLabels));
		}
		if (ModelUtils.hasProperty(configProps, ShowGlassBallEffect)) {
			setShowGlassBallEffect(ModelUtils.getBooleanProperty(configProps,ShowGlassBallEffect));
		}
		if (ModelUtils.hasProperty(configProps, ShowSingletons)) {
			setShowSingletons(ModelUtils.getBooleanProperty(configProps,ShowSingletons));
		}


		if (ModelUtils.hasProperty(configProps, "species")) {
			setDefaultSpecies(ModelUtils.getStringProperty(configProps,"species"));
		}
		if (ModelUtils.hasProperty(configProps, "defaultConfidence")) {
			setDefaultConfidence(ModelUtils.getDoubleProperty(configProps,"defaultConfidence"));
		}
		if (ModelUtils.hasProperty(configProps, "additionalProteins")) {
			setDefaultAdditionalProteins(ModelUtils.getIntegerProperty(configProps,"additionalProteins"));
		}
		if (ModelUtils.hasProperty(configProps, "maxProteins")) {
			setDefaultMaxProteins(ModelUtils.getIntegerProperty(configProps,"maxProteins"));
		}

		if (ModelUtils.hasProperty(configProps, "overlapCutoff")) {
			setOverlapCutoff(null, ModelUtils.getDoubleProperty(configProps,"overlapCutoff"));
		}
		if (ModelUtils.hasProperty(configProps, "topTerms")) {
			setTopTerms(null, ModelUtils.getIntegerProperty(configProps,"topTerms"));
		}
		if (ModelUtils.hasProperty(configProps, "chartType")) {
			setChartType(null, ModelUtils.getStringProperty(configProps,"chartType"));
		}
		if (ModelUtils.hasProperty(configProps, "brewerPalette")) {
			setBrewerPalette(null, ModelUtils.getStringProperty(configProps,"brewerPalette"));
		}
		if (ModelUtils.hasProperty(configProps, "categoryFilter")) {
			setCategoryFilter(null, ModelUtils.getStringProperty(configProps,"categoryFilter"));
		}
		if (ModelUtils.hasProperty(configProps, "removeOverlap")) {
			setRemoveOverlap(null, ModelUtils.getBooleanProperty(configProps,"removeOverlap"));
		}
		if (ModelUtils.hasProperty(configProps, "channelColors")) {
			setChannelColors(ModelUtils.getStringProperty(configProps,"channelColors"));
		}

		// If we already have networks loaded, see if they are string networks
		for (CyNetwork network: registrar.getService(CyNetworkManager.class).getNetworkSet()) {
			if (ModelUtils.isStringNetwork(network)) {
				StringNetwork stringNet = new StringNetwork(this);
				addStringNetwork(stringNet, network);
			}
		}

		// Get a session property file for the current session
		sessionProperties = ModelUtils.getPropertyService(this, SavePolicy.SESSION_FILE);

	}

	public void updateURIsFromConfig() {
		// Update urls with those from the sever
		Map<String, String> args = new HashMap<>();
		String url = CONFIGURI +CallerIdentity+ ".json";
		StringManager manager = this;

		// Run this in the background in case we have a timeout
		Executors.newCachedThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				JSONObject uris = null;
				// use alternative config URI if available and otherwise retrieve the default one
				// based on the app version
				if (alternativeCONFIGURI != null && alternativeCONFIGURI.length() > 0) {
					uris = ModelUtils.getResultsFromJSON(
							HttpUtils.getJSON(alternativeCONFIGURI, args, manager),
							JSONObject.class);
				} else {
					uris = ModelUtils.getResultsFromJSON(HttpUtils.getJSON(url, args, manager),
							JSONObject.class);
				}
				if (uris != null) {
					if (uris.containsKey("URI")) {
						URI = uris.get("URI").toString();
					}
					if (uris.containsKey("STRINGResolveURI")) {
						STRINGResolveURI = uris.get("STRINGResolveURI").toString();
					} 
					if (uris.containsKey("STITCHResolveURI")) {
						STITCHResolveURI = uris.get("STITCHResolveURI").toString();
					} 
					if (uris.containsKey("VIRUSESResolveURI")) {
						VIRUSESResolveURI = uris.get("VIRUSESResolveURI").toString();
					}
					if (uris.containsKey("DataVersion")) {
						DATAVERSION = uris.get("DataVersion").toString();
					}
					if (uris.containsKey("messageUserError")) {
						error(uris.get("messageUserError").toString());
					}
					if (uris.containsKey("messageUserCriticalError")) {
						critical(uris.get("messageUserCriticalError").toString());
					}
					if (uris.containsKey("messageUserWarning")) {
						warn(uris.get("messageUserWarning").toString());
					}
					if (uris.containsKey("messageUserInfo")) {
						info(uris.get("messageUserInfo").toString());
					}
				}
				haveURIs = true;
   		 }
		});
	}

	public CyNetwork createNetwork(String name) {
		CyNetwork network = registrar.getService(CyNetworkFactory.class).createNetwork();
		CyNetworkManager netMgr = registrar.getService(CyNetworkManager.class);
		
		Set<CyNetwork> nets = netMgr.getNetworkSet();
		Set<CyNetwork> allNets = new HashSet<CyNetwork>(nets);
		for (CyNetwork net : nets) {
			allNets.add(((CySubNetwork)net).getRootNetwork());
		}
		// See if this name is already taken by a network or a network collection (root network)
		int index = -1;
		boolean match = false;
		for (CyNetwork net: allNets) {			
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

	public CyNetwork createStringNetwork(String name, StringNetwork stringNet, 
	                                     String useDATABASE, String species) {
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

	public List<StringNetwork> getStringNetworks() {
		return new ArrayList<>(stringNetworkMap.values());
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

	public CyNetworkView getCurrentNetworkView() {
		return registrar.getService(CyApplicationManager.class).getCurrentNetworkView();
	}

	public boolean showImage() { return showImage; }

	public void setShowImage(boolean set) { 
		showImage = set; 
	}

	public boolean showEnhancedLabels() { return showEnhancedLabels; }
	
	public void setShowEnhancedLabels(boolean set) { 
		showEnhancedLabels = set; 
	}

	public boolean showGlassBallEffect() { return showGlassBallEffect; }
	
	public void setShowGlassBallEffect(boolean set) { 
		showGlassBallEffect = set; 
	}

	public boolean showStringColors() { return showStringColors; }
	
	public void setShowStringColors(boolean set) { 
		showStringColors = set; 
	}
	
	public boolean showSingletons() { return showSingletons; }

	public void setShowSingletons(boolean set) { 
		showSingletons = set; 
	}

	public void setCytoPanel(StringCytoPanel panel) {
		this.cytoPanel = panel;
	}

	public void updateControls() {
		if (cytoPanel != null)
			cytoPanel.updateControls();
	}

	public ShowImagesTaskFactory getImagesTaskFactory() { return imagesTaskFactory; }
	public ShowEnhancedLabelsTaskFactory getEnhancedLabelsTaskFactory() { return labelsTaskFactory; }
	public ShowGlassBallEffectTaskFactory getGlassBallTaskFactory() { return glassBallTaskFactory; }

	public Species getDefaultSpecies() { 
		if (species == null) {
			// Set Human as the default
			for (Species s: Species.getSpecies()) {
				if (s.toString().equals("Homo sapiens")) {
					species = s;
					break;
				}
			}
		}
		return species; 
	}
	public void setDefaultSpecies(Species defaultSpecies) {
		species = defaultSpecies;
	}
	public void setDefaultSpecies(String defaultSpecies) {
		species = Species.getSpecies(defaultSpecies);
	}

	public double getDefaultConfidence() { return defaultConfidence; }
	public void setDefaultConfidence(double conf) { defaultConfidence = conf; }

	public int getDefaultAdditionalProteins() { return additionalProteins; }
	public void setDefaultAdditionalProteins(int ap) { additionalProteins = ap; }

	public int getDefaultMaxProteins() { return maximumProteins; }
	public void setDefaultMaxProteins(int max) { maximumProteins = max; }

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

	public void executeCommand(String namespace, String command, 
	                           Map<String, Object> args, TaskObserver observer) {
		TaskIterator ti = commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
		execute(ti, true);
	}

	public String getDataVersion() {
		return DATAVERSION;
	}
	
	public String getOldDataVersion() {
		return OLD_DATAVERSION;
	}
	
	private String getDataAPIURL() {
		String alternativeAPI = (String) ModelUtils.getStringProperty(configProps,
				alternativeAPIProperty);
		if (alternativeAPI != null && alternativeAPI.length() > 0) return alternativeAPI;
		return URI;
	}
	
	public String getNetworkURL() {
		return getDataAPIURL()+"network";
	}

	public String getTextMiningURL() {
		return getDataAPIURL()+"Textmining";
	}

	public String getEntityQueryURL() {
		return getDataAPIURL()+"EntityQuery";
	}

	public String getIntegrationURL() {
		return getDataAPIURL()+"Integration";
	}

	public String getResolveURL(String useDATABASE) {
		if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			return STITCHResolveURI;
		else if (useDATABASE.equals(Databases.VIRUSES.getAPIName()))
			return VIRUSESResolveURI;
		else if (useSTRINGDevelopmentVersion)
			return STRINGDevelopmentURI;
		
		return STRINGResolveURI;
	}

	public boolean isVirusesEnabled() {
		return enableViruses;
	}
	
	public void info(String info) {
		logger.info(info);
	}

	public void warn(String warn) {
		logger.warn(warn);
	}

	public void error(String error) {
		logger.error(error);
	}

	public void critical(String criticalError) {
		logger.error(criticalError);
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						JOptionPane.showMessageDialog(null, "<html><p style=\"width:200px;\">" + criticalError + "</p></html>", "Critical stringApp error", JOptionPane.ERROR_MESSAGE);
					}
				}
			);
	}

	public void ignoreAdd() {
		ignore = true;
	}

	public void listenToAdd() {
		ignore = false;
	}

	public void updateSettings() {
		ModelUtils.setStringProperty(configProps, "confidence", Double.toString(overlapCutoff));
		ModelUtils.setStringProperty(configProps, "showImage", Boolean.toString(showImage));
		ModelUtils.setStringProperty(configProps, "showEnhancedLabels", Boolean.toString(showEnhancedLabels));
		ModelUtils.setStringProperty(configProps, "showGlassBallEffect", Boolean.toString(showGlassBallEffect));
		ModelUtils.setStringProperty(configProps, "showStringColors", Boolean.toString(showStringColors));

		ModelUtils.setStringProperty(configProps, "species", getDefaultSpecies().toString());
		ModelUtils.setStringProperty(configProps, "defaultConfidence", Double.toString(getDefaultConfidence()));
		ModelUtils.setStringProperty(configProps, "additionalProteins", Integer.toString(getDefaultAdditionalProteins()));
		ModelUtils.setStringProperty(configProps, "maxProteins", Integer.toString(getDefaultMaxProteins()));

		ModelUtils.setStringProperty(configProps, "overlapCutoff", Double.toString(overlapCutoff));
		ModelUtils.setStringProperty(configProps,"topTerms", Integer.toString(topTerms));
		ModelUtils.setStringProperty(configProps,"chartType", chartType.name());
		ModelUtils.setStringProperty(configProps,"brewerPalette", brewerPalette.name());
		ModelUtils.setStringProperty(configProps,"removeOverlap", Boolean.toString(removeOverlap));
		{
			String categories = "";
			for (TermCategory c: categoryFilter) {
				categories += c.name()+",";
			}
			if (categories.length() > 1)
				categories = categories.substring(categories.length()-1);
			ModelUtils.setStringProperty(configProps,"categoryFilter", categories);
		}

		ModelUtils.setStringProperty(configProps, "channelColors", getChannelColorString());
		updateControls();
	}

	public void handleEvent(NetworkAddedEvent nae) {
		CyNetwork network = nae.getNetwork();
		if (ignore) return;

		// This is a string network only if we have a confidence score in the network table,
		// "@id", "species", "canonical name", and "sequence" columns in the node table, and 
		// a "score" column in the edge table
		if (ModelUtils.isStringNetwork(network)) {
			StringNetwork stringNet = new StringNetwork(this);
			addStringNetwork(stringNet, network);
			showResultsPanel();
		}
	}

	public void handleEvent(SessionLoadedEvent arg0) {
		// Get any properties we stored in the session
		sessionProperties = ModelUtils.getPropertyService(this, SavePolicy.SESSION_FILE);

		// Create string networks for any networks loaded by string
		Set<CyNetwork> networks = arg0.getLoadedSession().getNetworks();
		Set<CyNetwork> networksToUpgrade = new HashSet<CyNetwork>();
		for (CyNetwork network: networks) {
			if (ModelUtils.isStringNetwork(network)) {
				if (ModelUtils.ifHaveStringNS(network)) {
					StringNetwork stringNet = new StringNetwork(this);
					addStringNetwork(stringNet, network);
				} else {
					networksToUpgrade.add(network);
				}
			}
		}

		// if there are old string networks, figure out what to do
		SynchronousTaskManager<?> taskM = getService(SynchronousTaskManager.class);
		for (CyNetwork networkToUpgrade : networksToUpgrade) {
			System.out.println("Adding namespaces to old STRING network: " + networkToUpgrade.toString());
			taskM.execute(new AddNamespacesTaskFactory(this).createTaskIterator(networkToUpgrade));
		}

		// load enrichment
		reloadEnrichmentPanel(networks);
		
		// check if enhanced labels should be shown or not
		if (labelsTaskFactory != null) {
			String sessionValueLabels = ModelUtils.getStringProperty(sessionProperties,
					ModelUtils.showEnhancedLabelsFlag);
			// System.out.println("show labels: " + sessionValueLabels);
			if (sessionValueLabels != null) {
				showEnhancedLabels = Boolean.parseBoolean(sessionValueLabels);
			} else {
				ModelUtils.setStringProperty(sessionProperties, ModelUtils.showEnhancedLabelsFlag,
						new Boolean(showEnhancedLabels));
			}
			labelsTaskFactory.reregister();
		}
		
		// check if glass ball effect should be shown or not
		if (glassBallTaskFactory != null) {
			String sessionValueLabels = ModelUtils.getStringProperty(sessionProperties,
					ModelUtils.showGlassBallEffectFlag);
			// System.out.println("show labels: " + sessionValueLabels);
			if (sessionValueLabels != null) {
				showGlassBallEffect = Boolean.parseBoolean(sessionValueLabels);
			} else {
				ModelUtils.setStringProperty(sessionProperties, ModelUtils.showGlassBallEffectFlag,
						new Boolean(showGlassBallEffect));
			}
			glassBallTaskFactory.reregister();
		}

		// check if structure images should be shown or not
		if (imagesTaskFactory != null) {
			String sessionValueImage = ModelUtils.getStringProperty(sessionProperties,
					ModelUtils.showStructureImagesFlag);
			// System.out.println("show image: " + sessionValueImage);
			if (sessionValueImage != null) {
				showImage = Boolean.parseBoolean(sessionValueImage);
			} else {
				ModelUtils.setStringProperty(sessionProperties, ModelUtils.showStructureImagesFlag,
						new Boolean(showImage));
			}
			imagesTaskFactory.reregister();
		}
		if (ModelUtils.ifHaveStringNS(getCurrentNetwork()))
			showResultsPanel();
		else
			hideResultsPanel();
	}

	public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
		CyNetwork network = e.getNetwork();
		ModelUtils.deleteEnrichmentTables(network, this);
		if (stringNetworkMap.containsKey(network))
			stringNetworkMap.remove(network);
		CyNetworkManager netManager = this.getService(CyNetworkManager.class);
		Set<CyNetwork> networks = netManager.getNetworkSet();
		reloadEnrichmentPanel(networks);
	}

	public void showResultsPanel() {
		if (cytoPanel == null) {
			execute(resultsPanelTaskFactory.createTaskIterator(), true);
		} else {
			// Make sure we show it
			cytoPanel.showCytoPanel();
		}
	}

	public void hideResultsPanel() {
		if (cytoPanel != null) {
			cytoPanel.hideCytoPanel();
		}
	}

	private void reloadEnrichmentPanel(Set<CyNetwork> networks) {
		if (enrichmentTaskFactory != null) {
			boolean show = false;
			for (CyNetwork net : networks) {
				if (ModelUtils.getEnrichmentNodes(net).size() > 0) {
					show = true;
					break;
				}
			}
			TaskIterator taskIt = null;
			TaskIterator taskIt2 = null;
			if (show) {
				taskIt = enrichmentTaskFactory.createTaskIterator(true, false);
				taskIt2 = publicationsTaskFactory.createTaskIterator(true, false);
			} else {
				taskIt = enrichmentTaskFactory.createTaskIterator(false, false);
				taskIt2 = publicationsTaskFactory.createTaskIterator(false, false);
			}
			SynchronousTaskManager<?> taskM = getService(SynchronousTaskManager.class);
			taskM.execute(taskIt);
			enrichmentTaskFactory.reregister();
			taskM.execute(taskIt2);
			publicationsTaskFactory.reregister();
		}
	}
	
	public void setShowImagesTaskFactory(ShowImagesTaskFactory factory) {
		imagesTaskFactory = factory;		
	}
	
	public ShowImagesTaskFactory getShowImagesTaskFactory() {
		return imagesTaskFactory;		
	}
	
	public void setShowEnhancedLabelsTaskFactory(ShowEnhancedLabelsTaskFactory factory) {
		labelsTaskFactory = factory;		
	}

	public ShowEnhancedLabelsTaskFactory getShowEnhancedLabelsTaskFactory() {
		return labelsTaskFactory;		
	}

	public void setShowGlassBallEffectTaskFactory(ShowGlassBallEffectTaskFactory factory) {
		glassBallTaskFactory = factory;		
	}

	public ShowGlassBallEffectTaskFactory getShowGlassBallEffectTaskFactory() {
		return glassBallTaskFactory;
	}
	
	public void setShowEnrichmentPanelTaskFactory(ShowEnrichmentPanelTaskFactory factory) {
		enrichmentTaskFactory = factory;		
	}

	public ShowEnrichmentPanelTaskFactory getShowEnrichmentPanelTaskFactory() {
		return enrichmentTaskFactory;		
	}
	
	public void setShowPublicationsPanelTaskFactory(ShowPublicationsPanelTaskFactory factory) {
		publicationsTaskFactory = factory;		
	}

	public ShowPublicationsPanelTaskFactory getShowPublicationsPanelTaskFactory() {
		return publicationsTaskFactory;		
	}
	
	public void setShowResultsPanelTaskFactory(ShowResultsPanelTaskFactory factory) {
		resultsPanelTaskFactory = factory;		
	}

	public ShowResultsPanelTaskFactory getShowResultsPanelTaskFactory() {
		return resultsPanelTaskFactory;		
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

	public boolean haveURIs() {
		return haveURIs;
	}

	public boolean haveChemViz() {
		if (haveChemViz == null)
			haveChemViz = availableCommands.getNamespaces().contains("chemviz");
		return haveChemViz;
	}

	public boolean haveCyBrowser() {
		if (haveCyBrowser == null)
			haveCyBrowser = availableCommands.getNamespaces().contains("cybrowser");
		return haveCyBrowser;
	}

	// Getters and Setters for defaults
	public double getOverlapCutoff(CyNetwork network) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return overlapCutoff; 
		return stringNetworkMap.get(network).getOverlapCutoff();
	}
	public void setOverlapCutoff(CyNetwork network, double cutoff) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			overlapCutoff = cutoff; 
			return;
		}
		stringNetworkMap.get(network).setOverlapCutoff(cutoff);
	}

	public int getTopTerms(CyNetwork network) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return topTerms; 
		return stringNetworkMap.get(network).getTopTerms();
	}
	public void setTopTerms(CyNetwork network, int topN) { 
		if (network == null || !stringNetworkMap.containsKey(network)) 
			topTerms = topN; 
		else
			stringNetworkMap.get(network).setTopTerms(topN);
	}

	public List<TermCategory> getCategoryFilter(CyNetwork network) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return categoryFilter; 
		return stringNetworkMap.get(network).getCategoryFilter();
	}
	public void setCategoryFilter(CyNetwork network, List<TermCategory> categories) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			categoryFilter = categories; 
		} else
			stringNetworkMap.get(network).setCategoryFilter(categories);
	}
	public void setCategoryFilter(CyNetwork network, String categories) { 
		List<TermCategory> catList = new ArrayList<>();
		if (categories == null) return;
		String[] catArray = categories.split(",");
		for (String c: catArray) {
			try {
				catList.add(Enum.valueOf(TermCategory.class, c));
			} catch (Exception e) {
			}
		}
		setCategoryFilter(network, catList);
	}

	public ColorBrewer getBrewerPalette(CyNetwork network) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return brewerPalette; 
		return stringNetworkMap.get(network).getBrewerPalette();
	}
	public void setBrewerPalette(CyNetwork network, ColorBrewer palette) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			brewerPalette = palette; 
		} else
			stringNetworkMap.get(network).setBrewerPalette(palette);
	}
	public void setBrewerPalette(CyNetwork network, String palette) { 
		setBrewerPalette(network, Enum.valueOf(ColorBrewer.class, palette));
	}

	public ChartType getChartType(CyNetwork network) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return chartType; 
		return stringNetworkMap.get(network).getChartType();
	}
	public void setChartType(CyNetwork network, ChartType type) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			chartType = type; 
		} else
			stringNetworkMap.get(network).setChartType(type);
	}
	public void setChartType(CyNetwork network, String type) { 
		setChartType(network, Enum.valueOf(ChartType.class, type));
	}

	public boolean getRemoveOverlap(CyNetwork network) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return removeOverlap; 
		return stringNetworkMap.get(network).getRemoveOverlap();
	}
	public void setRemoveOverlap(CyNetwork network, boolean remove) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			removeOverlap = remove; 
		} else
			stringNetworkMap.get(network).setRemoveOverlap(remove);
	}

	public Map<String, Color> getChannelColors() { return channelColors; }

	public void setChannelColors(Map<String, Color> colorMap) { channelColors = colorMap; }
	public void setChannelColors(String colors) { 
		String[] colorStrs = colors.split("\\|");
		if (colorStrs.length != 7) return;

		channelColors = new HashMap<>();
		for (int i = 0; i < colorStrs.length; i++) {
			System.out.println("color["+i+"] = "+colorStrs[i]);
			System.out.println(channels[i]+" = "+parseColor(colorStrs[i]));
			channelColors.put(channels[i], parseColor(colorStrs[i]));
		}
	}

	public String getChannelColorString() {
		String str = "";
		for (int i = 0; i < 7; i++) {
			Color clr = channelColors.get(channels[i]);
			int rgb = clr.getRGB();
			str += "#"+Integer.toUnsignedString(rgb, 16)+"|"; // get the hex
		}

		return str.substring(0, str.length()-1);
	}

	public CyProperty<Properties> getConfigProperties() {
		return configProps;
	}

	// Assumes hex color: #ff000000
	private Color parseColor(String s) {
		int r = 0, g = 0, b = 0;
		if (s.length() == 9)
			s = s.substring(3);
		else if (s.length() == 7)
			s = s.substring(1);
		else return Color.BLACK;

		r = Integer.parseInt(s.substring(0,2), 16);
		g = Integer.parseInt(s.substring(2,4), 16);
		b = Integer.parseInt(s.substring(4,6), 16);
		return new Color(r,g,b);
	}

}
