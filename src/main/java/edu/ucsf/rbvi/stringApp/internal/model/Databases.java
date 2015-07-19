package edu.ucsf.rbvi.stringApp.internal.model;

import java.util.Properties;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

public enum Databases {
	STRING("String", "string"),
	STITCH("Stitch", "stitch");

	String dbName;
	String apiName;
	Databases(String dbName, String apiName) {
		this.dbName = dbName;
		this.apiName = apiName;
	}

	public String toString() { return dbName; }
	public String getAPIName() { return apiName; }
}
