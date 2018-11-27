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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.SystemOfMeasurement.SoMChangeListener;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeEvent;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeListener;
import org.openstreetmap.josm.data.gpx.Line;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.MapViewGraphics;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.MapViewEvent;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationEvent;
import org.openstreetmap.josm.gui.layer.MapViewPaintable.PaintableInvalidationListener;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ColorScale;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class that helps to draw large set of GPS tracks with different colors and options
 * @since 7319
 */
public class GpxDrawHelper implements SoMChangeListener, MapViewPaintable.LayerPainter, PaintableInvalidationListener, GpxDataChangeListener {

    /**
     * The color that is used for drawing GPX points.
     * @since 10824
     */
    public static final NamedColorProperty DEFAULT_COLOR = new NamedColorProperty(marktr("gps point"), Color.magenta);

    private final GpxData data;
    private final GpxLayer layer;

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

    private static final double PHI = Utils.toRadians(15);

    //// Variables used only to check cache validity
    private boolean computeCacheInSync;
    private int computeCacheMaxLineLengthUsed;
    private Color computeCacheColorUsed;
    private boolean computeCacheColorDynamic;
    private ColorMode computeCacheColored;
    private int computeCacheColorTracksTune;
    private int computeCacheHeatMapDrawColorTableIdx;
    private boolean computeCacheHeatMapDrawPointMode;
    private int computeCacheHeatMapDrawGain;
    private int computeCacheHeatMapDrawLowerLimit;

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
    // use point or line draw mode
    private boolean heatMapDrawPointMode;
    // extra gain > 0 or < 0 attenuation, 0 = default
    private int heatMapDrawGain;
    // do not draw elements with value lower than this limit
    private int heatMapDrawLowerLimit;

    // normal buffered image and draw object (cached)
    private BufferedImage heatMapImgGray;
    private Graphics2D heatMapGraph2d;

    // some cached values
    Rectangle heatMapCacheScreenBounds = new Rectangle();
    MapViewState heatMapMapViewState;
    int heatMapCacheLineWith;

    // copied value for line drawing
    private final List<Integer> heatMapPolyX = new ArrayList<>();
    private final List<Integer> heatMapPolyY = new ArrayList<>();

    // setup color maps used by heat map
    private static Color[] heatMapLutColorJosmInferno = createColorFromResource("inferno");
    private static Color[] heatMapLutColorJosmViridis = createColorFromResource("viridis");
    private static Color[] heatMapLutColorJosmBrown2Green = createColorFromResource("brown2green");
    private static Color[] heatMapLutColorJosmRed2Blue = createColorFromResource("red2blue");

    // user defined heatmap color
    private Color[] heatMapLutColor = createColorLut(0, Color.BLACK, Color.WHITE);

    // The heat map was invalidated since the last draw.
    private boolean gpxLayerInvalidated;

    private void setupColors() {
        hdopAlpha = Config.getPref().getInt("hdop.color.alpha", -1);
        velocityScale = ColorScale.createHSBScale(256);
        /** Colors (without custom alpha channel, if given) for HDOP painting. **/
        hdopScale = ColorScale.createHSBScale(256).makeReversed().addTitle(tr("HDOP"));
        dateScale = ColorScale.createHSBScale(256).addTitle(tr("Time"));
        directionScale = ColorScale.createCyclicScale(256).setIntervalCount(4).addTitle(tr("Direction"));

        systemOfMeasurementChanged(null, null);
    }

    @Override
    public void systemOfMeasurementChanged(String oldSoM, String newSoM) {
        SystemOfMeasurement som = SystemOfMeasurement.getSystemOfMeasurement();
        velocityScale.addTitle(tr("Velocity, {0}", som.speedName));
        layer.invalidate();
    }

    /**
     * Different color modes
     */
    public enum ColorMode {
        /**
         * No special colors
         */
        NONE,
        /**
         * Color by velocity
         */
        VELOCITY,
        /**
         * Color by accuracy
         */
        HDOP,
        /**
         * Color by traveling direction
         */
        DIRECTION,
        /**
         * Color by time
         */
        TIME,
        /**
         * Color using a heatmap instead of normal lines
         */
        HEATMAP;

        static ColorMode fromIndex(final int index) {
            return values()[index];
        }

        int toIndex() {
            return Arrays.asList(values()).indexOf(this);
        }
    }

    /**
     * Constructs a new {@code GpxDrawHelper}.
     * @param gpxLayer The layer to draw
     * @since 12157
     */
    public GpxDrawHelper(GpxLayer gpxLayer) {
        layer = gpxLayer;
        data = gpxLayer.data;
        data.addChangeListener(this);

        layer.addInvalidationListener(this);
        SystemOfMeasurement.addSoMChangeListener(this);
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
            return DEFAULT_COLOR.getChildColor(
                    NamedColorProperty.COLOR_CATEGORY_LAYER,
                    layerName,
                    DEFAULT_COLOR.getName()).get();
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
            int i = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.colors", specName(layerName), 0);
            return ColorMode.fromIndex(i);
        } catch (IndexOutOfBoundsException e) {
            Logging.warn(e);
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
        forceLines = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.lines.force", spec, false);
        direction = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.direction", spec, false);
        lineWidth = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.linewidth", spec, 0);
        alphaLines = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.lines.alpha-blend", spec, false);

        if (!data.fromServer) {
            maxLineLength = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.max-line-length.local", spec, -1);
            lines = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.lines.local", spec, true);
        } else {
            maxLineLength = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.max-line-length", spec, 200);
            lines = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.lines", spec, true);
        }
        large = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.large", spec, false);
        largesize = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.large.size", spec, 3);
        hdopCircle = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.hdopcircle", spec, false);
        colored = getColorMode(layerName);
        alternateDirection = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.alternatedirection", spec, false);
        delta = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.min-arrow-distance", spec, 40);
        colorTracksTune = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.colorTracksTune", spec, 45);
        colorModeDynamic = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.colors.dynamic", spec, false);
        /* good HDOP's are between 1 and 3, very bad HDOP's go into 3 digit values */
        hdoprange = Config.getPref().getInt("hdop.range", 7);
        minTrackDurationForTimeColoring = Config.getPref().getInt("draw.rawgps.date-coloring-min-dt", 60);
        largePointAlpha = Config.getPref().getInt("draw.rawgps.large.alpha", -1) & 0xFF;

        // get heatmap parameters
        heatMapEnabled = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.heatmap.enabled", spec, false);
        heatMapDrawExtraLine = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.heatmap.line-extra", spec, false);
        heatMapDrawColorTableIdx = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.heatmap.colormap", spec, 0);
        heatMapDrawPointMode = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.heatmap.use-points", spec, false);
        heatMapDrawGain = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.heatmap.gain", spec, 0);
        heatMapDrawLowerLimit = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.heatmap.lower-limit", spec, 0);

        // shrink to range
        heatMapDrawGain = Utils.clamp(heatMapDrawGain, -10, 10);

        neutralColor = getColor(layerName, true);
        velocityScale.setNoDataColor(neutralColor);
        dateScale.setNoDataColor(neutralColor);
        hdopScale.setNoDataColor(neutralColor);
        directionScale.setNoDataColor(neutralColor);

        largesize += lineWidth;
    }

    @Override
    public void paint(MapViewGraphics graphics) {
        List<WayPoint> visibleSegments = listVisibleSegments(graphics.getClipBounds().getLatLonBoundsBox());
        if (!visibleSegments.isEmpty()) {
            readPreferences(layer.getName());
            drawAll(graphics.getDefaultGraphics(), graphics.getMapView(), visibleSegments);
            if (graphics.getMapView().getLayerManager().getActiveLayer() == layer) {
                drawColorBar(graphics.getDefaultGraphics(), graphics.getMapView());
            }
        }
    }

    private List<WayPoint> listVisibleSegments(Bounds box) {
        WayPoint last = null;
        LinkedList<WayPoint> visibleSegments = new LinkedList<>();

        ensureTrackVisibilityLength();
        for (Line segment : data.getLinesIterable(layer.trackVisibility)) {

            for (WayPoint pt : segment) {
                Bounds b = new Bounds(pt.getCoor());
                if (pt.drawLine && last != null) {
                    b.extend(last.getCoor());
                }
                if (b.intersects(box)) {
                    if (last != null && (visibleSegments.isEmpty()
                            || visibleSegments.getLast() != last)) {
                        if (last.drawLine) {
                            WayPoint l = new WayPoint(last);
                            l.drawLine = false;
                            visibleSegments.add(l);
                        } else {
                            visibleSegments.add(last);
                        }
                    }
                    visibleSegments.add(pt);
                }
                last = pt;
            }
        }
        return visibleSegments;
    }

    /** ensures the trackVisibility array has the correct length without losing data.
     * TODO: Make this nicer by syncing the trackVisibility automatically.
     * additional entries are initialized to true;
     */
    private void ensureTrackVisibilityLength() {
        final int l = data.getTracks().size();
        if (l == layer.trackVisibility.length)
            return;
        final int m = Math.min(l, layer.trackVisibility.length);
        layer.trackVisibility = Arrays.copyOf(layer.trackVisibility, l);
        for (int i = m; i < l; i++) {
            layer.trackVisibility[i] = true;
        }
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
            Config.getPref().getBoolean("mappaint.gpx.use-antialiasing", false) ?
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
        if (Logging.isDebugEnabled() && !visibleSegments.isEmpty()) {
            final long timeDiff = System.currentTimeMillis() - timeStart;

            Logging.debug("gpxdraw::draw takes " +
                         Utils.getDurationString(timeDiff) +
                         "(" +
                         "segments= " + visibleSegments.size() +
                         ", per 10000 = " + Utils.getDurationString(10_000 * timeDiff / visibleSegments.size()) +
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
                for (Line segment : data.getLinesIterable(null)) {
                    if (!forceLines) {
                        oldWp = null;
                    }
                    for (WayPoint trkPnt : segment) {
                        if (!trkPnt.isLatLonKnown()) {
                            continue;
                        }
                        if (oldWp != null && trkPnt.getTimeInMillis() > oldWp.getTimeInMillis()) {
                            double vel = trkPnt.getCoor().greatCircleDistance(oldWp.getCoor())
                                    / (trkPnt.getTime() - oldWp.getTime());
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
                for (Line segment : data.getLinesIterable(null)) {
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
        for (Line segment : data.getLinesIterable(null)) {
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
                        double dtime = trkPnt.getTime() - oldWp.getTime();
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
                        double t = trkPnt.getTime();
                        // skip bad timestamps and very short tracks
                        if (t > 0 && t <= now && maxval - minval > minTrackDurationForTimeColoring) {
                            color = dateScale.getColor(t);
                        } else {
                            color = dateScale.getNoDataColor();
                        }
                        break;
                    default: // Do nothing
                    }
                    if (!noDraw && !segment.isUnordered() && (maxLineLength == -1 || dist <= maxLineLength)) {
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
        if (ColorMode.HEATMAP == colored) {

            // get new user color map and refresh visibility level
            heatMapLutColor = createColorLut(heatMapDrawLowerLimit,
                                             selectColorMap(neutralColor != null ? neutralColor : Color.WHITE, heatMapDrawColorTableIdx));

            // force redraw of image
            heatMapMapViewState = null;
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
                if (!trkPnt.isLatLonKnown()) {
                    old = null;
                    continue;
                }
                Point screen = mv.getPoint(trkPnt);
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
                if (!trkPnt.isLatLonKnown()) {
                    old = null;
                    continue;
                }
                if (trkPnt.drawLine) {
                    Point screen = mv.getPoint(trkPnt);
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
                    Point screen = mv.getPoint(trkPnt);
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
                Point screen = mv.getPoint(trkPnt);

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
                    Point screen = mv.getPoint(trkPnt);
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
                Point screen = mv.getPoint(trkPnt);
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
        final int globalLineWidth = Utils.clamp(lineWidth, 1, 20);

        // cache scale of view
        final double zoomScale = mv.getDist100Pixel() / 50.0f;

        // 3rd. determine current paint parameters -----------------------------

        // alpha value is based on zoom and line with combined with global layer alpha
        float theLineAlpha = (float) Utils.clamp((0.50 / zoomScale) / (globalLineWidth + 1), 0.01, 0.50) * layerAlpha;
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
            final Point paintPnt = mv.getPoint(trkPnt);

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
     * Generates a linear gradient map image
     *
     * @param width  image width
     * @param height image height
     * @param colors 1..n color descriptions
     * @return image object
     */
    protected static BufferedImage createImageGradientMap(int width, int height, Color... colors) {

        // create image an paint object
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();

        float[] fract = new float[ colors.length ];

        // distribute fractions (define position of color in map)
        for (int i = 0; i < colors.length; ++i) {
            fract[i] = i * (1.0f / colors.length);
        }

        // draw the gradient map
        LinearGradientPaint gradient = new LinearGradientPaint(0, 0, width, height, fract, colors,
                                                               MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g.setPaint(gradient);
        g.fillRect(0, 0, width, height);
        g.dispose();

        // access it via raw interface
        return img;
    }

    /**
     * Creates a distributed colormap by linear blending between colors
     * @param lowerLimit lower limit for first visible color
     * @param colors 1..n colors
     * @return array of Color objects
     */
    protected static Color[] createColorLut(int lowerLimit, Color... colors) {

        // number of lookup entries
        final int tableSize = 256;

        // access it via raw interface
        final Raster imgRaster = createImageGradientMap(tableSize, 1, colors).getData();

        // the pixel storage
        int[] pixel = new int[1];

        Color[] colorTable = new Color[tableSize];

        // map the range 0..255 to 0..pi/2
        final double mapTo90Deg = Math.PI / 2.0 / 255.0;

        // create the lookup table
        for (int i = 0; i < tableSize; i++) {

            // get next single pixel
            imgRaster.getDataElements(i, 0, pixel);

            // get color and map
            Color c = new Color(pixel[0]);

            // smooth alpha like sin curve
            int alpha = (i > lowerLimit) ? (int) (Math.sin((i-lowerLimit) * mapTo90Deg) * 255) : 0;

            // alpha with pre-offset, first color -> full transparent
            alpha = alpha > 0 ? (20 + alpha) : 0;

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
     * Creates a darker color
     * @param in        Color object
     * @param adjust    darker adjustment amount
     * @return          new Color
     */
    protected static Color darkerColor(Color in, float adjust) {

        final float r = (float) in.getRed()/255;
        final float g = (float) in.getGreen()/255;
        final float b = (float) in.getBlue()/255;

        return new Color(r*adjust, g*adjust, b*adjust);
    }

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
        } else {
            // add additional darker elements to end of list
            final Color lastColor = colorList.get(colorList.size() - 1);
            colorList.add(darkerColor(lastColor, 0.975f));
            colorList.add(darkerColor(lastColor, 0.950f));
        }

        return createColorLut(0, colorList.toArray(new Color[0]));
    }

    /**
     * Returns the next user color map
     *
     * @param userColor - default or fallback user color
     * @param tableIdx  - selected user color index
     * @return color array
     */
    protected static Color[] selectColorMap(Color userColor, int tableIdx) {

        // generate new user color map ( dark, user color, white )
        Color[] userColor1 = createColorLut(0, userColor.darker(), userColor, userColor.brighter(), Color.WHITE);

        // generate new user color map ( white -> color )
        Color[] userColor2 = createColorLut(0, Color.WHITE, Color.WHITE, userColor);

        // generate new user color map
        Color[] colorTrafficLights = createColorLut(0, Color.WHITE, Color.GREEN.darker(), Color.YELLOW, Color.RED);

        // decide what, keep order is sync with setting on GUI
        Color[][] lut = {
                userColor1,
                userColor2,
                colorTrafficLights,
                heatMapLutColorJosmInferno,
                heatMapLutColorJosmViridis,
                heatMapLutColorJosmBrown2Green,
                heatMapLutColorJosmRed2Blue
        };

        // default case
        Color[] nextUserColor = userColor1;

        // select by index
        if (tableIdx < lut.length) {
            nextUserColor = lut[ tableIdx ];
        }

        // adjust color map
        return nextUserColor;
    }

    /**
     * Generates a Icon
     *
     * @param userColor selected user color
     * @param tableIdx tabled index
     * @param size size of the image
     * @return a image icon that shows the
     */
    public static ImageIcon getColorMapImageIcon(Color userColor, int tableIdx, int size) {
        return new ImageIcon(createImageGradientMap(size, size, selectColorMap(userColor, tableIdx)));
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
    private void drawHeatGrayLineMap(Graphics2D gB, MapView mv, List<WayPoint> listSegm,
                                     Composite foreComp, Stroke foreStroke,
                                     Composite backComp, Stroke backStroke) {

        // draw foreground
        boolean drawForeground = foreComp != null && foreStroke != null;

        // set initial values
        gB.setStroke(backStroke); gB.setComposite(backComp);

        // get last point in list
        final WayPoint lastPnt = !listSegm.isEmpty() ? listSegm.get(listSegm.size() - 1) : null;

        // for all points, draw single lines by using optimized drawing
        for (WayPoint trkPnt : listSegm) {

            // get transformed coordinates
            final Point paintPnt = mv.getPoint(trkPnt);

            // end of line segment or end of list reached
            if (!trkPnt.drawLine || (lastPnt == trkPnt)) {

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

                // drop used points
                heatMapPolyX.clear(); heatMapPolyY.clear();
            }

            // store only the integer part (make sense because pixel is 1:1 here)
            heatMapPolyX.add((int) paintPnt.getX());
            heatMapPolyY.add((int) paintPnt.getY());
        }
    }

    /**
     * Map the gray map to heat map and draw them with current Graphics2D setting
     * @param g               the common draw object to use
     * @param imgGray         gray scale input image
     * @param sampleRaster    the line with for drawing
     * @param outlineWidth     line width for outlines
     */
    private void drawHeatMapGrayMap(Graphics2D g, BufferedImage imgGray, int sampleRaster, int outlineWidth) {

        final int[] imgPixels = ((DataBufferInt) imgGray.getRaster().getDataBuffer()).getData();

        // samples offset and bounds are scaled with line width derived from zoom level
        final int offX = Math.max(1, sampleRaster);
        final int offY = Math.max(1, sampleRaster);

        final int maxPixelX = imgGray.getWidth();
        final int maxPixelY = imgGray.getHeight();

        // always full or outlines at big samples rasters
        final boolean drawOutlines = (outlineWidth > 0) && ((0 == sampleRaster) || (sampleRaster > 10));

        // backup stroke
        final Stroke oldStroke = g.getStroke();

        // use basic stroke for outlines and default transparency
        g.setStroke(new BasicStroke(outlineWidth));

        int lastPixelX = 0;
        int lastPixelColor = 0;

        // resample gray scale image with line linear weight of next sample in line
        // process each line and draw pixels / rectangles with same color with one operations
        for (int y = 0; y < maxPixelY; y += offY) {

            // the lines offsets
            final int lastLineOffset = maxPixelX * (y+0);
            final int nextLineOffset = maxPixelX * (y+1);

            for (int x = 0; x < maxPixelX; x += offX) {

                int thePixelColor = 0; int thePixelCount = 0;

                // sample the image (it is gray scale)
                int offset = lastLineOffset + x;

                // merge next pixels of window of line
                for (int k = 0; k < offX && (offset + k) < nextLineOffset; k++) {
                    thePixelColor += imgPixels[offset+k] & 0xFF;
                    thePixelCount++;
                }

                // mean value
                thePixelColor = thePixelCount > 0 ? (thePixelColor / thePixelCount) : 0;

                // restart -> use initial sample
                if (0 == x) {
                    lastPixelX = 0; lastPixelColor = thePixelColor - 1;
                }

                boolean bDrawIt = false;

                // when one of segment is mapped to black
                bDrawIt = bDrawIt || (lastPixelColor == 0) || (thePixelColor == 0);

                // different color
                bDrawIt = bDrawIt || (Math.abs(lastPixelColor-thePixelColor) > 0);

                // when line is finished draw always
                bDrawIt = bDrawIt || (y >= (maxPixelY-offY));

                if (bDrawIt) {

                    // draw only foreground pixels
                    if (lastPixelColor > 0) {

                        // gray to RGB mapping
                        g.setColor(heatMapLutColor[ lastPixelColor ]);

                        // box from from last Y pixel to current pixel
                        if (drawOutlines) {
                            g.drawRect(lastPixelX, y, offX + x - lastPixelX, offY);
                        } else {
                            g.fillRect(lastPixelX, y, offX + x - lastPixelX, offY);
                        }
                    }

                    // restart detection
                    lastPixelX = x; lastPixelColor = thePixelColor;
                }
            }
        }

        // recover
        g.setStroke(oldStroke);
    }

    /**
     * Collect and draw GPS segments and displays a heat-map
     * @param g               the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param visibleSegments segments visible in the current scope of mv
     */
    private void drawHeatMap(Graphics2D g, MapView mv, List<WayPoint> visibleSegments) {

        // get bounds of screen image and projection, zoom and adjust input parameters
        final Rectangle screenBounds = new Rectangle(mv.getWidth(), mv.getHeight());
        final MapViewState mapViewState = mv.getState();
        final double zoomScale = mv.getDist100Pixel() / 50.0f;

        // adjust global settings ( zero = default line width )
        final int globalLineWidth = (0 == lineWidth) ? 1 : Utils.clamp(lineWidth, 1, 20);

        // 1st setup virtual paint area ----------------------------------------

        // new image buffer needed
        final boolean imageSetup = null == heatMapImgGray || !heatMapCacheScreenBounds.equals(screenBounds);

        // screen bounds changed, need new image buffer ?
        if (imageSetup) {
            // we would use a "pure" grayscale image, but there is not efficient way to map gray scale values to RGB)
            heatMapImgGray = new BufferedImage(screenBounds.width, screenBounds.height, BufferedImage.TYPE_INT_ARGB);
            heatMapGraph2d = heatMapImgGray.createGraphics();
            heatMapGraph2d.setBackground(new Color(0, 0, 0, 255));
            heatMapGraph2d.setColor(Color.WHITE);

            // fast draw ( maybe help or not )
            heatMapGraph2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            heatMapGraph2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            heatMapGraph2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            heatMapGraph2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            heatMapGraph2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            heatMapGraph2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            heatMapGraph2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

            // cache it
            heatMapCacheScreenBounds = screenBounds;
        }

        // 2nd. determine current scale factors -------------------------------

        // the line width (foreground: draw extra small footprint line of track)
        int lineWidthB = (int) Math.max(1.5f * (globalLineWidth / zoomScale) + 1, 2);
        int lineWidthF = lineWidthB > 2 ? (globalLineWidth - 1) : 0;

        // global alpha adjustment
        float lineAlpha = (float) Utils.clamp((0.40 / zoomScale) / (globalLineWidth + 1), 0.01, 0.40);

        // adjust 0.15 .. 1.85
        float scaleAlpha = 1.0f + ((heatMapDrawGain/10.0f) * 0.85f);

        // add to calculated values
        float lineAlphaBPoint = (float) Utils.clamp((lineAlpha * 0.65) * scaleAlpha, 0.001, 0.90);
        float lineAlphaBLine = (float) Utils.clamp((lineAlpha * 1.00) * scaleAlpha, 0.001, 0.90);
        float lineAlphaFLine = (float) Utils.clamp((lineAlpha / 1.50) * scaleAlpha, 0.001, 0.90);

        // 3rd Calculate the heat map data by draw GPX traces with alpha value ----------

        // recalculation of image needed
        final boolean imageRecalc = !mapViewState.equalsInWindow(heatMapMapViewState)
                || gpxLayerInvalidated
                || heatMapCacheLineWith != globalLineWidth;

        // need re-generation of gray image ?
        if (imageSetup || imageRecalc) {

            // clear background
            heatMapGraph2d.clearRect(0, 0, heatMapImgGray.getWidth(), heatMapImgGray.getHeight());

            // point or line blending
            if (heatMapDrawPointMode) {
                heatMapGraph2d.setComposite(AlphaComposite.SrcOver.derive(lineAlphaBPoint));
                drawHeatGrayDotMap(heatMapGraph2d, mv, visibleSegments, lineWidthB);

            } else {
                drawHeatGrayLineMap(heatMapGraph2d, mv, visibleSegments,
                                    lineWidthF > 1 ? AlphaComposite.SrcOver.derive(lineAlphaFLine) : null,
                                    new BasicStroke(lineWidthF, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
                                    AlphaComposite.SrcOver.derive(lineAlphaBLine),
                                    new BasicStroke(lineWidthB, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }

            // remember draw parameter
            heatMapMapViewState = mapViewState;
            heatMapCacheLineWith = globalLineWidth;
            gpxLayerInvalidated = false;
        }

        // 4th. Draw data on target layer, map data via color lookup table --------------
        drawHeatMapGrayMap(g, heatMapImgGray, lineWidthB > 2 ? (int) (lineWidthB*1.25f) : 1, lineWidth > 2 ? (lineWidth - 2) : 1);
    }

    /**
     * Draw a dotted heat map
     *
     * @param gB              the common draw object to use
     * @param mv              the meta data to current displayed area
     * @param listSegm        segments visible in the current scope of mv
     * @param drawSize        draw size of draw element
     */
    private static void drawHeatGrayDotMap(Graphics2D gB, MapView mv, List<WayPoint> listSegm, int drawSize) {

        // typical rendering rate -> use realtime preview instead of accurate display
        final double maxSegm = 25_000, nrSegms = listSegm.size();

        // determine random drop rate
        final double randomDrop = Math.min(nrSegms > maxSegm ? (nrSegms - maxSegm) / nrSegms : 0, 0.70f);

        // http://www.nstb.tc.faa.gov/reports/PAN94_0716.pdf#page=22
        // Global Average Position Domain Accuracy, typical -> not worst case !
        // < 4.218 m Vertical
        // < 2.168 m Horizontal
        final double pixelRmsX = (100 / mv.getDist100Pixel()) * 2.168;
        final double pixelRmsY = (100 / mv.getDist100Pixel()) * 4.218;

        Point lastPnt = null;

        // for all points, draw single lines
        for (WayPoint trkPnt : listSegm) {

            // get transformed coordinates
            final Point paintPnt = mv.getPoint(trkPnt);

            // end of line segment or end of list reached
            if (trkPnt.drawLine && null != lastPnt) {
                drawHeatSurfaceLine(gB, paintPnt, lastPnt, drawSize, pixelRmsX, pixelRmsY, randomDrop);
            }

            // remember
            lastPnt = paintPnt;
        }
    }

    /**
     * Draw a dotted surface line
     *
     * @param g                 the common draw object to use
     * @param fromPnt           start point
     * @param toPnt             end point
     * @param drawSize          size of draw elements
     * @param rmsSizeX          RMS size of circle for X (width)
     * @param rmsSizeY          RMS size of circle for Y (height)
     * @param dropRate          Pixel render drop rate
     */
    private static void drawHeatSurfaceLine(Graphics2D g,
            Point fromPnt, Point toPnt, int drawSize, double rmsSizeX, double rmsSizeY, double dropRate) {

        // collect frequently used items
        final int fromX = (int) fromPnt.getX(); final int deltaX = (int) (toPnt.getX() - fromX);
        final int fromY = (int) fromPnt.getY(); final int deltaY = (int) (toPnt.getY() - fromY);

        // use same random values for each point
        final Random heatMapRandom = new Random(fromX+fromY+deltaX+deltaY);

        // cache distance between start and end point
        final int dist = (int) Math.abs(fromPnt.distance(toPnt));

        // number of increment ( fill wide distance tracks )
        double scaleStep = Math.max(1.0f / dist, dist > 100 ? 0.10f : 0.20f);

        // number of additional random points
        int rounds = Math.min(drawSize/2, 1)+1;

        // decrease random noise at high drop rate ( more accurate draw of fewer points )
        rmsSizeX *= (1.0d - dropRate);
        rmsSizeY *= (1.0d - dropRate);

        double scaleVal = 0;

        // interpolate line draw ( needs separate point instead of line )
        while (scaleVal < (1.0d-0.0001d)) {

            // get position
            final double pntX = fromX + scaleVal * deltaX;
            final double pntY = fromY + scaleVal * deltaY;

            // add random distribution around sampled point
            for (int k = 0; k < rounds; k++) {

                // add error distribution, first point with less error
                int x = (int) (pntX + heatMapRandom.nextGaussian() * (k > 0 ? rmsSizeX : rmsSizeX/4));
                int y = (int) (pntY + heatMapRandom.nextGaussian() * (k > 0 ? rmsSizeY : rmsSizeY/4));

                // draw it, even drop is requested
                if (heatMapRandom.nextDouble() >= dropRate) {
                    g.fillRect(x-drawSize, y-drawSize, drawSize, drawSize);
                }
            }
            scaleVal += scaleStep;
        }
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
        // CHECKSTYLE.OFF: BooleanExpressionComplexity
        if ((computeCacheMaxLineLengthUsed != maxLineLength)
                || (computeCacheColored != colored)
                || (computeCacheColorTracksTune != colorTracksTune)
                || (computeCacheColorDynamic != colorModeDynamic)
                || (computeCacheHeatMapDrawColorTableIdx != heatMapDrawColorTableIdx)
                || (!neutralColor.equals(computeCacheColorUsed)
                || (computeCacheHeatMapDrawPointMode != heatMapDrawPointMode)
                || (computeCacheHeatMapDrawGain != heatMapDrawGain))
                || (computeCacheHeatMapDrawLowerLimit != heatMapDrawLowerLimit)
        ) {
            // CHECKSTYLE.ON: BooleanExpressionComplexity
            computeCacheMaxLineLengthUsed = maxLineLength;
            computeCacheInSync = false;
            computeCacheColorUsed = neutralColor;
            computeCacheColored = colored;
            computeCacheColorTracksTune = colorTracksTune;
            computeCacheColorDynamic = colorModeDynamic;
            computeCacheHeatMapDrawColorTableIdx = heatMapDrawColorTableIdx;
            computeCacheHeatMapDrawPointMode = heatMapDrawPointMode;
            computeCacheHeatMapDrawGain = heatMapDrawGain;
            computeCacheHeatMapDrawLowerLimit = heatMapDrawLowerLimit;
        }
    }

    /**
     *  callback when data is changed, invalidate cached configuration parameters
     */
    @Override
    public void gpxDataChanged(GpxDataChangeEvent e) {
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

    @Override
    public void paintableInvalidated(PaintableInvalidationEvent event) {
        gpxLayerInvalidated = true;
    }

    @Override
    public void detachFromMapView(MapViewEvent event) {
        SystemOfMeasurement.removeSoMChangeListener(this);
        layer.removeInvalidationListener(this);
        data.removeChangeListener(this);
    }
}
