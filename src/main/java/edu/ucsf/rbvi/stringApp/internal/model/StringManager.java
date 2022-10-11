package edu.ucsf.rbvi.stringApp.internal.model;

import java.awt.Color;
import java.net.SocketTimeoutException;
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
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.command.AvailableCommands;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.CyProperty.SavePolicy;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.Palette;
import org.cytoscape.util.color.PaletteProvider;
import org.cytoscape.util.color.PaletteProviderManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
// import org.jcolorbrewer.ColorBrewer;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.EnrichmentTerm.TermCategory;
import edu.ucsf.rbvi.stringApp.internal.tasks.AddNamespacesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.SetConfidenceTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnhancedLabelsTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowEnrichmentPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowGlassBallEffectTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowImagesTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowPublicationsPanelTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.ShowResultsPanelTaskFactory;

import edu.ucsf.rbvi.stringApp.internal.tasks.CrossSpeciesSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.StringSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.StitchSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.DiseaseSearchTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.tasks.PubmedSearchTaskFactory;

import edu.ucsf.rbvi.stringApp.internal.ui.CrossSpeciesWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.DiseaseNetworkWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.StitchWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.StringWebServiceClient;
import edu.ucsf.rbvi.stringApp.internal.ui.TextMiningWebServiceClient;

import edu.ucsf.rbvi.stringApp.internal.ui.EnrichmentCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.PublicationsCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.StringCytoPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringManager implements NetworkAddedListener, SessionLoadedListener, NetworkAboutToBeDestroyedListener, SetCurrentNetworkListener {
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
	private EnrichmentCytoPanel enrichPanel = null;
	private PublicationsCytoPanel publPanel = null;

	public static String CONFIGURI = "https://jensenlab.org/assets/stringapp/";
	
	public static String STRINGResolveURI = "https://string-db.org/api/";
	public static String STITCHResolveURI = "http://stitch.embl.de/api/";
	public static String VIRUSESResolveURI = "http://viruses.string-db.org/cgi/webservice_handler.pl";
	public static String SpeciesURI = Species.class.getResource("/species_v11.5.tsv").toString();
	public static String PairsURI = Species.class.getResource("/pairs_v11.5.tsv").toString();
	//public static String STITCHResolveURI = "http://beta.stitch-db.org/api/";
	public static String URI = "https://api11.jensenlab.org/";
	public static String DATAVERSION = "11.5";
	public static String OLD_DATAVERSION = "10";
	public static String alternativeAPIProperty = "alternativeAPI";
	public static String alternativeCONFIGURIProperty = "alternativeCONFIGURI";
	public static String alternativeCONFIGURI = "";
	public static String CallerIdentity = "string_app";
	public static String APIVERSION = "String-api-version";
	public static String RESULT = "QueryResult";
	
	public static String STRINGDevelopmentURI = "https://string-gamma.org/api/";
	// public static String STRING_AGOTOOLenrichmentURI = "https://string-pythongamma.org/api/";
	// public static String AGOTOOLenrichmentURI = "https://agotool.org/api/";
	
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
	private boolean highlightNeighbors = false;
	private String species;
	private double defaultConfidence = 0.40;
	private int additionalProteins = 0;
	private int maximumProteins = 100;
	private NetworkType networkType = NetworkType.FUNCTIONAL;

	private int topTerms = 5;
	private double overlapCutoff = 0.5;
	private Palette brewerPalette;
	private List<TermCategory> categoryFilter = TermCategory.getValues();
	private ChartType chartType = ChartType.SPLIT;
	private boolean removeOverlap = false;
	private Map<String, Color> channelColors;

  private CyNetwork newNetwork = null;

	public static String ShowStructureImages = "showStructureImages";
	public static String ShowEnhancedLabels = "showEnhancedLabels";
	public static String ShowGlassBallEffect = "showGlassBallEffect";
	public static String ShowStringColors = "showStringColors";
	public static String ShowSingletons = "showSingletons";
	public static String HighlightNeighbors = "highlightNeighbors";

	public static String[] channels = { "databases", "experiments", "neighborhood", "fusion",
	                                    "cooccurrence", "textmining", // Lime green 
																			"coexpression", 
																			"similarity" // Lila
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

		PaletteProviderManager pm = registrar.getService(PaletteProviderManager.class);
		PaletteProvider brewerProvider = pm.getPaletteProvider("ColorBrewer");
		brewerPalette = brewerProvider.getPalette("Paired colors");

		channelColors = new HashMap<>();
		// Set up our default channel colors
		channelColors.put("databases",Color.CYAN);
		channelColors.put("experiments",Color.MAGENTA);
		channelColors.put("neighborhood",Color.GREEN);
		channelColors.put("fusion",Color.RED);
		channelColors.put("cooccurrence",Color.BLUE);
		channelColors.put("textmining",new Color(199,234,70)); // Lime green
		channelColors.put("coexpression", Color.BLACK);
		channelColors.put("similarity", new Color(163, 161, 255)); // Lila

		// Get our default settings
		configProps = ModelUtils.getPropertyService(this, SavePolicy.CONFIG_DIR);

		// check for an alternative config URI
		if (ModelUtils.hasProperty(configProps, alternativeCONFIGURIProperty)) {
			alternativeCONFIGURI = (String) ModelUtils.getStringProperty(configProps,
					alternativeCONFIGURIProperty);
		} else {
			ModelUtils.setStringProperty(configProps, alternativeCONFIGURIProperty, alternativeCONFIGURI);
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

	public void updateSpecies() {
		if (Species.getSpecies() != null)
			return;
		// Make sure we've read in our species
		try {
			Species.readSpecies(this);
			Species.readPairs(this);
		} catch (Exception e) {
			throw new RuntimeException("Can't read species information");
		}
	}

	public void updateProperties() {
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
		if (ModelUtils.hasProperty(configProps, HighlightNeighbors)) {
			setHighlightNeighbors(ModelUtils.getBooleanProperty(configProps,HighlightNeighbors));
		}


		if (ModelUtils.hasProperty(configProps, "species")) {
			setDefaultSpecies(ModelUtils.getStringProperty(configProps,"species"));
		}
		if (ModelUtils.hasProperty(configProps, "networkType")) {
			setDefaultNetworkType(ModelUtils.getStringProperty(configProps,"networkType"));
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

		// TODO: [N] Is it ok to set the group to null here
		if (ModelUtils.hasProperty(configProps, "overlapCutoff")) {
			setOverlapCutoff(null, ModelUtils.getDoubleProperty(configProps,"overlapCutoff"), null);
		}
		if (ModelUtils.hasProperty(configProps, "topTerms")) {
			setTopTerms(null, ModelUtils.getIntegerProperty(configProps,"topTerms"), null);
		}
		if (ModelUtils.hasProperty(configProps, "chartType")) {
			setChartType(null, ModelUtils.getStringProperty(configProps,"chartType"), null);
		}
		if (ModelUtils.hasProperty(configProps, "brewerPalette")) {
			setBrewerPalette(null, ModelUtils.getStringProperty(configProps,"brewerPalette"), null);
		}
		if (ModelUtils.hasProperty(configProps, "enrichmentPalette")) {
			setEnrichmentPalette(null, ModelUtils.getStringProperty(configProps,"enrichmentPalette"), null);
		}
		if (ModelUtils.hasProperty(configProps, "categoryFilter")) {
			setCategoryFilter(null, ModelUtils.getStringProperty(configProps,"categoryFilter"), null);
		}
		if (ModelUtils.hasProperty(configProps, "removeOverlap")) {
			setRemoveOverlap(null, ModelUtils.getBooleanProperty(configProps,"removeOverlap"), null);
		}
		if (ModelUtils.hasProperty(configProps, "channelColors")) {
			setChannelColors(ModelUtils.getStringProperty(configProps,"channelColors"));
		}
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
					try {
						if (alternativeCONFIGURI != null && alternativeCONFIGURI.length() > 0) {
							uris = ModelUtils.getResultsFromJSON(
									HttpUtils.getJSON(alternativeCONFIGURI, args, manager, 10000),
									JSONObject.class);
						} else {
							uris = ModelUtils.getResultsFromJSON(HttpUtils.getJSON(url, args, manager, 10000), JSONObject.class);
						}
					} catch (ConnectionException e) {
						e.printStackTrace();
					} catch (SocketTimeoutException e) {
						System.out.println("SocketTimeoutException");
						updateSpecies();
						registerSearchFactories();
						registerWebServiceFactories();
						return;
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
						if (uris.containsKey("SpeciesURI")) {
							SpeciesURI = uris.get("SpeciesURI").toString();
						}
						if (uris.containsKey("PairsURI")) {
							PairsURI = uris.get("PairsURI").toString();
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
					updateSpecies();
					registerSearchFactories();
					registerWebServiceFactories();
 	  		 }
			});
		}


	void registerWebServiceFactories() {
		{
			// Register our web service client
			StringWebServiceClient client = new StringWebServiceClient(this);
			registrar.registerAllServices(client, new Properties());
		}
		
		{
			// Register our text mining web service client
			TextMiningWebServiceClient client = new TextMiningWebServiceClient(this);
			registrar.registerAllServices(client, new Properties());
		}
		
		{
			// Register our disease network web service client
			DiseaseNetworkWebServiceClient client = new DiseaseNetworkWebServiceClient(this);
			registrar.registerAllServices(client, new Properties());
		}
		
		{
			// Register our stitch network web service client
			StitchWebServiceClient client = new StitchWebServiceClient(this);
			registrar.registerAllServices(client, new Properties());
		}
		
		{
			// Register our cross species network web service client
			CrossSpeciesWebServiceClient client = new CrossSpeciesWebServiceClient(this);
			registrar.registerAllServices(client, new Properties());
		}

	}

	void registerSearchFactories() {
		// Register our Network search factories
    {
      StringSearchTaskFactory stringSearch = new StringSearchTaskFactory(this);
      Properties propsSearch = new Properties();
      registrar.registerService(stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
    {
      StitchSearchTaskFactory stringSearch = new StitchSearchTaskFactory(this);
      Properties propsSearch = new Properties();
      registrar.registerService(stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
    {
      PubmedSearchTaskFactory stringSearch = new PubmedSearchTaskFactory(this);
      Properties propsSearch = new Properties();
      registrar.registerService(stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
    {
      DiseaseSearchTaskFactory stringSearch = new DiseaseSearchTaskFactory(this);
      Properties propsSearch = new Properties();
      registrar.registerService(stringSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
		
		{
      CrossSpeciesSearchTaskFactory xpSearch = new CrossSpeciesSearchTaskFactory(this);
      Properties propsSearch = new Properties();
      registrar.registerService(xpSearch, NetworkSearchTaskFactory.class, propsSearch);
    }
	}

	
	public String adaptNetworkName(String name) {
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
		return name;
	}
	
	public CyNetwork createNetwork(String name, String rootNetName) {
		CyNetwork network = registrar.getService(CyNetworkFactory.class).createNetwork();		
		network.getRow(network).set(CyNetwork.NAME, adaptNetworkName(name));
		CyNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
		rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, adaptNetworkName(rootNetName));
		return network;
	}

//	public CyNetwork createStringNetwork(String name, StringNetwork stringNet, 
//	                                     String useDATABASE, String species, String netType) {
//		CyNetwork network = createNetwork(name);
//		ModelUtils.setDatabase(network, useDATABASE);
//		ModelUtils.setNetSpecies(network, species);
//		ModelUtils.setNetworkType(network, netType);
//		addStringNetwork(stringNet, network);
//		return network;
//	}

	public void addStringNetwork(StringNetwork stringNet, CyNetwork network) {
		stringNetworkMap.put(network, stringNet);
		stringNet.setNetwork(network);
    newNetwork = network; // Do this in case we don't have a "current" network
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

	public String getRootNetworkName(CyNetwork net) {
		CyRootNetwork rootNet = ((CySubNetwork)net).getRootNetwork();
		return rootNet.getRow(rootNet).get(CyNetwork.NAME, String.class);
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
		CyNetwork network = registrar.getService(CyApplicationManager.class).getCurrentNetwork();
    if (network != null) return network;
    return newNetwork;
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
	
	public boolean highlightNeighbors() { return highlightNeighbors; }

	public void setHighlightNeighbors(boolean set) { 
		highlightNeighbors = set; 
	}

	public void setEnrichPanel(EnrichmentCytoPanel panel) {
		this.enrichPanel = panel;
	}
	
	public void setPublPanel(PublicationsCytoPanel panel) {
		this.publPanel = panel;
	}

	public void setCytoPanel(StringCytoPanel panel) {
		this.cytoPanel = panel;
	}

	public void updateControls() {
		if (cytoPanel != null)
			cytoPanel.updateControls();
	}

	public String getDefaultSpecies() { 
		if (species == null) {
			// Set Human as the default
			return "Homo sapiens";
		}
		return species;
	}
	public void setDefaultSpecies(Species defaultSpecies) {
		species = defaultSpecies.getName();
	}
	public void setDefaultSpecies(String defaultSpecies) {
		species = defaultSpecies;
	}

	public double getDefaultConfidence() { return defaultConfidence; }
	public void setDefaultConfidence(double conf) { defaultConfidence = conf; }

	public int getDefaultAdditionalProteins() { return additionalProteins; }
	public void setDefaultAdditionalProteins(int ap) { additionalProteins = ap; }

	public int getDefaultMaxProteins() { return maximumProteins; }
	public void setDefaultMaxProteins(int max) { maximumProteins = max; }

	public NetworkType getDefaultNetworkType() { return networkType; }
	public void setDefaultNetworkType(NetworkType type) { networkType = type; }
	
	public void setDefaultNetworkType(String type) { 
		networkType = NetworkType.getType(type); 
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

	public void executeCommand(String namespace, String command, 
	                           Map<String, Object> args, TaskObserver observer) {
		TaskIterator ti = commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
		execute(ti, true);
	}

	public TaskIterator getCommandTaskIterator(String namespace, String command, 
	                           Map<String, Object> args, TaskObserver observer) {
		return commandExecutorTaskFactory.createTaskIterator(namespace, command, args, observer);
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
		// else if (useDATABASE.equals(Databases.AGOTOOL.getAPIName()))
		//	return AGOTOOLenrichmentURI;
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
		// ModelUtils.setStringProperty(configProps, "confidence", Double.toString(overlapCutoff));
		ModelUtils.setStringProperty(configProps, "showImage", Boolean.toString(showImage));
		ModelUtils.setStringProperty(configProps, "showEnhancedLabels", Boolean.toString(showEnhancedLabels));
		ModelUtils.setStringProperty(configProps, "showGlassBallEffect", Boolean.toString(showGlassBallEffect));
		ModelUtils.setStringProperty(configProps, "showStringColors", Boolean.toString(showStringColors));
		ModelUtils.setStringProperty(configProps, "showSingletons", Boolean.toString(showSingletons));
		ModelUtils.setStringProperty(configProps, "highlightNeighbors", Boolean.toString(highlightNeighbors));

		ModelUtils.setStringProperty(configProps, "species", getDefaultSpecies().toString());
		ModelUtils.setStringProperty(configProps, "networkType", getDefaultNetworkType().toString());
		ModelUtils.setStringProperty(configProps, "defaultConfidence", Double.toString(getDefaultConfidence()));
		ModelUtils.setStringProperty(configProps, "additionalProteins", Integer.toString(getDefaultAdditionalProteins()));
		ModelUtils.setStringProperty(configProps, "maxProteins", Integer.toString(getDefaultMaxProteins()));

		ModelUtils.setStringProperty(configProps, "overlapCutoff", Double.toString(overlapCutoff));
		ModelUtils.setStringProperty(configProps, "topTerms", Integer.toString(topTerms));
		ModelUtils.setStringProperty(configProps, "chartType", chartType.name());
		ModelUtils.setStringProperty(configProps, "enrichmentPalette", brewerPalette.toString());
		ModelUtils.setStringProperty(configProps, "removeOverlap", Boolean.toString(removeOverlap));
		{
			String categories = "";
			for (TermCategory c: categoryFilter) {
				categories += c.name()+",";
			}
			if (categories.length() > 1)
				categories = categories.substring(categories.length()-1);
			ModelUtils.setStringProperty(configProps, "categoryFilter", categories);
		}

		ModelUtils.setStringProperty(configProps, "channelColors", getChannelColorString());
		updateControls();
	}

	
	public void processNewNetwork(CyNetwork network) {
		// This is a string network only if we have a confidence score in the network table,
		// "@id", "species", "canonical name", and "sequence" columns in the node table, and 
		// a "score" column in the edge table
		boolean foundStringNet = false;
		if (ModelUtils.isStringNetwork(network)) {
			StringNetwork stringNet = new StringNetwork(this);
			addStringNetwork(stringNet, network);
			foundStringNet = true;
		} else if (getNetworkName(network).endsWith("--clustered") && ModelUtils.isMergedStringNetwork(network)) {
			execute(new SetConfidenceTaskFactory(this).createTaskIterator(network));
			foundStringNet = true;
		} else if ((getRootNetworkName(network).startsWith(ModelUtils.DEFAULT_NAME_STRING)
				|| getRootNetworkName(network).startsWith(ModelUtils.DEFAULT_NAME_STITCH))
				&& ModelUtils.isMergedStringNetwork(network)) {
			execute(new SetConfidenceTaskFactory(this).createTaskIterator(network));
			foundStringNet = true;
		}

		if (foundStringNet) {
			showResultsPanel();
			showEnrichmentPanel();
			showPublicationPanel();
		}
	}
	
	public void handleEvent(SetCurrentNetworkEvent event) {
		CyNetwork network = event.getNetwork();
		if (ignore || network == null || getStringNetwork(network) != null) return;
		
		processNewNetwork(network);
	}
	
	public void handleEvent(NetworkAddedEvent nae) {
		CyNetwork network = nae.getNetwork();
		if (ignore) return;

		processNewNetwork(network);
	}

	public void handleEvent(SessionLoadedEvent arg0) {
		// Get any properties we stored in the session
		sessionProperties = ModelUtils.getPropertyService(this, SavePolicy.SESSION_FILE);

		// Create string networks for any networks loaded by string
		Set<CyNetwork> networks = arg0.getLoadedSession().getNetworks();
		if (networks.size() == 0)
			return;
		Set<CyNetwork> networksToUpgrade = new HashSet<CyNetwork>();
		for (CyNetwork network: networks) {
			if (ModelUtils.isStringNetwork(network)) {
				if (ModelUtils.ifHaveStringNS(network)) {
					StringNetwork stringNet = new StringNetwork(this);
					addStringNetwork(stringNet, network);
				} else if (ModelUtils.getDataVersion(network) == null) {
					networksToUpgrade.add(network);
				}
			}
		}

		// if there are old string networks, figure out what to do
		if (networksToUpgrade.size() > 0) {
			// System.out.println("found networks to upgrade");
			execute(new AddNamespacesTaskFactory(this).createTaskIterator(networksToUpgrade), true);
		}

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
			//labelsTaskFactory.reregister();
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
			//glassBallTaskFactory.reregister();
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
			//imagesTaskFactory.reregister();
		}

		// if string network, show results panel and enrichment if any enrichment found
		if (ModelUtils.ifHaveStringNS(getCurrentNetwork())) {
			showResultsPanel();
			// load enrichment & publications if computed earlier
			showEnrichmentPanel();
			showPublicationPanel();
		} else {
			hideResultsPanel();
			// TODO: Hide is not implemented yet, do we need it?
			//hideEnrichmentPanel();
			//hidePublicationPanel();
		}
	}

	public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
		CyNetwork network = e.getNetwork();
		// delete enrichment tables
		CyTableManager tableManager = getService(CyTableManager.class);
		Set<CyTable> oldTables = ModelUtils.getAllEnrichmentTables(this, network, EnrichmentTerm.ENRICHMENT_TABLE_PREFIX);
		for (CyTable table : oldTables) {
			tableManager.deleteTable(table.getSUID());
		}
		// TODO: are we sure that we don't need to reload?
		// reloadEnrichmentPanel();
		// remove as string network
		if (stringNetworkMap.containsKey(network))
			stringNetworkMap.remove(network);
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

	public void reinitResultsPanel(CyNetwork network) {
		if (cytoPanel == null) {
			execute(resultsPanelTaskFactory.createTaskIterator(), true);
		} else {
			// Make sure we show it
			cytoPanel.reinitCytoPanel();
		}
	}

	public void showEnrichmentPanel() {
		CyTableManager tableManager = getService(CyTableManager.class);
		Set<CyTable> tables = tableManager.getAllTables(true);
		for (CyTable table : tables) {
			if (table.getTitle().contains(EnrichmentTerm.ENRICHMENT_TABLE_PREFIX)) {
				// System.out.println("manager: found table ALL");
				if (enrichPanel == null) {
					// System.out.println("manager: showEnrichPanel -> createTaskIterator");
					execute(enrichmentTaskFactory.createTaskIterator(), true);
				}
				else {
					// System.out.println("manager: showEnrichPanel -> show panel");
					enrichPanel.showCytoPanel();
				}
				break;
			}
		}
	}
	
	public void showPublicationPanel() {
		CyTableManager tableManager = getService(CyTableManager.class);
		Set<CyTable> tables = tableManager.getAllTables(true);
		for (CyTable table : tables) {
			if (table.getTitle().equals(TermCategory.PMID.getTable())) {
				// System.out.println("manager: found table PMID");
				if (publPanel == null) {	
					// System.out.println("manager: showPublPanel -> createTaskIterator");
					execute(publicationsTaskFactory.createTaskIterator(), true);
				}
				else { 
					// System.out.println("manager: showPublPanel -> show panel");
					publPanel.showCytoPanel();
				}
				break;
			}
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

	public boolean haveClusterMaker() {
		return availableCommands.getNamespaces().contains("cluster");
	}

	public boolean haveEnrichmentMap() {
		return availableCommands.getNamespaces().contains("enrichmentmap");
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
	// TODO: [N] Is it ok to just add a group to all of them?
	public double getOverlapCutoff(CyNetwork network, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return overlapCutoff; 
		return stringNetworkMap.get(network).getOverlapCutoff(group);
	}
	public void setOverlapCutoff(CyNetwork network, double cutoff, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			overlapCutoff = cutoff; 
			return;
		}
		stringNetworkMap.get(network).setOverlapCutoff(group, cutoff);
	}

	public int getTopTerms(CyNetwork network, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return topTerms; 
		return stringNetworkMap.get(network).getTopTerms(group);
	}
	public void setTopTerms(CyNetwork network, int topN, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network)) 
			topTerms = topN; 
		else
			stringNetworkMap.get(network).setTopTerms(group, topN);
	}

	public List<TermCategory> getCategoryFilter(CyNetwork network, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return categoryFilter; 
		return stringNetworkMap.get(network).getCategoryFilter(group);
	}
	public void setCategoryFilter(CyNetwork network, List<TermCategory> categories, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			categoryFilter = categories; 
		} else
			stringNetworkMap.get(network).setCategoryFilter(group, categories);
	}
	public void setCategoryFilter(CyNetwork network, String categories, String group) { 
		List<TermCategory> catList = new ArrayList<>();
		if (categories == null) return;
		String[] catArray = categories.split(",");
		for (String c: catArray) {
			try {
				catList.add(Enum.valueOf(TermCategory.class, c));
			} catch (Exception e) {
			}
		}
		setCategoryFilter(network, catList, group);
	}

	/*
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
*/
	public Palette getEnrichmentPalette(CyNetwork network, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return brewerPalette;
		return stringNetworkMap.get(network).getEnrichmentPalette(group);
	}

	public void setEnrichmentPalette(CyNetwork network, Palette palette, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			brewerPalette = palette;
		else
			stringNetworkMap.get(network).setEnrichmentPalette(group, palette);
	}

	public void setEnrichmentPalette(CyNetwork network, String palette, String group) { 
		PaletteProviderManager pm = registrar.getService(PaletteProviderManager.class);
		for (PaletteProvider provider: pm.getPaletteProviders(BrewerType.QUALITATIVE, false)) {
			for (Object id: provider.listPaletteIdentifiers(BrewerType.QUALITATIVE, false)) {
				Palette p = provider.getPalette(id);
				if (p.toString().equals(palette))
					setEnrichmentPalette(network, p, group);
			}
		}
	}

	// Retained for backwards compatability
	public void setBrewerPalette(CyNetwork network, String palette, String group) { 
		if (palette.startsWith("ColorBrewer "))
			setEnrichmentPalette(network, palette, group);
		setEnrichmentPalette(network, "ColorBrewer "+palette, group);
	}

	public ChartType getChartType(CyNetwork network, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return chartType; 
		return stringNetworkMap.get(network).getChartType(group);
	}
	public void setChartType(CyNetwork network, ChartType type, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			chartType = type; 
		} else
			stringNetworkMap.get(network).setChartType(group, type);
	}
	public void setChartType(CyNetwork network, String type, String group) { 
		setChartType(network, Enum.valueOf(ChartType.class, type), group);
	}

	public boolean getRemoveOverlap(CyNetwork network, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network))
			return removeOverlap; 
		return stringNetworkMap.get(network).getRemoveOverlap(group);
	}
	public void setRemoveOverlap(CyNetwork network, boolean remove, String group) { 
		if (network == null || !stringNetworkMap.containsKey(network)) {
			removeOverlap = remove; 
		} else
			stringNetworkMap.get(network).setRemoveOverlap(group, remove);
	}

	public Map<String, Color> getChannelColors() { return channelColors; }

	public void setChannelColors(Map<String, Color> colorMap) { channelColors = colorMap; }
	public void setChannelColors(String colors) { 
		String[] colorStrs = colors.split("\\|");
		if (colorStrs.length != 8) return;

		channelColors = new HashMap<>();
		for (int i = 0; i < colorStrs.length; i++) {
			channelColors.put(channels[i], parseColor(colorStrs[i]));
		}
	}

	public String getChannelColorString() {
		String str = "";
		for (int i = 0; i < 8; i++) {
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
