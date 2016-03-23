package edu.ucsf.rbvi.stringApp.internal.model;

public class EntityIdentifier implements Comparable<EntityIdentifier> {
	final private String matchedName;
	final private String primaryName;
	final private long type;
	final private String identifier;

	public EntityIdentifier(final String matched, final String primary, 
	                        final long type, final String identifier) {
		this.matchedName = matched;
		this.primaryName = primary;
		this.type = type;
		this.identifier = identifier;
	}

	public String getPrimaryName() { return primaryName; }
	public String getMatchedName() { return matchedName; }

	public long getType() { return type; }
	public String getIdentifier() { return identifier; }

	public int compareTo(EntityIdentifier t) {
		return primaryName.compareTo(t.getPrimaryName());
	}
}
