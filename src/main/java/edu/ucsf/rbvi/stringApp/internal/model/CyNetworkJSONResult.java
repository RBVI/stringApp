package edu.ucsf.rbvi.stringApp.internal.model;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.json.ExampleJSONString;
import org.cytoscape.work.json.JSONResult;

public class CyNetworkJSONResult implements JSONResult {
	final CyNetwork network;

	public CyNetworkJSONResult(final CyNetwork network) {
		this.network = network;
	}

	@Override
	@ExampleJSONString(value="{\"SUID\":1234}")
	public String getJSON() {
		long SUID = network.getSUID();
		return "{\"SUID\":"+SUID+", \"errors\": []}";
	}
}
