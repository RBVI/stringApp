package edu.ucsf.rbvi.stringApp.internal.model;



/**
 * Enumerates the external databases/APIs stringApp can query (STRING, Viruses, STITCH,
 * String-db, JensenLab), pairing each one's display name with the name used to identify
 * it in API calls.
 */
public enum Databases {
	STRING("String", "string"),
	// AGOTOOL("aGOtool", "agotool")
	VIRUSES("Viruses", "viruses"),
	STITCH("Stitch", "stitch"),
	STRINGDB("String-db", "string-db"),
	JENSENLAB("Jensenlab", "jensenlab");

	String dbName;
	String apiName;
	Databases(String dbName, String apiName) {
		this.dbName = dbName;
		this.apiName = apiName;
	}

	public String toString() { return dbName; }
	public String getAPIName() { return apiName; }
}
