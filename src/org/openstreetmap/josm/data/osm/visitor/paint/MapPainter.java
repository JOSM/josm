// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.ImageProvider;

public class MapPainter {
    private static final double PHI = Math.toRadians(20);

    private final Graphics2D g;
    private final NavigatableComponent nc;
    private final boolean inactive;
    private final boolean useStrokes;

    private final Color inactiveColor;
    private final Color textColor;
    private final Color selectedColor;
    private final Color areaTextColor;

    private Font orderFont;
    private int fillAlpha;

    public MapPainter(Graphics2D g, boolean inactive, NavigatableComponent nc, boolean useStrokes) {
        this.g = g;
        this.inactive = inactive;
        this.nc = nc;
        this.useStrokes = useStrokes;

        this.inactiveColor = PaintColors.INACTIVE.get();
        this.textColor = PaintColors.TEXT.get();
        this.selectedColor = PaintColors.SELECTED.get();
        this.areaTextColor = PaintColors.AREA_TEXT.get();

        this.orderFont = new Font(Main.pref.get("mappaint.font", "Helvetica"), Font.PLAIN, Main.pref.getInteger("mappaint.fontsize", 8));
        this.fillAlpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fillalpha", 50))));
    }

    public void drawWay(Way way, Color color, int width, float dashed[], Color dashedColor, boolean showDirection,
            boolean showHeadArrowOnly) {

        GeneralPath path = new GeneralPath();

        Point lastPoint = null;
        Iterator<Node> it = way.getNodes().iterator();
        while (it.hasNext()) {
            Node n = it.next();
            Point p = nc.getPoint(n);
            if(lastPoint != null) {
                drawSegment(path, lastPoint, p, (showHeadArrowOnly ? !it.hasNext() : showDirection));
            }
            lastPoint = p;
        }
        displaySegments(path, color, width, dashed, dashedColor);
    }

    private void displaySegments(GeneralPath path, Color color, int width, float dashed[], Color dashedColor) {
        g.setColor(inactive ? inactiveColor : color);
        if (useStrokes) {
            if (dashed.length > 0) {
                g.setStroke(new BasicStroke(width,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,0, dashed,0));
            } else {
                g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            }
        }
        g.draw(path);

        if(!inactive && useStrokes && dashedColor != null) {
            g.setColor(dashedColor);
            if (dashed.length > 0) {
                float[] dashedOffset = new float[dashed.length];
                System.arraycopy(dashed, 1, dashedOffset, 0, dashed.length - 1);
                dashedOffset[dashed.length-1] = dashed[0];
                float offset = dashedOffset[0];
                g.setStroke(new BasicStroke(width,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,0,dashedOffset,offset));
            } else {
                g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            }
            g.draw(path);
        }

        if(useStrokes) {
            g.setStroke(new BasicStroke());
        }
    }

    private void drawSegment(GeneralPath path, Point p1, Point p2, boolean showDirection) {
        if (isSegmentVisible(p1, p2)) {
            path.moveTo(p1.x, p1.y);
            path.lineTo(p2.x, p2.y);

            if (showDirection) {
                double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
                path.lineTo((int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
                path.moveTo((int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
                path.lineTo(p2.x, p2.y);
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


    protected void drawNodeIcon(Node n, ImageIcon icon, boolean annotate, boolean selected, String name) {
        Point p = nc.getPoint(n);
        if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth()) || (p.y > nc.getHeight())) return;

        int w = icon.getIconWidth(), h=icon.getIconHeight();
        icon.paintIcon ( Main.map.mapView, g, p.x-w/2, p.y-h/2 );
        if(name != null) {
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
        if (selected)
        {
            g.setColor (  selectedColor );
            g.drawRect (p.x-w/2-2, p.y-h/2-2, w+4, h+4);
        }
    }


    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n  The node to draw.
     * @param color The color of the node.
     */
    public void drawNode(Node n, Color color, int size, int radius, boolean fill, String name) {
        if (size > 1) {
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

            if(name != null)            {
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

    protected void drawArea(Polygon polygon, Color color, String name) {

        /* set the opacity (alpha) level of the filled polygon */
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
        g.fillPolygon(polygon);

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

    public void drawRestriction(ImageIcon icon, Point pVia, double vx, double vx2, double vy, double vy2, double iconAngle, boolean selected) {
        /* rotate icon with direction last node in from to */
        ImageIcon rotatedIcon = ImageProvider.createRotatedImage(null /*icon2*/, icon, iconAngle);

        /* scale down icon to 16*16 pixels */
        ImageIcon smallIcon = new ImageIcon(rotatedIcon.getImage().getScaledInstance(16 , 16, Image.SCALE_SMOOTH));
        int w = smallIcon.getIconWidth(), h=smallIcon.getIconHeight();
        smallIcon.paintIcon (nc, g, (int)(pVia.x+vx+vx2)-w/2, (int)(pVia.y+vy+vy2)-h/2 );

        if (selected) {
            g.setColor(selectedColor);
            g.drawRect((int)(pVia.x+vx+vx2)-w/2-2,(int)(pVia.y+vy+vy2)-h/2-2, w+4, h+4);
        }
    }

}
