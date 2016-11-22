package edu.ucsf.rbvi.stringApp.internal.view;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class DrawPill {
	BufferedImage image;
	Color color;
	Color background;
	Shape nodeShape;
	boolean selected = false;
	float xScale = 1.0f;
	float yScale = 1.0f;
	float xOff = 0.0f;
	float yOff = 0.0f;

	public DrawPill(Color color, Color background, BufferedImage image, Shape nodeShape, boolean selected) {
		this.color = color;
		this.background = background;
		this.image = image;
		this.selected = selected;
		this.nodeShape = nodeShape;
	}


	public void draw(Graphics2D g2, Rectangle2D bounds) {
		Paint oldPaint = g2.getPaint();

		xScale = (float)((bounds.getWidth()/40.0)*1.1);
		yScale = (float)(bounds.getHeight()/40.0);
		xOff = (float)bounds.getX()-(float)bounds.getWidth()/2-xScale*1.0f;
		yOff = (float)bounds.getY()-(float)bounds.getHeight()/2;

		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING,
		                    RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		                    RenderingHints.VALUE_ANTIALIAS_ON);

		{
			// Painting a white background helps out the images
			g2.setPaint(Color.WHITE);
			fillPill(g2, xOff, yOff, xScale*40f, yScale*40f);
		}

		{
			// Paint a shadow
			// Change this to a radial gradient with a steep drop-off in the end:w
			Stops s1 = new Stops(3);
			s1.addStop(0.0f, "#000000", 0.6f);
			s1.addStop(0.98f, "#000000", 0.6f);
			s1.addStop(1.0f, "#000000", 0.0f);
			Paint p = new RadialGradientPaint(scaleX(20.0f), scaleY(28f), xScale*19f*2f, 
			                                  s1.getStops(), s1.getColors());
			g2.setPaint(p);
			g2.scale(1.05, 0.9);
			System.out.println("shadow");
			fillPill(g2, scaleX(1.0f), scaleY(9.5f), xScale*19f*2f, yScale*19f*2f);
			g2.scale(1.0/1.05, 1.0/0.9);
		}

		{
			Stops s1 = new Stops(8);
			s1.addStop( 0.2472f, "#fafafa");
			s1.addStop( 0.3381f, "#d0d0d0");
			s1.addStop( 0.4517f, "#a2a2a2");
			s1.addStop( 0.5658f, "#7c7c7c");
			s1.addStop( 0.6785f, "#5e5e5e");
			s1.addStop( 0.7893f, "#494949");
			s1.addStop( 0.8975f, "#3c3c3c");
			s1.addStop( 1.0f, "#383838");

			Paint p = new LinearGradientPaint(scaleX(20f), scaleY(40f), scaleX(20f), scaleY(0f), 
			                                  s1.getStops(), s1.getColors());
			g2.setPaint(p);
			fillPill(g2, xOff, yOff, xScale*40f, yScale*40f);
		}

		{
			Stops s2 = new Stops(12, 0.333f);
			s2.addStop(0f,"#FFFFFF");
			s2.addStop(0.3726f,"#FDFDFD");
			s2.addStop(0.5069f,"#F6F6F6");
			s2.addStop(0.6026f,"#EBEBEB");
			s2.addStop(0.68f,"#DADADA");
			s2.addStop(0.7463f,"#C4C4C4");
			s2.addStop(0.805f,"#A8A8A8");
			s2.addStop(0.8581f,"#888888");
			s2.addStop(0.9069f,"#626262");
			s2.addStop(0.9523f,"#373737");
			s2.addStop(0.9926f,"#090909");
			s2.addStop(1.0f,"#000000");
			Paint p = new RadialGradientPaint(scaleX(20f), scaleY(20f), yScale*75f, 
			                                  s2.getStops(), s2.getColors());
			g2.setPaint(p);
			fillPill(g2, xOff, yOff, xScale*40f, yScale*40f);
		}

		// Draw our image (if we have one);
		// For some reason, I can't make the compositing to work right.  The image is transparent,
		// but when I composite it, the translucent areas come out white.  Doesn't make sense to
		// me
		if (image != null) {
			g2.setClip(nodeShape);
			g2.drawImage(image, (int)xOff, (int)yOff, (int)bounds.getWidth(), (int)bounds.getHeight(), null);
			g2.setClip(null);
		}

		{
			// Color clr = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(((float)255)*0.4f));
			if (selected) {
				Stops s1 = new Stops(2, 0.6f);
				s1.addStop(0f, Color.YELLOW);
				s1.addStop(1f, Color.YELLOW);
				Paint p = new LinearGradientPaint(scaleX(-1f), scaleY(41f), scaleX(-1f), scaleY(-1f), 
				                                  s1.getStops(), s1.getColors());
				g2.setPaint(p);
				// Make the oval slightly larger
				fillPill(g2, xOff-1, yOff-1, xScale*42f, yScale*42f);
			} else {
				Stops s1 = new Stops(2, 0.4f);
				s1.addStop(0f, color);
				s1.addStop(1f, color);
				Paint p = new LinearGradientPaint(scaleX(0f), scaleY(40f), scaleX(0f), scaleY(0f), 
				                                  s1.getStops(), s1.getColors());
				g2.setPaint(p);
				fillPill(g2, xOff, yOff, xScale*40f, yScale*40f);
			}
		}

		{
			Stops s3 = new Stops(3);
			s3.addStop(0f, "#FFFFFF", 1.0f);
			s3.addStop(0.1f, "#FFFFFF", 0.99f);
			s3.addStop(1.0f, "#FFFFFF", 0f);
			Paint p = new LinearGradientPaint(scaleX(20f), scaleY(2f), scaleX(20f), scaleY(2f+12f), 
			                                  s3.getStops(), s3.getColors());
			g2.setPaint(p);
			fillPill(g2, scaleX(20f-11.5f), scaleY(2f), xScale*23f, yScale*12f);
		}

		// Restores the previous state
		g2.setPaint(oldPaint);
		// g2.setTransform(xform);
	}

	int scaleX(int value) {
		return (int)(((float)value*xScale)+xOff);
	}

	int scaleY(int value) {
		return (int)(((float)value*yScale)+yOff);
	}

	float scaleX(float value) {
		return value*xScale+xOff;
	}

	float scaleY(float value) {
		return value*yScale+yOff;
	}

	/**
	 * Draw a path that corresponds to the SVG: 
	 * 	"M485.5,182 A15,15 0 0,0 485.5,212 L530.5,212 A15,15 0 0,0 530.5,182 z"
	 * note that when moved to the origin, this becomes:
	 *  "M0.0,0.0 A15,15 0 0,0 0.0,30.0 L45.0,30.0 A15,15 0 0,0 45.0,0.0 z"
	 */
	private void fillPill(Graphics2D g2, float x, float y, float width, float height) {
		double wScale = width/75.0;
		double hScale = height/30.0;
		Path2D path = new Path2D.Double();

		double xStart = x+15*wScale; // Take into account the curve
		double yStart = y;

		// M485.5,182
		/*
		path.moveTo(xStart,yStart);
		path.lineTo(xStart,yStart+30.0*hScale);
		path.lineTo(xStart+45.0*wScale,yStart+30.0*hScale);
		path.lineTo(xStart+45.0*wScale,yStart);
		path.closePath();
		g2.fill(path);
		*/

		System.out.println("x,y="+x+","+y);
		System.out.println("width,height="+width+","+height);

		path.moveTo(xStart,yStart);
		// A15,15 0 0,0 485.5,212
		// Create an arc with radii 15,15 that starts at the current point and ends at 485.5,212
		Arc2D arc = new Arc2D.Double(x,y,30*wScale,30*hScale, 270, -180, Arc2D.OPEN);
		path.append(arc, true);
		Rectangle2D rect = new Rectangle2D.Double(xStart,y,45*wScale,30*hScale);
		path.append(rect, true);

		// L530.5,212
		// path.lineTo(xStart+45.0*wScale,y+30*hScale);
		Arc2D arc2 = new Arc2D.Double(x+45.0*wScale,y,30*wScale,30*hScale, 90, -180, Arc2D.OPEN);
		path.append(arc2, true);

		// path.lineTo(xStart+45.0*wScale,y);
		// path.lineTo(xStart, yStart);

		// A15,15 0 0,0 530.5,182
		// arc = new Arc2D.Double(new Rectangle2D.Double(x+45*wScale,y-30.0*hScale,15*wScale,30*hScale), 270, 180, Arc2D.OPEN);
		// path.append(arc, true);

		// z
		// path.closePath();

		// BasicStroke st = new BasicStroke(1.0f);
		// Shape s = st.createStrokedShape(path);
		g2.fill(path);

		//g2.fill(path);
	}

	class Stops {
		float[] stops;
		Color[] colors;
		float alpha;
		int stopCount;

		public Stops(int size) {
			this(size, 1.0f);
		}

		public Stops(int size, float alpha) {
			stops = new float[size];
			colors = new Color[size];
			stopCount = 0;
			this.alpha = alpha;
		}


		public void addStop(float stop, Color color, float opacity) {
			stops[stopCount] = stop;
			float stopAlpha = opacity*alpha;
			if (stopAlpha != 1.0f) {
				int alphav = (int)(255.0f*stopAlpha);
				colors[stopCount] = new Color(color.getRed(), color.getGreen(), color.getBlue(), alphav);
			} else {
				colors[stopCount] = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
			}
			stopCount++;
		}

		public void addStop(float stop, Color color) {
			addStop(stop, color, 1.0f);
		}

		public void addStop(float stop, int color) {
			Color c = new Color(color);
			addStop(stop, c);
		}

		public void addStop(float stop, String clr) {
			addStop(stop, clr, 1.0f);
		}

		public void addStop(float stop, String clr, float opacity) {
			Integer clrInt;
			if (clr.startsWith("#")) {
				clrInt = Integer.parseInt(clr.substring(1), 16);
			} else {
				clrInt = Integer.parseInt(clr);
			}
			addStop(stop, new Color(clrInt), opacity);
		}

		public float[] getStops() { return stops; }
		public Color[] getColors() { return colors; }

	}

}
