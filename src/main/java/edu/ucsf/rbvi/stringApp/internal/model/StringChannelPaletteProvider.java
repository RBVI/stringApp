package edu.ucsf.rbvi.stringApp.internal.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.Palette;
import org.cytoscape.util.color.PaletteProvider;
import org.cytoscape.util.color.PaletteType;

public class StringChannelPaletteProvider implements PaletteProvider {
	Palette[] palettes = null;
	public StringChannelPaletteProvider() {
		palettes = new Palette[]{new StringChannelPalette()};
	}

	public Palette getPalette(Object paletteIdentifier) {
		for (Palette palette: palettes) {
			if (palette.getIdentifier().equals(paletteIdentifier))
				return palette;
		}
		return null;
	}

	public Palette getPalette(Object paletteIdentifier, int size) {
		for (Palette palette: palettes) {
			if (palette.getIdentifier().equals(paletteIdentifier))
				return palette;
		}
		return null;
	}

	public Palette getPalette(String paletteName) {
		for (Palette palette: palettes) {
			if (palette.getName().equals(paletteName))
				return palette;
		}
		return null;
	}

	public Palette getPalette(String paletteName, int size) {
		for (Palette palette: palettes) {
			if (palette.getName().equals(paletteName))
				return palette;
		}
		return null;
	}

	public List<PaletteType> getPaletteTypes() {
		return Collections.singletonList(BrewerType.QUALITATIVE);
	}

	public String getProviderName() { return "STRING"; }

	public List<Object> listPaletteIdentifiers(PaletteType type, boolean colorBlindSafe) {
		List<Object> paletteIds = new ArrayList<>();
		if (type != BrewerType.QUALITATIVE) return paletteIds;
		for (Palette palette: palettes) {
			paletteIds.add(palette.getIdentifier());
		}
		return paletteIds;
	}

	public List<String> listPaletteNames(PaletteType type, boolean colorBlindSave) {
		List<String> paletteIds = new ArrayList<>();
		if (type != BrewerType.QUALITATIVE) return paletteIds;
		for (Palette palette: palettes) {
			paletteIds.add(palette.getName());
		}
		return paletteIds;
	}

	class StringChannelPalette implements Palette {
		Color[] colors = new Color[] {
						Color.CYAN, Color.MAGENTA, Color.GREEN, Color.RED, Color.BLUE,
						new Color(199,234,70), Color.BLACK, new Color(153,153,255) };
		public Color[] getColors() { return colors; }
		public Color[] getColors(int nColors) { return colors; }
		public Object getIdentifier() { return "default channel colors"; }
		public PaletteType getType() { return BrewerType.QUALITATIVE; }
		public boolean isColorBlindSafe() { return false; }
		public int size() { return 8; }
		public String toString() { return "default channel colors"; }
		public String getName() { return "default"; }
	}

}
