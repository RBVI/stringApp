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
	@ExampleJSONString(value="{\"Error\": Error String, \"Data\": {\"SUID\":1234\"}}")
	public String getJSON() {
		if (network == null) {
			return "{\"Error\": \"No network returned\", \"Data\": {}}";
		}
		long SUID = network.getSUID();
		return "{\"Error\": \"\", \"Data\": {\"SUID\":"+SUID+"\"}}";
	}
}
