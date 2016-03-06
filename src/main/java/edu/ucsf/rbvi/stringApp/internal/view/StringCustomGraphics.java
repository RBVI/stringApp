package edu.ucsf.rbvi.stringApp.internal.view;

import java.awt.Image;
import java.util.Collections;
import java.util.List;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class StringCustomGraphics implements CyCustomGraphics<StringLayer> {
	StringManager manager;
	String input;
	Long id = null;
	float fitRatio = 1.0f;
	List<StringLayer> layers;
	String displayName;
	int width = 50;
	int height = 50;
	BufferedImage bi = null;

	public StringCustomGraphics(StringManager manager, String input) {
		this.manager = manager;
		this.input = input;
		if (input != null && input.length() > 0) {
			if (input.startsWith("data:")) {
				input = input.substring(input.indexOf(","));
			}
			byte[] byteStream = Base64.decodeBase64(input);
			try {
				ByteArrayInputStream bis = new ByteArrayInputStream(byteStream);
				bi = ImageIO.read(bis);
				bis.close();
			} catch (Exception e) { 
				bi = null;
				e.printStackTrace(); 
			}
		}
	}

	public Long getIdentifier() { return id; }

	public void setIdentifier(Long id) { this.id = id; }

	public void setWidth(final int width) { this.width = width; }
 	public void setHeight(final int height) { this.height = height; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public List<StringLayer> getLayers(CyNetworkView networkView, View<? extends CyIdentifiable> nodeView) { 
		if (manager.showImage())
			return Collections.singletonList(new StringLayer(manager, bi)); 
		else
			return Collections.singletonList(new StringLayer(manager, null)); 
	}
	public String getDisplayName() { return displayName; }
	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}
	public float getFitRatio() { return fitRatio; }
	public void setFitRatio(float fitRatio) { this.fitRatio = fitRatio; }
	public String toString() {
		if (displayName != null)
			return displayName;
		return "String Custom Graphics";
	}

	public Image getRenderedImage() {return null;}
	public String toSerializableString() {return null;}


}
