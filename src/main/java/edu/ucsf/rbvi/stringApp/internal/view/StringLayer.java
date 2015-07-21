package edu.ucsf.rbvi.stringApp.internal.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.Cy2DGraphicLayer;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class StringLayer implements Cy2DGraphicLayer {
	StringManager manager;
	Rectangle2D bounds;
	Image image;

	public StringLayer(StringManager manager, Image image) {
		this.manager = manager;
		bounds = new Rectangle(0, 0, 100, 100);
		this.image = image;
	}

	public void draw(Graphics2D g, Shape shape,
	                 CyNetworkView networkView, View<? extends CyIdentifiable> view) {
		if (! (view.getModel() instanceof CyNode) ) return;

		Paint fill = view.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
		Paint background = networkView.getVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT);
		DrawSphere ds = new DrawSphere((Color)fill, (Color)background, image);
		ds.draw(g, bounds);
	}

	public Rectangle2D getBounds2D() { return bounds; }

	public Paint getPaint(Rectangle2D bounds) { return null; }

	public CustomGraphicLayer transform(AffineTransform xform) { 
		final Shape s = xform.createTransformedShape(bounds);
		bounds = s.getBounds2D();

		return this;
	}

}
