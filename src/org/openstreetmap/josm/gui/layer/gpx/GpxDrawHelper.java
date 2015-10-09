// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.ColorScale;

/**
 * Class that helps to draw large set of GPS tracks with different colors and options
 * @since 7319
 */
public class GpxDrawHelper {
    private GpxData data;

    // draw lines between points belonging to different segments
    private boolean forceLines;
    // draw direction arrows on the lines
    private boolean direction;
    /** don't draw lines if longer than x meters **/
    private int lineWidth;
    private int maxLineLength;
    private boolean lines;
    /** paint large dots for points **/
    private boolean large;
    private int largesize;
    private boolean hdopCircle;
    /** paint direction arrow with alternate math. may be faster **/
    private boolean alternateDirection;
    /** don't draw arrows nearer to each other than this **/
    private int delta;
    private double minTrackDurationForTimeColoring;

    private int hdopfactor;

    private static final double PHI = Math.toRadians(15);

    //// Variables used only to check cache validity
    private boolean computeCacheInSync;
    private int computeCacheMaxLineLengthUsed;
    private Color computeCacheColorUsed;
    private boolean computeCacheColorDynamic;
    private ColorMode computeCacheColored;
    private int computeCacheColorTracksTune;

    //// Color-related fields
    /** Mode of the line coloring **/
    private ColorMode colored;
    /** max speed for coloring - allows to tweak line coloring for different speed levels. **/
    private int colorTracksTune;
    private boolean colorModeDynamic;
    private Color neutralColor;
    private int largePointAlpha;

    // default access is used to allow changing from plugins
    private ColorScale velocityScale;
    /** Colors (without custom alpha channel, if given) for HDOP painting. **/
    private ColorScale hdopScale;
    private ColorScale dateScale;
    private ColorScale directionScale;

    /** Opacity for hdop points **/
    private int hdopAlpha;


    // lookup array to draw arrows without doing any math
    private static final int ll0 = 9;
    private static final int sl4 = 5;
    private static final int sl9 = 3;
    private static final int[][] dir = {
        {+sl4, +ll0, +ll0, +sl4}, {-sl9, +ll0, +sl9, +ll0}, {-ll0, +sl4, -sl4, +ll0},
        {-ll0, -sl9, -ll0, +sl9}, {-sl4, -ll0, -ll0, -sl4}, {+sl9, -ll0, -sl9, -ll0},
        {+ll0, -sl4, +sl4, -ll0}, {+ll0, +sl9, +ll0, -sl9}, {+sl4, +ll0, +ll0, +sl4},
        {-sl9, +ll0, +sl9, +ll0}, {-ll0, +sl4, -sl4, +ll0}, {-ll0, -sl9, -ll0, +sl9}};

    private void setupColors() {
        hdopAlpha = Main.pref.getInteger("hdop.color.alpha", -1);
        velocityScale = ColorScale.createHSBScale(256).addTitle(tr("Velocity, km/h"));
        /** Colors (without custom alpha channel, if given) for HDOP painting. **/
        hdopScale = ColorScale.createHSBScale(256).makeReversed().addTitle(tr("HDOP, m"));
        dateScale = ColorScale.createHSBScale(256).addTitle(tr("Time"));
        directionScale = ColorScale.createCyclicScale(256).setIntervalCount(4).addTitle(tr("Direction"));
    }

    /**
     * Different color modes
     */
    public enum ColorMode {
        NONE, VELOCITY, HDOP, DIRECTION, TIME
    }

    /**
     * Constructs a new {@code GpxDrawHelper}.
     * @param gpxData GPX data
     */
    public GpxDrawHelper(GpxData gpxData) {
        data = gpxData;
        setupColors();
    }

    private static String specName(String layerName) {
        return "layer " + layerName;
    }

    /**
     * Get the default color for gps tracks for specified layer
     * @param layerName name of the GpxLayer
     * @param ignoreCustom do not use preferences
     * @return the color or null if the color is not constant
     */
    public Color getColor(String layerName, boolean ignoreCustom) {
        Color c = Main.pref.getColor(marktr("gps point"), specName(layerName), Color.gray);
        return ignoreCustom || getColorMode(layerName) == ColorMode.NONE ? c : null;
    }

    /**
     * Read coloring mode for specified layer from preferences
     * @param layerName name of the GpxLayer
     * @return coloting mode
     */
    public ColorMode getColorMode(String layerName) {
        try {
            int i = Main.pref.getInteger("draw.rawgps.colors", specName(layerName), 0);
            return ColorMode.values()[i];
        } catch (Exception e) {
            Main.warn(e);
        }
        return ColorMode.NONE;
    }

    /** Reads generic color from preferences (usually gray)
     * @return the color
     **/
    public static Color getGenericColor() {
        return Main.pref.getColor(marktr("gps point"), Color.gray);
    }

    /**
     * Read all drawing-related settings from preferences
     * @param layerName layer name used to access its specific preferences
     **/
    public void readPreferences(String layerName) {
        String spec = specName(layerName);
        forceLines = Main.pref.getBoolean("draw.rawgps.lines.force", spec, false);
        direction = Main.pref.getBoolean("draw.rawgps.direction", spec, false);
        lineWidth = Main.pref.getInteger("draw.rawgps.linewidth", spec, 0);

        if (!data.fromServer) {
            maxLineLength = Main.pref.getInteger("draw.rawgps.max-line-length.local", spec, -1);
            lines = Main.pref.getBoolean("draw.rawgps.lines.local", spec, true);
        } else {
            maxLineLength = Main.pref.getInteger("draw.rawgps.max-line-length", spec, 200);
            lines = Main.pref.getBoolean("draw.rawgps.lines", spec, true);
        }
        large = Main.pref.getBoolean("draw.rawgps.large", spec, false);
        largesize = Main.pref.getInteger("draw.rawgps.large.size", spec, 3);
        hdopCircle = Main.pref.getBoolean("draw.rawgps.hdopcircle", spec, false);
        colored = getColorMode(layerName);
        alternateDirection = Main.pref.getBoolean("draw.rawgps.alternatedirection", spec, false);
        delta = Main.pref.getInteger("draw.rawgps.min-arrow-distance", spec, 40);
        colorTracksTune = Main.pref.getInteger("draw.rawgps.colorTracksTune", spec, 45);
        colorModeDynamic = Main.pref.getBoolean("draw.rawgps.colors.dynamic", spec, false);
        hdopfactor = Main.pref.getInteger("hdop.factor", 25);
        minTrackDurationForTimeColoring = Main.pref.getInteger("draw.rawgps.date-coloring-min-dt", 60);
        largePointAlpha = Main.pref.getInteger("draw.rawgps.large.alpha", -1) & 0xFF;

        neutralColor = getColor(layerName, true);
        velocityScale.setNoDataColor(neutralColor);
        dateScale.setNoDataColor(neutralColor);
        hdopScale.setNoDataColor(neutralColor);
        directionScale.setNoDataColor(neutralColor);

        largesize += lineWidth;
    }

    public void drawAll(Graphics2D g, MapView mv, List<WayPoint> visibleSegments) {

        checkCache();

        // STEP 2b - RE-COMPUTE CACHE DATA *********************
        if (!computeCacheInSync) { // don't compute if the cache is good
            calculateColors();
        }

        Stroke storedStroke = g.getStroke();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            Main.pref.getBoolean("mappaint.gpx.use-antialiasing", false) ?
                    RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        if (lineWidth != 0) {
            g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        fixColors(visibleSegments);
        drawLines(g, mv, visibleSegments);
        drawArrows(g, mv, visibleSegments);
        drawPoints(g, mv, visibleSegments);
        if (lineWidth != 0) {
            g.setStroke(storedStroke);
        }
    }

    public void calculateColors() {
        double minval = +1e10;
        double maxval = -1e10;
        WayPoint oldWp = null;

        if (colorModeDynamic) {
            if (colored == ColorMode.VELOCITY) {
                for (Collection<WayPoint> segment : data.getLinesIterable(null)) {
                    if (!forceLines) {
                        oldWp = null;
                    }
                    for (WayPoint trkPnt : segment) {
                        LatLon c = trkPnt.getCoor();
                        if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                            continue;
                        }
                        if (oldWp != null && trkPnt.time > oldWp.time) {
                            double vel = c.greatCircleDistance(oldWp.getCoor())
                                    / (trkPnt.time - oldWp.time);
                            if (vel > maxval) {
                                maxval = vel;
                            }
                            if (vel < minval) {
                                minval = vel;
                            }
                        }
                        oldWp = trkPnt;
                    }
                }
                if (minval >= maxval) {
                    velocityScale.setRange(0, 120/3.6);
                } else {
                    velocityScale.setRange(minval, maxval);
                }
            } else if (colored == ColorMode.HDOP) {
                for (Collection<WayPoint> segment : data.getLinesIterable(null)) {
                    for (WayPoint trkPnt : segment) {
                        Object val = trkPnt.get(GpxConstants.PT_HDOP);
                        if (val != null) {
                            double hdop = ((Float) val).doubleValue();
                            if (hdop > maxval) {
                                maxval = hdop;
                            }
                            if (hdop < minval) {
                                minval = hdop;
                            }
                        }
                    }
                }
                if (minval >= maxval) {
                    hdopScale.setRange(0, 100);
                } else {
                    hdopScale.setRange(minval, maxval);
                }
            }
            oldWp = null;
        } else { // color mode not dynamic
            velocityScale.setRange(0, colorTracksTune);
            hdopScale.setRange(0, 1.0/hdopfactor);
        }
        double now = System.currentTimeMillis()/1000.0;
        if (colored == ColorMode.TIME) {
            Date[] bounds = data.getMinMaxTimeForAllTracks();
            if (bounds.length >= 2) {
                minval = bounds[0].getTime()/1000.0;
                maxval = bounds[1].getTime()/1000.0;
            } else {
                minval = 0;
                maxval = now;
            }
            dateScale.setRange(minval, maxval);
        }


        // Now the colors for all the points will be assigned
        for (Collection<WayPoint> segment : data.getLinesIterable(null)) {
            if (!forceLines) { // don't draw lines between segments, unless forced to
                oldWp = null;
            }
            for (WayPoint trkPnt : segment) {
                LatLon c = trkPnt.getCoor();
                trkPnt.customColoring = neutralColor;
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                 // now we are sure some color will be assigned
                Color color = null;

                if (colored == ColorMode.HDOP) {
                    Float hdop = (Float) trkPnt.get(GpxConstants.PT_HDOP);
                    color = hdopScale.getColor(hdop);
                }
                if (oldWp != null) { // other coloring modes need segment for calcuation
                    double dist = c.greatCircleDistance(oldWp.getCoor());
                    boolean noDraw = false;
                    switch (colored) {
                    case VELOCITY:
                        double dtime = trkPnt.time - oldWp.time;
                        if (dtime > 0) {
                            color = velocityScale.getColor(dist / dtime);
                        } else {
                            color = velocityScale.getNoDataColor();
                        }
                        break;
                    case DIRECTION:
                        double dirColor = oldWp.getCoor().heading(trkPnt.getCoor());
                        color = directionScale.getColor(dirColor);
                        break;
                    case TIME:
                        double t = trkPnt.time;
                        // skip bad timestamps and very short tracks
                        if (t > 0 && t <= now && maxval - minval > minTrackDurationForTimeColoring) {
                            color = dateScale.getColor(t);
                        } else {
                            color = dateScale.getNoDataColor();
                        }
                        break;
                    }
                    if (!noDraw && (maxLineLength == -1 || dist <= maxLineLength)) {
                        trkPnt.drawLine = true;
                        trkPnt.dir = (int) oldWp.getCoor().heading(trkPnt.getCoor());
                    } else {
                        trkPnt.drawLine = false;
                    }
                } else { // make sure we reset outdated data
                    trkPnt.drawLine = false;
                    color = neutralColor;
                }
                if (color != null) {
                    trkPnt.customColoring = color;
                }
                oldWp = trkPnt;
            }
        }

        computeCacheInSync = true;
    }

    private void drawLines(Graphics2D g, MapView mv, List<WayPoint> visibleSegments) {
        if (lines) {
            Point old = null;
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                Point screen = mv.getPoint(trkPnt.getEastNorth());
                // skip points that are on the same screenposition
                if (trkPnt.drawLine && old != null && ((old.x != screen.x) || (old.y != screen.y))) {
                    g.setColor(trkPnt.customColoring);
                    g.drawLine(old.x, old.y, screen.x, screen.y);
                }
                old = screen;
            }
        }
    }

    private void drawArrows(Graphics2D g, MapView mv, List<WayPoint> visibleSegments) {
        /****************************************************************
         ********** STEP 3b - DRAW NICE ARROWS **************************
         ****************************************************************/
        if (lines && direction && !alternateDirection) {
            Point old = null;
            Point oldA = null; // last arrow painted
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                if (trkPnt.drawLine) {
                    Point screen = mv.getPoint(trkPnt.getEastNorth());
                    // skip points that are on the same screenposition
                    if (old != null
                            && (oldA == null || screen.x < oldA.x - delta || screen.x > oldA.x + delta
                            || screen.y < oldA.y - delta || screen.y > oldA.y + delta)) {
                        g.setColor(trkPnt.customColoring);
                        double t = Math.atan2(screen.y - old.y, screen.x - old.x) + Math.PI;
                        g.drawLine(screen.x, screen.y, (int) (screen.x + 10 * Math.cos(t - PHI)),
                                (int) (screen.y + 10 * Math.sin(t - PHI)));
                        g.drawLine(screen.x, screen.y, (int) (screen.x + 10 * Math.cos(t + PHI)),
                                (int) (screen.y + 10 * Math.sin(t + PHI)));
                        oldA = screen;
                    }
                    old = screen;
                }
            } // end for trkpnt
        }

        /****************************************************************
         ********** STEP 3c - DRAW FAST ARROWS **************************
         ****************************************************************/
        if (lines && direction && alternateDirection) {
            Point old = null;
            Point oldA = null; // last arrow painted
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                if (trkPnt.drawLine) {
                    Point screen = mv.getPoint(trkPnt.getEastNorth());
                    // skip points that are on the same screenposition
                    if (old != null
                            && (oldA == null || screen.x < oldA.x - delta || screen.x > oldA.x + delta
                            || screen.y < oldA.y - delta || screen.y > oldA.y + delta)) {
                        g.setColor(trkPnt.customColoring);
                        g.drawLine(screen.x, screen.y, screen.x + dir[trkPnt.dir][0], screen.y
                                + dir[trkPnt.dir][1]);
                        g.drawLine(screen.x, screen.y, screen.x + dir[trkPnt.dir][2], screen.y
                                + dir[trkPnt.dir][3]);
                        oldA = screen;
                    }
                    old = screen;
                }
            } // end for trkpnt
        }
    }

    private void drawPoints(Graphics2D g, MapView mv, List<WayPoint> visibleSegments) {
        /****************************************************************
         ********** STEP 3d - DRAW LARGE POINTS AND HDOP CIRCLE *********
         ****************************************************************/
        if (large || hdopCircle) {
            final int halfSize = largesize/2;
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                Point screen = mv.getPoint(trkPnt.getEastNorth());


                if (hdopCircle && trkPnt.get(GpxConstants.PT_HDOP) != null) {
                    // hdop value
                    float hdop = (Float) trkPnt.get(GpxConstants.PT_HDOP);
                    if (hdop < 0) {
                        hdop = 0;
                    }
                    Color customColoringTransparent = hdopAlpha < 0 ? trkPnt.customColoring :
                        new Color(trkPnt.customColoring.getRGB() & 0x00ffffff | hdopAlpha << 24, true);
                    g.setColor(customColoringTransparent);
                    // hdop cirles
                    int hdopp = mv.getPoint(new LatLon(
                            trkPnt.getCoor().lat(),
                            trkPnt.getCoor().lon() + 2*6*hdop*360/40000000d)).x - screen.x;
                    g.drawArc(screen.x-hdopp/2, screen.y-hdopp/2, hdopp, hdopp, 0, 360);
                }
                if (large) {
                    // color the large GPS points like the gps lines
                    if (trkPnt.customColoring != null) {
                        Color customColoringTransparent = largePointAlpha < 0 ? trkPnt.customColoring :
                            new Color(trkPnt.customColoring.getRGB() & 0x00ffffff | largePointAlpha << 24, true);

                        g.setColor(customColoringTransparent);
                    }
                    g.fillRect(screen.x-halfSize, screen.y-halfSize, largesize, largesize);
                }
            } // end for trkpnt
        } // end if large || hdopcircle

        /****************************************************************
         ********** STEP 3e - DRAW SMALL POINTS FOR LINES ***************
         ****************************************************************/
        if (!large && lines) {
            g.setColor(neutralColor);
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                if (!trkPnt.drawLine) {
                    Point screen = mv.getPoint(trkPnt.getEastNorth());
                    g.drawRect(screen.x, screen.y, 0, 0);
                }
            } // end for trkpnt
        } // end if large

        /****************************************************************
         ********** STEP 3f - DRAW SMALL POINTS INSTEAD OF LINES ********
         ****************************************************************/
        if (!large && !lines) {
            g.setColor(neutralColor);
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                Point screen = mv.getPoint(trkPnt.getEastNorth());
                g.setColor(trkPnt.customColoring);
                g.drawRect(screen.x, screen.y, 0, 0);
            } // end for trkpnt
        } // end if large
    }

    private void fixColors(List<WayPoint> visibleSegments) {
        for (WayPoint trkPnt : visibleSegments) {
            if (trkPnt.customColoring == null) {
                trkPnt.customColoring = neutralColor;
            }
        }
    }

    /**
     * Check cache validity set necessary flags
     */
    private void checkCache() {
        if ((computeCacheMaxLineLengthUsed != maxLineLength) || (!neutralColor.equals(computeCacheColorUsed))
                || (computeCacheColored != colored) || (computeCacheColorTracksTune != colorTracksTune)
                || (computeCacheColorDynamic != colorModeDynamic)) {
            computeCacheMaxLineLengthUsed = maxLineLength;
            computeCacheInSync = false;
            computeCacheColorUsed = neutralColor;
            computeCacheColored = colored;
            computeCacheColorTracksTune = colorTracksTune;
            computeCacheColorDynamic = colorModeDynamic;
        }
    }

    public void dataChanged() {
        computeCacheInSync = false;
    }

    public void drawColorBar(Graphics2D g, MapView mv) {
        int w = mv.getWidth();
        if (colored == ColorMode.HDOP) {
            hdopScale.drawColorBar(g, w-30, 50, 20, 100, 1.0);
        } else if (colored == ColorMode.VELOCITY) {
            velocityScale.drawColorBar(g, w-30, 50, 20, 100, 3.6);
        } else if (colored == ColorMode.DIRECTION) {
            directionScale.drawColorBar(g, w-30, 50, 20, 100, 180.0/Math.PI);
        }
    }
}
