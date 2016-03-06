package edu.ucsf.rbvi.stringApp.internal.model;

public class TextMiningResult implements Comparable<TextMiningResult> {
	final private String id;
	final private String name;
	final private int foreground;
	final private int background;
	final private double score;

	public TextMiningResult(final String id, final String name, 
	                        final int foreground, final int background, final double score) {
		this.id = id;
		this.name = name;
		this.foreground = foreground;
		this.background = background;
		this.score = score;
	}

	public String getID() { return id; }
	public String getName() { return name; }

	public int getForeground() { return foreground; }
	public int getBackground() { return background; }
	public double getScore() { return score; }

	public int compareTo(TextMiningResult t) {
		if (score == t.getScore()) return 0;
		if (score < t.getScore()) return -1;
		return 1;
	}
}
