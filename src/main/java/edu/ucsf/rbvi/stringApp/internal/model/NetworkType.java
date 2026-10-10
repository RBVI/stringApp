package edu.ucsf.rbvi.stringApp.internal.model;

/**
 * Enumerates the kinds of STRING networks (the full functional network, the
 * physical-interaction-only subnetwork and the directed regulatory subnetwork), pairing
 * each with its display name and the name used in STRING API calls.
 */
public enum NetworkType {
	FUNCTIONAL("full STRING network", "functional"),
	PHYSICAL("physical subnetwork", "physical"),
	REGULATORY("regulatory subnetwork", "regulatory");
	
	String name;
	String apiName;
	NetworkType(String name, String api) {
		this.name = name;
		this.apiName = api;
	}

	public String getAPIName() { return apiName; }
	
	public String toString() { return name; }

	/**
	 * Regulatory networks are only served by the STRING-DB API, not by the Jensenlab API that
	 * backs STITCH, viral and cross-species queries.
	 */
	public boolean isJensenlabSupported() { return this != REGULATORY; }

	public static boolean isJensenlabSupported(String apiName) {
		return !REGULATORY.apiName.equals(apiName);
	}

	public static final String JENSENLAB_UNSUPPORTED_MESSAGE = 
			"Regulatory networks are only available for proteins from a single STRING species "+
			"(not for compounds, viruses or cross-species networks).";

	public static NetworkType getType(String type) {
		if (type == null)
			return null;
		else if (type.equals(PHYSICAL.name))
			return NetworkType.PHYSICAL;
		else if (type.equals(REGULATORY.name))
			return NetworkType.REGULATORY;
		else
			return NetworkType.FUNCTIONAL;
	}
}
