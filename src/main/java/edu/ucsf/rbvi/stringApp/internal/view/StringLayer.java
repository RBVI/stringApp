package edu.ucsf.rbvi.stringApp.internal.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.Cy2DGraphicLayer;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class StringLayer implements Cy2DGraphicLayer {
	StringManager manager;
	Rectangle2D bounds;
	BufferedImage image;
	boolean useNewNodeEffect;

	public StringLayer(StringManager manager, BufferedImage image) {
		this.manager = manager;
		this.image = image;
		if (image != null)
			bounds = new Rectangle2D.Double(0.0, 0.0, (double)image.getWidth(), (double)image.getHeight());
		else
			bounds = new Rectangle2D.Double(0.0, 0.0, 150, 150);

		useNewNodeEffect = manager.showNewNodeEffect();
	}

	public void draw(Graphics2D g, Shape shape,
	                 CyNetworkView networkView, View<? extends CyIdentifiable> view) {
		if (! (view.getModel() instanceof CyNode) ) return;
		CyNetwork network = networkView.getModel();
		CyNode node = (CyNode)view.getModel();
		boolean usePill = ModelUtils.isCompound(network, node);

		Paint fill = view.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
		Paint background = networkView.getVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT);
		boolean selected = false;
		if (network.getRow(view.getModel()).get(CyNetwork.SELECTED, Boolean.class))
			selected = true;
		if (usePill) {
			DrawPill ds = new DrawPill((Color)fill, (Color)background, image, shape, selected, useNewNodeEffect);
			ds.draw(g, bounds);
		} else {
			DrawSphere ds = new DrawSphere((Color)fill, (Color)background, image, shape, selected, useNewNodeEffect);
			ds.draw(g, bounds);
		}
	}

	public Rectangle2D getBounds2D() { return bounds; }

	public Paint getPaint(Rectangle2D bounds) { return null; }

	public CustomGraphicLayer transform(AffineTransform xform) { 
		final Shape s = xform.createTransformedShape(bounds);
		bounds = s.getBounds2D();

		return this;
	}

}
