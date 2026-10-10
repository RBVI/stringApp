package edu.ucsf.rbvi.stringApp.internal.model;

/**
 * Enumerates the two kinds of STRING networks (the full functional network versus the
 * physical-interaction-only subnetwork), pairing each with its display name and the name
 * used in STRING API calls.
 */
public enum NetworkType {
	FUNCTIONAL("full STRING network", "functional"),
	PHYSICAL("physical subnetwork", "physical");
	
	String name;
	String apiName;
	NetworkType(String name, String api) {
		this.name = name;
		this.apiName = api;
	}

	public String getAPIName() { return apiName; }
	
	public String toString() { return name; }
	
	public static NetworkType getType(String type) {
		if (type == null)
			return null;
		else if (type.equals(PHYSICAL.name))
			return NetworkType.PHYSICAL;
		else
			return NetworkType.FUNCTIONAL;
	}
}

