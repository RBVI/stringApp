package edu.ucsf.rbvi.stringApp.internal.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum EvidenceType {

	DATABASES("databases", Color.CYAN),
	TEXTMINING("textmining", new Color(199,234,70)), // Lime green
	EXPERIMENTS("experiments", Color.MAGENTA),
	COEXPRESSION("coexpression", Color.BLACK),
	NEIGHBORHOOD("neighborhood", Color.GREEN),
	COOCCURRENCE("cooccurrence", Color.BLUE),
	GENEFUSIONS("fusion", Color.RED),
	SIMILARITY("similarity", new Color(163, 161, 255)); // Lila
	
	String name;
	Color color;

	EvidenceType(String name, Color color) {
		this.name = name;
		this.color = color;
	}

	public Color getColor() { return color; }
	
	public String toString() { return name; }
	
	public static List<String> getOrderedEvidenceTypes(String netType) {
		if (netType != null && netType.equals(NetworkType.PHYSICAL.toString()))
			return new ArrayList<String>(Arrays.asList(DATABASES.name, TEXTMINING.name, EXPERIMENTS.name));
		else // assume it is functional
			return new ArrayList<String>(Arrays.asList(DATABASES.name, TEXTMINING.name, EXPERIMENTS.name, COEXPRESSION.name,
					NEIGHBORHOOD.name, COOCCURRENCE.name, GENEFUSIONS.name));
	}
	
	public static Map<String, Color> getEvidenceColors() {
		Map<String, Color> channelColors = new HashMap<String, Color>();
		// Set up our default channel colors
		channelColors.put(DATABASES.name, DATABASES.color);
		channelColors.put(TEXTMINING.name, TEXTMINING.color);
		channelColors.put(EXPERIMENTS.name, EXPERIMENTS.color);
		channelColors.put(COEXPRESSION.name, COEXPRESSION.color);
		channelColors.put(NEIGHBORHOOD.name, NEIGHBORHOOD.color);
		channelColors.put(COOCCURRENCE.name, COOCCURRENCE.color); 
		channelColors.put(GENEFUSIONS.name, GENEFUSIONS.color);
		channelColors.put(SIMILARITY.name, SIMILARITY.color); 
		return channelColors;
	}
	
}
