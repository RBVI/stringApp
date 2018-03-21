package edu.ucsf.rbvi.stringApp.internal.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

public abstract class IconUtils {
	
	// stringApp Icon
	public static final String STRING_ICON = "a";
	public static final String STRING_ICON_LAYER_1 = "b";
	public static final String STRING_ICON_LAYER_2 = "c";
	public static final String STRING_ICON_LAYER_3 = "d";
	// Search Icons -- extra layers
	public static final String DISEASE_LAYER_1 = "e";
	public static final String DISEASE_LAYER_2 = "f";
	public static final String PUBMED_LAYER_1 = "g";
	public static final String PUBMED_LAYER_2 = "h";
	public static final String STITCH_LAYER_1 = "i";
	public static final String STITCH_LAYER_2 = "j";
	public static final String STRING_LAYER_1 = "k";
	public static final String STRING_LAYER_2 = "l";
	// EnrichmentMap Icon
	public static final String EM_ICON_LAYER_1 = "m";
	public static final String EM_ICON_LAYER_2 = "n";
	public static final String EM_ICON_LAYER_3 = "o";
	
	public static final String[] LAYERED_STRING_ICON = new String[] { STRING_ICON_LAYER_1, STRING_ICON_LAYER_2, STRING_ICON_LAYER_3 };
	public static final Color[] STRING_COLORS = new Color[] { new Color(163, 172, 216), Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK };
	
	public static final String[] DISEASE_LAYERS = new String[] { STRING_ICON_LAYER_1, STRING_ICON_LAYER_2, STRING_ICON_LAYER_3, DISEASE_LAYER_1, DISEASE_LAYER_2 };
	public static final String[] PUBMED_LAYERS = new String[] { STRING_ICON_LAYER_1, STRING_ICON_LAYER_2, STRING_ICON_LAYER_3, PUBMED_LAYER_1, PUBMED_LAYER_2 };
	public static final String[] STITCH_LAYERS = new String[] { STRING_ICON_LAYER_1, STRING_ICON_LAYER_2, STRING_ICON_LAYER_3, STITCH_LAYER_1, STITCH_LAYER_2 };
	public static final String[] STRING_LAYERS = new String[] { STRING_ICON_LAYER_1, STRING_ICON_LAYER_2, STRING_ICON_LAYER_3, STRING_LAYER_1, STRING_LAYER_2 };
	
	public static final String[] LAYERED_EM_ICON = new String[] { EM_ICON_LAYER_1, EM_ICON_LAYER_2, EM_ICON_LAYER_3 };
	public static final Color[] EM_COLORS = new Color[] { Color.WHITE, new Color(31, 120, 180), new Color(52, 160, 44) };
	
	private static Font iconFont;

	static {
		try {
			iconFont = Font.createFont(Font.TRUETYPE_FONT, IconUtils.class.getResourceAsStream("/fonts/string.ttf"));
		} catch (FontFormatException e) {
			throw new RuntimeException();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public static Font getIconFont(float size) {
		return iconFont.deriveFont(size);
	}

	private IconUtils() {
		// ...
	}
}
