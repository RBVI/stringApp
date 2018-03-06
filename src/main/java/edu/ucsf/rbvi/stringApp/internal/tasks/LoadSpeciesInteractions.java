package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TunableSetter;
import org.json.simple.JSONObject;

import edu.ucsf.rbvi.stringApp.internal.io.HttpUtils;
import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stringApp.internal.utils.ViewUtils;

public class LoadSpeciesInteractions extends AbstractTask {

	final StringNetwork stringNet;
	final String species;
	final int taxonId;
	final int confidence;
	// final int additionalNodes;
	// final List<String> stringIds;
	// final Map<String, String> queryTermMap;
	final String netName;
	final String useDATABASE;

	public LoadSpeciesInteractions(final StringNetwork stringNet, final String species,
			final int taxonId, final int confidence, final String netName,
			final String useDATABASE) {

		this.stringNet = stringNet;
		this.taxonId = taxonId;
		this.confidence = confidence;
		this.species = species;
		this.netName = netName;
		this.useDATABASE = useDATABASE;
	}

	public void run(TaskMonitor monitor) {
		if (useDATABASE.equals(Databases.STRING.getAPIName()))
			monitor.setTitle("Loading interactions from string-db");
		else if (useDATABASE.equals(Databases.STITCH.getAPIName()))
			monitor.setTitle("Loading interactions from STITCH");
		StringManager manager = stringNet.getManager();

		String conf = "0." + confidence;
		if (confidence == 100)
			conf = "1.0";

		Map<String, String> args = new HashMap<>();
		args.put("database", Databases.STITCH.getAPIName());
		args.put("organism", String.valueOf(taxonId));
		args.put("score", conf);
		args.put("caller_identity", StringManager.CallerIdentity);

		// double time = System.currentTimeMillis();
		JSONObject results = HttpUtils.postJSON(manager.getNetworkURL(), args, manager);
		// System.out.println(
		// "postJSON method " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		// time = System.currentTimeMillis();

		CyNetwork network = ModelUtils.createNetworkFromJSON(stringNet, species, results, null,
		                                                     null, netName, useDATABASE);
		// System.out.println("createNetworkFromJSON method "
		// + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		// time = System.currentTimeMillis();

		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "String returned no results");
			return;
		}

		// Set our confidence score
		ModelUtils.setConfidence(network, ((double) confidence) / 100.0);
		ModelUtils.setDatabase(network, useDATABASE);
		ModelUtils.setNetSpecies(network, species);
		stringNet.setNetwork(network);

		int viewThreshold = ModelUtils.getViewThreshold(manager);
		int networkSize = network.getNodeList().size() + network.getEdgeList().size();
		if (networkSize < viewThreshold) {
			// Now style the network
			CyNetworkView networkView = manager.createNetworkView(network);
			ViewUtils.styleNetwork(manager, network, networkView);

			// And lay it out
			CyLayoutAlgorithm alg = manager.getService(CyLayoutAlgorithmManager.class)
					.getLayout("force-directed");
			Object context = alg.createLayoutContext();
			TunableSetter setter = manager.getService(TunableSetter.class);
			Map<String, Object> layoutArgs = new HashMap<>();
			layoutArgs.put("defaultNodeMass", 10.0);
			setter.applyTunables(context, layoutArgs);
			Set<View<CyNode>> nodeViews = new HashSet<>(networkView.getNodeViews());
			insertTasksAfterCurrentTask(
					alg.createTaskIterator(networkView, context, nodeViews, "score"));

		} else {
			ViewUtils.styleNetwork(manager, network, null);
		}
	}

	@ProvidesTitle
	public String getTitle() {
		return "Loading whole species interactions";
	}
}
