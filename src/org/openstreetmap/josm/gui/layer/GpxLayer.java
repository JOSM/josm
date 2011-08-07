// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTaskList;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.preferences.GPXSettingsPanel;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.JpgImporter;
import org.openstreetmap.josm.tools.AudioUtil;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;
import org.openstreetmap.josm.tools.Utils;

public class GpxLayer extends Layer {

    private static final String PREF_DOWNLOAD_ALONG_TRACK_DISTANCE = "gpxLayer.downloadAlongTrack.distance";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_AREA = "gpxLayer.downloadAlongTrack.area";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_NEAR = "gpxLayer.downloadAlongTrack.near";

    public GpxData data;
    protected static final double PHI = Math.toRadians(15);
    private boolean computeCacheInSync;
    private int computeCacheMaxLineLengthUsed;
    private Color computeCacheColorUsed;
    private boolean computeCacheColorDynamic;
    private colorModes computeCacheColored;
    private int computeCacheColorTracksTune;
    private boolean isLocalFile;

    private final List<GpxTrack> lastTracks = new ArrayList<GpxTrack>(); // List of tracks at last paint
    private int lastUpdateCount;

    private static class Markers {
        public boolean timedMarkersOmitted = false;
        public boolean untimedMarkersOmitted = false;
    }

    public GpxLayer(GpxData d) {
        super((String) d.attr.get("name"));
        data = d;
        computeCacheInSync = false;
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

    @Override
    public Icon getIcon() {
        return ImageProvider.get("layer", "gpx_small");
    }

    @Override
    public Object getInfoComponent() {
        StringBuilder info = new StringBuilder();

        if (data.attr.containsKey("name")) {
            info.append(tr("Name: {0}", data.attr.get(GpxData.META_NAME))).append("<br>");
        }

        if (data.attr.containsKey("desc")) {
            info.append(tr("Description: {0}", data.attr.get(GpxData.META_DESC))).append("<br>");
        }

        if (data.tracks.size() > 0) {
            info.append("<table><thead align='center'><tr><td colspan='5'>"
                    + trn("{0} track", "{0} tracks", data.tracks.size(), data.tracks.size())
                    + "</td></tr><tr align='center'><td>" + tr("Name") + "</td><td>"
                    + tr("Description") + "</td><td>" + tr("Timespan")
                    + "</td><td>" + tr("Length") + "</td><td>" + tr("URL")
                    + "</td></tr></thead>");

            for (GpxTrack trk : data.tracks) {
                WayPoint earliest = null, latest = null;

                info.append("<tr><td>");
                if (trk.getAttributes().containsKey("name")) {
                    info.append(trk.getAttributes().get("name"));
                }
                info.append("</td><td>");
                if (trk.getAttributes().containsKey("desc")) {
                    info.append(" ").append(trk.getAttributes().get("desc"));
                }
                info.append("</td><td>");

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

                if (earliest != null && latest != null) {
                    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
                    String earliestDate = df.format(earliest.getTime());
                    String latestDate = df.format(latest.getTime());

                    if (earliestDate.equals(latestDate)) {
                        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
                        info.append(earliestDate).append(" ");
                        info.append(tf.format(earliest.getTime())).append(" - ").append(tf.format(latest.getTime()));
                    } else {
                        DateFormat dtf = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                        info.append(dtf.format(earliest.getTime())).append(" - ").append(dtf.format(latest.getTime()));
                    }

                    int diff = (int) (latest.time - earliest.time);
                    info.append(String.format(" (%d:%02d)", diff / 3600, (diff % 3600) / 60));
                }

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

        final JScrollPane sp = new JScrollPane(new HtmlPanel(info.toString()), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setPreferredSize(new Dimension(sp.getPreferredSize().width, 350));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                sp.getVerticalScrollBar().setValue(0);
            }
        });
        return sp;
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
        }
        return colorModes.none;
    }

    /* for preferences */
    static public Color getGenericColor() {
        return Main.pref.getColor(marktr("gps point"), Color.gray);
    }

    @Override
    public Action[] getMenuEntries() {
        if (Main.applet)
            return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new CustomizeColor(this),
                new CustomizeDrawing(this),
                new ConvertToDataLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new RenameLayerAction(getAssociatedFile(), this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this) };
        return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new LayerSaveAction(this),
                new LayerSaveAsAction(this),
                new CustomizeColor(this),
                new CustomizeDrawing(this),
                new ImportImages(),
                new ImportAudio(),
                new MarkersFromNamedPoins(),
                new ConvertToDataLayerAction(),
                new DownloadAlongTrackAction(),
                SeparatorLayerAction.INSTANCE,
                new RenameLayerAction(getAssociatedFile(), this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this) };
    }

    @Override
    public String getToolTipText() {
        StringBuilder info = new StringBuilder().append("<html>");

        if (data.attr.containsKey("name")) {
            info.append(tr("Name: {0}", data.attr.get(GpxData.META_NAME))).append("<br>");
        }

        if (data.attr.containsKey("desc")) {
            info.append(tr("Description: {0}", data.attr.get(GpxData.META_DESC))).append("<br>");
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
     *  w(0)=1, w(1)=0, 0<=w(x)<=1
     * @param x number: 0<=x<=1
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
        // Long startTime = System.currentTimeMillis();
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
        if (this.isLocalFile) {
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

        if(lineWidth != 0)
        {
            g.setStroke(new BasicStroke(lineWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
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
                    for (GpxTrack trk : data.tracks) {
                        for (GpxTrackSegment segment : trk.getSegments()) {
                            if(!forceLines) {
                                oldWp = null;
                            }
                            for (WayPoint trkPnt : segment.getWayPoints()) {
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
                    }
                } else if (colored == colorModes.dilution) {
                    for (GpxTrack trk : data.tracks) {
                        for (GpxTrackSegment segment : trk.getSegments()) {
                            for (WayPoint trkPnt : segment.getWayPoints()) {
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
                }
                oldWp = null;
            }
            if (colored == colorModes.time) {
                for (GpxTrack trk : data.tracks) {
                    for (GpxTrackSegment segment : trk.getSegments()) {
                        for (WayPoint trkPnt : segment.getWayPoints()) {
                            double t=trkPnt.time;
                            if (t==0) {
                                continue; // skip non-dated trackpoints
                            }
                            if(t > maxval) {
                                maxval = t;
                            }
                            if(t < minval) {
                                minval = t;
                            }
                        }
                    }
                }
            }

            for (GpxTrack trk : data.tracks) {
                for (GpxTrackSegment segment : trk.getSegments()) {
                    if (!forceLines) { // don't draw lines between segments, unless forced to
                        oldWp = null;
                    }
                    for (WayPoint trkPnt : segment.getWayPoints()) {
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
                                    trkPnt.customColoring = colors[velColor > 255 ? 255 : velColor];
                                } else {
                                    trkPnt.customColoring = colors[255];
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
                                if (trkPnt.time>0){
                                    int tColor = (int) Math.round((trkPnt.time-minval)*255/(maxval-minval));
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
            }
            computeCacheInSync = true;
        }

        LinkedList<WayPoint> visibleSegments = new LinkedList<WayPoint>();
        WayPoint last = null;
        for (GpxTrack trk: data.tracks) {
            for (GpxTrackSegment trkSeg: trk.getSegments()) {
                for(WayPoint pt : trkSeg.getWayPoints())
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

        // Long duration = System.currentTimeMillis() - startTime;
        // System.out.println(duration);
    } // end paint

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        v.visit(data.recalculateBounds());
    }

    public class ConvertToDataLayerAction extends AbstractAction {
        public ConvertToDataLayerAction() {
            super(tr("Convert to data layer"), ImageProvider.get("converttoosm"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JPanel msg = new JPanel(new GridBagLayout());
            msg
            .add(
                    new JLabel(
                            tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>If you want to upload traces, look here:</html>")),
                            GBC.eol());
            msg.add(new UrlLabel(tr("http://www.openstreetmap.org/traces")), GBC.eop());
            if (!ConditionalOptionPaneUtil.showConfirmationDialog("convert_to_data", Main.parent, msg, tr("Warning"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION))
                return;
            DataSet ds = new DataSet();
            for (GpxTrack trk : data.tracks) {
                for (GpxTrackSegment segment : trk.getSegments()) {
                    List<Node> nodes = new ArrayList<Node>();
                    for (WayPoint p : segment.getWayPoints()) {
                        Node n = new Node(p.getCoor());
                        String timestr = p.getString("time");
                        if (timestr != null) {
                            n.setTimestamp(DateUtils.fromString(timestr));
                        }
                        ds.addPrimitive(n);
                        nodes.add(n);
                    }
                    Way w = new Way();
                    w.setNodes(nodes);
                    ds.addPrimitive(w);
                }
            }
            Main.main
            .addLayer(new OsmDataLayer(ds, tr("Converted from: {0}", GpxLayer.this.getName()), getAssociatedFile()));
            Main.main.removeLayer(GpxLayer.this);
        }
    }

    @Override
    public File getAssociatedFile() {
        return data.storageFile;
    }

    @Override
    public void setAssociatedFile(File file) {
        data.storageFile = file;
    }

    /**
     * Action that issues a series of download requests to the API, following the GPX track.
     *
     * @author fred
     */
    public class DownloadAlongTrackAction extends AbstractAction {
        public DownloadAlongTrackAction() {
            super(tr("Download from OSM along this track"), ImageProvider.get("downloadalongtrack"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JPanel msg = new JPanel(new GridBagLayout());
            Integer dist[] = { 5000, 500, 50 };
            Integer area[] = { 20, 10, 5, 1 };

            msg.add(new JLabel(tr("Download everything within:")), GBC.eol());
            String s[] = new String[dist.length];
            for (int i = 0; i < dist.length; ++i) {
                s[i] = tr("{0} meters", dist[i]);
            }
            JList buffer = new JList(s);
            buffer.setSelectedIndex(Main.pref.getInteger(PREF_DOWNLOAD_ALONG_TRACK_DISTANCE, 0));
            msg.add(buffer, GBC.eol());

            msg.add(new JLabel(tr("Maximum area per request:")), GBC.eol());
            s = new String[area.length];
            for (int i = 0; i < area.length; ++i) {
                s[i] = tr("{0} sq km", area[i]);
            }
            JList maxRect = new JList(s);
            maxRect.setSelectedIndex(Main.pref.getInteger(PREF_DOWNLOAD_ALONG_TRACK_AREA, 0));
            msg.add(maxRect, GBC.eol());

            msg.add(new JLabel(tr("Download near:")), GBC.eol());
            JList downloadNear = new JList(new String[] { tr("track only"), tr("waypoints only"), tr("track and waypoints") });
            int NEAR_TRACK=0;
            int NEAR_WAYPOINTS=1;
            int NEAR_BOTH=2;

            downloadNear.setSelectedIndex(Main.pref.getInteger(PREF_DOWNLOAD_ALONG_TRACK_NEAR, 0));
            msg.add(downloadNear, GBC.eol());

            int ret = JOptionPane.showConfirmDialog(
                    Main.parent,
                    msg,
                    tr("Download from OSM along this track"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                    );
            switch(ret) {
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                return;
            default:
                // continue
            }

            Main.pref.putInteger(PREF_DOWNLOAD_ALONG_TRACK_DISTANCE, buffer.getSelectedIndex());
            Main.pref.putInteger(PREF_DOWNLOAD_ALONG_TRACK_AREA, maxRect.getSelectedIndex());
            int near = downloadNear.getSelectedIndex();
            Main.pref.putInteger(PREF_DOWNLOAD_ALONG_TRACK_NEAR, near);

            /*
             * Find the average latitude for the data we're contemplating, so we can know how many
             * metres per degree of longitude we have.
             */
            double latsum = 0;
            int latcnt = 0;

            if (near == NEAR_TRACK || near == NEAR_BOTH) {
                for (GpxTrack trk : data.tracks) {
                    for (GpxTrackSegment segment : trk.getSegments()) {
                        for (WayPoint p : segment.getWayPoints()) {
                            latsum += p.getCoor().lat();
                            latcnt++;
                        }
                    }
                }
            }

            if (near == NEAR_WAYPOINTS || near == NEAR_BOTH) {
                for (WayPoint p : data.waypoints) {
                    latsum += p.getCoor().lat();
                    latcnt++;
                }
            }

            double avglat = latsum / latcnt;
            double scale = Math.cos(Math.toRadians(avglat));

            /*
             * Compute buffer zone extents and maximum bounding box size. Note that the maximum we
             * ever offer is a bbox area of 0.002, while the API theoretically supports 0.25, but as
             * soon as you touch any built-up area, that kind of bounding box will download forever
             * and then stop because it has more than 50k nodes.
             */
            Integer i = buffer.getSelectedIndex();
            int buffer_dist = dist[i < 0 ? 0 : i];
            double buffer_y = buffer_dist / 100000.0;
            double buffer_x = buffer_y / scale;
            i = maxRect.getSelectedIndex();
            double max_area = area[i < 0 ? 0 : i] / 10000.0 / scale;
            Area a = new Area();
            Rectangle2D r = new Rectangle2D.Double();

            /*
             * Collect the combined area of all gpx points plus buffer zones around them. We ignore
             * points that lie closer to the previous point than the given buffer size because
             * otherwise this operation takes ages.
             */
            LatLon previous = null;
            if (near == NEAR_TRACK || near == NEAR_BOTH) {
                for (GpxTrack trk : data.tracks) {
                    for (GpxTrackSegment segment : trk.getSegments()) {
                        for (WayPoint p : segment.getWayPoints()) {
                            LatLon c = p.getCoor();
                            if (previous == null || c.greatCircleDistance(previous) > buffer_dist) {
                                // we add a buffer around the point.
                                r.setRect(c.lon() - buffer_x, c.lat() - buffer_y, 2 * buffer_x, 2 * buffer_y);
                                a.add(new Area(r));
                                previous = c;
                            }
                        }
                    }
                }
            }
            if (near == NEAR_WAYPOINTS || near == NEAR_BOTH) {
                for (WayPoint p : data.waypoints) {
                    LatLon c = p.getCoor();
                    if (previous == null || c.greatCircleDistance(previous) > buffer_dist) {
                        // we add a buffer around the point.
                        r.setRect(c.lon() - buffer_x, c.lat() - buffer_y, 2 * buffer_x, 2 * buffer_y);
                        a.add(new Area(r));
                        previous = c;
                    }
                }
            }

            /*
             * Area "a" now contains the hull that we would like to download data for. however we
             * can only download rectangles, so the following is an attempt at finding a number of
             * rectangles to download.
             *
             * The idea is simply: Start out with the full bounding box. If it is too large, then
             * split it in half and repeat recursively for each half until you arrive at something
             * small enough to download. The algorithm is improved by always using the intersection
             * between the rectangle and the actual desired area. For example, if you have a track
             * that goes like this: +----+ | /| | / | | / | |/ | +----+ then we would first look at
             * downloading the whole rectangle (assume it's too big), after that we split it in half
             * (upper and lower half), but we donot request the full upper and lower rectangle, only
             * the part of the upper/lower rectangle that actually has something in it.
             */

            List<Rectangle2D> toDownload = new ArrayList<Rectangle2D>();

            addToDownload(a, a.getBounds(), toDownload, max_area);

            msg = new JPanel(new GridBagLayout());

            msg.add(new JLabel(
                    tr("<html>This action will require {0} individual<br>"
                            + "download requests. Do you wish<br>to continue?</html>",
                            toDownload.size())), GBC.eol());

            if (toDownload.size() > 1) {
                ret = JOptionPane.showConfirmDialog(
                        Main.parent,
                        msg,
                        tr("Download from OSM along this track"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                        );
                switch(ret) {
                case JOptionPane.CANCEL_OPTION:
                case JOptionPane.CLOSED_OPTION:
                    return;
                default:
                    // continue
                }
            }
            final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download data"));
            final Future<?> future = new DownloadOsmTaskList().download(false, toDownload, monitor);
            Main.worker.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                future.get();
                            } catch(Exception e) {
                                e.printStackTrace();
                                return;
                            }
                            monitor.close();
                        }
                    }
                    );
        }
    }

    private static void addToDownload(Area a, Rectangle2D r, Collection<Rectangle2D> results, double max_area) {
        Area tmp = new Area(r);
        // intersect with sought-after area
        tmp.intersect(a);
        if (tmp.isEmpty())
            return;
        Rectangle2D bounds = tmp.getBounds2D();
        if (bounds.getWidth() * bounds.getHeight() > max_area) {
            // the rectangle gets too large; split it and make recursive call.
            Rectangle2D r1;
            Rectangle2D r2;
            if (bounds.getWidth() > bounds.getHeight()) {
                // rectangles that are wider than high are split into a left and right half,
                r1 = new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth() / 2, bounds.getHeight());
                r2 = new Rectangle2D.Double(bounds.getX() + bounds.getWidth() / 2, bounds.getY(),
                        bounds.getWidth() / 2, bounds.getHeight());
            } else {
                // others into a top and bottom half.
                r1 = new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight() / 2);
                r2 = new Rectangle2D.Double(bounds.getX(), bounds.getY() + bounds.getHeight() / 2, bounds.getWidth(),
                        bounds.getHeight() / 2);
            }
            addToDownload(a, r1, results, max_area);
            addToDownload(a, r2, results, max_area);
        } else {
            results.add(bounds);
        }
    }

    /**
     * Makes a new marker layer derived from this GpxLayer containing at least one audio marker
     * which the given audio file is associated with. Markers are derived from the following (a)
     * explict waypoints in the GPX layer, or (b) named trackpoints in the GPX layer, or (d)
     * timestamp on the wav file (e) (in future) voice recognised markers in the sound recording (f)
     * a single marker at the beginning of the track
     * @param wavFile : the file to be associated with the markers in the new marker layer
     * @param markers : keeps track of warning messages to avoid repeated warnings
     */
    private void importAudio(File wavFile, MarkerLayer ml, double firstStartTime, Markers markers) {
        URL url = null;
        try {
            url = wavFile.toURI().toURL();
        } catch (MalformedURLException e) {
            System.err.println("Unable to convert filename " + wavFile.getAbsolutePath() + " to URL");
        }
        Collection<WayPoint> waypoints = new ArrayList<WayPoint>();
        boolean timedMarkersOmitted = false;
        boolean untimedMarkersOmitted = false;
        double snapDistance = Main.pref.getDouble("marker.audiofromuntimedwaypoints.distance", 1.0e-3); /*
         * about
         * 25
         * m
         */
        WayPoint wayPointFromTimeStamp = null;

        // determine time of first point in track
        double firstTime = -1.0;
        if (data.tracks != null && !data.tracks.isEmpty()) {
            for (GpxTrack track : data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        firstTime = w.time;
                        break;
                    }
                    if (firstTime >= 0.0) {
                        break;
                    }
                }
                if (firstTime >= 0.0) {
                    break;
                }
            }
        }
        if (firstTime < 0.0) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No GPX track available in layer to associate audio with."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
                    );
            return;
        }

        // (a) try explicit timestamped waypoints - unless suppressed
        if (Main.pref.getBoolean("marker.audiofromexplicitwaypoints", true) && data.waypoints != null
                && !data.waypoints.isEmpty()) {
            for (WayPoint w : data.waypoints) {
                if (w.time > firstTime) {
                    waypoints.add(w);
                } else if (w.time > 0.0) {
                    timedMarkersOmitted = true;
                }
            }
        }

        // (b) try explicit waypoints without timestamps - unless suppressed
        if (Main.pref.getBoolean("marker.audiofromuntimedwaypoints", true) && data.waypoints != null
                && !data.waypoints.isEmpty()) {
            for (WayPoint w : data.waypoints) {
                if (waypoints.contains(w)) {
                    continue;
                }
                WayPoint wNear = nearestPointOnTrack(w.getEastNorth(), snapDistance);
                if (wNear != null) {
                    WayPoint wc = new WayPoint(w.getCoor());
                    wc.time = wNear.time;
                    if (w.attr.containsKey("name")) {
                        wc.attr.put("name", w.getString("name"));
                    }
                    waypoints.add(wc);
                } else {
                    untimedMarkersOmitted = true;
                }
            }
        }

        // (c) use explicitly named track points, again unless suppressed
        if ((Main.pref.getBoolean("marker.audiofromnamedtrackpoints", false)) && data.tracks != null
                && !data.tracks.isEmpty()) {
            for (GpxTrack track : data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        if (w.attr.containsKey("name") || w.attr.containsKey("desc")) {
                            waypoints.add(w);
                        }
                    }
                }
            }
        }

        // (d) use timestamp of file as location on track
        if ((Main.pref.getBoolean("marker.audiofromwavtimestamps", false)) && data.tracks != null
                && !data.tracks.isEmpty()) {
            double lastModified = wavFile.lastModified() / 1000.0; // lastModified is in
            // milliseconds
            double duration = AudioUtil.getCalibratedDuration(wavFile);
            double startTime = lastModified - duration;
            startTime = firstStartTime + (startTime - firstStartTime)
                    / Main.pref.getDouble("audio.calibration", "1.0" /* default, ratio */);
            WayPoint w1 = null;
            WayPoint w2 = null;

            for (GpxTrack track : data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        if (startTime < w.time) {
                            w2 = w;
                            break;
                        }
                        w1 = w;
                    }
                    if (w2 != null) {
                        break;
                    }
                }
            }

            if (w1 == null || w2 == null) {
                timedMarkersOmitted = true;
            } else {
                wayPointFromTimeStamp = new WayPoint(w1.getCoor().interpolate(w2.getCoor(),
                        (startTime - w1.time) / (w2.time - w1.time)));
                wayPointFromTimeStamp.time = startTime;
                String name = wavFile.getName();
                int dot = name.lastIndexOf(".");
                if (dot > 0) {
                    name = name.substring(0, dot);
                }
                wayPointFromTimeStamp.attr.put("name", name);
                waypoints.add(wayPointFromTimeStamp);
            }
        }

        // (e) analyse audio for spoken markers here, in due course

        // (f) simply add a single marker at the start of the track
        if ((Main.pref.getBoolean("marker.audiofromstart") || waypoints.isEmpty()) && data.tracks != null
                && !data.tracks.isEmpty()) {
            boolean gotOne = false;
            for (GpxTrack track : data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint w : seg.getWayPoints()) {
                        WayPoint wStart = new WayPoint(w.getCoor());
                        wStart.attr.put("name", "start");
                        wStart.time = w.time;
                        waypoints.add(wStart);
                        gotOne = true;
                        break;
                    }
                    if (gotOne) {
                        break;
                    }
                }
                if (gotOne) {
                    break;
                }
            }
        }

        /* we must have got at least one waypoint now */

        Collections.sort((ArrayList<WayPoint>) waypoints, new Comparator<WayPoint>() {
            @Override
            public int compare(WayPoint a, WayPoint b) {
                return a.time <= b.time ? -1 : 1;
            }
        });

        firstTime = -1.0; /* this time of the first waypoint, not first trackpoint */
        for (WayPoint w : waypoints) {
            if (firstTime < 0.0) {
                firstTime = w.time;
            }
            double offset = w.time - firstTime;
            AudioMarker am = new AudioMarker(w.getCoor(), w, url, ml, w.time, offset);
            /*
             * timeFromAudio intended for future use to shift markers of this type on
             * synchronization
             */
            if (w == wayPointFromTimeStamp) {
                am.timeFromAudio = true;
            }
            ml.data.add(am);
        }

        if (timedMarkersOmitted && !markers.timedMarkersOmitted) {
            JOptionPane
            .showMessageDialog(
                    Main.parent,
                    tr("Some waypoints with timestamps from before the start of the track or after the end were omitted or moved to the start."));
            markers.timedMarkersOmitted = timedMarkersOmitted;
        }
        if (untimedMarkersOmitted && !markers.untimedMarkersOmitted) {
            JOptionPane
            .showMessageDialog(
                    Main.parent,
                    tr("Some waypoints which were too far from the track to sensibly estimate their time were omitted."));
            markers.untimedMarkersOmitted = untimedMarkersOmitted;
        }
    }

    /**
     * Makes a WayPoint at the projection of point P onto the track providing P is less than
     * tolerance away from the track
     *
     * @param P : the point to determine the projection for
     * @param tolerance : must be no further than this from the track
     * @return the closest point on the track to P, which may be the first or last point if off the
     * end of a segment, or may be null if nothing close enough
     */
    public WayPoint nearestPointOnTrack(EastNorth P, double tolerance) {
        /*
         * assume the coordinates of P are xp,yp, and those of a section of track between two
         * trackpoints are R=xr,yr and S=xs,ys. Let N be the projected point.
         *
         * The equation of RS is Ax + By + C = 0 where A = ys - yr B = xr - xs C = - Axr - Byr
         *
         * Also, note that the distance RS^2 is A^2 + B^2
         *
         * If RS^2 == 0.0 ignore the degenerate section of track
         *
         * PN^2 = (Axp + Byp + C)^2 / RS^2 that is the distance from P to the line
         *
         * so if PN^2 is less than PNmin^2 (initialized to tolerance) we can reject the line;
         * otherwise... determine if the projected poijnt lies within the bounds of the line: PR^2 -
         * PN^2 <= RS^2 and PS^2 - PN^2 <= RS^2
         *
         * where PR^2 = (xp - xr)^2 + (yp-yr)^2 and PS^2 = (xp - xs)^2 + (yp-ys)^2
         *
         * If so, calculate N as xn = xr + (RN/RS) B yn = y1 + (RN/RS) A
         *
         * where RN = sqrt(PR^2 - PN^2)
         */

        double PNminsq = tolerance * tolerance;
        EastNorth bestEN = null;
        double bestTime = 0.0;
        double px = P.east();
        double py = P.north();
        double rx = 0.0, ry = 0.0, sx, sy, x, y;
        if (data.tracks == null)
            return null;
        for (GpxTrack track : data.tracks) {
            for (GpxTrackSegment seg : track.getSegments()) {
                WayPoint R = null;
                for (WayPoint S : seg.getWayPoints()) {
                    EastNorth c = S.getEastNorth();
                    if (R == null) {
                        R = S;
                        rx = c.east();
                        ry = c.north();
                        x = px - rx;
                        y = py - ry;
                        double PRsq = x * x + y * y;
                        if (PRsq < PNminsq) {
                            PNminsq = PRsq;
                            bestEN = c;
                            bestTime = R.time;
                        }
                    } else {
                        sx = c.east();
                        sy = c.north();
                        double A = sy - ry;
                        double B = rx - sx;
                        double C = -A * rx - B * ry;
                        double RSsq = A * A + B * B;
                        if (RSsq == 0.0) {
                            continue;
                        }
                        double PNsq = A * px + B * py + C;
                        PNsq = PNsq * PNsq / RSsq;
                        if (PNsq < PNminsq) {
                            x = px - rx;
                            y = py - ry;
                            double PRsq = x * x + y * y;
                            x = px - sx;
                            y = py - sy;
                            double PSsq = x * x + y * y;
                            if (PRsq - PNsq <= RSsq && PSsq - PNsq <= RSsq) {
                                double RNoverRS = Math.sqrt((PRsq - PNsq) / RSsq);
                                double nx = rx - RNoverRS * B;
                                double ny = ry + RNoverRS * A;
                                bestEN = new EastNorth(nx, ny);
                                bestTime = R.time + RNoverRS * (S.time - R.time);
                                PNminsq = PNsq;
                            }
                        }
                        R = S;
                        rx = sx;
                        ry = sy;
                    }
                }
                if (R != null) {
                    EastNorth c = R.getEastNorth();
                    /* if there is only one point in the seg, it will do this twice, but no matter */
                    rx = c.east();
                    ry = c.north();
                    x = px - rx;
                    y = py - ry;
                    double PRsq = x * x + y * y;
                    if (PRsq < PNminsq) {
                        PNminsq = PRsq;
                        bestEN = c;
                        bestTime = R.time;
                    }
                }
            }
        }
        if (bestEN == null)
            return null;
        WayPoint best = new WayPoint(Main.getProjection().eastNorth2latlon(bestEN));
        best.time = bestTime;
        return best;
    }

    private class CustomizeDrawing extends AbstractAction implements LayerAction, MultiLayerAction {
        List<Layer> layers;

        public CustomizeDrawing(List<Layer> l) {
            this();
            layers = l;
        }

        public CustomizeDrawing(Layer l) {
            this();
            layers = new LinkedList<Layer>();
            layers.add(l);
        }

        private CustomizeDrawing() {
            super(tr("Customize track drawing"), ImageProvider.get("mapmode/addsegment"));
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            for(Layer layer: layers) {
                if(!(layer instanceof GpxLayer))
                    return false;
            }
            return true;
        }

        @Override
        public Component createMenuComponent() {
            return new JMenuItem(this);
        }

        @Override
        public Action getMultiLayerAction(List<Layer> layers) {
            return new CustomizeDrawing(layers);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean hasLocal = false, hasNonlocal = false;
            for (Layer layer : layers) {
                if (layer instanceof GpxLayer) {
                    if (((GpxLayer) layer).isLocalFile) {
                        hasLocal = true;
                    } else {
                        hasNonlocal = true;
                    }
                }
            }
            GPXSettingsPanel panel=new GPXSettingsPanel(getName(), hasLocal, hasNonlocal);

            int answer = JOptionPane.showConfirmDialog(Main.parent, panel,
                    tr("Customize track drawing"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) return;
            for(Layer layer : layers) {
                // save preferences for all layers
                boolean f=false;
                if (layer instanceof GpxLayer) {
                    f=((GpxLayer)layer).isLocalFile;
                }
                panel.savePreferences(layer.getName(),f);
            }
            Main.map.repaint();
        }
    }

    private class MarkersFromNamedPoins extends AbstractAction {

        public MarkersFromNamedPoins() {
            super(tr("Markers From Named Points"), ImageProvider.get("addmarkers"));
            putValue("help", ht("/Action/MarkersFromNamedPoints"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GpxData namedTrackPoints = new GpxData();
            for (GpxTrack track : data.tracks) {
                for (GpxTrackSegment seg : track.getSegments()) {
                    for (WayPoint point : seg.getWayPoints())
                        if (point.attr.containsKey("name") || point.attr.containsKey("desc")) {
                            namedTrackPoints.waypoints.add(point);
                        }
                }
            }

            MarkerLayer ml = new MarkerLayer(namedTrackPoints, tr("Named Trackpoints from {0}", getName()),
                    getAssociatedFile(), GpxLayer.this);
            if (ml.data.size() > 0) {
                Main.main.addLayer(ml);
            }

        }
    }

    private class ImportAudio extends AbstractAction {

        public ImportAudio() {
            super(tr("Import Audio"), ImageProvider.get("importaudio"));
            putValue("help", ht("/Action/ImportAudio"));
        }

        private void warnCantImportIntoServerLayer(GpxLayer layer) {
            String msg = tr("<html>The data in the GPX layer ''{0}'' has been downloaded from the server.<br>"
                    + "Because its way points do not include a timestamp we cannot correlate them with audio data.</html>",
                    layer.getName()
                    );
            HelpAwareOptionPane.showOptionDialog(
                    Main.parent,
                    msg,
                    tr("Import not possible"),
                    JOptionPane.WARNING_MESSAGE,
                    ht("/Action/ImportAudio#CantImportIntoGpxLayerFromServer")
                    );
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (GpxLayer.this.data.fromServer) {
                warnCantImportIntoServerLayer(GpxLayer.this);
                return;
            }
            String dir = Main.pref.get("markers.lastaudiodirectory");
            JFileChooser fc = new JFileChooser(dir);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".wav");
                }

                @Override
                public String getDescription() {
                    return tr("Wave Audio files (*.wav)");
                }
            });
            fc.setMultiSelectionEnabled(true);
            if (fc.showOpenDialog(Main.parent) == JFileChooser.APPROVE_OPTION) {
                if (!fc.getCurrentDirectory().getAbsolutePath().equals(dir)) {
                    Main.pref.put("markers.lastaudiodirectory", fc.getCurrentDirectory().getAbsolutePath());
                }

                File sel[] = fc.getSelectedFiles();
                // sort files in increasing order of timestamp (this is the end time, but so
                // long as they don't overlap, that's fine)
                if (sel.length > 1) {
                    Arrays.sort(sel, new Comparator<File>() {
                        @Override
                        public int compare(File a, File b) {
                            return a.lastModified() <= b.lastModified() ? -1 : 1;
                        }
                    });
                }

                String names = null;
                for (int i = 0; i < sel.length; i++) {
                    if (names == null) {
                        names = " (";
                    } else {
                        names += ", ";
                    }
                    names += sel[i].getName();
                }
                if (names != null) {
                    names += ")";
                } else {
                    names = "";
                }
                MarkerLayer ml = new MarkerLayer(new GpxData(), tr("Audio markers from {0}", getName()) + names,
                        getAssociatedFile(), GpxLayer.this);
                double firstStartTime = sel[0].lastModified() / 1000.0 /* ms -> seconds */
                        - AudioUtil.getCalibratedDuration(sel[0]);

                Markers m = new Markers();
                for (int i = 0; i < sel.length; i++) {
                    importAudio(sel[i], ml, firstStartTime, m);
                }
                Main.main.addLayer(ml);
                Main.map.repaint();
            }

        }
    }

    private class ImportImages extends AbstractAction {

        public ImportImages() {
            super(tr("Import images"), ImageProvider.get("dialogs/geoimage"));
            putValue("help", ht("/Action/ImportImages"));
        }

        private void warnCantImportIntoServerLayer(GpxLayer layer) {
            String msg = tr("<html>The data in the GPX layer ''{0}'' has been downloaded from the server.<br>"
                    + "Because its way points do not include a timestamp we cannot correlate them with images.</html>",
                    layer.getName()
                    );
            HelpAwareOptionPane.showOptionDialog(
                    Main.parent,
                    msg,
                    tr("Import not possible"),
                    JOptionPane.WARNING_MESSAGE,
                    ht("/Action/ImportImages#CantImportIntoGpxLayerFromServer")
                    );
        }

        private void addRecursiveFiles(LinkedList<File> files, File[] sel) {
            for (File f : sel) {
                if (f.isDirectory()) {
                    addRecursiveFiles(files, f.listFiles());
                } else if (f.getName().toLowerCase().endsWith(".jpg")) {
                    files.add(f);
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (GpxLayer.this.data.fromServer) {
                warnCantImportIntoServerLayer(GpxLayer.this);
                return;
            }
            String curDir = Main.pref.get("geoimage.lastdirectory", Main.pref.get("lastDirectory"));
            if (curDir.equals("")) {
                curDir = ".";
            }
            JFileChooser fc = new JFileChooser(new File(curDir));

            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setMultiSelectionEnabled(true);
            fc.setAcceptAllFileFilterUsed(false);
            JpgImporter importer = new JpgImporter(GpxLayer.this);
            fc.setFileFilter(importer.filter);
            fc.showOpenDialog(Main.parent);
            LinkedList<File> files = new LinkedList<File>();
            File[] sel = fc.getSelectedFiles();
            if (sel == null || sel.length == 0)
                return;
            if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir)) {
                Main.pref.put("geoimage.lastdirectory", fc.getCurrentDirectory().getAbsolutePath());
            }
            addRecursiveFiles(files, sel);
            importer.importDataHandleExceptions(files, NullProgressMonitor.INSTANCE);
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
}
