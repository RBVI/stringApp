package edu.ucsf.rbvi.stringApp.internal.model;

public class TextMiningResult implements Comparable<TextMiningResult> {
	final private String id;
	final private String name;
	final private String linkout;
	final private int foreground;
	final private int background;
	final private double score;

	public TextMiningResult(final String id, final String name, 
	                        final int foreground, final int background, final double score,
													final String linkout) {
		this.id = id;
		this.name = name;
		this.foreground = foreground;
		this.background = background;
		this.score = score;
		this.linkout = linkout;
	}

	public String getID() { return id; }
	public String getName() { return name; }

	public int getForeground() { return foreground; }
	public int getBackground() { return background; }
	public double getScore() { return score; }
	public String getLinkout() { return linkout; }

	public int compareTo(TextMiningResult t) {
		if (score == t.getScore()) return 0;
		if (score < t.getScore()) return -1;
		return 1;
	}
}
