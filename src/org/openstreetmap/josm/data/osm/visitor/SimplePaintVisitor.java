// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;

import java.util.Iterator;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.ColorHelper;

/**
 * A visitor that paints a simple scheme of every primitive it visits to a 
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
	protected Color dfltWayColor;
	protected Color untaggedWayColor;
	protected Color incompleteColor;
	protected Color backgroundColor;
    protected boolean showDirectionArrow;
    protected boolean showRelevantDirectionsOnly;
	protected boolean showOrderNumber;
	
	/**
	 * Draw subsequent segments of same color as one Path
	 */
	protected Color currentColor = null;
	protected GeneralPath currentPath = new GeneralPath();

	Rectangle bbox = new Rectangle();

	public void visitAll(DataSet data) {
		inactiveColor = Preferences.getPreferencesColor("inactive", Color.DARK_GRAY);
		selectedColor = Preferences.getPreferencesColor("selected", Color.WHITE);
		nodeColor = Preferences.getPreferencesColor("node", Color.RED);
		dfltWayColor = Preferences.getPreferencesColor("way", darkblue);
		untaggedWayColor = Preferences.getPreferencesColor("untagged way", darkgreen);
		incompleteColor = Preferences.getPreferencesColor("incomplete way", darkerblue);
		backgroundColor = Preferences.getPreferencesColor("background", Color.BLACK);
		showDirectionArrow = Main.pref.getBoolean("draw.segment.direction");
		showRelevantDirectionsOnly = Main.pref.getBoolean("draw.segment.relevant_directions_only");
		showOrderNumber = Main.pref.getBoolean("draw.segment.order_number");
		
		// draw tagged ways first, then untagged ways. takes
		// time to iterate through list twice, OTOH does not
		// require changing the colour while painting...
		for (final OsmPrimitive osm : data.ways)
			if (!osm.deleted && !osm.selected && osm.tagged)
				osm.visit(this);
		displaySegments(null);

	    for (final OsmPrimitive osm : data.ways)
			if (!osm.deleted && !osm.selected && !osm.tagged)
				osm.visit(this);
		displaySegments(null);
	    
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
		if (n.incomplete) return;

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
	 * Draw a darkblue line for all segments.
	 * @param w The way to draw.
	 */
	public void visit(Way w) {
		if (w.incomplete) return;

                // show direction arrows, if draw.segment.relevant_directions_only is not set, the way is tagged with a direction key
                // (even if the tag is negated as in oneway=false) or the way is selected

                boolean showThisDirectionArrow = w.selected
                                                 || (showDirectionArrow
                                                     && (!showRelevantDirectionsOnly || w.hasDirectionKeys));
		Color wayColor;
		
		if (inactive) {
			wayColor = inactiveColor;
		} else if (!w.tagged) {
			wayColor = untaggedWayColor;
		} else {
			wayColor = dfltWayColor;
		}

		Iterator<Node> it = w.nodes.iterator();
		if (it.hasNext()) {
			Point lastP = nc.getPoint(it.next().eastNorth);
			for (int orderNumber = 1; it.hasNext(); orderNumber++) {
				Point p = nc.getPoint(it.next().eastNorth);
				drawSegment(lastP, p, w.selected && !inactive ? selectedColor : wayColor, showThisDirectionArrow);
				if (showOrderNumber)
					drawOrderNumber(lastP, p, orderNumber);
				lastP = p;
			}
		}
	}

	public void visit(Relation e) {
		// relations are not (yet?) drawn.
	}
	
	/**
	 * Draw an number of the order of the two consecutive nodes within the
	 * parents way
	 */
	protected void drawOrderNumber(Point p1, Point p2, int orderNumber) {
		int strlen = (""+orderNumber).length();
		int x = (p1.x+p2.x)/2 - 4*strlen;
		int y = (p1.y+p2.y)/2 + 4;

		if (isSegmentVisible(p1, p2)) {
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
        if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;

        g.setColor(color);

        if (n.tagged) {
            g.drawRect(p.x, p.y, 0, 0);
            g.drawRect(p.x-2, p.y-2, 4, 4);
        } else {
            g.drawRect(p.x-1, p.y-1, 2, 2);
        }
	}

	/**
	 * Draw a line with the given color.
	 */
	protected void drawSegment(Point p1, Point p2, Color col, boolean showDirection) {

		if (col != currentColor) displaySegments(col);
		
		if (isSegmentVisible(p1, p2)) {
			currentPath.moveTo(p1.x, p1.y);
			currentPath.lineTo(p2.x, p2.y);
			
			if (showDirection) {
				double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
				currentPath.lineTo((int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
				currentPath.moveTo((int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
				currentPath.lineTo(p2.x, p2.y);  
			}
		}
	}
        
    private boolean isSegmentVisible(Point p1, Point p2) {
        if ((p1.x < 0) && (p2.x < 0)) return false;
        if ((p1.y < 0) && (p2.y < 0)) return false;
        if ((p1.x > nc.getWidth()) && (p2.x > nc.getWidth())) return false;
        if ((p1.y > nc.getHeight()) && (p2.y > nc.getHeight())) return false;
        return true;
    }
	
	public void setGraphics(Graphics g) {
		this.g = g;
	}

	public void setNavigatableComponent(NavigatableComponent nc) {
    	this.nc = nc;
    }

	protected void displaySegments(Color newColor) {
		if (currentPath != null) {
			g.setColor(currentColor);
			((Graphics2D) g).draw(currentPath);
			currentPath = new GeneralPath();
			currentColor = newColor;
		}
	}
	
	/**
	 * Provided for backwards compatibility only.
	 * FIXME: remove this once not used by plugins any longer.
	 */
	public static Color getPreferencesColor(String name, Color dflt) {
		return Preferences.getPreferencesColor(name, dflt);
	}
}
