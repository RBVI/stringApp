package edu.ucsf.rbvi.stringApp.internal.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.json.JSONResult;

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
