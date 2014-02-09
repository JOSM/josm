// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.gpx.ChooseTrackVisibilityAction;
import org.openstreetmap.josm.gui.layer.gpx.ConvertToDataLayerAction;
import org.openstreetmap.josm.gui.layer.gpx.CustomizeDrawingAction;
import org.openstreetmap.josm.gui.layer.gpx.DownloadAlongTrackAction;
import org.openstreetmap.josm.gui.layer.gpx.DownloadWmsAlongTrackAction;
import org.openstreetmap.josm.gui.layer.gpx.ImportAudioAction;
import org.openstreetmap.josm.gui.layer.gpx.ImportImagesAction;
import org.openstreetmap.josm.gui.layer.gpx.MarkersFromNamedPointsAction;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

public class GpxLayer extends Layer {

    public GpxData data;
    protected static final double PHI = Math.toRadians(15);
    private boolean computeCacheInSync;
    private int computeCacheMaxLineLengthUsed;
    private Color computeCacheColorUsed;
    private boolean computeCacheColorDynamic;
    private colorModes computeCacheColored;
    private int computeCacheColorTracksTune;
    private boolean isLocalFile;
    // used by ChooseTrackVisibilityAction to determine which tracks to show/hide
    public boolean[] trackVisibility = new boolean[0];

    private final List<GpxTrack> lastTracks = new ArrayList<GpxTrack>(); // List of tracks at last paint
    private int lastUpdateCount;

    public GpxLayer(GpxData d) {
        super((String) d.attr.get("name"));
        data = d;
        computeCacheInSync = false;
        ensureTrackVisibilityLength();
    }

    public GpxLayer(GpxData d, String name) {
        this(d);
        this.setName(name);
    }

    public GpxLayer(GpxData d, String name, boolean isLocal) {
        this(d);
        this.setName(name);
        this.isLocalFile = isLocal;
    }

    /**
     * returns minimum and maximum timestamps in the track
     */
    public static Date[] getMinMaxTimeForTrack(GpxTrack trk) {
        WayPoint earliest = null, latest = null;

        for (GpxTrackSegment seg : trk.getSegments()) {
            for (WayPoint pnt : seg.getWayPoints()) {
                if (latest == null) {
                    latest = earliest = pnt;
                } else {
                    if (pnt.compareTo(earliest) < 0) {
                        earliest = pnt;
                    } else {
                        latest = pnt;
                    }
                }
            }
        }
        if (earliest==null || latest==null) return null;
        return new Date[]{earliest.getTime(), latest.getTime()};
    }

    /**
    * Returns minimum and maximum timestamps for all tracks
    * Warning: there are lot of track with broken timestamps,
    * so we just ingore points from future and from year before 1970 in this method
    * works correctly @since 5815
    */
    public Date[] getMinMaxTimeForAllTracks() {
        double min=1e100, max=-1e100, t;
        double now = System.currentTimeMillis()/1000.0;
        for (GpxTrack trk: data.tracks) {
            for (GpxTrackSegment seg : trk.getSegments()) {
                for (WayPoint pnt : seg.getWayPoints()) {
                    t = pnt.time;
                    if (t>0 && t<=now) {
                        if (t>max) max=t;
                        if (t<min) min=t;
                    }
                }
            }
        }
        if (min==1e100 || max==-1e100) return null;
        return new Date[]{new Date((long) (min * 1000)), new Date((long) (max * 1000)), };
    }


    /**
     * returns a human readable string that shows the timespan of the given track
     */
    public static String getTimespanForTrack(GpxTrack trk) {
        Date[] bounds = getMinMaxTimeForTrack(trk);
        String ts = "";
        if (bounds != null) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
            String earliestDate = df.format(bounds[0]);
            String latestDate = df.format(bounds[1]);

            if (earliestDate.equals(latestDate)) {
                DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
                ts += earliestDate + " ";
                ts += tf.format(bounds[0]) + " - " + tf.format(bounds[1]);
            } else {
                DateFormat dtf = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                ts += dtf.format(bounds[0]) + " - " + dtf.format(bounds[1]);
            }

            int diff = (int) (bounds[1].getTime() - bounds[0].getTime()) / 1000;
            ts += String.format(" (%d:%02d)", diff / 3600, (diff % 3600) / 60);
        }
        return ts;
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("layer", "gpx_small");
    }

    @Override
    public Object getInfoComponent() {
        StringBuilder info = new StringBuilder();

        if (data.attr.containsKey("name")) {
            info.append(tr("Name: {0}", data.attr.get(GpxConstants.META_NAME))).append("<br>");
        }

        if (data.attr.containsKey("desc")) {
            info.append(tr("Description: {0}", data.attr.get(GpxConstants.META_DESC))).append("<br>");
        }

        if (!data.tracks.isEmpty()) {
            info.append("<table><thead align='center'><tr><td colspan='5'>"
                    + trn("{0} track", "{0} tracks", data.tracks.size(), data.tracks.size())
                    + "</td></tr><tr align='center'><td>" + tr("Name") + "</td><td>"
                    + tr("Description") + "</td><td>" + tr("Timespan")
                    + "</td><td>" + tr("Length") + "</td><td>" + tr("URL")
                    + "</td></tr></thead>");

            for (GpxTrack trk : data.tracks) {
                info.append("<tr><td>");
                if (trk.getAttributes().containsKey("name")) {
                    info.append(trk.getAttributes().get("name"));
                }
                info.append("</td><td>");
                if (trk.getAttributes().containsKey("desc")) {
                    info.append(" ").append(trk.getAttributes().get("desc"));
                }
                info.append("</td><td>");
                info.append(getTimespanForTrack(trk));
                info.append("</td><td>");
                info.append(NavigatableComponent.getSystemOfMeasurement().getDistText(trk.length()));
                info.append("</td><td>");
                if (trk.getAttributes().containsKey("url")) {
                    info.append(trk.getAttributes().get("url"));
                }
                info.append("</td></tr>");
            }

            info.append("</table><br><br>");

        }

        info.append(tr("Length: {0}", NavigatableComponent.getSystemOfMeasurement().getDistText(data.length()))).append("<br>");

        info.append(trn("{0} route, ", "{0} routes, ", data.routes.size(), data.routes.size())).append(
                trn("{0} waypoint", "{0} waypoints", data.waypoints.size(), data.waypoints.size())).append("<br>");

        final JScrollPane sp = new JScrollPane(new HtmlPanel(info.toString()));
        sp.setPreferredSize(new Dimension(sp.getPreferredSize().width+20, 370));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                sp.getVerticalScrollBar().setValue(0);
            }
        });
        return sp;
    }

    @Override
    public boolean isInfoResizable() {
        return true;
    }

    @Override
    public Color getColor(boolean ignoreCustom) {
        Color c = Main.pref.getColor(marktr("gps point"), "layer " + getName(), Color.gray);

        return ignoreCustom || getColorMode() == colorModes.none ? c : null;
    }

    public colorModes getColorMode() {
        try {
            int i=Main.pref.getInteger("draw.rawgps.colors", "layer " + getName(), 0);
            return colorModes.values()[i];
        } catch (Exception e) {
            Main.warn(e);
        }
        return colorModes.none;
    }

    /* for preferences */
    static public Color getGenericColor() {
        return Main.pref.getColor(marktr("gps point"), Color.gray);
    }

    @Override
    public Action[] getMenuEntries() {
        if (Main.applet) {
            return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new CustomizeColor(this),
                new CustomizeDrawingAction(this),
                new ConvertToDataLayerAction(this),
                SeparatorLayerAction.INSTANCE,
                new ChooseTrackVisibilityAction(this),
                new RenameLayerAction(getAssociatedFile(), this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this) };
        }
        return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new LayerSaveAction(this),
                new LayerSaveAsAction(this),
                new CustomizeColor(this),
                new CustomizeDrawingAction(this),
                new ImportImagesAction(this),
                new ImportAudioAction(this),
                new MarkersFromNamedPointsAction(this),
                new ConvertToDataLayerAction(this),
                new DownloadAlongTrackAction(data),
                new DownloadWmsAlongTrackAction(data),
                SeparatorLayerAction.INSTANCE,
                new ChooseTrackVisibilityAction(this),
                new RenameLayerAction(getAssociatedFile(), this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this) };
    }

    public boolean isLocalFile() {
        return isLocalFile;
    }

    @Override
    public String getToolTipText() {
        StringBuilder info = new StringBuilder().append("<html>");

        if (data.attr.containsKey("name")) {
            info.append(tr("Name: {0}", data.attr.get(GpxConstants.META_NAME))).append("<br>");
        }

        if (data.attr.containsKey("desc")) {
            info.append(tr("Description: {0}", data.attr.get(GpxConstants.META_DESC))).append("<br>");
        }

        info.append(trn("{0} track, ", "{0} tracks, ", data.tracks.size(), data.tracks.size()));
        info.append(trn("{0} route, ", "{0} routes, ", data.routes.size(), data.routes.size()));
        info.append(trn("{0} waypoint", "{0} waypoints", data.waypoints.size(), data.waypoints.size())).append("<br>");

        info.append(tr("Length: {0}", NavigatableComponent.getSystemOfMeasurement().getDistText(data.length())));
        info.append("<br>");

        return info.append("</html>").toString();
    }

    @Override
    public boolean isMergable(Layer other) {
        return other instanceof GpxLayer;
    }

    private int sumUpdateCount() {
        int updateCount = 0;
        for (GpxTrack track: data.tracks) {
            updateCount += track.getUpdateCount();
        }
        return updateCount;
    }

    @Override
    public boolean isChanged() {
        if (data.tracks.equals(lastTracks))
            return sumUpdateCount() != lastUpdateCount;
        else
            return true;
    }

    public void filterTracksByDate(Date fromDate, Date toDate, boolean showWithoutDate) {
        int i = 0;
        long from = fromDate.getTime();
        long to = toDate.getTime();
        for (GpxTrack trk : data.tracks) {
            Date[] t = GpxLayer.getMinMaxTimeForTrack(trk);

            if (t==null) continue;
            long tm = t[1].getTime();
            trackVisibility[i]= (tm==0 && showWithoutDate) || (from<=tm && tm <= to);
            i++;
        }
    }

    @Override
    public void mergeFrom(Layer from) {
        data.mergeFrom(((GpxLayer) from).data);
        computeCacheInSync = false;
    }

    private final static Color[] colors = new Color[256];
    static {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.getHSBColor(i / 300.0f, 1, 1);
        }
    }

    private final static Color[] colors_cyclic = new Color[256];
    static {
        for (int i = 0; i < colors_cyclic.length; i++) {
            //                    red   yellow  green   blue    red
            int[] h = new int[] { 0,    59,     127,    244,    360};
            int[] s = new int[] { 100,  84,     99,     100 };
            int[] b = new int[] { 90,   93,     74,     83 };

            float angle = 4 - i / 256f * 4;
            int quadrant = (int) angle;
            angle -= quadrant;
            quadrant = Utils.mod(quadrant+1, 4);

            float vh = h[quadrant] * w(angle) + h[quadrant+1] * (1 - w(angle));
            float vs = s[quadrant] * w(angle) + s[Utils.mod(quadrant+1, 4)] * (1 - w(angle));
            float vb = b[quadrant] * w(angle) + b[Utils.mod(quadrant+1, 4)] * (1 - w(angle));

            colors_cyclic[i] = Color.getHSBColor(vh/360f, vs/100f, vb/100f);
        }
    }

    /**
     * transition function:
     *  w(0)=1, w(1)=0, 0&lt;=w(x)&lt;=1
     * @param x number: 0&lt;=x&lt;=1
     * @return the weighted value
     */
    private static float w(float x) {
        if (x < 0.5)
            return 1 - 2*x*x;
        else
            return 2*(1-x)*(1-x);
    }

    // lookup array to draw arrows without doing any math
    private final static int ll0 = 9;
    private final static int sl4 = 5;
    private final static int sl9 = 3;
    private final static int[][] dir = { { +sl4, +ll0, +ll0, +sl4 }, { -sl9, +ll0, +sl9, +ll0 }, { -ll0, +sl4, -sl4, +ll0 },
        { -ll0, -sl9, -ll0, +sl9 }, { -sl4, -ll0, -ll0, -sl4 }, { +sl9, -ll0, -sl9, -ll0 },
        { +ll0, -sl4, +sl4, -ll0 }, { +ll0, +sl9, +ll0, -sl9 }, { +sl4, +ll0, +ll0, +sl4 },
        { -sl9, +ll0, +sl9, +ll0 }, { -ll0, +sl4, -sl4, +ll0 }, { -ll0, -sl9, -ll0, +sl9 } };

    // the different color modes
    enum colorModes {
        none, velocity, dilution, direction, time
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        lastUpdateCount = sumUpdateCount();
        lastTracks.clear();
        lastTracks.addAll(data.tracks);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.gpx.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        /****************************************************************
         ********** STEP 1 - GET CONFIG VALUES **************************
         ****************************************************************/
        Color neutralColor = getColor(true);
        String spec="layer "+getName();

        // also draw lines between points belonging to different segments
        boolean forceLines = Main.pref.getBoolean("draw.rawgps.lines.force", spec, false);
        // draw direction arrows on the lines
        boolean direction = Main.pref.getBoolean("draw.rawgps.direction", spec, false);
        // don't draw lines if longer than x meters
        int lineWidth = Main.pref.getInteger("draw.rawgps.linewidth", spec, 0);

        int maxLineLength;
        boolean lines;
        if (!this.data.fromServer) {
            maxLineLength = Main.pref.getInteger("draw.rawgps.max-line-length.local", spec, -1);
            lines = Main.pref.getBoolean("draw.rawgps.lines.local", spec, true);
        } else {
            maxLineLength = Main.pref.getInteger("draw.rawgps.max-line-length", spec, 200);
            lines = Main.pref.getBoolean("draw.rawgps.lines", spec, true);
        }
        // paint large dots for points
        boolean large = Main.pref.getBoolean("draw.rawgps.large", spec, false);
        int largesize = Main.pref.getInteger("draw.rawgps.large.size", spec, 3);
        boolean hdopcircle = Main.pref.getBoolean("draw.rawgps.hdopcircle", spec, false);
        // color the lines
        colorModes colored = getColorMode();
        // paint direction arrow with alternate math. may be faster
        boolean alternatedirection = Main.pref.getBoolean("draw.rawgps.alternatedirection", spec, false);
        // don't draw arrows nearer to each other than this
        int delta = Main.pref.getInteger("draw.rawgps.min-arrow-distance", spec, 40);
        // allows to tweak line coloring for different speed levels.
        int colorTracksTune = Main.pref.getInteger("draw.rawgps.colorTracksTune", spec, 45);
        boolean colorModeDynamic = Main.pref.getBoolean("draw.rawgps.colors.dynamic", spec, false);
        int hdopfactor = Main.pref.getInteger("hdop.factor", 25);

        Stroke storedStroke = g.getStroke();
        if(lineWidth != 0)
        {
            g.setStroke(new BasicStroke(lineWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            largesize += lineWidth;
        }

        /****************************************************************
         ********** STEP 2a - CHECK CACHE VALIDITY **********************
         ****************************************************************/
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

        /****************************************************************
         ********** STEP 2b - RE-COMPUTE CACHE DATA *********************
         ****************************************************************/
        if (!computeCacheInSync) { // don't compute if the cache is good
            double minval = +1e10;
            double maxval = -1e10;
            WayPoint oldWp = null;
            if (colorModeDynamic) {
                if (colored == colorModes.velocity) {
                    for (Collection<WayPoint> segment : data.getLinesIterable(null)) {
                        if(!forceLines) {
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
                                if(vel > maxval) {
                                    maxval = vel;
                                }
                                if(vel < minval) {
                                    minval = vel;
                                }
                            }
                            oldWp = trkPnt;
                        }
                    }
                } else if (colored == colorModes.dilution) {
                    for (Collection<WayPoint> segment : data.getLinesIterable(null)) {
                        for (WayPoint trkPnt : segment) {
                            Object val = trkPnt.attr.get("hdop");
                            if (val != null) {
                                double hdop = ((Float) val).doubleValue();
                                if(hdop > maxval) {
                                    maxval = hdop;
                                }
                                if(hdop < minval) {
                                    minval = hdop;
                                }
                            }
                        }
                    }
                }
                oldWp = null;
            }
            double now = System.currentTimeMillis()/1000.0;
            if (colored == colorModes.time) {
                Date[] bounds = getMinMaxTimeForAllTracks();
                if (bounds!=null) {
                    minval = bounds[0].getTime()/1000.0;
                    maxval = bounds[1].getTime()/1000.0;
                } else {
                    minval = 0; maxval=now;
                }
            }

            for (Collection<WayPoint> segment : data.getLinesIterable(null)) {
                if (!forceLines) { // don't draw lines between segments, unless forced to
                    oldWp = null;
                }
                for (WayPoint trkPnt : segment) {
                    LatLon c = trkPnt.getCoor();
                    if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                        continue;
                    }
                    trkPnt.customColoring = neutralColor;
                    if(colored == colorModes.dilution && trkPnt.attr.get("hdop") != null) {
                        float hdop = ((Float) trkPnt.attr.get("hdop")).floatValue();
                        int hdoplvl =(int) Math.round(colorModeDynamic ? ((hdop-minval)*255/(maxval-minval))
                                : (hdop <= 0 ? 0 : hdop * hdopfactor));
                        // High hdop is bad, but high values in colors are green.
                        // Therefore inverse the logic
                        int hdopcolor = 255 - (hdoplvl > 255 ? 255 : hdoplvl);
                        trkPnt.customColoring = colors[hdopcolor];
                    }
                    if (oldWp != null) {
                        double dist = c.greatCircleDistance(oldWp.getCoor());
                        boolean noDraw=false;
                        switch (colored) {
                        case velocity:
                            double dtime = trkPnt.time - oldWp.time;
                            if(dtime > 0) {
                                float vel = (float) (dist / dtime);
                                int velColor =(int) Math.round(colorModeDynamic ? ((vel-minval)*255/(maxval-minval))
                                        : (vel <= 0 ? 0 : vel / colorTracksTune * 255));
                                final int vIndex = Math.max(0, Math.min(velColor, 255));
                                trkPnt.customColoring = vIndex == 255 ? neutralColor : colors[vIndex];
                            } else {
                                trkPnt.customColoring = neutralColor;
                            }
                            break;
                        case direction:
                            double dirColor = oldWp.getCoor().heading(trkPnt.getCoor()) / (2.0 * Math.PI) * 256;
                            // Bad case first
                            if (dirColor != dirColor || dirColor < 0.0 || dirColor >= 256.0) {
                                trkPnt.customColoring = colors_cyclic[0];
                            } else {
                                trkPnt.customColoring = colors_cyclic[(int) (dirColor)];
                            }
                            break;
                        case time:
                            double t=trkPnt.time;
                            if (t > 0 && t <= now && maxval - minval > 1000) { // skip bad timestamps and very short tracks
                                int tColor = (int) Math.round((t-minval)*255/(maxval-minval));
                                trkPnt.customColoring = colors[tColor];
                            } else {
                                trkPnt.customColoring = neutralColor;
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
                    }
                    oldWp = trkPnt;
                }
            }
            computeCacheInSync = true;
        }

        LinkedList<WayPoint> visibleSegments = new LinkedList<WayPoint>();
        WayPoint last = null;
        ensureTrackVisibilityLength();
        for (Collection<WayPoint> segment : data.getLinesIterable(trackVisibility)) {

            for(WayPoint pt : segment)
            {
                Bounds b = new Bounds(pt.getCoor());
                // last should never be null when this is true!
                if(pt.drawLine) {
                    b.extend(last.getCoor());
                }
                if(b.intersects(box))
                {
                    if(last != null && (visibleSegments.isEmpty()
                            || visibleSegments.getLast() != last)) {
                        if(last.drawLine) {
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
        if(visibleSegments.isEmpty())
            return;

        /****************************************************************
         ********** STEP 3a - DRAW LINES ********************************
         ****************************************************************/
        if (lines) {
            Point old = null;
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                Point screen = mv.getPoint(trkPnt.getEastNorth());
                if (trkPnt.drawLine) {
                    // skip points that are on the same screenposition
                    if (old != null && ((old.x != screen.x) || (old.y != screen.y))) {
                        g.setColor(trkPnt.customColoring);
                        g.drawLine(old.x, old.y, screen.x, screen.y);
                    }
                }
                old = screen;
            } // end for trkpnt
        } // end if lines

        /****************************************************************
         ********** STEP 3b - DRAW NICE ARROWS **************************
         ****************************************************************/
        if (lines && direction && !alternatedirection) {
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
        } // end if lines

        /****************************************************************
         ********** STEP 3c - DRAW FAST ARROWS **************************
         ****************************************************************/
        if (lines && direction && alternatedirection) {
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
        } // end if lines

        /****************************************************************
         ********** STEP 3d - DRAW LARGE POINTS AND HDOP CIRCLE *********
         ****************************************************************/
        if (large || hdopcircle) {
            g.setColor(neutralColor);
            for (WayPoint trkPnt : visibleSegments) {
                LatLon c = trkPnt.getCoor();
                if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                    continue;
                }
                Point screen = mv.getPoint(trkPnt.getEastNorth());
                g.setColor(trkPnt.customColoring);
                if (hdopcircle && trkPnt.attr.get("hdop") != null) {
                    // hdop value
                    float hdop = ((Float)trkPnt.attr.get("hdop")).floatValue();
                    if (hdop < 0) {
                        hdop = 0;
                    }
                    // hdop pixels
                    int hdopp = mv.getPoint(new LatLon(trkPnt.getCoor().lat(), trkPnt.getCoor().lon() + 2*6*hdop*360/40000000)).x - screen.x;
                    g.drawArc(screen.x-hdopp/2, screen.y-hdopp/2, hdopp, hdopp, 0, 360);
                }
                if (large) {
                    g.fillRect(screen.x-1, screen.y-1, largesize, largesize);
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

        if(lineWidth != 0)
        {
            g.setStroke(storedStroke);
        }
    } // end paint

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        v.visit(data.recalculateBounds());
    }

    @Override
    public File getAssociatedFile() {
        return data.storageFile;
    }

    @Override
    public void setAssociatedFile(File file) {
        data.storageFile = file;
    }

    /** ensures the trackVisibility array has the correct length without losing data.
     * additional entries are initialized to true;
     */
    final private void ensureTrackVisibilityLength() {
        final int l = data.tracks.size();
        if (l == trackVisibility.length)
            return;
        final int m = Math.min(l, trackVisibility.length);
        trackVisibility = Arrays.copyOf(trackVisibility, l);
        for (int i = m; i < l; i++) {
            trackVisibility[i] = true;
        }
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        if (newValue == null) return;
        if (data.waypoints != null) {
            for (WayPoint wp : data.waypoints){
                wp.invalidateEastNorthCache();
            }
        }
        if (data.tracks != null){
            for (GpxTrack track: data.tracks) {
                for (GpxTrackSegment segment: track.getSegments()) {
                    for (WayPoint wp: segment.getWayPoints()) {
                        wp.invalidateEastNorthCache();
                    }
                }
            }
        }
        if (data.routes != null) {
            for (GpxRoute route: data.routes) {
                if (route.routePoints == null) {
                    continue;
                }
                for (WayPoint wp: route.routePoints) {
                    wp.invalidateEastNorthCache();
                }
            }
        }
    }

    @Override
    public boolean isSavable() {
        return true; // With GpxExporter
    }

    @Override
    public boolean checkSaveConditions() {
        return data != null;
    }

    @Override
    public File createAndOpenSaveFileChooser() {
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save GPX file"), GpxImporter.FILE_FILTER);
    }

}
