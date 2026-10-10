package edu.ucsf.rbvi.stringApp.internal.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.json.JSONResult;

/**
 * A {@link JSONResult} wrapper around a {@link CyNetwork} that renders the network as a
 * small JSON object containing just its SUID, for use as the JSON output of Commands API
 * tasks that return a network.
 */
public class CyNetworkJSONResult implements JSONResult {
	final CyNetwork network;

	public CyNetworkJSONResult(final CyNetwork network) {
		this.network = network;
	}

	@Override
	public String getJSON() {
		long SUID = network.getSUID();
		return "{\"SUID\":"+SUID+"}";
	}
}
