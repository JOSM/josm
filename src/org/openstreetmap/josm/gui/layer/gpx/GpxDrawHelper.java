// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.SystemOfMeasurement.SoMChangeListener;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.ColorScale;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class that helps to draw large set of GPS tracks with different colors and options
 * @since 7319
 */
public class GpxDrawHelper implements SoMChangeListener {

    /**
     * The color that is used for drawing GPX points.
     * @since 10824
     */
    public static final ColorProperty DEFAULT_COLOR = new ColorProperty(marktr("gps point"), Color.magenta);

    private final GpxData data;

    // draw lines between points belonging to different segments
    private boolean forceLines;
    // use alpha blending for line draw
    private boolean alphaLines;
    // draw direction arrows on the lines
    private boolean direction;
    /** width of line for paint **/
    private int lineWidth;
    /** don't draw lines if longer than x meters **/
    private int maxLineLength;
    // draw lines
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

    /** maximum value of displayed HDOP, minimum is 0 */
    private int hdoprange;

    private static final double PHI = Math.toRadians(15);

    //// Variables used only to check cache validity
    private boolean computeCacheInSync;
    private int computeCacheMaxLineLengthUsed;
    private Color computeCacheColorUsed;
    private boolean computeCacheColorDynamic;
    private ColorMode computeCacheColored;
    private int computeCacheColorTracksTune;
    private int computeCacheHeatMapDrawColorTableIdx;

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
        {+sl4, +ll0, +ll0, +sl4}, {-sl9, +ll0, +sl9, +ll0},
        {-ll0, +sl4, -sl4, +ll0}, {-ll0, -sl9, -ll0, +sl9},
        {-sl4, -ll0, -ll0, -sl4}, {+sl9, -ll0, -sl9, -ll0},
        {+ll0, -sl4, +sl4, -ll0}, {+ll0, +sl9, +ll0, -sl9}
    };

    /** heat map parameters **/

    // enabled or not (override by settings)
    private boolean heatMapEnabled;
    // draw small extra line
    private boolean heatMapDrawExtraLine;
    // used index for color table (parameter)
    private int heatMapDrawColorTableIdx;

    // normal buffered image and draw object (cached)
    private BufferedImage heatMapImgGray;
    private Graphics2D heatMapGraph2d;

    // some cached values
    Rectangle heatMapCacheScreenBounds = new Rectangle();
    int heatMapCacheVisibleSegments;
    double heatMapCacheZoomScale;
    int heatMapCacheLineWith;

    // copied value for line drawing
    private List<Integer> heatMapPolyX = new ArrayList<>();
    private List<Integer> heatMapPolyY = new ArrayList<>();

    // setup color maps used by heat map
    private static Color[] heatMapLutColorJosmInferno = createColorFromResource("inferno");
    private static Color[] heatMapLutColorJosmViridis = createColorFromResource("viridis");
    private static Color[] heatMapLutColorJosmBrown2Green = createColorFromResource("brown2green");
    private static Color[] heatMapLutColorJosmRed2Blue = createColorFromResource("red2blue");

    // user defined heatmap color
    private Color[] heatMapLutUserColor = createColorLut(Color.BLACK, Color.WHITE);

    // heat map color in use
    private Color[] heatMapLutColor;

    private void setupColors() {
        hdopAlpha = Main.pref.getInteger("hdop.color.alpha", -1);
        velocityScale = ColorScale.createHSBScale(256);
        /** Colors (without custom alpha channel, if given) for HDOP painting. **/
        hdopScale = ColorScale.createHSBScale(256).makeReversed().addTitle(tr("HDOP"));
        dateScale = ColorScale.createHSBScale(256).addTitle(tr("Time"));
        directionScale = ColorScale.createCyclicScale(256).setIntervalCount(4).addTitle(tr("Direction"));
        heatMapLutColor = heatMapLutUserColor;

        systemOfMeasurementChanged(null, null);
    }

    @Override
    public void systemOfMeasurementChanged(String oldSoM, String newSoM) {
        SystemOfMeasurement som = SystemOfMeasurement.getSystemOfMeasurement();
        velocityScale.addTitle(tr("Velocity, {0}", som.speedName));
        if (Main.isDisplayingMapView() && oldSoM != null && newSoM != null) {
            Main.map.mapView.repaint();
        }
    }

    /**
     * Different color modes
     */
    public enum ColorMode {
        NONE, VELOCITY, HDOP, DIRECTION, TIME, HEATMAP;

        static ColorMode fromIndex(final int index) {
            return values()[index];
        }

        int toIndex() {
            return Arrays.asList(values()).indexOf(this);
        }
    }

    /**
     * Constructs a new {@code GpxDrawHelper}.
     * @param gpxData GPX data
     * @param abstractProperty The color to draw with
     * @since 10824
     */
    public GpxDrawHelper(GpxData gpxData, AbstractProperty<Color> abstractProperty) {
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
        if (ignoreCustom || getColorMode(layerName) == ColorMode.NONE) {
            return DEFAULT_COLOR.getChildColor(specName(layerName)).get();
        } else {
            return null;
        }
    }

    /**
     * Read coloring mode for specified layer from preferences
     * @param layerName name of the GpxLayer
     * @return coloring mode
     */
    public ColorMode getColorMode(String layerName) {
        try {
            int i = Main.pref.getInteger("draw.rawgps.colors", specName(layerName), 0);
            return ColorMode.fromIndex(i);
        } catch (IndexOutOfBoundsException e) {
            Main.warn(e);
        }
        return ColorMode.NONE;
    }

    /** Reads generic color from preferences (usually gray)
     * @return the color
     **/
    public static Color getGenericColor() {
        return DEFAULT_COLOR.get();
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
        alphaLines = Main.pref.getBoolean("draw.rawgps.lines.alpha-blend", spec, false);

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
        /* good HDOP's are between 1 and 3, very bad HDOP's go into 3 digit values */
        hdoprange = Main.pref.getInteger("hdop.range", 7);
        minTrackDurationForTimeColoring = Main.pref.getInteger("draw.rawgps.date-coloring-min-dt", 60);
        largePointAlpha = Main.pref.getInteger("draw.rawgps.large.alpha", -1) & 0xFF;

        // get heatmap parameters
        heatMapEnabled = Main.pref.getBoolean("draw.rawgps.heatmap.enabled", spec, false);
        heatMapDrawExtraLine = Main.pref.getBoolean("draw.rawgps.heatmap.line-extra", spec, false);
        heatMapDrawColorTableIdx = Main.pref.getInteger("draw.rawgps.heatmap.colormap", specName(layerName), 0);

        neutralColor = getColor(layerName, true);
        velocityScale.setNoDataColor(neutralColor);
        dateScale.setNoDataColor(neutralColor);
        hdopScale.setNoDataColor(neutralColor);
        directionScale.setNoDataColor(neutralColor);

        largesize += lineWidth;
    }

    /**
     * Draw all enabled GPX elements of layer.
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param visibleSegments segments visible in the current scope of mv
     */
    public void drawAll(Graphics2D g, MapView mv, List<WayPoint> visibleSegments) {

        final long timeStart = System.currentTimeMillis();

        checkCache();

        // STEP 2b - RE-COMPUTE CACHE DATA *********************
        if (!computeCacheInSync) { // don't compute if the cache is good
            calculateColors();
        }

        fixColors(visibleSegments);

        // backup the environment
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        // set hints for the render
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            Main.pref.getBoolean("mappaint.gpx.use-antialiasing", false) ?
                    RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        if (lineWidth != 0) {
            g.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }

        // global enabled or select via color
        boolean useHeatMap = heatMapEnabled || ColorMode.HEATMAP == colored;

        // default global alpha level
        float layerAlpha = 1.00f;

        // extract current alpha blending value
        if (oldComposite instanceof AlphaComposite) {
            layerAlpha = ((AlphaComposite) oldComposite).getAlpha();
        }

        // use heatmap background layer
        if (useHeatMap) {
            drawHeatMap(g, mv, visibleSegments);
        } else {
            // use normal line style or alpha-blending lines
            if (!alphaLines) {
                drawLines(g, mv, visibleSegments);
            } else {
                drawLinesAlpha(g, mv, visibleSegments, layerAlpha);
            }
        }

        // override global alpha settings (smooth overlay)
        if (alphaLines || useHeatMap) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.25f * layerAlpha));
        }

        // normal overlays
        drawArrows(g, mv, visibleSegments);
        drawPoints(g, mv, visibleSegments);

        // restore environment
        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);

        // show some debug info
        if (Main.isDebugEnabled() && !visibleSegments.isEmpty()) {
            final long timeDiff = System.currentTimeMillis() - timeStart;

            Main.debug("gpxdraw::draw takes " +
                         Utils.getDurationString(timeDiff) +
                         "(" +
                         "segments= " + visibleSegments.size() +
                         ", per 10000 = " + Utils.getDurationString(10000 * timeDiff / visibleSegments.size()) +
                         ")"
              );
        }
    }

    /**
     *  Calculate colors of way segments based on latest configuration settings
     */
    public void calculateColors() {
        double minval = +1e10;
        double maxval = -1e10;
        WayPoint oldWp = null;

        if (colorModeDynamic) {
            if (colored == ColorMode.VELOCITY) {
                final List<Double> velocities = new ArrayList<>();
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
                            velocities.add(vel);
                        }
                        oldWp = trkPnt;
                    }
                }
                Collections.sort(velocities);
                if (velocities.isEmpty()) {
                    velocityScale.setRange(0, 120/3.6);
                } else {
                    minval = velocities.get(velocities.size() / 20); // 5% percentile to remove outliers
                    maxval = velocities.get(velocities.size() * 19 / 20); // 95% percentile to remove outliers
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
            hdopScale.setRange(0, hdoprange);
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
                        double dirColor = oldWp.getCoor().bearing(trkPnt.getCoor());
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
                    default: // Do nothing
                    }
                    if (!noDraw && (maxLineLength == -1 || dist <= maxLineLength)) {
                        trkPnt.drawLine = true;
                        double bearing = oldWp.getCoor().bearing(trkPnt.getCoor());
                        trkPnt.dir = ((int) (bearing / Math.PI * 4 + 1.5)) % 8;
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

        // heat mode
        if (ColorMode.HEATMAP == colored && neutralColor != null) {

            // generate new user color map
            heatMapLutUserColor = createColorLut(Color.BLACK, neutralColor.darker(),
                                                 neutralColor, neutralColor.brighter(), Color.WHITE);

            // decide what, keep order is sync with setting on GUI
            Color[][] lut = {
                    heatMapLutUserColor,
                    heatMapLutColorJosmInferno,
                    heatMapLutColorJosmViridis,
                    heatMapLutColorJosmBrown2Green,
                    heatMapLutColorJosmRed2Blue
            };

            // select by index
            if (heatMapDrawColorTableIdx < lut.length) {
                heatMapLutColor = lut[ heatMapDrawColorTableIdx ];
            } else {
                // fallback
                heatMapLutColor = heatMapLutUserColor;
            }

            // force redraw of image
            heatMapCacheVisibleSegments = 0;
        }

        computeCacheInSync = true;
    }

    /**
     * Draw all GPX ways segments
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param visibleSegments segments visible in the current scope of mv
     */
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

    /**
     * Draw all GPX arrays
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param visibleSegments segments visible in the current scope of mv
     */
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
                        double t = Math.atan2((double) screen.y - old.y, (double) screen.x - old.x) + Math.PI;
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

    /**
     * Draw all GPX points
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param visibleSegments segments visible in the current scope of mv
     */
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
                        new Color((trkPnt.customColoring.getRGB() & 0x00ffffff) | (hdopAlpha << 24), true);
                    g.setColor(customColoringTransparent);
                    // hdop circles
                    int hdopp = mv.getPoint(new LatLon(
                            trkPnt.getCoor().lat(),
                            trkPnt.getCoor().lon() + 2d*6*hdop*360/40000000d)).x - screen.x;
                    g.drawArc(screen.x-hdopp/2, screen.y-hdopp/2, hdopp, hdopp, 0, 360);
                }
                if (large) {
                    // color the large GPS points like the gps lines
                    if (trkPnt.customColoring != null) {
                        Color customColoringTransparent = largePointAlpha < 0 ? trkPnt.customColoring :
                            new Color((trkPnt.customColoring.getRGB() & 0x00ffffff) | (largePointAlpha << 24), true);

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

   /**
     * Draw GPX lines by using alpha blending
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param visibleSegments segments visible in the current scope of mv
     * @param layerAlpha      the color alpha value set for that operation
     */
    private void drawLinesAlpha(Graphics2D g, MapView mv, List<WayPoint> visibleSegments, float layerAlpha) {

        // 1st. backup the paint environment ----------------------------------
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        // 2nd. determine current scale factors -------------------------------

        // adjust global settings
        final int globalLineWidth = Math.min(Math.max(lineWidth, 1), 20);

        // cache scale of view
        final double zoomScale = mv.getScale();

        // 3rd. determine current paint parameters -----------------------------

        // alpha value is based on zoom and line with combined with global layer alpha
        float theLineAlpha = Math.min(Math.max((0.50f/(float) zoomScale)/(globalLineWidth + 1), 0.001f), 0.50f) * layerAlpha;
        final int theLineWith = (int) (lineWidth / zoomScale) + 1;

        // 4th setup virtual paint area ----------------------------------------

        // set line format and alpha channel for all overlays (more lines -> few overlap -> more transparency)
        g.setStroke(new BasicStroke(theLineWith, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setComposite(AlphaComposite.SrcOver.derive(theLineAlpha));

        // last used / calculated entries
        Point lastPaintPnt = null;

        // 5th draw the layer ---------------------------------------------------

        // for all points
        for (WayPoint trkPnt : visibleSegments) {

            // transform coordinates
            final Point paintPnt = mv.getPoint(trkPnt.getEastNorth());

            // skip single points
            if (lastPaintPnt != null && trkPnt.drawLine && !lastPaintPnt.equals(paintPnt)) {

                // set different color
                g.setColor(trkPnt.customColoring);

                // draw it
                g.drawLine(lastPaintPnt.x, lastPaintPnt.y, paintPnt.x, paintPnt.y);
            }

            lastPaintPnt = paintPnt;
        }

        // @last restore modified paint environment -----------------------------
        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
    }

    /**
     * Creates a linear distributed colormap by linear blending between colors
     * @param colors 1..n colors
     * @return array of Color objects
     */
    protected static Color[] createColorLut(Color... colors) {

        // number of lookup entries
        int tableSize = 256;

        // create image an paint object
        BufferedImage img = new BufferedImage(tableSize, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        float[] fract = new float[ colors.length ];

        // distribute fractions (define position of color in map)
        for (int i = 0; i < colors.length; ++i) {
            fract[i] = i * (1.0f / colors.length);
        }

        // draw the gradient map
        LinearGradientPaint gradient = new LinearGradientPaint(0, 0, tableSize, 1, fract, colors,
                                                               MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g.setPaint(gradient);
        g.fillRect(0, 0, tableSize, 1);
        g.dispose();

        // access it via raw interface
        final Raster imgRaster = img.getData();

        // the pixel storage
        int[] pixel = new int[1];

        Color[] colorTable = new Color[tableSize];

        // map the range 0..255 to 0..pi/2
        final double mapTo90Deg = Math.PI / 2.0 / 255.0;

        // create the lookup table
        for (int i = 0; i < tableSize; i++) {

            // get next single pixel
            imgRaster.getDataElements((int) (i * (double) img.getWidth() / tableSize), 0, pixel);

            // get color and map
            Color c = new Color(pixel[0]);

            // smooth alpha like sin curve
            int alpha = (int) (Math.sin(i * mapTo90Deg) * 255);

            // alpha with pre-offset, first color -> full transparent
            alpha = i > 0 ? (75 + alpha) : 0;

            // shrink to maximum bound
            if (alpha > 255) {
                alpha = 255;
            }

            // increase transparency for higher values ( avoid big saturation )
            if (i > 240 && 255 == alpha) {
                alpha -= (i - 240);
            }

            // fill entry in table, assign a alpha value
            colorTable[i] = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }

        // transform into lookup table
        return colorTable;
    }

    /**
     * Creates a colormap by using a static color map with 1..n colors (RGB 0.0 ..1.0)
     * @param data array of multiple RGB color [n][3]
     * @return a dynamic list of color objects
     */
    protected static Color[] createColorFromRawArray(double[][] data) {

        // create the array
        Color[] color = new Color[ data.length ];

        for (int k = 0; k < data.length; k++) {
           // cast an map to linear array
           color[k] = new Color((float) data[k][0], (float) data[k][1], (float) data[k][2]);
        }

        // forward
        return createColorLut(color);
    /**
     * Creates a colormap by using a static color map with 1..n colors (RGB 0.0 ..1.0)
     * @param str the filename (without extension) to look for into data/gpx
     * @return the parsed colormap
     */
    protected static Color[] createColorFromResource(String str) {

        // create resource string
        final String colorFile = "resource://data/gpx/" + str + ".txt";

        List<Color> colorList = new ArrayList<>();

        // try to load the file
        try (CachedFile cf = new CachedFile(colorFile); BufferedReader br = cf.getContentReader()) {

            String line;

            // process lines
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] column = line.split(",");

                // empty or comment line
                if (column.length < 3 || column[0].startsWith("#")) {
                    continue;
                }

                // extract RGB value
                float r = Float.parseFloat(column[0]);
                float g = Float.parseFloat(column[1]);
                float b = Float.parseFloat(column[2]);

                // some color tables are 0..1.0 and some 0.255
                float scale = (r < 1 && g < 1 && b < 1) ? 1 : 255;

                colorList.add(new Color(r/scale, g/scale, b/scale));
            }
        } catch (IOException e) {
            throw new JosmRuntimeException(e);
        }

        // fallback if empty or failed
        if (colorList.isEmpty()) {
            colorList.add(Color.BLACK);
            colorList.add(Color.WHITE);
        }

        return createColorLut(colorList.toArray(new Color[ colorList.size() ]));
    }

    /**
     * Draw gray heat map with current Graphics2D setting
     * @param gB              the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param listSegm        segments visible in the current scope of mv
     * @param foreComp        composite use to draw foreground objects
     * @param foreStroke      stroke use to draw foreground objects
     * @param backComp        composite use to draw background objects
     * @param backStroke      stroke use to draw background objects
     */
    private void drawHeatGrayMap(Graphics2D gB, MapView mv, List<WayPoint> listSegm,
                                 Composite foreComp, Stroke foreStroke,
                                 Composite backComp, Stroke backStroke) {

        // draw foreground
        boolean drawForeground = foreComp != null && foreStroke != null;

        // set initial values
        gB.setStroke(backStroke); gB.setComposite(backComp);

        // for all points, draw single lines by using optimize drawing
        for (WayPoint trkPnt : listSegm) {

            // something to paint or color changed (new segment needed, decrease performance ;-()
            if (!trkPnt.drawLine && !heatMapPolyX.isEmpty()) {

                // convert to primitive type
                final int[] polyXArr = heatMapPolyX.stream().mapToInt(Integer::intValue).toArray();
                final int[] polyYArr = heatMapPolyY.stream().mapToInt(Integer::intValue).toArray();

                // a.) draw background
                gB.drawPolyline(polyXArr, polyYArr, polyXArr.length);

                // b.) draw extra foreground
                if (drawForeground && heatMapDrawExtraLine) {

                    gB.setStroke(foreStroke); gB.setComposite(foreComp);
                    gB.drawPolyline(polyXArr, polyYArr, polyXArr.length);
                    gB.setStroke(backStroke); gB.setComposite(backComp);
                }

                // drop used pints
                heatMapPolyX.clear(); heatMapPolyY.clear();

            } else {

                // get transformed coordinates
                final Point paintPnt = mv.getPoint(trkPnt.getEastNorth());

                // store only the integer part (make sense because pixel is 1:1 here)
                heatMapPolyX.add((int) paintPnt.getX());
                heatMapPolyY.add((int) paintPnt.getY());
            }
        }
    }

    /**
     * Map the gray map to heat map and draw them with current Graphics2D setting
     * @param g               the common draw object to use
     * @param imgGray         gray scale input image
     * @param sampleRaster    the line with for drawing
     */
    private void drawHeatMapGrayMap(Graphics2D g, BufferedImage imgGray, int sampleRaster) {

        final int[] imgPixels = ((DataBufferInt) imgGray.getRaster().getDataBuffer()).getData();

        // samples offset and bounds are scaled with line width derived from zoom level
        final int offX = Math.max(1, sampleRaster / 2);
        final int offY = Math.max(1, sampleRaster / 2);

        final int maxPixelX = imgGray.getWidth();
        final int maxPixelY = imgGray.getHeight();

        int lastPixelY = 0;
        int lastPixelColor = 0;

        // resample gray scale image with line linear weight of next sample in line
        // process each line and draw pixels / rectangles with same color with one operations
        for (int x = 0; x < maxPixelX; x += offX) {
            for (int y = 0; y < maxPixelY; y += offY) {

                int thePixelColor = 0;

                // sample the image (it is gray scale)
                int offset = (x * maxPixelX) + y;

                // merge next pixels of window of line
                for (int k = 0; k < offX && offset + k < imgPixels.length; k++) {
                    thePixelColor += imgPixels[offset+k] & 0xFF;
                }

                // mean value
                thePixelColor /= offX;

                // restart -> use initial sample
                if (0 == y) {
                    lastPixelY = 0; lastPixelColor = thePixelColor;
                }

                // different color to last one ?
                if (Math.abs(lastPixelColor - thePixelColor) > 1) {

                    // draw only foreground pixels, skip small variations
                    if (lastPixelColor > 1+1) {

                        // gray to RGB mapping
                        g.setColor(heatMapLutColor[ lastPixelColor ]);

                        // start point for draw (
                        int yN = lastPixelY > 0 ? lastPixelY : y;

                        // box from from last Y pixel to current pixel
                        if (offX < sampleRaster) {
                            g.fillRect(yN, x, offY + y - yN, offX);
                        } else {
                            g.drawRect(yN, x, offY + y - yN, offX);
                        }
                    }
                    // restart detection
                    lastPixelY = y; lastPixelColor = thePixelColor;
                }
            }
        }
    }

    /**
     * Collect and draw GPS segments and displays a heat-map
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param visibleSegments segments visible in the current scope of mv
     */
    private void drawHeatMap(Graphics2D g, MapView mv, List<WayPoint> visibleSegments) {

        // get bounds of screen image and projection, zoom and adjust input parameters
        final Rectangle screenBounds = g.getDeviceConfiguration().getBounds();
        final double zoomScale = mv.getScale();

        // adjust global settings
        final int globalLineWidth = Math.min(Math.max(lineWidth, 1), 20);

        // 1st setup virtual paint area ----------------------------------------

        // HACK: sometime screen bounds does not return valid values when picture is shifted
        // therefore we use a bigger area to avoid missing parts of image
        screenBounds.width = screenBounds.width * 3 / 2;
        screenBounds.height = screenBounds.height * 3 / 2;

        // new image buffer needed
        final boolean imageSetup = null == heatMapImgGray || !heatMapCacheScreenBounds.equals(screenBounds);

        // screen bounds changed, need new image buffer ?
        if (imageSetup) {
            // we would use a "pure" grayscale image, but there is not efficient way to map gray scale values to RGB)
            heatMapImgGray = new BufferedImage(screenBounds.width, screenBounds.height, BufferedImage.TYPE_INT_ARGB);
            heatMapGraph2d = heatMapImgGray.createGraphics();
            heatMapGraph2d.setBackground(new Color(0, 0, 0, 255));
            heatMapGraph2d.setColor(Color.WHITE);

            // cache it
            heatMapCacheScreenBounds = screenBounds;
        }

        // 2nd. determine current scale factors -------------------------------

        // the line width (foreground: draw extra small footprint line of track)
        final int lineWidthB = Math.max((int) (globalLineWidth / zoomScale) + 1, 2);
        final int lineWidthF = lineWidthB > 2 ? (globalLineWidth - 1) : 0;

        // recalculation of image needed
        final boolean imageRecalc = heatMapCacheVisibleSegments != visibleSegments.size() ||
                                    heatMapCacheZoomScale != zoomScale ||
                                    heatMapCacheLineWith != globalLineWidth;

        // 3rd Calculate the heat map data by draw GPX traces with alpha value ----------

        // need re-generation of gray image ?
        if (imageSetup || imageRecalc) {

            // clear background
            heatMapGraph2d.clearRect(0, 0, heatMapImgGray.getWidth(), heatMapImgGray.getHeight());

            // alpha combines both values, therefore the foreground shall be lighter
            final float lineAlphaB = Math.min(Math.max((0.40f/(float) zoomScale)/(globalLineWidth + 1), 0.001f), 0.50f);
            final float lineAlphaF = lineAlphaB / 1.5f;

            // derive draw parameters and draw
            drawHeatGrayMap(heatMapGraph2d, mv, visibleSegments,
                            lineWidthF > 1 ? AlphaComposite.SrcOver.derive(lineAlphaF) : null,
                            new BasicStroke(lineWidthF, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
                            AlphaComposite.SrcOver.derive(lineAlphaB),
                            new BasicStroke(lineWidthB, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // remember draw parameters
            heatMapCacheVisibleSegments = visibleSegments.size();
            heatMapCacheZoomScale = zoomScale;
            heatMapCacheLineWith = lineWidth;
        }

        // 4th. Draw data on target layer, map data via color lookup table --------------
        drawHeatMapGrayMap(g, heatMapImgGray, lineWidthB);
    }

    /**
     * Apply default color configuration to way segments
     * @param visibleSegments segments visible in the current scope of mv
     */
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
                || (computeCacheColorDynamic != colorModeDynamic)
                || (computeCacheHeatMapDrawColorTableIdx != heatMapDrawColorTableIdx)
      ) {
            computeCacheMaxLineLengthUsed = maxLineLength;
            computeCacheInSync = false;
            computeCacheColorUsed = neutralColor;
            computeCacheColored = colored;
            computeCacheColorTracksTune = colorTracksTune;
            computeCacheColorDynamic = colorModeDynamic;
            computeCacheHeatMapDrawColorTableIdx = heatMapDrawColorTableIdx;
        }
    }

    /**
     *  callback when data is changed, invalidate cached configuration parameters
     */
    public void dataChanged() {
        computeCacheInSync = false;
    }

    /**
     * Draw all GPX arrays
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     */
    public void drawColorBar(Graphics2D g, MapView mv) {
        int w = mv.getWidth();

        // set do default
        g.setComposite(AlphaComposite.SrcOver.derive(1.00f));

        if (colored == ColorMode.HDOP) {
            hdopScale.drawColorBar(g, w-30, 50, 20, 100, 1.0);
        } else if (colored == ColorMode.VELOCITY) {
            SystemOfMeasurement som = SystemOfMeasurement.getSystemOfMeasurement();
            velocityScale.drawColorBar(g, w-30, 50, 20, 100, som.speedValue);
        } else if (colored == ColorMode.DIRECTION) {
            directionScale.drawColorBar(g, w-30, 50, 20, 100, 180.0/Math.PI);
        }
    }
}
