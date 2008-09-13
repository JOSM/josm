// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.Iterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.NavigatableComponent;

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
	public final static Color teal = new Color(0,128,128);

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
	protected Color relationColor;
	protected Color untaggedWayColor;
	protected Color incompleteColor;
	protected Color backgroundColor;
	protected boolean showDirectionArrow;
	protected boolean showRelevantDirectionsOnly;
	protected boolean showOrderNumber;
	protected boolean fillSelectedNode;
	protected boolean fillUnselectedNode;
	protected int selectedNodeRadius;
	protected int unselectedNodeRadius;
	protected int selectedNodeSize;
	protected int unselectedNodeSize;
	protected int defaultSegmentWidth = 2;
	protected int virtualNodeSize;
	protected int virtualNodeSpace;
	protected int segmentNumberSpace;
	protected int taggedNodeRadius;
	protected int taggedNodeSize;

	/**
	 * Draw subsequent segments of same color as one Path
	 */
	protected Color currentColor = null;
	protected GeneralPath currentPath = new GeneralPath();

	Rectangle bbox = new Rectangle();

	protected void getSettings(Boolean virtual) {
		inactiveColor = Main.pref.getColor(marktr("inactive"), Color.DARK_GRAY);
		selectedColor = Main.pref.getColor(marktr("selected"), Color.WHITE);
		nodeColor = Main.pref.getColor(marktr("node"), Color.RED);
		dfltWayColor = Main.pref.getColor(marktr("way"), darkblue);
		relationColor = Main.pref.getColor(marktr("relation"), teal);
		untaggedWayColor = Main.pref.getColor(marktr("untagged way"), darkgreen);
		incompleteColor = Main.pref.getColor(marktr("incomplete way"), darkerblue);
		backgroundColor = Main.pref.getColor(marktr("background"), Color.BLACK);
		showDirectionArrow = Main.pref.getBoolean("draw.segment.direction");
		showRelevantDirectionsOnly = Main.pref.getBoolean("draw.segment.relevant_directions_only");
		showOrderNumber = Main.pref.getBoolean("draw.segment.order_number");
		selectedNodeRadius = Main.pref.getInteger("mappaint.node.selected-size", 5) / 2;
		selectedNodeSize = selectedNodeRadius * 2;
		unselectedNodeRadius = Main.pref.getInteger("mappaint.node.unselected-size", 3) / 2;
		unselectedNodeSize = unselectedNodeRadius * 2;
		taggedNodeRadius = Main.pref.getInteger("mappaint.node.tagged-size", 5) / 2;
		taggedNodeSize = taggedNodeRadius * 2;
		defaultSegmentWidth = Main.pref.getInteger("mappaint.segment.default-width", 2);
		fillSelectedNode = Main.pref.getBoolean("mappaint.node.fill-selected", true);
		fillUnselectedNode = Main.pref.getBoolean("mappaint.node.fill-unselected", false);
		virtualNodeSize = virtual ? Main.pref.getInteger("mappaint.node.virtual-size", 6) / 2 : 0;
		virtualNodeSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
		segmentNumberSpace = Main.pref.getInteger("mappaint.segmentnumber.space", 40);

		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			Main.pref.getBoolean("mappaint.use-antialiasing", false) ?
			RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
	}

	public void visitAll(DataSet data, Boolean virtual) {
		getSettings(virtual);
		// draw tagged ways first, then untagged ways. takes
		// time to iterate through list twice, OTOH does not
		// require changing the colour while painting...
		for (final OsmPrimitive osm : data.relations)
			if (!osm.deleted && !osm.selected)
				osm.visit(this);

		for (final OsmPrimitive osm : data.ways)
			if (!osm.deleted && !osm.selected && osm.tagged)
				osm.visit(this);
		displaySegments();

		for (final OsmPrimitive osm : data.ways)
			if (!osm.deleted && !osm.selected && !osm.tagged)
				osm.visit(this);
		displaySegments();

		for (final OsmPrimitive osm : data.getSelected())
			if (!osm.deleted)
				osm.visit(this);
		displaySegments();

		for (final OsmPrimitive osm : data.nodes)
			if (!osm.deleted && !osm.selected)
				osm.visit(this);
		if(virtualNodeSize != 0)
		{
			currentColor = nodeColor;
			for (final OsmPrimitive osm : data.ways)
				if (!osm.deleted)
					visitVirtual((Way)osm);
			displaySegments();
		}
	}

	/**
	 * Draw a small rectangle.
	 * White if selected (as always) or red otherwise.
	 *
	 * @param n The node to draw.
	 */
	public void visit(Node n) {
		if (n.incomplete) return;

		if (inactive)
			drawNode(n, inactiveColor, unselectedNodeSize, unselectedNodeRadius, fillUnselectedNode);
		else if (n.selected)
			drawNode(n, selectedColor, selectedNodeSize, selectedNodeRadius, fillSelectedNode);
		else if(n.tagged)
			drawNode(n, nodeColor, taggedNodeSize, taggedNodeRadius, fillUnselectedNode);
		else
			drawNode(n, nodeColor, unselectedNodeSize, unselectedNodeRadius, fillUnselectedNode);
	}

	public static Boolean isLargeSegment(Point p1, Point p2, int space)
	{
		int xd = p1.x-p2.x; if(xd < 0) xd = -xd;
		int yd = p1.y-p2.y; if(yd < 0) yd = -yd;
		return (xd+yd > space);
	}

	public void visitVirtual(Way w) {
		Iterator<Node> it = w.nodes.iterator();
		if (it.hasNext()) {
			Point lastP = nc.getPoint(it.next().eastNorth);
			while(it.hasNext())
			{
				Point p = nc.getPoint(it.next().eastNorth);
				if(isSegmentVisible(lastP, p) && isLargeSegment(lastP, p, virtualNodeSpace))
				{
					int x = (p.x+lastP.x)/2;
					int y = (p.y+lastP.y)/2;
					currentPath.moveTo(x-virtualNodeSize, y);
					currentPath.lineTo(x+virtualNodeSize, y);
					currentPath.moveTo(x, y-virtualNodeSize);
					currentPath.lineTo(x, y+virtualNodeSize);
				}
				lastP = p;
			}
		}
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

	private Stroke relatedWayStroke = new BasicStroke(
		4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
	public void visit(Relation r) {
		if (r.incomplete) return;

		Color col;
		if (inactive) {
			col = inactiveColor;
		} else if (r.selected) {
			col = selectedColor;
		} else {
			col = relationColor;
		}
		g.setColor(col);

		for (RelationMember m : r.members) {
			if (m.member.incomplete || m.member.deleted) continue;

			if (m.member instanceof Node) {
				Point p = nc.getPoint(((Node) m.member).eastNorth);
				if (p.x < 0 || p.y < 0
					|| p.x > nc.getWidth() || p.y > nc.getHeight()) continue;

				g.drawOval(p.x-3, p.y-3, 6, 6);
			} else if (m.member instanceof Way) {
				GeneralPath path = new GeneralPath();

				boolean first = true;
				for (Node n : ((Way) m.member).nodes) {
					if (n.incomplete || n.deleted) continue;
					Point p = nc.getPoint(n.eastNorth);
					if (first) {
						path.moveTo(p.x, p.y);
						first = false;
					} else {
						path.lineTo(p.x, p.y);
					}
				}

				((Graphics2D) g).draw(relatedWayStroke.createStrokedShape(path));
			}
		}
	}

	/**
	 * Draw an number of the order of the two consecutive nodes within the
	 * parents way
	 */
	protected void drawOrderNumber(Point p1, Point p2, int orderNumber) {
		if (isSegmentVisible(p1, p2) && isLargeSegment(p1, p2, segmentNumberSpace)) {
			String on = Integer.toString(orderNumber);
			int strlen = on.length();
			int x = (p1.x+p2.x)/2 - 4*strlen;
			int y = (p1.y+p2.y)/2 + 4;

			if(virtualNodeSize != 0 && isLargeSegment(p1, p2, virtualNodeSpace))
			{
				y = (p1.y+p2.y)/2 - virtualNodeSize - 3;
			}

			displaySegments(); // draw nodes on top!
			Color c = g.getColor();
			g.setColor(backgroundColor);
			g.fillRect(x-1, y-12, 8*strlen+1, 14);
			g.setColor(c);
			g.drawString(on, x, y);
		}
	}

	/**
	 * Draw the node as small rectangle with the given color.
	 *
	 * @param n		The node to draw.
	 * @param color The color of the node.
	 */
	public void drawNode(Node n, Color color, int size, int radius, boolean fill) {
		if (size > 1) {
			Point p = nc.getPoint(n.eastNorth);
			if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth())
					|| (p.y > nc.getHeight()))
				return;
			g.setColor(color);
			if (fill) {
				g.fillRect(p.x - radius, p.y - radius, size, size);
				g.drawRect(p.x - radius, p.y - radius, size, size);
			} else
				g.drawRect(p.x - radius, p.y - radius, size, size);
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

	protected boolean isSegmentVisible(Point p1, Point p2) {
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

	protected void displaySegments() {
		displaySegments(null);
	}
	protected void displaySegments(Color newColor) {
		if (currentPath != null) {
			g.setColor(currentColor);
			((Graphics2D) g).draw(currentPath);
			currentPath = new GeneralPath();
			currentColor = newColor;
		}
	}
}
