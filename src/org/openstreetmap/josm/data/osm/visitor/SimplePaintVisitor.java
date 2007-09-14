// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.ColorHelper;

/**
 * A visitor that paint a simple scheme of every primitive it visits to a 
 * previous set graphic environment.
 * 
 * @author imi
 */
public class SimplePaintVisitor implements Visitor {

	public final static Color darkerblue = new Color(0,0,96);
	public final static Color darkblue = new Color(0,0,128);
	public final static Color darkgreen = new Color(0,128,0);
	
	/**
	 * The environment to paint to.
	 */
	protected Graphics g;
	/**
	 * MapView to get screen coordinates.
	 */
	protected NavigatableComponent nc;
	
	public boolean inactive;
	
	protected static final double PHI = Math.toRadians(20);

	/**
	 * Preferences
	 */
	protected Color inactiveColor;
	protected Color selectedColor;
	protected Color nodeColor;
	protected Color segmentColor;
	protected Color dfltWayColor;
	protected Color incompleteColor;
	protected Color backgroundColor;
	protected boolean showDirectionArrow;
	protected boolean showOrderNumber;
	
	/**
	 * Draw subsequent segments of same color as one Path
	 */
	protected Color currentColor = null;
	protected GeneralPath currrentPath = new GeneralPath();
	
	public void visitAll(DataSet data) {
		inactiveColor = getPreferencesColor("inactive", Color.DARK_GRAY);
		selectedColor = getPreferencesColor("selected", Color.WHITE);
		nodeColor = getPreferencesColor("node", Color.RED);
		segmentColor = getPreferencesColor("segment", darkgreen);
		dfltWayColor = getPreferencesColor("way", darkblue);
		incompleteColor = getPreferencesColor("incomplete way", darkerblue);
		backgroundColor = getPreferencesColor("background", Color.BLACK);
		showDirectionArrow = Main.pref.getBoolean("draw.segment.direction");
		showOrderNumber = Main.pref.getBoolean("draw.segment.order_number");
		
		for (final OsmPrimitive osm : data.segments)
			if (!osm.deleted && !osm.selected)
				osm.visit(this);
		for (final OsmPrimitive osm : data.ways)
			if (!osm.deleted && !osm.selected)
				osm.visit(this);
		displaySegments(null);	// Flush segment cache before nodes
		for (final OsmPrimitive osm : data.nodes)
			if (!osm.deleted && !osm.selected)
				osm.visit(this);
		for (final OsmPrimitive osm : data.getSelected())
			if (!osm.deleted)
				osm.visit(this);
		displaySegments(null);
	}

	/**
	 * Draw a small rectangle. 
	 * White if selected (as always) or red otherwise.
	 * 
	 * @param n The node to draw.
	 */
	public void visit(Node n) {
		Color color = null;
		if (inactive)
			color = inactiveColor;
		else if (n.selected)
			color = selectedColor;
		else
			color = nodeColor;
		drawNode(n, color);
	}

	/**
	 * Draw just a line between the points.
	 * White if selected (as always) or green otherwise.
	 */
	public void visit(Segment ls) {
		Color color;
		if (inactive)
			color = inactiveColor;
		else if (ls.selected)
			color = selectedColor;
		else
			color = segmentColor;
		drawSegment(ls, color, showDirectionArrow);
	}

	/**
	 * Draw a darkblue line for all segments.
	 * @param w The way to draw.
	 */
	public void visit(Way w) {
		Color wayColor;
		if (inactive)
			wayColor = inactiveColor;
		else {
			wayColor = dfltWayColor;
			for (Segment ls : w.segments) {
				if (ls.incomplete) {
					wayColor = incompleteColor;
					break;
				}
			}
		}

		int orderNumber = 0;
		for (Segment ls : w.segments) {
			orderNumber++;
			if (!ls.selected) // selected already in good color
				drawSegment(ls, w.selected && !inactive ? selectedColor : wayColor, showDirectionArrow);
			if (!ls.incomplete && showOrderNumber)
				drawOrderNumber(ls, orderNumber);
		}
	}

	/**
	 * Draw an number of the order of the segment within the parents way
	 */
	protected void drawOrderNumber(Segment ls, int orderNumber) {
		int strlen = (""+orderNumber).length();
		Point p1 = nc.getPoint(ls.from.eastNorth);
		Point p2 = nc.getPoint(ls.to.eastNorth);
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

	/**
	 * Draw the node as small rectangle with the given color.
	 *
	 * @param n		The node to draw.
	 * @param color The color of the node.
	 */
	public void drawNode(Node n, Color color) {
		Point p = nc.getPoint(n.eastNorth);
		g.setColor(color);
		Rectangle screen = g.getClipBounds();

		if ( screen.contains(p.x, p.y) )
			g.drawRect(p.x-1, p.y-1, 2, 2);
	}

	/**
	 * Draw a line with the given color.
	 */
	protected void drawSegment(Segment ls, Color col, boolean showDirection) {
		if (ls.incomplete)
			return;
		if (col != currentColor) {
			displaySegments(col);
		}
		
		Point p1 = nc.getPoint(ls.from.eastNorth);
		Point p2 = nc.getPoint(ls.to.eastNorth);
		
		Rectangle screen = g.getClipBounds();		
		Line2D line = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
		if (screen.contains(p1.x, p1.y, p2.x, p2.y) || screen.intersectsLine(line))
		{
			currrentPath.moveTo(p1.x, p1.y);
			currrentPath.lineTo(p2.x, p2.y);
	
			if (showDirection) {
				double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
				currrentPath.lineTo((int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
				currrentPath.moveTo((int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
				currrentPath.lineTo(p2.x, p2.y);			}
		}
	}
	
	protected void displaySegments(Color newColor) {
		if (currrentPath != null) {
			g.setColor(currentColor);
			((Graphics2D) g).draw(currrentPath);
			currrentPath = new GeneralPath();
			currentColor = newColor;
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


	public void setGraphics(Graphics g) {
    	this.g = g;
    }

	public void setNavigatableComponent(NavigatableComponent nc) {
    	this.nc = nc;
    }
}
