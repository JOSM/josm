// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle.HorizontalTextAlignment;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle.Symbol;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle.TextElement;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle.VerticalTextAlignment;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;

public class MapPainter {

    private final Graphics2D g;
    private final NavigatableComponent nc;
    private final boolean inactive;

    private final boolean useStrokes;
    private final boolean showNames;
    private final boolean showIcons;

    private final Color inactiveColor;
    private final Color textColor;
    private final Color selectedColor;
    private final Color relationSelectedColor;
    private final Color areaTextColor;
    private final Color nodeColor;
    private final Color backgroundColor;

    private final Font orderFont;
    private final int virtualNodeSize;
    private final int virtualNodeSpace;
    private final int segmentNumberSpace;

    private final double circum;

    private final boolean leftHandTraffic;

    private final Collection<String> regionalNameOrder;

    private static final double PHI = Math.toRadians(20);
    private static final double cosPHI = Math.cos(PHI);
    private static final double sinPHI = Math.sin(PHI);

    public MapPainter(MapPaintSettings settings, Graphics2D g,
            boolean inactive, NavigatableComponent nc, boolean virtual,
            double circum, boolean leftHandTraffic)
    {
        this.g = g;
        this.inactive = inactive;
        this.nc = nc;
        this.useStrokes = settings.getUseStrokesDistance() > circum;
        this.showNames = settings.getShowNamesDistance() > circum;
        this.showIcons = settings.getShowIconsDistance() > circum;

        this.inactiveColor = PaintColors.INACTIVE.get();
        this.textColor = PaintColors.TEXT.get();
        this.selectedColor = PaintColors.SELECTED.get();
        this.relationSelectedColor = PaintColors.RELATIONSELECTED.get();
        this.areaTextColor = PaintColors.AREA_TEXT.get();
        this.nodeColor = PaintColors.NODE.get();
        this.backgroundColor = PaintColors.BACKGROUND.get();

        this.orderFont = new Font(Main.pref.get("mappaint.font", "Helvetica"), Font.PLAIN, Main.pref.getInteger("mappaint.fontsize", 8));
        this.virtualNodeSize = virtual ? Main.pref.getInteger("mappaint.node.virtual-size", 8) / 2 : 0;
        this.virtualNodeSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
        this.segmentNumberSpace = Main.pref.getInteger("mappaint.segmentnumber.space", 40);

        String[] names = {"name:" + LanguageInfo.getJOSMLocaleCode(), "name", "int_name", "ref", "operator", "brand", "addr:housenumber"};
        this.regionalNameOrder = Main.pref.getCollection("mappaint.nameOrder", Arrays.asList(names));
        this.circum = circum;
        this.leftHandTraffic = leftHandTraffic;
    }

    public void drawWay(Way way, Color color, BasicStroke line, BasicStroke dashes, Color dashedColor, boolean showDirection,
            boolean reversedDirection, boolean showHeadArrowOnly) {

        GeneralPath path = new GeneralPath();
        GeneralPath arrows = new GeneralPath();
        Rectangle bounds = g.getClipBounds();
        bounds.grow(100, 100);                  // avoid arrow heads at the border

        Point lastPoint = null;
        boolean initialMoveToNeeded = true;
        Iterator<Node> it = way.getNodes().iterator();
        while (it.hasNext()) {
            Node n = it.next();
            Point p = nc.getPoint(n);
            if(lastPoint != null) {
                Point p1 = lastPoint;
                Point p2 = p;

                /**
                 * Do custom clipping to work around openjdk bug. It leads to
                 * drawing artefacts when zooming in a lot. (#4289, #4424)
                 * (Looks like int overflow.)
                 */
                LineClip clip = new LineClip(p1, p2, bounds);
                if (clip.execute()) {
                    if (!p1.equals(clip.getP1())) {
                        p1 = clip.getP1();
                        path.moveTo(p1.x, p1.y);
                    } else if (initialMoveToNeeded) {
                        initialMoveToNeeded = false;
                        path.moveTo(p1.x, p1.y);
                    }
                    p2 = clip.getP2();
                    path.lineTo(p2.x, p2.y);

                    /* draw arrow */
                    if (showHeadArrowOnly ? !it.hasNext() : showDirection) {
                        if (reversedDirection) {
                            Point tmp = p1;
                            p1 = p2;
                            p2 = tmp;
                        }
                        final double l =  10. / p1.distance(p2);

                        final double sx = l * (p1.x - p2.x);
                        final double sy = l * (p1.y - p2.y);

                        arrows.moveTo(p2.x, p2.y);
                        arrows.lineTo (p2.x + (int) Math.round(cosPHI * sx - sinPHI * sy), p2.y + (int) Math.round(sinPHI * sx + cosPHI * sy));
                        arrows.moveTo (p2.x + (int) Math.round(cosPHI * sx + sinPHI * sy), p2.y + (int) Math.round(- sinPHI * sx + cosPHI * sy));
                        arrows.lineTo(p2.x, p2.y);
                    }
                }
            }
            lastPoint = p;
        }
        displaySegments(path, arrows, color, line, dashes, dashedColor);
    }

    private void displaySegments(GeneralPath path, GeneralPath arrows, Color color, BasicStroke line, BasicStroke dashes, Color dashedColor) {
        g.setColor(inactive ? inactiveColor : color);
        if (useStrokes) {
            g.setStroke(line);
        }
        g.draw(path);
        g.draw(arrows);

        if(!inactive && useStrokes && dashes != null) {
            g.setColor(dashedColor);
            g.setStroke(dashes);
            g.draw(path);
            g.draw(arrows);
        }

        if(useStrokes) {
            g.setStroke(new BasicStroke());
        }
    }

    private boolean isSegmentVisible(Point p1, Point p2) {
        if ((p1.x < 0) && (p2.x < 0)) return false;
        if ((p1.y < 0) && (p2.y < 0)) return false;
        if ((p1.x > nc.getWidth()) && (p2.x > nc.getWidth())) return false;
        if ((p1.y > nc.getHeight()) && (p2.y > nc.getHeight())) return false;
        return true;
    }

    public void drawNodeIcon(Node n, ImageIcon icon, float iconAlpha, boolean selected, boolean member, TextElement text) {
        Point p = nc.getPoint(n);
        if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;

        int w = icon.getIconWidth(), h=icon.getIconHeight();
        if (iconAlpha != 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, iconAlpha));
        }
        icon.paintIcon ( nc, g, p.x-w/2, p.y-h/2 );
        g.setPaintMode();
        drawNodeText(n, text, p, w/2, h/2);
        if (selected || member)
        {
            g.setColor(selected? selectedColor : relationSelectedColor);
            g.drawRect(p.x-w/2-2, p.y-h/2-2, w+4, h+4);
        }
    }

    public void drawNodeSymbol(Node n, Symbol s, Color fillColor, Color strokeColor, TextElement text) {
        Point p = nc.getPoint(n);
        if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;
        int radius = s.size / 2;

        if (fillColor != null) {
            g.setColor(fillColor);
            switch (s.symbol) {
                case SQUARE:
                    g.fillRect(p.x - radius, p.y - radius, s.size, s.size);
                    break;
                case CIRCLE:
                    g.fillOval(p.x - radius, p.y - radius, s.size, s.size);
                    break;
                default:
                    throw new AssertionError();
            }
        }
        if (s.stroke != null) {
            g.setStroke(s.stroke);
            g.setColor(strokeColor);
            switch (s.symbol) {
                case SQUARE:
                    g.drawRect(p.x - radius, p.y - radius, s.size - 1, s.size - 1);
                    break;
                case CIRCLE:
                    g.drawOval(p.x - radius, p.y - radius, s.size - 1, s.size - 1);
                    break;
                default:
                    throw new AssertionError();
            }
            g.setStroke(new BasicStroke());
        }
        drawNodeText(n, text, p, radius, radius);
    }

    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n  The node to draw.
     * @param color The color of the node.
     */
    public void drawNode(Node n, Color color, int size, boolean fill, TextElement text) {
        if (size > 1) {
            Point p = nc.getPoint(n);
            if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;
            int radius = size / 2;

            if (inactive || n.isDisabled()) {
                g.setColor(inactiveColor);
            } else {
                g.setColor(color);
            }
            if (fill) {
                g.fillRect(p.x - radius, p.y - radius, size + 1, size + 1);
            } else {
                g.drawRect(p.x - radius, p.y - radius, size, size);
            }

            drawNodeText(n, text, p, radius, radius + 4);
        }
    }

    private void drawNodeText(Node n, TextElement text, Point p, int w_half, int h_half) {
        if (!isShowNames() || text == null)
            return;

        String s = text.textKey == null ? getNodeName(n) : n.get(text.textKey);
        if (s == null)
            return;

        if (inactive || n.isDisabled()) {
            g.setColor(inactiveColor);
        } else {
            g.setColor(text.color);
        }
        Font defaultFont = g.getFont();
        g.setFont(text.font);

        int x = p.x + text.xOffset;
        int y = p.y + text.yOffset;
        /**
         *
         *       left-above __center-above___ right-above
         *         left-top|                 |right-top
         *                 |                 |
         *      left-center|  center-center  |right-center
         *                 |                 |
         *      left-bottom|_________________|right-bottom
         *       left-below   center-below    right-below
         *
         */
        if (text.hAlign == HorizontalTextAlignment.RIGHT) {
            x += w_half + 2;
        } else {
            FontRenderContext frc = g.getFontRenderContext();
            Rectangle2D bounds = text.font.getStringBounds(s, frc);
            int textWidth = (int) bounds.getWidth();
            if (text.hAlign == HorizontalTextAlignment.CENTER) {
                x -= textWidth / 2;
            } else if (text.hAlign == HorizontalTextAlignment.LEFT) {
                x -= w_half + 4 + textWidth;
            } else throw new AssertionError();
        }

        if (text.vAlign == VerticalTextAlignment.BOTTOM) {
            y += h_half - 2;
        } else {
            FontRenderContext frc = g.getFontRenderContext();
            LineMetrics metrics = text.font.getLineMetrics(s, frc);
            if (text.vAlign == VerticalTextAlignment.ABOVE) {
                y -= h_half + metrics.getDescent();
            } else if (text.vAlign == VerticalTextAlignment.TOP) {
                y -= h_half - metrics.getAscent();
            } else if (text.vAlign == VerticalTextAlignment.CENTER) {
                y += (metrics.getAscent() - metrics.getDescent()) / 2;
            } else if (text.vAlign == VerticalTextAlignment.BELOW) {
                y += h_half + metrics.getAscent();
            } else throw new AssertionError();
        }
        g.drawString(s, x, y);
        g.setFont(defaultFont);
    }

    private Polygon getPolygon(Way w) {
        Polygon polygon = new Polygon();

        for (Node n : w.getNodes()) {
            Point p = nc.getPoint(n);
            polygon.addPoint(p.x,p.y);
        }
        return polygon;
    }

    public void drawArea(Way w, Color color, BufferedImage fillImage, float fillImageAlpha, String name) {
        Polygon polygon = getPolygon(w);
        drawArea(polygon, color, fillImage, fillImageAlpha, name);
    }

    protected void drawArea(Polygon polygon, Color color, BufferedImage fillImage, float fillImageAlpha, String name) {

        if (fillImage == null) {
            g.setColor(color);
            g.fillPolygon(polygon);
        } else {
            TexturePaint texture = new TexturePaint(fillImage, 
                    new Rectangle(polygon.xpoints[0], polygon.ypoints[0], fillImage.getWidth(), fillImage.getHeight()));
            g.setPaint(texture);
            if (fillImageAlpha != 1f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillImageAlpha));
            }
            g.fill(polygon);
            g.setPaintMode();
        }

        if (name != null) {
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

    public void drawArea(Relation r, Color color, BufferedImage fillImage, float fillImageAlpha, String name) {
        Multipolygon multipolygon = new Multipolygon(nc);
        multipolygon.load(r);
        if(!r.isDisabled() && !multipolygon.getOuterWays().isEmpty()) {
            for (PolyData pd : multipolygon.getCombinedPolygons()) {
                Polygon p = pd.get();
                if(!isPolygonVisible(p)) {
                    continue;
                }
                drawArea(p, color, fillImage, fillImageAlpha, getAreaName(r));
            }
        }
    }

    private boolean isPolygonVisible(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        if (bounds.width == 0 && bounds.height == 0) return false;
        if (bounds.x > nc.getWidth()) return false;
        if (bounds.y > nc.getHeight()) return false;
        if (bounds.x + bounds.width < 0) return false;
        if (bounds.y + bounds.height < 0) return false;
        return true;
    }

    public void drawRestriction(ImageIcon icon, Point pVia, double vx, double vx2, double vy, double vy2, double iconAngle, boolean selected) {
        /* rotate icon with direction last node in from to */
        ImageIcon rotatedIcon = ImageProvider.createRotatedImage(null /*icon2*/, icon, iconAngle);

        /* scale down icon to 16*16 pixels */
        ImageIcon smallIcon = new ImageIcon(rotatedIcon.getImage().getScaledInstance(16 , 16, Image.SCALE_SMOOTH));
        int w = smallIcon.getIconWidth(), h=smallIcon.getIconHeight();
        smallIcon.paintIcon (nc, g, (int)(pVia.x+vx+vx2)-w/2, (int)(pVia.y+vy+vy2)-h/2 );

        if (selected) {
            g.setColor(relationSelectedColor);
            g.drawRect((int)(pVia.x+vx+vx2)-w/2-2,(int)(pVia.y+vy+vy2)-h/2-2, w+4, h+4);
        }
    }

    public void drawRestriction(Relation r, NodeElemStyle icon) {

        Way fromWay = null;
        Way toWay = null;
        OsmPrimitive via = null;

        /* find the "from", "via" and "to" elements */
        for (RelationMember m : r.getMembers())
        {
            if(m.getMember().isIncomplete())
                return;
            else
            {
                if(m.isWay())
                {
                    Way w = m.getWay();
                    if(w.getNodesCount() < 2) {
                        continue;
                    }

                    if("from".equals(m.getRole())) {
                        if(fromWay == null) {
                            fromWay = w;
                        }
                    } else if("to".equals(m.getRole())) {
                        if(toWay == null) {
                            toWay = w;
                        }
                    } else if("via".equals(m.getRole())) {
                        if(via == null) {
                            via = w;
                        }
                    }
                }
                else if(m.isNode())
                {
                    Node n = m.getNode();
                    if("via".equals(m.getRole()) && via == null) {
                        via = n;
                    }
                }
            }
        }

        if (fromWay == null || toWay == null || via == null)
            return;

        Node viaNode;
        if(via instanceof Node)
        {
            viaNode = (Node) via;
            if(!fromWay.isFirstLastNode(viaNode))
                return;
        }
        else
        {
            Way viaWay = (Way) via;
            Node firstNode = viaWay.firstNode();
            Node lastNode = viaWay.lastNode();
            Boolean onewayvia = false;

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
                    if (onewayvia == null) {
                        onewayvia = false;
                    }
                }
            }

            if(fromWay.isFirstLastNode(firstNode)) {
                viaNode = firstNode;
            } else if (!onewayvia && fromWay.isFirstLastNode(lastNode)) {
                viaNode = lastNode;
            } else
                return;
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

        drawRestriction(inactive || r.isDisabled() ? icon.getDisabledIcon() : icon.icon,
                pVia, vx, vx2, vy, vy2, iconAngle, r.isSelected());
    }

    public void drawVirtualNodes(Collection<Way> ways) {

        if (virtualNodeSize != 0) {
            GeneralPath path = new GeneralPath();
            for (Way osm: ways){
                if (osm.isUsable() && !osm.isDisabled()) {
                    visitVirtual(path, osm);
                }
            }
            g.setColor(nodeColor);
            g.draw(path);
        }
    }

    public void visitVirtual(GeneralPath path, Way w) {
        Iterator<Node> it = w.getNodes().iterator();
        if (it.hasNext()) {
            Point lastP = nc.getPoint(it.next());
            while(it.hasNext())
            {
                Point p = nc.getPoint(it.next());
                if(isSegmentVisible(lastP, p) && isLargeSegment(lastP, p, virtualNodeSpace))
                {
                    int x = (p.x+lastP.x)/2;
                    int y = (p.y+lastP.y)/2;
                    path.moveTo(x-virtualNodeSize, y);
                    path.lineTo(x+virtualNodeSize, y);
                    path.moveTo(x, y-virtualNodeSize);
                    path.lineTo(x, y+virtualNodeSize);
                }
                lastP = p;
            }
        }
    }

    private static boolean isLargeSegment(Point p1, Point p2, int space)  {
        int xd = p1.x-p2.x; if(xd < 0) {
            xd = -xd;
        }
        int yd = p1.y-p2.y; if(yd < 0) {
            yd = -yd;
        }
        return (xd+yd > space);
    }

    /**
     * Draw a number of the order of the two consecutive nodes within the
     * parents way
     */
    public void drawOrderNumber(Node n1, Node n2, int orderNumber) {
        Point p1 = nc.getPoint(n1);
        Point p2 = nc.getPoint(n2);
        drawOrderNumber(p1, p2, orderNumber);
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

            Color c = g.getColor();
            g.setColor(backgroundColor);
            g.fillRect(x-1, y-12, 8*strlen+1, 14);
            g.setColor(c);
            g.drawString(on, x, y);
        }
    }

    //TODO Not a good place for this method
    public String getNodeName(Node n) {
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

    //TODO Not a good place for this method
    public String getAreaName(OsmPrimitive w) {
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

    public boolean isInactive() {
        return inactive;
    }

    public boolean isShowNames() {
        return showNames;
    }

    public double getCircum() {
        return circum;
    }

    public boolean isShowIcons() {
        return showIcons;
    }

}
