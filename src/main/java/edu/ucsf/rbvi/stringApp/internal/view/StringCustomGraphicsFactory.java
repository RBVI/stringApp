package edu.ucsf.rbvi.stringApp.internal.view;

import java.net.URL;

import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class StringCustomGraphicsFactory implements CyCustomGraphicsFactory<StringLayer> {
	StringManager manager;

	public StringCustomGraphicsFactory(StringManager manager) {
		this.manager = manager;
	}

	public CyCustomGraphics<StringLayer> getInstance(String input) {
		return new StringCustomGraphics(manager, input);
	}

	public CyCustomGraphics<StringLayer> getInstance(URL url) { return null; }

	public Class<? extends CyCustomGraphics> getSupportedClass() {return StringCustomGraphics.class;}

	public CyCustomGraphics<StringLayer> parseSerializableString(String string) { return null; }

	public boolean supportsMime(String mimeType) { return false; }

	public String getPrefix() { return "string"; }

}
