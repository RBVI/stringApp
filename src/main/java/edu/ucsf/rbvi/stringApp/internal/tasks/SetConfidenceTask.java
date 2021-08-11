package edu.ucsf.rbvi.stringApp.internal.tasks;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class SetConfidenceTask extends AbstractTask implements ObservableTask {
	final StringManager manager;
	private CyNetwork net;

	@Tunable(description="Network to set as a STRING network", 
	         longDescription=StringToModel.CY_NETWORK_LONG_DESCRIPTION,
	         exampleStringValue=StringToModel.CY_NETWORK_EXAMPLE_STRING,
	         context="nogui", required=true)
	public CyNetwork network = null;

	public SetConfidenceTask(final StringManager manager, final CyNetwork net) {
		this.manager = manager;
		this.net = net;
	}

	public void run(TaskMonitor monitor) {
		monitor.setTitle("Set as STRING network");

		if (network != null)
			net = network;
		else if (net == null) {
			net = manager.getService(CyApplicationManager.class).getCurrentNetwork();
		}

		double minScore = 1.0;
		for (CyEdge edge : net.getEdgeList()) {
			Double score = net.getRow(edge).get(ModelUtils.SCORE, Double.class);
			if (score == null || score >= minScore)
				continue;
			minScore = score;
		}

		ModelUtils.setConfidence(net, minScore);

		// TODO: Find a better way to set the database, e.g. check certain node column
		ModelUtils.setDatabase(net, Databases.STRING.getAPIName());
		ModelUtils.setNetworkType(net, manager.getDefaultNetworkType().toString());
		ModelUtils.setNetSpecies(net, ModelUtils.getMostCommonNetSpecies(net));
		ModelUtils.setDataVersion(net, manager.getDataVersion());
		ModelUtils.setNetURI(net, manager.getNetworkURL());


		StringNetwork stringNet = new StringNetwork(manager);
		stringNet.setNetwork(net);
		manager.addStringNetwork(stringNet, net);
	}

	@Override
	public <R> R getResults(Class<? extends R> clzz) {
		if (clzz.equals(CyNetwork.class)) {
			return (R) net;
		} else if (clzz.equals(Long.class)) {
			if (net == null)
				return null;
			return (R) net.getSUID();
		} else if (clzz.equals(JSONResult.class)) {
			JSONResult res = () -> {
				if (net == null) return "{}";
				else return "{\"network\": "+net.getSUID()+"}";
      };
      return (R)res;
		} else if (clzz.equals(String.class)) {
			if (net == null)
				return (R) "No network was set";
			String resp = "Set network '"
					+ net.getRow(net).get(CyNetwork.NAME, String.class);
			resp += " as STRING network";
			return (R) resp;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(JSONResult.class, String.class, Long.class, CyNetwork.class);
	}

}
