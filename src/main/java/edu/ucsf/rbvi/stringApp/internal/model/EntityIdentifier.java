package edu.ucsf.rbvi.stringApp.internal.model;

public class EntityIdentifier implements Comparable<EntityIdentifier> {
	final private String matchedName;
	final private String primaryName;
	final private int type;
	final private String identifier;

	public EntityIdentifier(final String matched, final String primary, 
	                        final int type, final String identifier) {
		this.matchedName = matched;
		this.primaryName = primary;
		this.type = type;
		// The identifier we get back from the EntityQuery
		// includes the type.  We need to ditch that first
		String[] t = identifier.split("\\.",2);
		this.identifier = t[1];
	}

	public String getPrimaryName() { return primaryName; }
	public String getMatchedName() { return matchedName; }

	public int getType() { return type; }
	public String getIdentifier() { return identifier; }

	public int compareTo(EntityIdentifier t) {
		return primaryName.compareTo(t.getPrimaryName());
	}
}
