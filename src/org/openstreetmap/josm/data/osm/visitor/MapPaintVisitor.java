// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.IconElemStyle;
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.tools.ColorHelper;

public class MapPaintVisitor implements Visitor {
	
	protected boolean useRealWidth;
	protected boolean zoomLevelDisplay;
	protected boolean fillAreas;
	protected int fillAlpha;
	protected Color untaggedColor;
	protected Color textColor;
	protected boolean currentDashed = false;
	protected int currentWidth = 0;
	protected Stroke currentStroke = null;    
	protected static final Font orderFont = new Font("Helvetica", Font.PLAIN, 8);
	
	public boolean inactive;
	
	/**
	 * The environment to paint to.
	 */
	protected Graphics g;
	
	/**
	 * MapView to get screen coordinates.
	 */
	protected NavigatableComponent nc;
	
	/**
	 * Draw subsequent segments of same color as one Path
	 */
	protected Color currentColor = null;
	protected GeneralPath currentPath = new GeneralPath();
	
	protected static final double PHI = Math.toRadians(20);
	
	/**
	 * Preferences
	*/
	protected Color inactiveColor;
	protected Color selectedColor;
	protected Color nodeColor;
	protected Color dfltWayColor;
	protected Color untaggedWayColor;
	protected Color incompleteColor;
	protected Color backgroundColor;
	protected boolean showDirectionArrow;
    protected boolean showRelevantDirectionsOnly;
	protected boolean showOrderNumber;
	
	public final static Color darkerblue = new Color(0,0,96);
	public final static Color darkblue = new Color(0,0,128);
	
	protected boolean isZoomOk(ElemStyle e) {
		double circum = Main.map.mapView.getScale()*100*Main.proj.scaleFactor()*40041455; // circumference of the earth in meter

		/* show everything if the user wishes so */
		if (!zoomLevelDisplay) {
			return true;
		}

		if (e == null) {
			/* the default for things that don't have a rule (show, if scale is smaller than 1500m) */
			if (circum < 1500)
				return true;
			return false;
		}

		// formula to calculate a map scale: natural size / map size = scale
		// example: 876000mm (876m as displayed) / 22mm (roughly estimated screen size of legend bar) = 39818
		//
		// so the exact "correcting value" below depends only on the screen size and resolution
		// XXX - do we need a Preference setting for this (if things vary widely)?
		/*System.out.println(
   "Circum: " + circum + 
   " max: " + e.getMaxScale() + "(" + e.getMaxScale()/22 + ")" +
   " min:" + e.getMinScale() + "(" + e.getMinScale()/22 + ")");*/
		if(circum>=e.getMaxScale() / 22 || circum<e.getMinScale() / 22)
			return false;
		return true;
	}

	/**
	 * Draw a small rectangle.
	 * White if selected (as always) or red otherwise.
	 *
	 * @param n The node to draw.
	 */
	public void visit(Node n) {
		ElemStyle nodeStyle = MapPaintStyles.getStyle(n);
		if (nodeStyle!=null) {
			if (nodeStyle instanceof IconElemStyle) {
				if (isZoomOk(nodeStyle)) {
					drawNode(n, ((IconElemStyle)nodeStyle).getIcon(), ((IconElemStyle)nodeStyle).doAnnotate());
				}
			} else {
				// throw some sort of exception
			}
		} else {
			drawNode(n, n.selected ? selectedColor : nodeColor);
		}
	}

	/**
	 * Draw a line for all segments, according to tags.
	 * @param w The way to draw.
	 */
	public void visit(Way w) {
		double circum = Main.map.mapView.getScale()*100*Main.proj.scaleFactor()*40041455; // circumference of the earth in meter
                // show direction arrows, if draw.segment.relevant_directions_only is not set, the way is tagged with a direction key
                // (even if the tag is negated as in oneway=false) or the way is selected
		boolean showDirection = w.selected ||
                                        ((!useRealWidth) && (showDirectionArrow
                                                             && (!showRelevantDirectionsOnly || w.hasDirectionKeys)));

		Color colour = untaggedColor;
		int width = 2;
		int realWidth = 0; //the real width of the element in meters 
		boolean dashed = false;
		boolean area = false;
		ElemStyle wayStyle = MapPaintStyles.getStyle(w);

		if(!isZoomOk(wayStyle)) {
			return;
		}

		if(wayStyle!=null)
		{
			if(wayStyle instanceof LineElemStyle)
			{
				colour = ((LineElemStyle)wayStyle).colour;
				width = ((LineElemStyle)wayStyle).width;
				realWidth = ((LineElemStyle)wayStyle).realWidth; 
				dashed = ((LineElemStyle)wayStyle).dashed;
			}
			else if (wayStyle instanceof AreaElemStyle)
			{
				colour = ((AreaElemStyle)wayStyle).getColour();
				area = true;
			}
		}

		if (area && fillAreas)
			drawWayAsArea(w, colour);
		int orderNumber = 0;

		Node lastN = null;
		for (Node n : w.nodes) {
			if (lastN == null) {
				lastN = n;
				continue;
			}
			orderNumber++;
			//  drawSegment(lastN, n, w.selected && !inactive ? selectedColor : wayColor, showDirectionArrow);

			if (area && fillAreas)
				//Draw segments in a different colour so direction arrows show against the fill
				drawSeg(lastN, n, w.selected ? selectedColor : untaggedColor, showDirection, width, true);
			else
				if (area)
					drawSeg(lastN, n, w.selected ? selectedColor : colour, showDirection, width, true);
				else
					if (realWidth > 0 && useRealWidth && !showDirection){
						int tmpWidth = (int) (100 /  (float) (circum / realWidth));
						if (tmpWidth > width) width = tmpWidth;
					}

			drawSeg(lastN, n, w.selected ? selectedColor : colour, showDirection, width, dashed);

			if (showOrderNumber)
				drawOrderNumber(lastN, n, orderNumber);

			lastN = n;
		}
	}

	public void visit(Relation e) {
		// relations are not (yet?) drawn.
	}
	
	// This assumes that all segments are aligned in the same direction!
	protected void drawWayAsArea(Way w, Color colour)
	{
		Polygon polygon = new Polygon();
		Point p;
		// set the opacity (alpha) level of the filled polygon
		Color coloura = new Color( colour.getRed(), colour.getGreen(), colour.getBlue(), fillAlpha);

		for (Node n : w.nodes)
		{
			p = nc.getPoint(n.eastNorth);
			polygon.addPoint(p.x,p.y);
		}

		g.setColor( w.selected ?
				selectedColor : coloura);

		g.fillPolygon(polygon);
	}

	// NEW
	protected void drawNode(Node n, ImageIcon icon, boolean annotate) {
		Point p = nc.getPoint(n.eastNorth);
		if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;
		int w = icon.getIconWidth(), h=icon.getIconHeight();
		icon.paintIcon ( Main.map.mapView, g, p.x-w/2, p.y-h/2 );
		String name = (n.keys==null) ? null : n.keys.get("name");
		if (name!=null && annotate)
		{
			g.setColor(textColor);
			Font defaultFont = g.getFont();
			g.setFont (orderFont);
			g.drawString (name, p.x+w/2+2, p.y+h/2+2);
			g.setFont(defaultFont);
		}
		if (n.selected)
		{
			g.setColor (  selectedColor );
			g.drawRect (p.x-w/2-2,p.y-w/2-2, w+4, h+4);
		}
	}

	/**
	 * Draw a line with the given color.
	 */
	protected void drawSegment(Node n1, Node n2, Color col, boolean showDirection) {
		if (useRealWidth && showDirection) showDirection = false;
		drawSeg(n1, n2, col, showDirection, 1, false);
	}

	private void drawSeg(Node n1, Node n2, Color col, boolean showDirection, int width, boolean dashed) {
		if (col != currentColor || width != currentWidth || dashed != currentDashed) {
			displaySegments(col, width, dashed);
		}
		Point p1 = nc.getPoint(n1.eastNorth);
		Point p2 = nc.getPoint(n2.eastNorth);

		// checking if this segment is visible
		if ((p1.x < 0) && (p2.x < 0)) return ;
		if ((p1.y < 0) && (p2.y < 0)) return ;
		if ((p1.x > nc.getWidth()) && (p2.x > nc.getWidth())) return ;
		if ((p1.y > nc.getHeight()) && (p2.y > nc.getHeight())) return ;
		//if (ls.selected)
		// col = selectedColor;
		//g.setColor(col);
		//g.setWidth(width);
		//if (dashed) 
		// g2d.setStroke(new BasicStroke(width,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,0,new float[] {9},0));
		//else 
		// g2d.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));

		//g.drawLine(p1.x, p1.y, p2.x, p2.y);
		currentPath.moveTo(p1.x, p1.y);
		currentPath.lineTo(p2.x, p2.y);

		if (showDirection) {
			double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
			//g.drawLine(p2.x,p2.y, (int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
			//g.drawLine(p2.x,p2.y, (int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
			currentPath.lineTo((int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
			currentPath.moveTo((int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
			currentPath.lineTo(p2.x, p2.y);
		}
		//g2d.setStroke(new BasicStroke(1));

	}

	protected void displaySegments() {
		displaySegments(null, 0, false);
	}

	protected void displaySegments(Color newColor, int newWidth, boolean newDash) {

		if (currentPath != null) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.setColor(inactive ? inactiveColor : currentColor);
			if (currentStroke == null) {
				if (currentDashed)
					g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,0,new float[] {9},0));
				else 
					g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
			}
			g2d.draw(currentPath);
			g2d.setStroke(new BasicStroke(1));

			currentPath = new GeneralPath();
			currentColor = newColor;
			currentWidth = newWidth;
			currentDashed = newDash;
			currentStroke = null;
		}
	}

	/**
	 * Draw the node as small rectangle with the given color.
	 *
	 * @param n  The node to draw.
	 * @param color The color of the node.
	 */
	public void drawNode(Node n, Color color) {
		if(isZoomOk(null)) {
			Point p = nc.getPoint(n.eastNorth);
			if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;
			g.setColor(color);
			g.drawRect(p.x-1, p.y-1, 2, 2);
		}
	}
	
	public static Color getPreferencesColor(String colName, Color def) {
		String colStr = Main.pref.get("color."+colName);
		if (colStr.equals("")) {
			Main.pref.put("color."+colName, ColorHelper.color2html(def));
			return def;
		}
		return ColorHelper.html2color(colStr);
	}

	// NW 111106 Overridden from SimplePaintVisitor in josm-1.4-nw1
	// Shows areas before non-areas
	public void visitAll(DataSet data) {
		inactiveColor = getPreferencesColor("inactive", Color.DARK_GRAY);
		selectedColor = getPreferencesColor("selected", Color.YELLOW);
		nodeColor = getPreferencesColor("node", Color.RED);
		dfltWayColor = getPreferencesColor("way", darkblue);
		incompleteColor = getPreferencesColor("incomplete way", darkerblue);
		backgroundColor = getPreferencesColor("background", Color.BLACK);
		untaggedColor = getPreferencesColor("untagged",Color.GRAY);
		textColor = getPreferencesColor ("text", Color.WHITE);
		showDirectionArrow = Main.pref.getBoolean("draw.segment.direction");
		showRelevantDirectionsOnly = Main.pref.getBoolean("draw.segment.relevant_directions_only");
		showOrderNumber = Main.pref.getBoolean("draw.segment.order_number");
		useRealWidth = Main.pref.getBoolean("mappaint.useRealWidth",false);
		zoomLevelDisplay = Main.pref.getBoolean("mappaint.zoomLevelDisplay",false);
		fillAreas = Main.pref.getBoolean("mappaint.fillareas", true);

		/* XXX - there must be a better way to get a bounded Integer pref! */
		try {
			fillAlpha = Integer.valueOf(Main.pref.get("mappaint.fillalpha", "50"));
			if (fillAlpha < 0) {
				fillAlpha = 0;
			}
			if (fillAlpha > 255) {
				fillAlpha = 255;
			}
		} catch (NumberFormatException nfe) {
			fillAlpha = 50;
		}

		Collection<Way> noAreaWays = new LinkedList<Way>();

		for (final OsmPrimitive osm : data.ways)
			if (!osm.incomplete && !osm.deleted && MapPaintStyles.isArea(osm))
				osm.visit(this);
			else if (!osm.deleted && !osm.incomplete)
				noAreaWays.add((Way)osm);

		for (final OsmPrimitive osm : noAreaWays)
			osm.visit(this);

		for (final OsmPrimitive osm : data.nodes)
			if (!osm.incomplete && !osm.deleted)
				osm.visit(this);

		for (final OsmPrimitive osm : data.getSelected())
			if (!osm.incomplete && !osm.deleted){
				osm.visit(this);
			}
		displaySegments();
	}
	
	/**
	 * Draw an number of the order of the two consecutive nodes within the
	 * parents way
	 */
	protected void drawOrderNumber(Node n1, Node n2, int orderNumber) {
		int strlen = (""+orderNumber).length();
		Point p1 = nc.getPoint(n1.eastNorth);
		Point p2 = nc.getPoint(n2.eastNorth);
		int x = (p1.x+p2.x)/2 - 4*strlen;
		int y = (p1.y+p2.y)/2 + 4;

		Rectangle screen = g.getClipBounds();
		if (screen.contains(x,y)) {
			Color c = g.getColor();
			g.setColor(backgroundColor);
			g.fillRect(x-1, y-12, 8*strlen+1, 14);
			g.setColor(c);
			g.drawString(""+orderNumber, x, y);
		}
    }
	
	public void setGraphics(Graphics g) {
    	this.g = g;
    }

	public void setNavigatableComponent(NavigatableComponent nc) {
    	this.nc = nc;
    }
}
