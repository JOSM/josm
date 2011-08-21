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
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.BoxTextElemStyle;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle;
import org.openstreetmap.josm.gui.mappaint.TextElement;
import org.openstreetmap.josm.gui.mappaint.BoxTextElemStyle.HorizontalTextAlignment;
import org.openstreetmap.josm.gui.mappaint.BoxTextElemStyle.VerticalTextAlignment;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle.Symbol;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Pair;

public class MapPainter {

    private final Graphics2D g;
    private final NavigatableComponent nc;
    private final boolean inactive;
    private final MapPaintSettings settings;
    private final Collection<WaySegment> highlightWaySegments;

    private final boolean useStrokes;
    private final boolean showNames;
    private final boolean showIcons;

    private final boolean  isOutlineOnly;

    private final Color inactiveColor;
    private final Color selectedColor;
    private final Color relationSelectedColor;
    private final Color nodeColor;
    private final Color highlightColor;
    private final Color highlightColorTransparent;
    private final Color backgroundColor;

    private final Font orderFont;
    private final int virtualNodeSize;
    private final int virtualNodeSpace;
    private final int segmentNumberSpace;

    private final double circum;

    private final boolean leftHandTraffic;

    private static final double PHI = Math.toRadians(20);
    private static final double cosPHI = Math.cos(PHI);
    private static final double sinPHI = Math.sin(PHI);

    public MapPainter(MapPaintSettings settings, Graphics2D g,
            boolean inactive, NavigatableComponent nc, boolean virtual,
            double circum, boolean leftHandTraffic,
            Collection<WaySegment> highlightWaySegments){
        this.settings = settings;
        this.g = g;
        this.inactive = inactive;
        this.nc = nc;
        this.highlightWaySegments = highlightWaySegments;
        this.useStrokes = settings.getUseStrokesDistance() > circum;
        this.showNames = settings.getShowNamesDistance() > circum;
        this.showIcons = settings.getShowIconsDistance() > circum;

        this.isOutlineOnly = settings.isOutlineOnly();

        this.inactiveColor = PaintColors.INACTIVE.get();
        this.selectedColor = PaintColors.SELECTED.get();
        this.relationSelectedColor = PaintColors.RELATIONSELECTED.get();
        this.nodeColor = PaintColors.NODE.get();
        this.highlightColor = PaintColors.HIGHLIGHT.get();
        this.highlightColorTransparent = new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 100);
        this.backgroundColor = PaintColors.getBackgroundColor();

        this.orderFont = new Font(Main.pref.get("mappaint.font", "Helvetica"), Font.PLAIN, Main.pref.getInteger("mappaint.fontsize", 8));
        this.virtualNodeSize = virtual ? Main.pref.getInteger("mappaint.node.virtual-size", 8) / 2 : 0;
        this.virtualNodeSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
        this.segmentNumberSpace = Main.pref.getInteger("mappaint.segmentnumber.space", 40);

        this.circum = circum;
        this.leftHandTraffic = leftHandTraffic;
    }

    /**
     * draw way
     * @param showOrientation show arrows that indicate the technical orientation of
     *              the way (defined by order of nodes)
     * @param showOneway show symbols that indicate the direction of the feature,
     *              e.g. oneway street or waterway
     * @param onewayReversed for oneway=-1 and similar
     */
    public void drawWay(Way way, Color color, BasicStroke line, BasicStroke dashes, Color dashedColor, int offset,
            boolean showOrientation, boolean showHeadArrowOnly,
            boolean showOneway, boolean onewayReversed) {

        GeneralPath path = new GeneralPath();
        GeneralPath orientationArrows = showOrientation ? new GeneralPath() : null;
        GeneralPath onewayArrows = showOneway ? new GeneralPath() : null;
        GeneralPath onewayArrowsCasing = showOneway ? new GeneralPath() : null;
        Rectangle bounds = g.getClipBounds();
        bounds.grow(100, 100);                  // avoid arrow heads at the border

        double wayLength = 0;
        Point lastPoint = null;
        boolean initialMoveToNeeded = true;
        List<Node> wayNodes = way.getNodes();
        if (wayNodes.size() < 2) return;

        // only highlight the segment if the way itself is not highlighted
        if(!way.isHighlighted()) {
            GeneralPath highlightSegs = null;
            for(WaySegment ws : highlightWaySegments) {
                if(ws.way != way || ws.lowerIndex < offset) {
                    continue;
                }
                if(highlightSegs == null) {
                    highlightSegs = new GeneralPath();
                }

                Point p1 = nc.getPoint(ws.getFirstNode());
                Point p2 = nc.getPoint(ws.getSecondNode());
                highlightSegs.moveTo(p1.x, p1.y);
                highlightSegs.lineTo(p2.x, p2.y);
            }

            drawPathHighlight(highlightSegs, line);
        }


        Iterator<Point> it = new OffsetIterator(wayNodes, offset);
        while (it.hasNext()) {
            Point p = it.next();
            if (lastPoint != null) {
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
                    if (showHeadArrowOnly ? !it.hasNext() : showOrientation) {
                        final double segmentLength = p1.distance(p2);
                        if (segmentLength != 0.0) {
                            final double l =  (10. + line.getLineWidth()) / segmentLength;

                            final double sx = l * (p1.x - p2.x);
                            final double sy = l * (p1.y - p2.y);

                            orientationArrows.moveTo (p2.x + cosPHI * sx - sinPHI * sy, p2.y + sinPHI * sx + cosPHI * sy);
                            orientationArrows.lineTo(p2.x, p2.y);
                            orientationArrows.lineTo (p2.x + cosPHI * sx + sinPHI * sy, p2.y - sinPHI * sx + cosPHI * sy);
                        }
                    }
                    if (showOneway) {
                        final double segmentLength = p1.distance(p2);
                        if (segmentLength != 0.0) {
                            final double nx = (p2.x - p1.x) / segmentLength;
                            final double ny = (p2.y - p1.y) / segmentLength;

                            final double interval = 60;
                            // distance from p1
                            double dist = interval - (wayLength % interval);

                            while (dist < segmentLength) {
                                for (Pair<Float, GeneralPath> sizeAndPath : Arrays.asList(new Pair[] {
                                        new Pair<Float, GeneralPath>(3f, onewayArrowsCasing),
                                        new Pair<Float, GeneralPath>(2f, onewayArrows)})) {

                                    // scale such that border is 1 px
                                    final double fac = - (onewayReversed ? -1 : 1) * sizeAndPath.a * (1 + sinPHI) / (sinPHI * cosPHI);
                                    final double sx = nx * fac;
                                    final double sy = ny * fac;

                                    // Attach the triangle at the incenter and not at the tip.
                                    // Makes the border even at all sides.
                                    final double x = p1.x + nx * (dist + (onewayReversed ? -1 : 1) * (sizeAndPath.a / sinPHI));
                                    final double y = p1.y + ny * (dist + (onewayReversed ? -1 : 1) * (sizeAndPath.a / sinPHI));

                                    sizeAndPath.b.moveTo(x, y);
                                    sizeAndPath.b.lineTo (x + cosPHI * sx - sinPHI * sy, y + sinPHI * sx + cosPHI * sy);
                                    sizeAndPath.b.lineTo (x + cosPHI * sx + sinPHI * sy, y - sinPHI * sx + cosPHI * sy);
                                    sizeAndPath.b.lineTo(x, y);
                                }
                                dist += interval;
                            }
                        }
                        wayLength += segmentLength;
                    }
                }
            }
            lastPoint = p;
        }
        if(way.isHighlighted()) {
            drawPathHighlight(path, line);
        }
        displaySegments(path, orientationArrows, onewayArrows, onewayArrowsCasing, color, line, dashes, dashedColor);
    }

    /**
     * Iterates over a list of Way Nodes and returns screen coordinates that
     * represent a line that is shifted by a certain offset perpendicular
     * to the way direction.
     * 
     * There is no intention, to handle consecutive duplicate Nodes in a
     * perfect way, but it is should not throw an exception.
     */
    public class OffsetIterator implements Iterator<Point> {

        private List<Node> nodes;
        private int offset;
        private int idx;

        private Point prev = null;
        /* 'prev0' is a point that has distance 'offset' from 'prev' and the
         * line from 'prev' to 'prev0' is perpendicular to the way segment from
         * 'prev' to the next point.
         */
        private int x_prev0, y_prev0;

        public OffsetIterator(List<Node> nodes, int offset) {
            this.nodes = nodes;
            this.offset = offset;
            idx = 0;
        }

        @Override
        public boolean hasNext() {
            return idx < nodes.size();
        }

        @Override
        public Point next() {
            if (offset == 0) return nc.getPoint(nodes.get(idx++));

            Point current = nc.getPoint(nodes.get(idx));

            if (idx == nodes.size() - 1) {
                ++idx;
                return new Point(x_prev0 + current.x - prev.x, y_prev0 + current.y - prev.y);
            }

            Point next = nc.getPoint(nodes.get(idx+1));

            int dx_next = next.x - current.x;
            int dy_next = next.y - current.y;
            double len_next = Math.sqrt(dx_next*dx_next + dy_next*dy_next);

            if (len_next == 0) {
                len_next = 1; // value does not matter, because dy_next and dx_next is 0
            }

            int x_current0 = current.x + (int) Math.round(offset * dy_next / len_next);
            int y_current0 = current.y - (int) Math.round(offset * dx_next / len_next);

            if (idx==0) {
                ++idx;
                prev = current;
                x_prev0 = x_current0;
                y_prev0 = y_current0;
                return new Point(x_current0, y_current0);
            } else {
                int dx_prev = current.x - prev.x;
                int dy_prev = current.y - prev.y;

                // determine intersection of the lines parallel to the two
                // segments
                int det = dx_next*dy_prev - dx_prev*dy_next;

                if (det == 0) {
                    ++idx;
                    prev = current;
                    x_prev0 = x_current0;
                    y_prev0 = y_current0;
                    return new Point(x_current0, y_current0);
                }

                int m = dx_next*(y_current0 - y_prev0) - dy_next*(x_current0 - x_prev0);

                int cx_ = x_prev0 + Math.round(m * dx_prev / det);
                int cy_ = y_prev0 + Math.round(m * dy_prev / det);
                ++idx;
                prev = current;
                x_prev0 = x_current0;
                y_prev0 = y_current0;
                return new Point(cx_, cy_);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private void displaySegments(GeneralPath path, GeneralPath orientationArrows, GeneralPath onewayArrows, GeneralPath onewayArrowsCasing,
            Color color, BasicStroke line, BasicStroke dashes, Color dashedColor) {
        g.setColor(inactive ? inactiveColor : color);
        if (useStrokes) {
            g.setStroke(line);
        }
        g.draw(path);

        if(!inactive && useStrokes && dashes != null) {
            g.setColor(dashedColor);
            g.setStroke(dashes);
            g.draw(path);
        }

        if (orientationArrows != null) {
            g.setColor(inactive ? inactiveColor : color);
            g.setStroke(new BasicStroke(line.getLineWidth(), line.getEndCap(), BasicStroke.JOIN_MITER, line.getMiterLimit()));
            g.draw(orientationArrows);
        }

        if (onewayArrows != null) {
            g.setStroke(new BasicStroke(1, line.getEndCap(), BasicStroke.JOIN_MITER, line.getMiterLimit()));
            g.fill(onewayArrowsCasing);
            g.setColor(inactive ? inactiveColor : backgroundColor);
            g.fill(onewayArrows);
        }

        if(useStrokes) {
            g.setStroke(new BasicStroke());
        }
    }

    /**
     * highlights a given GeneralPath using the settings from BasicStroke to match the line's
     * style. Width of the highlight is hard coded.
     * @param path
     * @param line
     */
    private void drawPathHighlight(GeneralPath path, BasicStroke line) {
        if(path == null)
            return;
        g.setColor(highlightColorTransparent);
        float w = (line.getLineWidth() + 4);
        while(w >= line.getLineWidth()) {
            g.setStroke(new BasicStroke(w, line.getEndCap(), line.getLineJoin(), line.getMiterLimit()));
            g.draw(path);
            w -= 4;
        }
    }

    private boolean isSegmentVisible(Point p1, Point p2) {
        if ((p1.x < 0) && (p2.x < 0)) return false;
        if ((p1.y < 0) && (p2.y < 0)) return false;
        if ((p1.x > nc.getWidth()) && (p2.x > nc.getWidth())) return false;
        if ((p1.y > nc.getHeight()) && (p2.y > nc.getHeight())) return false;
        return true;
    }

    public void drawTextOnPath(Way way, TextElement text) {
        if (text == null)
            return;
        String name = text.getString(way);
        if (name == null || name.equals(""))
            return;

        Polygon poly = new Polygon();
        Point lastPoint = null;
        Iterator<Node> it = way.getNodes().iterator();
        double pathLength = 0;
        int dx, dy;
        while (it.hasNext()) {
            Node n = it.next();
            Point p = nc.getPoint(n);
            poly.addPoint(p.x, p.y);

            if(lastPoint != null) {
                dx = p.x - lastPoint.x;
                dy = p.y - lastPoint.y;
                pathLength += Math.sqrt(dx*dx + dy*dy);
            }
            lastPoint = p;
        }

        FontMetrics fontMetrics = g.getFontMetrics(text.font); // if slow, use cache
        Rectangle2D rec = fontMetrics.getStringBounds(name, g); // if slow, approximate by strlen()*maxcharbounds(font)

        if (rec.getWidth() > pathLength)
            return;

        double t1 = (pathLength/2 - rec.getWidth()/2) / pathLength;
        double t2 = (pathLength/2 + rec.getWidth()/2) / pathLength;

        double[] p1 = pointAt(t1, poly, pathLength);
        double[] p2 = pointAt(t2, poly, pathLength);

        double angleOffset;
        double offsetSign;
        double tStart;

        if (p1[0] < p2[0] &&
                p1[2] < Math.PI/2 &&
                p1[2] > -Math.PI/2) {
            angleOffset = 0;
            offsetSign = 1;
            tStart = t1;
        } else {
            angleOffset = Math.PI;
            offsetSign = -1;
            tStart = t2;
        }

        FontRenderContext frc = g.getFontRenderContext();
        GlyphVector gv = text.font.createGlyphVector(frc, name);

        for (int i=0; i<gv.getNumGlyphs(); ++i) {
            Rectangle2D rect = gv.getGlyphLogicalBounds(i).getBounds2D();
            double t = tStart + offsetSign * (rect.getX() + rect.getWidth()/2) / pathLength;
            double[] p = pointAt(t, poly, pathLength);
            AffineTransform trfm = AffineTransform.getTranslateInstance(p[0] - rect.getX(), p[1]);
            trfm.rotate(p[2]+angleOffset);
            double off = -rect.getY() - rect.getHeight()/2 + text.yOffset;
            trfm.translate(-rect.getWidth()/2, off);
            gv.setGlyphTransform(i, trfm);
        }
        if (text.haloRadius != null) {
            Shape textOutline = gv.getOutline();
            g.setStroke(new BasicStroke(2*text.haloRadius, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.setColor(text.haloColor);
            g.draw(textOutline);
            g.setStroke(new BasicStroke());
            g.setColor(text.color);
            g.fill(textOutline);
        } else {
            g.setColor(text.color);
            g.drawGlyphVector(gv, 0, 0);
        }
    }

    private double[] pointAt(double t, Polygon poly, double pathLength) {
        double totalLen = t * pathLength;
        double curLen = 0;
        int dx, dy;
        double segLen;

        // Yes, it is ineffecient to iterate from the beginning for each glyph.
        // Can be optimized if it turns out to be slow.
        for (int i = 1; i < poly.npoints; ++i) {
            dx = poly.xpoints[i] - poly.xpoints[i-1];
            dy = poly.ypoints[i] - poly.ypoints[i-1];
            segLen = Math.sqrt(dx*dx + dy*dy);
            if (totalLen > curLen + segLen) {
                curLen += segLen;
                continue;
            }
            return new double[] {poly.xpoints[i-1]+(totalLen - curLen)/segLen*dx,
                    poly.ypoints[i-1]+(totalLen - curLen)/segLen*dy,
                    Math.atan2(dy, dx)};
        }
        return null;
    }

    public void drawLinePattern(Way way, ImageIcon pattern) {
        final int width = pattern.getIconWidth();
        final int height = pattern.getIconHeight();

        Point lastP = null;
        double wayLength = 0;

        Iterator<Node> it = way.getNodes().iterator();
        while (it.hasNext()) {
            Node n = it.next();
            Point thisP = nc.getPoint(n);

            if (lastP != null) {
                final double segmentLength = thisP.distance(lastP);

                final double dx = thisP.x - lastP.x;
                final double dy = thisP.y - lastP.y;

                double dist = wayLength == 0 ? 0 : width - (wayLength % width);

                AffineTransform saveTransform = g.getTransform();
                g.translate(lastP.x, lastP.y);
                g.rotate(Math.atan2(dy, dx));

                if (dist > 0) {
                    g.drawImage(pattern.getImage(), 0, 0, (int) dist, height,
                            width - (int) dist, 0, width, height, null);
                }
                while (dist < segmentLength) {
                    if (dist + width > segmentLength) {
                        g.drawImage(pattern.getImage(), (int) dist, 0, (int) segmentLength, height,
                                0, 0, (int) segmentLength - (int) dist, height, null);
                    } else {
                        pattern.paintIcon(nc, g, (int) dist, 0);
                    }
                    dist += width;
                }
                g.setTransform(saveTransform);

                wayLength += segmentLength;
            }
            lastP = thisP;
        }
    }

    public void drawNodeIcon(Node n, ImageIcon icon, float iconAlpha, boolean selected, boolean member) {
        Point p = nc.getPoint(n);

        final int w = icon.getIconWidth(), h=icon.getIconHeight();
        if(n.isHighlighted()) {
            drawPointHighlight(p, Math.max(w, h));
        }

        if (iconAlpha != 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, iconAlpha));
        }
        icon.paintIcon ( nc, g, p.x-w/2, p.y-h/2 );
        g.setPaintMode();
        if (selected || member)
        {
            g.setColor(selected? selectedColor : relationSelectedColor);
            g.drawRect(p.x-w/2-2, p.y-h/2-2, w+4, h+4);
        }
    }

    private Polygon buildPolygon(Point center, int radius, int sides, double rotation) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < sides; i++) {
            double angle = ((2 * Math.PI / sides) * i) - rotation;
            int x = (int) Math.round(center.x + radius * Math.cos(angle));
            int y = (int) Math.round(center.y + radius * Math.sin(angle));
            polygon.addPoint(x, y);
        }
        return polygon;
    }

    private Polygon buildPolygon(Point center, int radius, int sides) {
        return buildPolygon(center, radius, sides, 0.0);
    }

    public void drawNodeSymbol(Node n, Symbol s, Color fillColor, Color strokeColor) {
        Point p = nc.getPoint(n);
        int radius = s.size / 2;

        if(n.isHighlighted()) {
            drawPointHighlight(p, s.size);
        }

        if (fillColor != null) {
            g.setColor(fillColor);
            switch (s.symbol) {
            case SQUARE:
                g.fillRect(p.x - radius, p.y - radius, s.size, s.size);
                break;
            case CIRCLE:
                g.fillOval(p.x - radius, p.y - radius, s.size, s.size);
                break;
            case TRIANGLE:
                g.fillPolygon(buildPolygon(p, radius, 3, Math.PI / 2));
                break;
            case PENTAGON:
                g.fillPolygon(buildPolygon(p, radius, 5, Math.PI / 2));
                break;
            case HEXAGON:
                g.fillPolygon(buildPolygon(p, radius, 6));
                break;
            case HEPTAGON:
                g.fillPolygon(buildPolygon(p, radius, 7, Math.PI / 2));
                break;
            case OCTAGON:
                g.fillPolygon(buildPolygon(p, radius, 8, Math.PI / 8));
                break;
            case NONAGON:
                g.fillPolygon(buildPolygon(p, radius, 9, Math.PI / 2));
                break;
            case DECAGON:
                g.fillPolygon(buildPolygon(p, radius, 10));
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
            case TRIANGLE:
                g.drawPolygon(buildPolygon(p, radius, 3, Math.PI / 2));
                break;
            case PENTAGON:
                g.drawPolygon(buildPolygon(p, radius, 5, Math.PI / 2));
                break;
            case HEXAGON:
                g.drawPolygon(buildPolygon(p, radius, 6));
                break;
            case HEPTAGON:
                g.drawPolygon(buildPolygon(p, radius, 7, Math.PI / 2));
                break;
            case OCTAGON:
                g.drawPolygon(buildPolygon(p, radius, 8, Math.PI / 8));
                break;
            case NONAGON:
                g.drawPolygon(buildPolygon(p, radius, 9, Math.PI / 2));
                break;
            case DECAGON:
                g.drawPolygon(buildPolygon(p, radius, 10));
                break;
            default:
                throw new AssertionError();
            }
            g.setStroke(new BasicStroke());
        }
    }

    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n  The node to draw.
     * @param color The color of the node.
     */
    public void drawNode(Node n, Color color, int size, boolean fill) {
        if(size <= 0 && !n.isHighlighted())
            return;

        Point p = nc.getPoint(n);

        if(n.isHighlighted()) {
            drawPointHighlight(p, size);
        }

        if (size > 1) {
            if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;
            int radius = size / 2;

            if (inactive || n.isDisabled()) {
                g.setColor(inactiveColor);
            } else {
                g.setColor(color);
            }
            if (fill) {
                g.fillRect(p.x-radius-1, p.y-radius-1, size + 1, size + 1);
            } else {
                g.drawRect(p.x-radius-1, p.y-radius-1, size, size);
            }
        }
    }

    /**
     * highlights a given point by drawing a rounded rectangle around it. Give the
     * size of the object you want to be highlighted, width is added automatically.
     */
    private void drawPointHighlight(Point p, int size) {
        g.setColor(highlightColorTransparent);
        int s = size + 7;
        while(s >= size) {
            int r = (int) Math.floor(s/2);
            g.fillRoundRect(p.x-r, p.y-r, s, s, r, r);
            s -= 4;
        }
    }

    public void drawBoxText(Node n, BoxTextElemStyle bs) {
        if (!isShowNames() || bs == null)
            return;

        Point p = nc.getPoint(n);
        TextElement text = bs.text;
        String s = text.labelCompositionStrategy.compose(n);
        if (s == null) return;

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
        if (bs.hAlign == HorizontalTextAlignment.RIGHT) {
            x += bs.box.x + bs.box.width + 2;
        } else {
            FontRenderContext frc = g.getFontRenderContext();
            Rectangle2D bounds = text.font.getStringBounds(s, frc);
            int textWidth = (int) bounds.getWidth();
            if (bs.hAlign == HorizontalTextAlignment.CENTER) {
                x -= textWidth / 2;
            } else if (bs.hAlign == HorizontalTextAlignment.LEFT) {
                x -= - bs.box.x + 4 + textWidth;
            } else throw new AssertionError();
        }

        if (bs.vAlign == VerticalTextAlignment.BOTTOM) {
            y += bs.box.y + bs.box.height;
        } else {
            FontRenderContext frc = g.getFontRenderContext();
            LineMetrics metrics = text.font.getLineMetrics(s, frc);
            if (bs.vAlign == VerticalTextAlignment.ABOVE) {
                y -= - bs.box.y + metrics.getDescent();
            } else if (bs.vAlign == VerticalTextAlignment.TOP) {
                y -= - bs.box.y - metrics.getAscent();
            } else if (bs.vAlign == VerticalTextAlignment.CENTER) {
                y += (metrics.getAscent() - metrics.getDescent()) / 2;
            } else if (bs.vAlign == VerticalTextAlignment.BELOW) {
                y += bs.box.y + bs.box.height + metrics.getAscent() + 2;
            } else throw new AssertionError();
        }
        if (inactive || n.isDisabled()) {
            g.setColor(inactiveColor);
        } else {
            g.setColor(text.color);
        }
        if (text.haloRadius != null) {
            g.setStroke(new BasicStroke(2*text.haloRadius, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.setColor(text.haloColor);
            FontRenderContext frc = g.getFontRenderContext();
            GlyphVector gv = text.font.createGlyphVector(frc, s);
            Shape textOutline = gv.getOutline(x, y);
            g.draw(textOutline);
            g.setStroke(new BasicStroke());
            g.setColor(text.color);
            g.fill(textOutline);
        } else {
            g.drawString(s, x, y);
        }
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

    public void drawArea(Way w, Color color, BufferedImage fillImage, float fillImageAlpha, TextElement text) {
        Polygon polygon = getPolygon(w);
        drawArea(w, polygon, color, fillImage, fillImageAlpha, text);
    }

    protected void drawArea(OsmPrimitive osm, Polygon polygon, Color color, BufferedImage fillImage, float fillImageAlpha, TextElement text) {

        if (!isOutlineOnly) {
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
        }

        if (text != null && isShowNames()) {
            /*
             * abort if we can't compose the label to be rendered
             */
            if (text.labelCompositionStrategy == null) return;
            String name = text.labelCompositionStrategy.compose(osm);
            if (name == null) return;

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
                g.setColor(text.color);
                Font defaultFont = g.getFont();
                g.setFont (text.font);
                g.drawString (name,
                        (int)(centeredNBounds.getMinX() - nb.getMinX()),
                        (int)(centeredNBounds.getMinY() - nb.getMinY()));
                g.setFont(defaultFont);
            }
        }
    }

    public void drawArea(Relation r, Color color, BufferedImage fillImage, float fillImageAlpha, TextElement text) {
        Multipolygon multipolygon = new Multipolygon(nc);
        multipolygon.load(r);
        if(!r.isDisabled() && !multipolygon.getOuterWays().isEmpty()) {
            for (PolyData pd : multipolygon.getCombinedPolygons()) {
                Polygon p = pd.get();
                if(!isPolygonVisible(p)) {
                    continue;
                }
                drawArea(r, p,
                        pd.selected ? settings.getRelationSelectedColor(color.getAlpha()) : color,
                                fillImage, fillImageAlpha, text);
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

    public void drawVirtualNodes(Collection<Way> ways, Collection<WaySegment> highlightVirtualNodes) {
        if (virtualNodeSize == 0)
            return;
        // print normal virtual nodes
        GeneralPath path = new GeneralPath();
        for (Way osm: ways){
            if (osm.isUsable() && !osm.isDisabled()) {
                visitVirtual(path, osm);
            }
        }
        g.setColor(nodeColor);
        g.draw(path);
        // print highlighted virtual nodes. Since only the color changes, simply
        // drawing them over the existing ones works fine (at least in their current
        // simple style)
        path = new GeneralPath();
        for (WaySegment wseg: highlightVirtualNodes){
            if (wseg.way.isUsable() && !wseg.way.isDisabled()) {
                visitVirtual(path, wseg.toWay());
            }
        }
        g.setColor(highlightColor);
        g.draw(path);
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
    public void drawOrderNumber(Node n1, Node n2, int orderNumber, Color clr) {
        Point p1 = nc.getPoint(n1);
        Point p2 = nc.getPoint(n2);
        drawOrderNumber(p1, p2, orderNumber, clr);
    }

    /**
     * Draw an number of the order of the two consecutive nodes within the
     * parents way
     */
    protected void drawOrderNumber(Point p1, Point p2, int orderNumber, Color clr) {
        if (isSegmentVisible(p1, p2) && isLargeSegment(p1, p2, segmentNumberSpace)) {
            String on = Integer.toString(orderNumber);
            int strlen = on.length();
            int x = (p1.x+p2.x)/2 - 4*strlen;
            int y = (p1.y+p2.y)/2 + 4;

            if(virtualNodeSize != 0 && isLargeSegment(p1, p2, virtualNodeSpace))
            {
                y = (p1.y+p2.y)/2 - virtualNodeSize - 3;
            }

            g.setColor(backgroundColor);
            g.fillRect(x-1, y-12, 8*strlen+1, 14);
            g.setColor(clr);
            g.drawString(on, x, y);
        }
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

    public boolean isInactiveMode() {
        return inactive;
    }
}
