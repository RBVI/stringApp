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
	@ExampleJSONString(value="{\"SUID\":1234, \"errors\":[]}")
	public String getJSON() {
		String returnValue = null;
		if (network == null) {
			returnValue = "{\"SUID\":-1, \"errors\": [\"No network returned\"]}";
		} else {
			long SUID = network.getSUID();
			returnValue = "{\"SUID\":"+SUID+", \"errors\": []}";
		}
		return returnValue;
	}
}
