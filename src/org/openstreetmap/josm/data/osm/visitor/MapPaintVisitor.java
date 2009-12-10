/* License: GPL. Copyright 2007 by Immanuel Scholz and others */
package org.openstreetmap.josm.data.osm.visitor;

/* To enable debugging or profiling remove the double / signs */

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.IconElemStyle;
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;

public class MapPaintVisitor extends SimplePaintVisitor {
    protected boolean useRealWidth;
    protected boolean zoomLevelDisplay;
    protected boolean drawMultipolygon;
    protected boolean drawRestriction;
    protected boolean leftHandTraffic;
    protected int showNames;
    protected int showIcons;
    protected int useStrokes;
    protected int fillAlpha;
    protected Color untaggedColor;
    protected Color textColor;
    protected Color areaTextColor;
    protected float[] currentDashed = new float[0];
    protected Color currentDashedColor;
    protected int currentWidth = 0;
    protected Stroke currentStroke = null;
    protected Font orderFont;
    protected ElemStyles.StyleSet styles;
    protected double circum;
    protected double dist;
    protected Collection<String> regionalNameOrder;
    protected boolean useStyleCache;
    private static int paintid = 0;
    private EastNorth minEN;
    private EastNorth maxEN;

    protected boolean isZoomOk(ElemStyle e) {
        if (!zoomLevelDisplay) /* show everything if the user wishes so */
            return true;

        if(e == null) /* the default for things that don't have a rule (show, if scale is smaller than 1500m) */
            return (circum < 1500);

        return !(circum >= e.maxScale || circum < e.minScale);
    }

    public ElemStyle getPrimitiveStyle(OsmPrimitive osm) {
        if(!useStyleCache)
            return (styles != null) ? styles.get(osm) : null;

            if(osm.mappaintStyle == null && styles != null) {
                osm.mappaintStyle = styles.get(osm);
                if(osm instanceof Way) {
                    ((Way)osm).isMappaintArea = styles.isArea(osm);
                }
            }
            return osm.mappaintStyle;
    }

    public IconElemStyle getPrimitiveNodeStyle(OsmPrimitive osm) {
        if(!useStyleCache)
            return (styles != null) ? styles.getIcon(osm) : null;

            if(osm.mappaintStyle == null && styles != null) {
                osm.mappaintStyle = styles.getIcon(osm);
            }

            return (IconElemStyle)osm.mappaintStyle;
    }

    public boolean isPrimitiveArea(Way osm) {
        if(!useStyleCache)
            return styles.isArea(osm);

        if(osm.mappaintStyle == null && styles != null) {
            osm.mappaintStyle = styles.get(osm);
            osm.isMappaintArea = styles.isArea(osm);
        }
        return osm.isMappaintArea;
    }

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    @Override
    public void visit(Node n) {}
    public void drawNode(Node n) {
        /* check, if the node is visible at all */
        if((n.getEastNorth().east()  > maxEN.east() ) ||
                (n.getEastNorth().north() > maxEN.north()) ||
                (n.getEastNorth().east()  < minEN.east() ) ||
                (n.getEastNorth().north() < minEN.north()))
            return;

        IconElemStyle nodeStyle = (IconElemStyle)getPrimitiveStyle(n);

        if (nodeStyle != null && isZoomOk(nodeStyle) && showIcons > dist) {
            if (inactive || n.isDisabled()) {
                drawNode(n, nodeStyle.getDisabledIcon(), nodeStyle.annotate, data.isSelected(n));
            } else {
                drawNode(n, nodeStyle.icon, nodeStyle.annotate, data.isSelected(n));
            }
        } else if (n.highlighted) {
            drawNode(n, highlightColor, selectedNodeSize, selectedNodeRadius, fillSelectedNode);
        } else if (data.isSelected(n)) {
            drawNode(n, selectedColor, selectedNodeSize, selectedNodeRadius, fillSelectedNode);
        } else if (n.isTagged()) {
            drawNode(n, nodeColor, taggedNodeSize, taggedNodeRadius, fillUnselectedNode);
        } else if (inactive || n.isDisabled()) {
            drawNode(n, inactiveColor, unselectedNodeSize, unselectedNodeRadius, fillUnselectedNode);
        } else {
            drawNode(n, nodeColor, unselectedNodeSize, unselectedNodeRadius, fillUnselectedNode);
        }
    }

    /**
     * Draw a line for all segments, according to tags.
     * @param w The way to draw.
     */
    @Override
    public void visit(Way w) {}
    public void drawWay(Way w, int fillAreas) {
        if(w.getNodesCount() < 2)
            return;

        if (w.hasIncompleteNodes())
            return;

        /* check, if the way is visible at all */
        double minx = 10000;
        double maxx = -10000;
        double miny = 10000;
        double maxy = -10000;

        for (Node n : w.getNodes())
        {
            if(n.getEastNorth().east() > maxx) {
                maxx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() > maxy) {
                maxy = n.getEastNorth().north();
            }
            if(n.getEastNorth().east() < minx) {
                minx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() < miny) {
                miny = n.getEastNorth().north();
            }
        }

        if ((minx > maxEN.east()) ||
                (miny > maxEN.north()) ||
                (maxx < minEN.east()) ||
                (maxy < minEN.north()))
            return;

        ElemStyle wayStyle = getPrimitiveStyle(w);

        if(!isZoomOk(wayStyle))
            return;

        if(wayStyle==null)
        {
            /* way without style */
            drawWay(w, null, untaggedColor, data.isSelected(w));
        }
        else if(wayStyle instanceof LineElemStyle)
        {
            /* way with line style */
            drawWay(w, (LineElemStyle)wayStyle, untaggedColor, data.isSelected(w));
        }
        else if (wayStyle instanceof AreaElemStyle)
        {
            AreaElemStyle areaStyle = (AreaElemStyle) wayStyle;
            /* way with area style */
            if (fillAreas > dist)
            {
                drawArea(w, data.isSelected(w) ? selectedColor : areaStyle.color);
                if(!w.isClosed()) {
                    putError(w, tr("Area style way is not closed."), true);
                }
            }
            drawWay(w, areaStyle.line, areaStyle.color, data.isSelected(w));
        }
    }

    public void drawWay(Way w, LineElemStyle l, Color color, boolean selected) {
        /* show direction arrows, if draw.segment.relevant_directions_only is not set,
           the way is tagged with a direction key
           (even if the tag is negated as in oneway=false) or the way is selected */
        boolean showDirection = data.isSelected(w) || ((!useRealWidth) && (showDirectionArrow
                && (!showRelevantDirectionsOnly || w.hasDirectionKeys())));
        /* head only takes over control if the option is true,
           the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showDirection && !data.isSelected(w) && showHeadArrowOnly;
        int width = defaultSegmentWidth;
        int realWidth = 0; /* the real width of the element in meters */
        float dashed[] = new float[0];
        Color dashedColor = null;
        Node lastN;

        if(l != null)
        {
            if (l.color != null) {
                color = l.color;
            }
            width = l.width;
            realWidth = l.realWidth;
            dashed = l.dashed;
            dashedColor = l.dashedColor;
        }
        if(selected) {
            color = selectedColor;
        }
        if (realWidth > 0 && useRealWidth && !showDirection)
        {
            int tmpWidth = (int) (100 /  (float) (circum / realWidth));
            if (tmpWidth > width) {
                width = tmpWidth;
            }

            /* if we have a "width" tag, try use it */
            /* (this might be slow and could be improved by caching the value in the Way, on the other hand only used if "real width" is enabled) */
            String widthTag = w.get("width");
            if(widthTag == null) {
                widthTag = w.get("est_width");
            }
            if(widthTag != null) {
                try {
                    width = Integer.parseInt(widthTag);
                }
                catch(NumberFormatException nfe) {
                }
            }
        }

        if(w.highlighted) {
            color = highlightColor;
        } else if(data.isSelected(w)) {
            color = selectedColor;
        } else if(w.isDisabled()) {
            color = inactiveColor;
        }

        /* draw overlays under the way */
        if(l != null && l.overlays != null)
        {
            for(LineElemStyle s : l.overlays)
            {
                if(!s.over)
                {
                    lastN = null;
                    for(Node n : w.getNodes())
                    {
                        if(lastN != null)
                        {
                            drawSeg(lastN, n, s.color != null  && !data.isSelected(w) ? s.color : color,
                                    false, s.getWidth(width), s.dashed, s.dashedColor);
                        }
                        lastN = n;
                    }
                }
            }
        }

        /* draw the way */
        lastN = null;
        Iterator<Node> it = w.getNodes().iterator();
        while (it.hasNext())
        {
            Node n = it.next();
            if(lastN != null) {
                drawSeg(lastN, n, color,
                        showOnlyHeadArrowOnly ? !it.hasNext() : showDirection, width, dashed, dashedColor);
            }
            lastN = n;
        }

        /* draw overlays above the way */
        if(l != null && l.overlays != null)
        {
            for(LineElemStyle s : l.overlays)
            {
                if(s.over)
                {
                    lastN = null;
                    for(Node n : w.getNodes())
                    {
                        if(lastN != null)
                        {
                            drawSeg(lastN, n, s.color != null && !data.isSelected(w) ? s.color : color,
                                    false, s.getWidth(width), s.dashed, s.dashedColor);
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
            for(Node n : w.getNodes())
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

    public Collection<PolyData> joinWays(Collection<Way> join, OsmPrimitive errs)
    {
        Collection<PolyData> res = new LinkedList<PolyData>();
        Object[] joinArray = join.toArray();
        int left = join.size();
        while(left != 0)
        {
            Way w = null;
            boolean selected = false;
            List<Node> n = null;
            boolean joined = true;
            while(joined && left != 0)
            {
                joined = false;
                for(int i = 0; i < joinArray.length && left != 0; ++i)
                {
                    if(joinArray[i] != null)
                    {
                        Way c = (Way)joinArray[i];
                        if(w == null)
                        { w = c; selected = data.isSelected(w); joinArray[i] = null; --left; }
                        else
                        {
                            int mode = 0;
                            int cl = c.getNodesCount()-1;
                            int nl;
                            if(n == null)
                            {
                                nl = w.getNodesCount()-1;
                                if(w.getNode(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if(w.getNode(nl) == c.getNode(cl)) {
                                    mode = 22;
                                } else if(w.getNode(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if(w.getNode(0) == c.getNode(cl)) {
                                    mode = 12;
                                }
                            }
                            else
                            {
                                nl = n.size()-1;
                                if(n.get(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if(n.get(0) == c.getNode(cl)) {
                                    mode = 12;
                                } else if(n.get(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if(n.get(nl) == c.getNode(cl)) {
                                    mode = 22;
                                }
                            }
                            if(mode != 0)
                            {
                                joinArray[i] = null;
                                joined = true;
                                if(data.isSelected(c)) {
                                    selected = true;
                                }
                                --left;
                                if(n == null) {
                                    n = w.getNodes();
                                }
                                n.remove((mode == 21 || mode == 22) ? nl : 0);
                                if(mode == 21) {
                                    n.addAll(c.getNodes());
                                } else if(mode == 12) {
                                    n.addAll(0, c.getNodes());
                                } else if(mode == 22)
                                {
                                    for(Node node : c.getNodes()) {
                                        n.add(nl, node);
                                    }
                                }
                                else /* mode == 11 */
                                {
                                    for(Node node : c.getNodes()) {
                                        n.add(0, node);
                                    }
                                }
                            }
                        }
                    }
                } /* for(i = ... */
            } /* while(joined) */
            if(n != null)
            {
                w = new Way(w);
                w.setNodes(n);
            }
            if(!w.isClosed())
            {
                if(errs != null)
                {
                    putError(errs, tr("multipolygon way ''{0}'' is not closed.",
                            w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
            }
            PolyData pd = new PolyData(w);
            pd.selected = selected;
            res.add(pd);
        } /* while(left != 0) */

        return res;
    }

    public void drawSelectedMember(OsmPrimitive osm, ElemStyle style, boolean area,
            boolean areaselected)
    {
        if(osm instanceof Way)
        {
            if(style instanceof AreaElemStyle)
            {
                Way way = (Way)osm;
                AreaElemStyle areaStyle = (AreaElemStyle)style;
                drawWay(way, areaStyle.line, selectedColor, true);
                if(area) {
                    drawArea(way, areaselected ? selectedColor : areaStyle.color);
                }
            }
            else
            {
                drawWay((Way)osm, (LineElemStyle)style, selectedColor, true);
            }
        }
        else if(osm instanceof Node)
        {
            if(style != null && isZoomOk(style)) {
                drawNode((Node)osm, ((IconElemStyle)style).icon,
                        ((IconElemStyle)style).annotate, true);
            } else {
                drawNode((Node)osm, selectedColor, selectedNodeSize, selectedNodeRadius, fillSelectedNode);
            }
        }
        osm.mappaintDrawnCode = paintid;
    }

    @Override
    public void visit(Relation r) {}
    public void paintUnselectedRelation(Relation r) {

        if (drawMultipolygon && "multipolygon".equals(r.get("type")))
        {
            if(drawMultipolygon(r))
                return;
        }
        else if (drawRestriction && "restriction".equals(r.get("type")))
        {
            drawRestriction(r);
        }

        if(data.isSelected(r)) /* draw ways*/
        {
            for (RelationMember m : r.getMembers())
            {
                if (m.isWay() && m.getMember().isDrawable())
                {
                    drawSelectedMember(m.getMember(), styles != null ? getPrimitiveStyle(m.getMember())
                            : null, true, true);
                }
            }
        }
    }

    public void drawRestriction(Relation r) {
        Way fromWay = null;
        Way toWay = null;
        OsmPrimitive via = null;

        /* find the "from", "via" and "to" elements */
        for (RelationMember m : r.getMembers())
        {
            if (m.getMember().isDeleted()) {
                putError(r, tr("Deleted member ''{0}'' in relation.",
                        m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
            } else if(m.getMember().isIncomplete())
                return;
            else
            {
                if(m.isWay())
                {
                    Way w = m.getWay();
                    if(w.getNodesCount() < 2)
                    {
                        putError(r, tr("Way ''{0}'' with less than two points.",
                                w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                    }
                    else if("from".equals(m.getRole())) {
                        if(fromWay != null) {
                            putError(r, tr("More than one \"from\" way found."), true);
                        } else {
                            fromWay = w;
                        }
                    } else if("to".equals(m.getRole())) {
                        if(toWay != null) {
                            putError(r, tr("More than one \"to\" way found."), true);
                        } else {
                            toWay = w;
                        }
                    } else if("via".equals(m.getRole())) {
                        if(via != null) {
                            putError(r, tr("More than one \"via\" found."), true);
                        } else {
                            via = w;
                        }
                    } else {
                        putError(r, tr("Unknown role ''{0}''.", m.getRole()), true);
                    }
                }
                else if(m.isNode())
                {
                    Node n = m.getNode();
                    if("via".equals(m.getRole()))
                    {
                        if(via != null) {
                            putError(r, tr("More than one \"via\" found."), true);
                        } else {
                            via = n;
                        }
                    } else {
                        putError(r, tr("Unknown role ''{0}''.", m.getRole()), true);
                    }
                } else {
                    putError(r, tr("Unknown member type for ''{0}''.", m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
            }
        }

        if (fromWay == null) {
            putError(r, tr("No \"from\" way found."), true);
            return;
        }
        if (toWay == null) {
            putError(r, tr("No \"to\" way found."), true);
            return;
        }
        if (via == null) {
            putError(r, tr("No \"via\" node or way found."), true);
            return;
        }

        Node viaNode;
        if(via instanceof Node)
        {
            viaNode = (Node) via;
            if(!fromWay.isFirstLastNode(viaNode)) {
                putError(r, tr("The \"from\" way doesn't start or end at a \"via\" node."), true);
                return;
            }
            if(!toWay.isFirstLastNode(viaNode)) {
                putError(r, tr("The \"to\" way doesn't start or end at a \"via\" node."), true);
            }
        }
        else
        {
            Way viaWay = (Way) via;
            Node firstNode = viaWay.firstNode();
            Node lastNode = viaWay.lastNode();
            boolean onewayvia = false;

            String onewayviastr = viaWay.get("oneway");
            if(onewayviastr != null)
            {
                if("-1".equals(onewayviastr)) {
                    onewayvia = true;
                    Node tmp = firstNode;
                    firstNode = lastNode;
                    lastNode = tmp;
                } else {
                    onewayvia = OsmUtils.getOsmBoolean(onewayviastr);
                }
            }

            if(fromWay.isFirstLastNode(firstNode)) {
                viaNode = firstNode;
            } else if (!onewayvia && fromWay.isFirstLastNode(lastNode)) {
                viaNode = lastNode;
            } else {
                putError(r, tr("The \"from\" way doesn't start or end at the \"via\" way."), true);
                return;
            }
            if(!toWay.isFirstLastNode(viaNode == firstNode ? lastNode : firstNode)) {
                putError(r, tr("The \"to\" way doesn't start or end at the \"via\" way."), true);
            }
        }

        /* find the "direct" nodes before the via node */
        Node fromNode = null;
        if(fromWay.firstNode() == via) {
            fromNode = fromWay.getNode(1);
        } else {
            fromNode = fromWay.getNode(fromWay.getNodesCount()-2);
        }

        Point pFrom = nc.getPoint(fromNode);
        Point pVia = nc.getPoint(viaNode);

        /* starting from via, go back the "from" way a few pixels
           (calculate the vector vx/vy with the specified length and the direction
           away from the "via" node along the first segment of the "from" way)
         */
        double distanceFromVia=14;
        double dx = (pFrom.x >= pVia.x) ? (pFrom.x - pVia.x) : (pVia.x - pFrom.x);
        double dy = (pFrom.y >= pVia.y) ? (pFrom.y - pVia.y) : (pVia.y - pFrom.y);

        double fromAngle;
        if(dx == 0.0) {
            fromAngle = Math.PI/2;
        } else {
            fromAngle = Math.atan(dy / dx);
        }
        double fromAngleDeg = Math.toDegrees(fromAngle);

        double vx = distanceFromVia * Math.cos(fromAngle);
        double vy = distanceFromVia * Math.sin(fromAngle);

        if(pFrom.x < pVia.x) {
            vx = -vx;
        }
        if(pFrom.y < pVia.y) {
            vy = -vy;
        }

        /* go a few pixels away from the way (in a right angle)
           (calculate the vx2/vy2 vector with the specified length and the direction
           90degrees away from the first segment of the "from" way)
         */
        double distanceFromWay=10;
        double vx2 = 0;
        double vy2 = 0;
        double iconAngle = 0;

        if(pFrom.x >= pVia.x && pFrom.y >= pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            }
            iconAngle = 270+fromAngleDeg;
        }
        if(pFrom.x < pVia.x && pFrom.y >= pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            }
            iconAngle = 90-fromAngleDeg;
        }
        if(pFrom.x < pVia.x && pFrom.y < pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            }
            iconAngle = 90+fromAngleDeg;
        }
        if(pFrom.x >= pVia.x && pFrom.y < pVia.y) {
            if(!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            }
            iconAngle = 270-fromAngleDeg;
        }

        IconElemStyle nodeStyle = getPrimitiveNodeStyle(r);

        if (nodeStyle == null) {
            putError(r, tr("Style for restriction {0} not found.", r.get("restriction")), true);
            return;
        }

        /* rotate icon with direction last node in from to */
        ImageIcon rotatedIcon = ImageProvider.createRotatedImage(null /*icon2*/, inactive || r.isDisabled() ?
                nodeStyle.getDisabledIcon() :
                    nodeStyle.icon, iconAngle);

        /* scale down icon to 16*16 pixels */
        ImageIcon smallIcon = new ImageIcon(rotatedIcon.getImage().getScaledInstance(16 , 16, Image.SCALE_SMOOTH));
        int w = smallIcon.getIconWidth(), h=smallIcon.getIconHeight();
        smallIcon.paintIcon ( Main.map.mapView, g, (int)(pVia.x+vx+vx2)-w/2, (int)(pVia.y+vy+vy2)-h/2 );

        if (data.isSelected(r))
        {
            g.setColor (  selectedColor );
            g.drawRect ((int)(pVia.x+vx+vx2)-w/2-2,(int)(pVia.y+vy+vy2)-h/2-2, w+4, h+4);
        }
    }

    class PolyData {
        public Polygon poly = new Polygon();
        public Way way;
        public boolean selected = false;
        private Point p = null;
        private Collection<Polygon> inner = null;
        PolyData(Way w)
        {
            way = w;
            for (Node n : w.getNodes())
            {
                p = nc.getPoint(n);
                poly.addPoint(p.x,p.y);
            }
        }
        public int contains(Polygon p)
        {
            int contains = p.npoints;
            for(int i = 0; i < p.npoints; ++i)
            {
                if(poly.contains(p.xpoints[i],p.ypoints[i])) {
                    --contains;
                }
            }
            if(contains == 0) return 1;
            if(contains == p.npoints) return 0;
            return 2;
        }
        public void addInner(Polygon p)
        {
            if(inner == null) {
                inner = new ArrayList<Polygon>();
            }
            inner.add(p);
        }
        public boolean isClosed()
        {
            return (poly.npoints >= 3
                    && poly.xpoints[0] == poly.xpoints[poly.npoints-1]
                                                       && poly.ypoints[0] == poly.ypoints[poly.npoints-1]);
        }
        public Polygon get()
        {
            if(inner != null)
            {
                for (Polygon pp : inner)
                {
                    for(int i = 0; i < pp.npoints; ++i) {
                        poly.addPoint(pp.xpoints[i],pp.ypoints[i]);
                    }
                    poly.addPoint(p.x,p.y);
                }
                inner = null;
            }
            return poly;
        }
    }
    void addInnerToOuters(Relation r, boolean incomplete, PolyData pdInner, LinkedList<PolyData> outerPolygons)
    {
        Way wInner = pdInner.way;
        if(wInner != null && !wInner.isClosed())
        {
            Point pInner = nc.getPoint(wInner.getNode(0));
            pdInner.poly.addPoint(pInner.x,pInner.y);
        }
        PolyData o = null;
        for (PolyData pdOuter : outerPolygons)
        {
            Integer c = pdOuter.contains(pdInner.poly);
            if(c >= 1)
            {
                if(c > 1 && pdOuter.way != null && pdOuter.way.isClosed())
                {
                    putError(r, tr("Intersection between ways ''{0}'' and ''{1}''.",
                            pdOuter.way.getDisplayName(DefaultNameFormatter.getInstance()), wInner.getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
                if(o == null || o.contains(pdOuter.poly) > 0) {
                    o = pdOuter;
                }
            }
        }
        if(o == null)
        {
            if(!incomplete)
            {
                putError(r, tr("Inner way ''{0}'' is outside.",
                        wInner.getDisplayName(DefaultNameFormatter.getInstance())), true);
            }
            o = outerPolygons.get(0);
        }
        o.addInner(pdInner.poly);
    }

    public boolean drawMultipolygon(Relation r) {
        Collection<Way> inner = new LinkedList<Way>();
        Collection<Way> outer = new LinkedList<Way>();
        Collection<Way> innerclosed = new LinkedList<Way>();
        Collection<Way> outerclosed = new LinkedList<Way>();
        boolean incomplete = false;
        boolean drawn = false;

        for (RelationMember m : r.getMembers())
        {
            if (m.getMember().isDeleted()) {
                putError(r, tr("Deleted member ''{0}'' in relation.",
                        m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
            } else if(m.getMember().isIncomplete()) {
                incomplete = true;
            } else {
                if(m.isWay()) {
                    Way w = m.getWay();
                    if(w.getNodesCount() < 2) {
                        putError(r, tr("Way ''{0}'' with less than two points.",
                                w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                    }
                    else if("inner".equals(m.getRole())) {
                        inner.add(w);
                    } else if("outer".equals(m.getRole())) {
                        outer.add(w);
                    } else {
                        putError(r, tr("No useful role ''{0}'' for Way ''{1}''.",
                                m.getRole(), w.getDisplayName(DefaultNameFormatter.getInstance())), true);
                        if(!m.hasRole()) {
                            outer.add(w);
                        } else if(data.isSelected(r)) {
                            drawSelectedMember(m.getMember(), styles != null
                                    ? getPrimitiveStyle(m.getMember()) : null, true, true);
                        }
                    }
                }
                else
                {
                    putError(r, tr("Non-Way ''{0}'' in multipolygon.",
                            m.getMember().getDisplayName(DefaultNameFormatter.getInstance())), true);
                }
            }
        }

        ElemStyle wayStyle = styles != null ? getPrimitiveStyle(r) : null;
        if(styles != null && (wayStyle == null || !(wayStyle instanceof AreaElemStyle)))
        {
            for (Way w : outer)
            {
                if(wayStyle == null) {
                    wayStyle = styles.getArea(w);
                }
            }
            r.mappaintStyle = wayStyle;
        }

        if(wayStyle != null && wayStyle instanceof AreaElemStyle)
        {
            boolean zoomok = isZoomOk(wayStyle);
            boolean visible = false;
            Collection<Way> outerjoin = new LinkedList<Way>();
            Collection<Way> innerjoin = new LinkedList<Way>();

            drawn = true;
            for (Way w : outer)
            {
                if(w.isClosed()) {
                    outerclosed.add(w);
                } else {
                    outerjoin.add(w);
                }
            }
            for (Way w : inner)
            {
                if(w.isClosed()) {
                    innerclosed.add(w);
                } else {
                    innerjoin.add(w);
                }
            }
            if(outerclosed.size() == 0 && outerjoin.size() == 0)
            {
                putError(r, tr("No outer way for multipolygon ''{0}''.",
                        r.getDisplayName(DefaultNameFormatter.getInstance())), true);
                visible = true; /* prevent killing remaining ways */
            }
            else if(zoomok)
            {
                LinkedList<PolyData> outerPoly = new LinkedList<PolyData>();
                for (Way w : outerclosed) {
                    outerPoly.add(new PolyData(w));
                }
                outerPoly.addAll(joinWays(outerjoin, incomplete ? null : r));
                for (Way wInner : innerclosed)
                {
                    PolyData pdInner = new PolyData(wInner);
                    // incomplete is probably redundant
                    addInnerToOuters(r, incomplete, pdInner, outerPoly);
                }
                for (PolyData pdInner : joinWays(innerjoin, incomplete ? null : r)) {
                    addInnerToOuters(r, incomplete, pdInner, outerPoly);
                }
                AreaElemStyle areaStyle = (AreaElemStyle)wayStyle;
                for (PolyData pd : outerPoly) {
                    Polygon p = pd.get();
                    if(!isPolygonVisible(p)) {
                        continue;
                    }

                    boolean selected = pd.selected || data.isSelected(pd.way) || data.isSelected(r);
                    drawAreaPolygon(p, selected ? selectedColor : areaStyle.color);
                    visible = true;
                }
            }
            if(!visible)
                return drawn;
            for (Way wInner : inner)
            {
                ElemStyle innerStyle = getPrimitiveStyle(wInner);
                if(innerStyle == null)
                {
                    if (data.isSelected(wInner)) {
                        continue;
                    }
                    if(zoomok && (wInner.mappaintDrawnCode != paintid
                            || outer.size() == 0))
                    {
                        drawWay(wInner, ((AreaElemStyle)wayStyle).line,
                                ((AreaElemStyle)wayStyle).color, data.isSelected(wInner)
                                || data.isSelected(r));
                    }
                    wInner.mappaintDrawnCode = paintid;
                }
                else
                {
                    if(data.isSelected(r))
                    {
                        drawSelectedMember(wInner, innerStyle,
                                !wayStyle.equals(innerStyle), data.isSelected(wInner));
                    }
                    if(wayStyle.equals(innerStyle))
                    {
                        putError(r, tr("Style for inner way ''{0}'' equals multipolygon.",
                                wInner.getDisplayName(DefaultNameFormatter.getInstance())), false);
                        if(!data.isSelected(r)) {
                            wInner.mappaintDrawnAreaCode = paintid;
                        }
                    }
                }
            }
            for (Way wOuter : outer)
            {
                ElemStyle outerStyle = getPrimitiveStyle(wOuter);
                if(outerStyle == null)
                {
                    // Selected ways are drawn at the very end
                    if (data.isSelected(wOuter)) {
                        continue;
                    }
                    if(zoomok)
                    {
                        drawWay(wOuter, ((AreaElemStyle)wayStyle).line,
                                ((AreaElemStyle)wayStyle).color, data.isSelected(wOuter)
                                || data.isSelected(r));
                    }
                    wOuter.mappaintDrawnCode = paintid;
                }
                else
                {
                    if(outerStyle instanceof AreaElemStyle
                            && !wayStyle.equals(outerStyle))
                    {
                        putError(r, tr("Style for outer way ''{0}'' mismatches.",
                                wOuter.getDisplayName(DefaultNameFormatter.getInstance())), true);
                    }
                    if(data.isSelected(r))
                    {
                        drawSelectedMember(wOuter, outerStyle, false, false);
                    }
                    else if(outerStyle instanceof AreaElemStyle) {
                        wOuter.mappaintDrawnAreaCode = paintid;
                    }
                }
            }
        }
        return drawn;
    }

    protected Polygon getPolygon(Way w)
    {
        Polygon polygon = new Polygon();

        for (Node n : w.getNodes())
        {
            Point p = nc.getPoint(n);
            polygon.addPoint(p.x,p.y);
        }
        return polygon;
    }

    protected Point2D getCentroid(Polygon p)
    {
        double cx = 0.0, cy = 0.0, a = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            a += (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
            cx += (p.xpoints[i] + p.xpoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
            cy += (p.ypoints[i] + p.ypoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
        }
        return new Point2D.Double(cx / (3.0*a), cy / (3.0*a));
    }

    protected double getArea(Polygon p)
    {
        double sum = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            sum = sum + (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
        }
        return Math.abs(sum/2.0);
    }

    protected void drawArea(Way w, Color color)
    {
        Polygon polygon = getPolygon(w);

        /* set the opacity (alpha) level of the filled polygon */
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
        g.fillPolygon(polygon);

        if (showNames > dist) {
            String name = getWayName(w);
            if (name!=null /* && annotate */) {
                Rectangle pb = polygon.getBounds();
                FontMetrics fontMetrics = g.getFontMetrics(orderFont); // if slow, use cache
                Rectangle2D nb = fontMetrics.getStringBounds(name, g); // if slow, approximate by strlen()*maxcharbounds(font)

                // Point2D c = getCentroid(polygon);
                // Using the Centroid is Nicer for buildings like: +--------+
                // but this needs to be fast.  As most houses are  |   42   |
                // boxes anyway, the center of the bounding box    +---++---+
                // will have to do.                                    ++
                // Centroids are not optimal either, just imagine a U-shaped house.
                // Point2D c = new Point2D.Double(pb.x + pb.width / 2.0, pb.y + pb.height / 2.0);
                // Rectangle2D.Double centeredNBounds =
                //     new Rectangle2D.Double(c.getX() - nb.getWidth()/2,
                //                            c.getY() - nb.getHeight()/2,
                //                            nb.getWidth(),
                //                            nb.getHeight());

                Rectangle centeredNBounds = new Rectangle(pb.x + (int)((pb.width - nb.getWidth())/2.0),
                        pb.y + (int)((pb.height - nb.getHeight())/2.0),
                        (int)nb.getWidth(),
                        (int)nb.getHeight());

                if ((pb.width >= nb.getWidth() && pb.height >= nb.getHeight()) && // quick check
                        polygon.contains(centeredNBounds) // slow but nice
                ) {
                    g.setColor(areaTextColor);
                    Font defaultFont = g.getFont();
                    g.setFont (orderFont);
                    g.drawString (name,
                            (int)(centeredNBounds.getMinX() - nb.getMinX()),
                            (int)(centeredNBounds.getMinY() - nb.getMinY()));
                    g.setFont(defaultFont);
                }
            }
        }
    }

    protected String getWayName(Way w) {
        String name = null;
        if (w.hasKeys()) {
            for (String rn : regionalNameOrder) {
                name = w.get(rn);
                if (name != null) {
                    break;
                }
            }
        }
        return name;
    }

    protected void drawAreaPolygon(Polygon polygon, Color color)
    {
        /* set the opacity (alpha) level of the filled polygon */
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
        g.fillPolygon(polygon);
    }

    protected void drawNode(Node n, ImageIcon icon, boolean annotate, boolean selected) {
        Point p = nc.getPoint(n);
        if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;

        int w = icon.getIconWidth(), h=icon.getIconHeight();
        icon.paintIcon ( Main.map.mapView, g, p.x-w/2, p.y-h/2 );
        if(showNames > dist)
        {
            String name = getNodeName(n);
            if (name!=null && annotate)
            {
                if (inactive || n.isDisabled()) {
                    g.setColor(inactiveColor);
                } else {
                    g.setColor(textColor);
                }
                Font defaultFont = g.getFont();
                g.setFont (orderFont);
                g.drawString (name, p.x+w/2+2, p.y+h/2+2);
                g.setFont(defaultFont);
            }
        }
        if (selected)
        {
            g.setColor (  selectedColor );
            g.drawRect (p.x-w/2-2, p.y-h/2-2, w+4, h+4);
        }
    }

    protected String getNodeName(Node n) {
        String name = null;
        if (n.hasKeys()) {
            for (String rn : regionalNameOrder) {
                name = n.get(rn);
                if (name != null) {
                    break;
                }
            }
        }
        return name;
    }

    private void drawSeg(Node n1, Node n2, Color col, boolean showDirection, int width, float dashed[], Color dashedColor) {
        if (col != currentColor || width != currentWidth || !Arrays.equals(dashed,currentDashed) || dashedColor != currentDashedColor) {
            displaySegments(col, width, dashed, dashedColor);
        }
        Point p1 = nc.getPoint(n1);
        Point p2 = nc.getPoint(n2);

        if (!isSegmentVisible(p1, p2))
            return;
        currentPath.moveTo(p1.x, p1.y);
        currentPath.lineTo(p2.x, p2.y);

        if (showDirection) {
            double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
            currentPath.lineTo((int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
            currentPath.moveTo((int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
            currentPath.lineTo(p2.x, p2.y);
        }
    }

    @Override
    protected void displaySegments() {
        displaySegments(null, 0, new float[0], null);
    }

    protected void displaySegments(Color newColor, int newWidth, float newDash[], Color newDashedColor) {
        if (currentPath != null) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setColor(inactive ? inactiveColor : currentColor);
            if (currentStroke == null && useStrokes > dist) {
                if (currentDashed.length > 0) {
                    try {
                        g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,0,currentDashed,0));
                    } catch (IllegalArgumentException e) {
                        g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    }
                } else {
                    g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                }
            }
            g2d.draw(currentPath);

            if(currentDashedColor != null) {
                g2d.setColor(currentDashedColor);
                if (currentStroke == null && useStrokes > dist) {
                    if (currentDashed.length > 0) {
                        float[] currentDashedOffset = new float[currentDashed.length];
                        System.arraycopy(currentDashed, 1, currentDashedOffset, 0, currentDashed.length - 1);
                        currentDashedOffset[currentDashed.length-1] = currentDashed[0];
                        float offset = currentDashedOffset[0];
                        try {
                            g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,0,currentDashedOffset,offset));
                        } catch (IllegalArgumentException e) {
                            g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                        }
                    } else {
                        g2d.setStroke(new BasicStroke(currentWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    }
                }
                g2d.draw(currentPath);
            }

            if(useStrokes > dist) {
                g2d.setStroke(new BasicStroke(1));
            }

            currentPath = new GeneralPath();
            currentColor = newColor;
            currentWidth = newWidth;
            currentDashed = newDash;
            currentDashedColor = newDashedColor;
            currentStroke = null;
        }
    }

    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n  The node to draw.
     * @param color The color of the node.
     */
    @Override
    public void drawNode(Node n, Color color, int size, int radius, boolean fill) {
        if (isZoomOk(null) && size > 1) {
            Point p = nc.getPoint(n);
            if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth())
                    || (p.y > nc.getHeight()))
                return;

            if (inactive || n.isDisabled()) {
                g.setColor(inactiveColor);
            } else {
                g.setColor(color);
            }
            if (fill) {
                g.fillRect(p.x - radius, p.y - radius, size, size);
                g.drawRect(p.x - radius, p.y - radius, size, size);
            } else {
                g.drawRect(p.x - radius, p.y - radius, size, size);
            }

            if(showNames > dist)
            {
                String name = getNodeName(n);
                if (name!=null /* && annotate */)
                {
                    if (inactive || n.isDisabled()) {
                        g.setColor(inactiveColor);
                    } else {
                        g.setColor(textColor);
                    }
                    Font defaultFont = g.getFont();
                    g.setFont (orderFont);
                    g.drawString (name, p.x+radius+2, p.y+radius+2);
                    g.setFont(defaultFont);
                }
            }
        }
    }

    @Override
    public void getColors()
    {
        super.getColors();
        untaggedColor = Main.pref.getColor(marktr("untagged"),Color.GRAY);
        textColor = Main.pref.getColor (marktr("text"), Color.WHITE);
        areaTextColor = Main.pref.getColor (marktr("areatext"), Color.LIGHT_GRAY);
    }

    DataSet data;

    <T extends OsmPrimitive> Collection<T> selectedLast(final DataSet data, Collection <T> prims) {
        ArrayList<T> sorted = new ArrayList<T>(prims);
        Collections.sort(sorted,
                new Comparator<T>() {
            public int compare(T o1, T o2) {
                boolean s1 = data.isSelected(o1);
                boolean s2 = data.isSelected(o2);
                if (s1 && !s2)
                    return 1;
                if (!s1 && s2)
                    return -1;
                return o1.compareTo(o2);
            }
        });
        return sorted;
    }

    /* Shows areas before non-areas */
    @Override
    public void visitAll(DataSet data, boolean virtual, Bounds bounds) {
        BBox bbox = new BBox(bounds);
        this.data = data;

        useStyleCache = Main.pref.getBoolean("mappaint.cache", true);
        int fillAreas = Main.pref.getInteger("mappaint.fillareas", 10000000);
        fillAlpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fillalpha", 50))));
        showNames = Main.pref.getInteger("mappaint.shownames", 10000000);
        showIcons = Main.pref.getInteger("mappaint.showicons", 10000000);
        useStrokes = Main.pref.getInteger("mappaint.strokes", 10000000);
        LatLon ll1 = nc.getLatLon(0, 0);
        LatLon ll2 = nc.getLatLon(100, 0);
        dist = ll1.greatCircleDistance(ll2);

        getSettings(virtual);
        useRealWidth = Main.pref.getBoolean("mappaint.useRealWidth", false);
        zoomLevelDisplay = Main.pref.getBoolean("mappaint.zoomLevelDisplay", false);
        circum = Main.map.mapView.getDist100Pixel();
        styles = MapPaintStyles.getStyles().getStyleSet();
        drawMultipolygon = Main.pref.getBoolean("mappaint.multipolygon", true);
        drawRestriction = Main.pref.getBoolean("mappaint.restriction", true);
        leftHandTraffic = Main.pref.getBoolean("mappaint.lefthandtraffic", false);
        orderFont = new Font(Main.pref.get("mappaint.font", "Helvetica"), Font.PLAIN, Main.pref.getInteger("mappaint.fontsize", 8));
        String[] names = {"name:" + LanguageInfo.getJOSMLocaleCode(), "name", "int_name", "ref", "operator", "brand", "addr:housenumber"};
        regionalNameOrder = Main.pref.getCollection("mappaint.nameOrder", Arrays.asList(names));
        minEN = nc.getEastNorth(0, nc.getHeight() - 1);
        maxEN = nc.getEastNorth(nc.getWidth() - 1, 0);

        data.clearErrors();

        ++paintid;

        if (fillAreas > dist && styles != null && styles.hasAreas()) {
            Collection<Way> noAreaWays = new LinkedList<Way>();

            /*** RELATIONS ***/
            for (final Relation osm: data.getRelations()) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** AREAS ***/
            for (final Way osm : selectedLast(data, data.searchWays(bbox))) {
                if (osm.isDrawable() && osm.mappaintDrawnCode != paintid) {
                    if (isPrimitiveArea(osm) && osm.mappaintDrawnAreaCode != paintid) {
                        drawWay(osm, fillAreas);
                    } else {
                        noAreaWays.add(osm);
                    }
                }
            }

            /*** WAYS ***/
            for (final Way osm : noAreaWays) {
                drawWay(osm, 0);
            }
        } else {
            drawMultipolygon = false;

            /*** RELATIONS ***/
            for (final Relation osm: data.getRelations()) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** WAYS (filling disabled)  ***/
            for (final Way way: data.getWays()) {
                if (way.isDrawable() && !data.isSelected(way)) {
                    drawWay(way, 0);
                }
            }
        }

        /*** SELECTED  ***/
        for (final OsmPrimitive osm : data.getSelected()) {
            if (osm.isUsable() && !(osm instanceof Node) && osm.mappaintDrawnCode != paintid
            ) {
                osm.visit(new AbstractVisitor() {
                    public void visit(Way w) {
                        drawWay(w, 0);
                    }

                    public void visit(Node n) {
                        drawNode(n);
                    }

                    public void visit(Relation r) {
                        /* TODO: is it possible to do this like the nodes/ways code? */
                        for (RelationMember m : r.getMembers()) {
                            if (m.isNode() && m.getMember().isDrawable()) {
                                drawSelectedMember(m.getMember(), styles != null ? getPrimitiveStyle(m.getMember()) : null, true, true);
                            }
                        }
                    }
                });
            }
        }

        /*** DISPLAY CACHED SEGMENTS (WAYS) NOW ***/
        displaySegments();

        /*** NODES ***/
        for (final Node osm: data.searchNodes(bbox)) {
            if (!osm.isIncomplete() && !osm.isDeleted() && (data.isSelected(osm) || !osm.isFiltered())
                    && osm.mappaintDrawnCode != paintid)
            {
                drawNode(osm);
            }
        }

        /*** VIRTUAL  ***/
        if (virtualNodeSize != 0) {
            currentColor = nodeColor;
            for (final OsmPrimitive osm: data.searchWays(bbox)) {
                if (osm.isUsable() && !osm.isFiltered())
                {
                    /* TODO: move this into the SimplePaint code? */
                    visitVirtual((Way)osm);
                }
            }
            displaySegments(null);
        }
    }

    /**
     * Draw a number of the order of the two consecutive nodes within the
     * parents way
     */
    protected void drawOrderNumber(Node n1, Node n2, int orderNumber) {
        Point p1 = nc.getPoint(n1);
        Point p2 = nc.getPoint(n2);
        drawOrderNumber(p1, p2, orderNumber);
    }

    public void putError(OsmPrimitive p, String text, boolean isError)
    {
        data.addError(p, isError ? tr("Error: {0}", text) : tr("Warning: {0}", text));
    }
}
