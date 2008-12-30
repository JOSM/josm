// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.IconElemStyle;
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;

public class MapPaintVisitor extends SimplePaintVisitor {
    protected boolean useRealWidth;
    protected boolean zoomLevelDisplay;
    protected boolean fillAreas;
    protected int fillAlpha;
    protected Color untaggedColor;
    protected Color textColor;
    protected boolean currentDashed = false;
    protected int currentWidth = 0;
    protected Stroke currentStroke = null;
    protected Font orderFont;
    protected ElemStyles styles;
    protected double circum;
    protected String regionalNameOrder[];
    protected Collection<Way> alreadyDrawnWays = new LinkedList<Way>();
    protected Collection<Way> alreadyDrawnAreas = new LinkedList<Way>();

    protected boolean isZoomOk(ElemStyle e) {
        if (!zoomLevelDisplay) /* show everything if the user wishes so */
            return true;

        if(e == null) /* the default for things that don't have a rule (show, if scale is smaller than 1500m) */
            return (circum < 1500);

        // formula to calculate a map scale: natural size / map size = scale
        // example: 876000mm (876m as displayed) / 22mm (roughly estimated screen size of legend bar) = 39818
        //
        // so the exact "correcting value" below depends only on the screen size and resolution
        // XXX - do we need a Preference setting for this (if things vary widely)?
        return !(circum >= e.maxScale / 22 || circum < e.minScale / 22);
    }

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    public void visit(Node n) {
        IconElemStyle nodeStyle = styles.get(n);
        if (nodeStyle != null && isZoomOk(nodeStyle))
            drawNode(n, nodeStyle.icon, nodeStyle.annotate);
        else if (n.selected)
            drawNode(n, selectedColor, selectedNodeSize, selectedNodeRadius, fillSelectedNode);
        else if (n.tagged)
            drawNode(n, nodeColor, taggedNodeSize, taggedNodeRadius, fillUnselectedNode);
        else
            drawNode(n, nodeColor, unselectedNodeSize, unselectedNodeRadius, fillUnselectedNode);
    }

    /**
     * Draw a line for all segments, according to tags.
     * @param w The way to draw.
     */
    public void visit(Way w) {
        if(w.nodes.size() < 2)
            return;

        ElemStyle wayStyle = styles.get(w);

        if(!isZoomOk(wayStyle))
            return;

        LineElemStyle l = null;
        Color areacolor = untaggedColor;
        if(wayStyle!=null)
        {
            boolean area = false;
            if(wayStyle instanceof LineElemStyle)
                l = (LineElemStyle)wayStyle;
            else if (wayStyle instanceof AreaElemStyle)
            {
                areacolor = ((AreaElemStyle)wayStyle).color;
                l = ((AreaElemStyle)wayStyle).line;
                area = true;
            }
            if (area && fillAreas)
                drawWayAsArea(w, areacolor);
        }

        drawWay(w, l, areacolor);
    }

    public void drawWay(Way w, LineElemStyle l, Color color) {
        // show direction arrows, if draw.segment.relevant_directions_only is not set,
        // the way is tagged with a direction key
        // (even if the tag is negated as in oneway=false) or the way is selected
        boolean showDirection = w.selected || ((!useRealWidth) && (showDirectionArrow
        && (!showRelevantDirectionsOnly || w.hasDirectionKeys)));
        int width = defaultSegmentWidth;
        int realWidth = 0; //the real width of the element in meters
        boolean dashed = false;

        if(l != null)
        {
            color = l.color;
            width = l.width;
            realWidth = l.realWidth;
            dashed = l.dashed;
        }
        if (realWidth > 0 && useRealWidth && !showDirection)
        {
            int tmpWidth = (int) (100 /  (float) (circum / realWidth));
            if (tmpWidth > width) width = tmpWidth;
        }
        if(w.selected)
            color = selectedColor;

        Node lastN;
        if(l != null && l.overlays != null)
        {
            for(LineElemStyle s : l.overlays)
            {
                if(!s.over)
                {
                    lastN = null;
                    for(Node n : w.nodes)
                    {
                        if(lastN != null)
                        {
                            drawSeg(lastN, n, s.color != null  && !w.selected ? s.color : color,
                            false, s.getWidth(width), s.dashed);
                        }
                        lastN = n;
                    }
                }
            }
        }

        lastN = null;
        for(Node n : w.nodes)
        {
            if(lastN != null)
                drawSeg(lastN, n, color, showDirection, width, dashed);
            lastN = n;
        }

        if(l != null && l.overlays != null)
        {
            for(LineElemStyle s : l.overlays)
            {
                if(s.over)
                {
                    lastN = null;
                    for(Node n : w.nodes)
                    {
                        if(lastN != null)
                        {
                            drawSeg(lastN, n, s.color != null && !w.selected ? s.color : color,
                            false, s.getWidth(width), s.dashed);
                        }
                        lastN = n;
                    }
                }
            }
        }

        if(showOrderNumber)
        {
            int orderNumber = 0;
            lastN = null;
            for(Node n : w.nodes)
            {
                if(lastN != null)
                {
                    orderNumber++;
                    drawOrderNumber(lastN, n, orderNumber);
                }
                lastN = n;
            }
        }
        displaySegments();
    }

    public Collection<Way> joinWays(Collection<Way> join)
    {
        Collection<Way> res = new LinkedList<Way>();
        Object[] joinArray = join.toArray();
        int left = join.size();
        while(left != 0)
        {
            Way w = null;
            Boolean selected = false;
            ArrayList<Node> n = null;
            Boolean joined = true;
            while(joined && left != 0)
            {
                joined = false;
                for(int i = 0; i < joinArray.length && left != 0; ++i)
                {
                    if(joinArray[i] != null)
                    {
                        Way c = (Way)joinArray[i];
                        if(w == null)
                        { w = c; selected = w.selected; joinArray[i] = null; --left; }
                        else
                        {
                            int mode = 0;
                            int cl = c.nodes.size()-1;
                            int nl;
                            if(n == null)
                            {
                                nl = w.nodes.size()-1;
                                if(w.nodes.get(nl) == c.nodes.get(0)) mode = 21;
                                else if(w.nodes.get(nl) == c.nodes.get(cl)) mode = 22;
                                else if(w.nodes.get(0) == c.nodes.get(0)) mode = 11;
                                else if(w.nodes.get(0) == c.nodes.get(cl)) mode = 12;
                            }
                            else
                            {
                                nl = n.size()-1;
                                if(n.get(nl) == c.nodes.get(0)) mode = 21;
                                else if(n.get(0) == c.nodes.get(cl)) mode = 12;
                                else if(n.get(0) == c.nodes.get(0)) mode = 11;
                                else if(n.get(nl) == c.nodes.get(cl)) mode = 22;
                            }
                            if(mode != 0)
                            {
                                joinArray[i] = null;
                                joined = true;
                                if(c.selected) selected = true;
                                --left;
                                if(n == null) n = new ArrayList(w.nodes);
                                n.remove((mode == 21 || mode == 22) ? nl : 0);
                                if(mode == 21)
                                    n.addAll(c.nodes);
                                else if(mode == 12)
                                    n.addAll(0, c.nodes);
                                else if(mode == 22)
                                {
                                    for(Node node : c.nodes)
                                        n.add(nl, node);
                                }
                                else /* mode == 11 */
                                {
                                    for(Node node : c.nodes)
                                        n.add(0, node);
                                }
                            }
                        }
                    }
                } /* for(i = ... */
            } /* while(joined) */
            if(n != null)
            {
                w = new Way(w);
                w.nodes.clear();
                w.nodes.addAll(n);
                w.selected = selected;
            }
            if(!w.isClosed())
            {
System.out.println("ERROR: multipolygon way is not closed." + w);
            }
            res.add(w);
        } /* while(left != 0) */

        return res;
    }

    public void visit(Relation r) {
        // draw multipolygon relations including their ways
        // other relations are not (yet?) drawn.
        if (r.incomplete) return;

        if(!Main.pref.getBoolean("mappaint.multipolygon",false)) return;

        if(!"multipolygon".equals(r.keys.get("type"))) return;

        Collection<Way> inner = new LinkedList<Way>();
        Collection<Way> outer = new LinkedList<Way>();
        Collection<Way> innerclosed = new LinkedList<Way>();
        Collection<Way> outerclosed = new LinkedList<Way>();

        for (RelationMember m : r.members)
        {
            if (!m.member.incomplete && !m.member.deleted)
            {
                if(m.member instanceof Way)
                {
                    Way w = (Way) m.member;
                    if(w.nodes.size() < 2)
                    {
System.out.println("ERROR: Way with less than two points " + w);
                    }
                    else if("inner".equals(m.role))
                        inner.add(w);
                    else if("outer".equals(m.role))
                        outer.add(w);
                    else
                    {
System.out.println("ERROR: No useful role for Way " + w);
                        if(m.role == null || m.role.length() == 0)
                            outer.add(w);
                    }
                }
                else
                {
System.out.println("ERROR: Non-Way in multipolygon " + m.member);
                }
            }
        }

        ElemStyle wayStyle = styles.get(r);
        /* find one wayStyle, prefer the style from Relation or take the first
        one of outer rings */
        if(wayStyle == null || !(wayStyle instanceof AreaElemStyle))
        {
            for (Way w : outer)
            {
               if(wayStyle == null || !(wayStyle instanceof AreaElemStyle))
                   wayStyle = styles.get(w);
            }
        }

        if(wayStyle != null && wayStyle instanceof AreaElemStyle)
        {
            Boolean zoomok = isZoomOk(wayStyle);
            Collection<Way> join = new LinkedList<Way>();

            /* parse all outer rings and join them */
            for (Way w : outer)
            {
                if(w.isClosed()) outerclosed.add(w);
                else join.add(w);
            }
            if(join.size() != 0)
            {
                for(Way w : joinWays(join))
                    outerclosed.add(w);
            }

            /* parse all inner rings and join them */
            join.clear();
            for (Way w : inner)
            {
                if(w.isClosed()) innerclosed.add(w);
                else join.add(w);
            }
            if(join.size() != 0)
            {
                for(Way w : joinWays(join))
                    innerclosed.add(w);
            }

            /* handle inside out stuff */

            if(zoomok) /* draw */
            {
                for (Way w : outerclosed)
                {
                    Color color = w.selected ? selectedColor
                    : ((AreaElemStyle)wayStyle).color;
                    Polygon polygon = new Polygon();
                    Point pOuter = null;

                    for (Node n : w.nodes)
                    {
                        pOuter = nc.getPoint(n.eastNorth);
                        polygon.addPoint(pOuter.x,pOuter.y);
                    }
                    for (Way wInner : innerclosed)
                    {
                        for (Node n : wInner.nodes)
                        {
                            Point pInner = nc.getPoint(n.eastNorth);
                            polygon.addPoint(pInner.x,pInner.y);
                        }
                        if(!wInner.isClosed())
                        {
                            Point pInner = nc.getPoint(wInner.nodes.get(0).eastNorth);
                            polygon.addPoint(pInner.x,pInner.y);
                        }
                        polygon.addPoint(pOuter.x,pOuter.y);
                    }

                    g.setColor(new Color( color.getRed(), color.getGreen(),
                    color.getBlue(), fillAlpha));

                    g.fillPolygon(polygon);
                    alreadyDrawnAreas.add(w);
                }
            }
            for (Way wInner : inner)
            {
                ElemStyle innerStyle = styles.get(wInner);
                if(innerStyle == null)
                {
                    if(zoomok)
                        drawWay(wInner, ((AreaElemStyle)wayStyle).line,
                        ((AreaElemStyle)wayStyle).color);
                    alreadyDrawnWays.add(wInner);
                }
                else if(wayStyle.equals(innerStyle))
                {
System.out.println("WARNING: Inner waystyle equals multipolygon for way " + wInner);
                    alreadyDrawnAreas.add(wInner);
                }
            }
            for (Way wOuter : outer)
            {
                ElemStyle outerStyle = styles.get(wOuter);
                if(outerStyle == null)
                {
                    if(zoomok)
                        drawWay(wOuter, ((AreaElemStyle)wayStyle).line,
                        ((AreaElemStyle)wayStyle).color);
                    alreadyDrawnWays.add(wOuter);
                }
                else
                {
                    if(!wayStyle.equals(outerStyle))
System.out.println("ERROR: Outer waystyle does not match multipolygon for way " + wOuter);
                    alreadyDrawnAreas.add(wOuter);
                }
            }
        }
    }

    protected void drawWayAsArea(Way w, Color color)
    {
        Polygon polygon = new Polygon();

        for (Node n : w.nodes)
        {
            Point p = nc.getPoint(n.eastNorth);
            polygon.addPoint(p.x,p.y);
        }

        Color mycolor = w.selected ? selectedColor : color;
        // set the opacity (alpha) level of the filled polygon
        g.setColor(new Color( mycolor.getRed(), mycolor.getGreen(), mycolor.getBlue(), fillAlpha));

        g.fillPolygon(polygon);
    }

    // NEW
    protected void drawNode(Node n, ImageIcon icon, boolean annotate) {
        Point p = nc.getPoint(n.eastNorth);
        if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;
        int w = icon.getIconWidth(), h=icon.getIconHeight();
        icon.paintIcon ( Main.map.mapView, g, p.x-w/2, p.y-h/2 );
        String name = getNodeName(n);
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

    protected String getNodeName(Node n) {
        String name = null;
        if (n.keys != null) {
            for (int i = 0; i < regionalNameOrder.length; i++) {
                name = n.keys.get(regionalNameOrder[i]);
                if (name != null) break;
            }
        }
        return name;
    }

    private void drawSeg(Node n1, Node n2, Color col, boolean showDirection, int width, boolean dashed) {
        if (col != currentColor || width != currentWidth || dashed != currentDashed) {
            displaySegments(col, width, dashed);
        }
        Point p1 = nc.getPoint(n1.eastNorth);
        Point p2 = nc.getPoint(n2.eastNorth);

        if (!isSegmentVisible(p1, p2)) {
            return;
        }
        currentPath.moveTo(p1.x, p1.y);
        currentPath.lineTo(p2.x, p2.y);

        if (showDirection) {
            double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
            currentPath.lineTo((int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
            currentPath.moveTo((int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
            currentPath.lineTo(p2.x, p2.y);
        }
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
    public void drawNode(Node n, Color color, int size, int radius, boolean fill) {
        if (isZoomOk(null) && size > 1) {
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

    // NW 111106 Overridden from SimplePaintVisitor in josm-1.4-nw1
    // Shows areas before non-areas
    public void visitAll(DataSet data, Boolean virtual) {
        getSettings(virtual);
        untaggedColor = Main.pref.getColor(marktr("untagged"),Color.GRAY);
        textColor = Main.pref.getColor (marktr("text"), Color.WHITE);
        useRealWidth = Main.pref.getBoolean("mappaint.useRealWidth",false);
        zoomLevelDisplay = Main.pref.getBoolean("mappaint.zoomLevelDisplay",false);
        fillAreas = Main.pref.getBoolean("mappaint.fillareas", true);
        fillAlpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fillalpha", 50))));
        circum = Main.map.mapView.getScale()*100*Main.proj.scaleFactor()*40041455; // circumference of the earth in meter
        styles = MapPaintStyles.getStyles();
        orderFont = new Font(Main.pref.get("mappaint.font","Helvetica"), Font.PLAIN, Main.pref.getInteger("mappaint.fontsize", 8));
        String currentLocale = Locale.getDefault().getLanguage();
        regionalNameOrder = Main.pref.get("mappaint.nameOrder", "name:"+currentLocale+";name;int_name").split(";");

        if (fillAreas && styles.hasAreas()) {
            Collection<Way> noAreaWays = new LinkedList<Way>();

            for (final Relation osm : data.relations)
            {
                if (!osm.deleted && !osm.selected)
                {
                    osm.visit(this);
                }
            }

            for (final Way osm : data.ways)
            {
                if (!osm.incomplete && !osm.deleted && !alreadyDrawnWays.contains(osm))
                {
                    if(styles.isArea((Way)osm) && !alreadyDrawnAreas.contains(osm))
                        osm.visit(this);
                    else
                        noAreaWays.add((Way)osm);
                }
            }
            // free that stuff
            alreadyDrawnWays = null;
            alreadyDrawnAreas = null;

            fillAreas = false;
            for (final OsmPrimitive osm : noAreaWays)
                osm.visit(this);
        }
        else
        {
            for (final OsmPrimitive osm : data.ways)
                if (!osm.incomplete && !osm.deleted)
                    osm.visit(this);
        }

        for (final OsmPrimitive osm : data.getSelected())
            if (!osm.incomplete && !osm.deleted){
                osm.visit(this);
            }

        displaySegments();

        for (final OsmPrimitive osm : data.nodes)
            if (!osm.incomplete && !osm.deleted)
                osm.visit(this);

        if (virtualNodeSize != 0)
        {
            currentColor = nodeColor;
            for (final OsmPrimitive osm : data.ways)
                if (!osm.deleted)
                    visitVirtual((Way)osm);
            displaySegments(null);
        }
    }

    /**
     * Draw a number of the order of the two consecutive nodes within the
     * parents way
     */
    protected void drawOrderNumber(Node n1, Node n2, int orderNumber) {
        Point p1 = nc.getPoint(n1.eastNorth);
        Point p2 = nc.getPoint(n2.eastNorth);
        drawOrderNumber(p1, p2, orderNumber);
    }
}
