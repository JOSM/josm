// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTaskList;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.AudioUtil;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;

public class GpxLayer extends Layer {
    public GpxData data;
    private final GpxLayer me;
    protected static final double PHI = Math.toRadians(15);
    private boolean computeCacheInSync;
    private int computeCacheMaxLineLengthUsed;
    private Color computeCacheColorUsed;
    private colorModes computeCacheColored;
    private int computeCacheColorTracksTune;
    private boolean isLocalFile;

    private class Markers {
        public boolean timedMarkersOmitted = false;
        public boolean untimedMarkersOmitted = false;
    }

    public GpxLayer(GpxData d) {
        super((String) d.attr.get("name"));
        data = d;
        me = this;
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
        return getToolTipText();
    }

    static public Color getColor(String name) {
        return Main.pref.getColor(marktr("gps point"), name != null ? "layer " + name : null, Color.gray);
    }

    @Override
    public Component[] getMenuEntries() {
        JMenuItem line = new JMenuItem(tr("Customize line drawing"), ImageProvider.get("mapmode/addsegment"));
        line.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButton[] r = new JRadioButton[3];
                r[0] = new JRadioButton(tr("Use global settings."));
                r[1] = new JRadioButton(tr("Draw lines between points for this layer."));
                r[2] = new JRadioButton(tr("Do not draw lines between points for this layer."));
                ButtonGroup group = new ButtonGroup();
                Box panel = Box.createVerticalBox();
                for (JRadioButton b : r) {
                    group.add(b);
                    panel.add(b);
                }
                String propName = "draw.rawgps.lines.layer " + getName();
                if (Main.pref.hasKey(propName)) {
                    group.setSelected(r[Main.pref.getBoolean(propName) ? 1 : 2].getModel(), true);
                } else {
                    group.setSelected(r[0].getModel(), true);
                }
                int answer = JOptionPane.showConfirmDialog(Main.parent, panel,
                        tr("Select line drawing options"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                switch (answer) {
                    case JOptionPane.CANCEL_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                        return;
                    default:
                        // continue
                }
                if (group.getSelection() == r[0].getModel()) {
                    Main.pref.put(propName, null);
                } else {
                    Main.pref.put(propName, group.getSelection() == r[1].getModel());
                }
                Main.map.repaint();
            }
        });

        JMenuItem color = new JMenuItem(tr("Customize Color"), ImageProvider.get("colorchooser"));
        color.putClientProperty("help", "Action/LayerCustomizeColor");
        color.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JColorChooser c = new JColorChooser(getColor(getName()));
                Object[] options = new Object[] { tr("OK"), tr("Cancel"), tr("Default") };
                int answer = JOptionPane.showOptionDialog(
                        Main.parent,
                        c,
                        tr("Choose a color"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options, options[0]
                );
                switch (answer) {
                    case 0:
                        Main.pref.putColor("layer " + getName(), c.getColor());
                        break;
                    case 1:
                        return;
                    case 2:
                        Main.pref.putColor("layer " + getName(), null);
                        break;
                }
                Main.map.repaint();
            }
        });

        JMenuItem markersFromNamedTrackpoints = new JMenuItem(tr("Markers From Named Points"), ImageProvider
                .get("addmarkers"));
        markersFromNamedTrackpoints.putClientProperty("help", "Action/MarkersFromNamedPoints");
        markersFromNamedTrackpoints.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GpxData namedTrackPoints = new GpxData();
                for (GpxTrack track : data.tracks) {
                    for (Collection<WayPoint> seg : track.trackSegs) {
                        for (WayPoint point : seg)
                            if (point.attr.containsKey("name") || point.attr.containsKey("desc")) {
                                namedTrackPoints.waypoints.add(point);
                            }
                    }
                }

                MarkerLayer ml = new MarkerLayer(namedTrackPoints, tr("Named Trackpoints from {0}", getName()),
                        getAssociatedFile(), me);
                if (ml.data.size() > 0) {
                    Main.main.addLayer(ml);
                }
            }
        });

        JMenuItem importAudio = new JMenuItem(tr("Import Audio"), ImageProvider.get("importaudio"));
        importAudio.putClientProperty("help", "ImportAudio");
        importAudio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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
                    if (sel != null) {
                        // sort files in increasing order of timestamp (this is the end time, but so
                        // long as they don't overlap, that's fine)
                        if (sel.length > 1) {
                            Arrays.sort(sel, new Comparator<File>() {
                                public int compare(File a, File b) {
                                    return a.lastModified() <= b.lastModified() ? -1 : 1;
                                }
                            });
                        }
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
                            getAssociatedFile(), me);
                    if (sel != null) {
                        double firstStartTime = sel[0].lastModified() / 1000.0 /* ms -> seconds */
                        - AudioUtil.getCalibratedDuration(sel[0]);

                        Markers m = new Markers();
                        for (int i = 0; i < sel.length; i++) {
                            importAudio(sel[i], ml, firstStartTime, m);
                        }
                    }
                    Main.main.addLayer(ml);
                    Main.map.repaint();
                }
            }
        });

        JMenuItem tagimage = new JMenuItem(tr("Import images"), ImageProvider.get("tagimages"));
        tagimage.putClientProperty("help", "Action/ImportImages");
        tagimage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(Main.pref.get("tagimages.lastdirectory"));
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fc.setMultiSelectionEnabled(true);
                fc.setAcceptAllFileFilterUsed(false);
                fc.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg");
                    }

                    @Override
                    public String getDescription() {
                        return tr("JPEG images (*.jpg)");
                    }
                });
                fc.showOpenDialog(Main.parent);
                File[] sel = fc.getSelectedFiles();
                if (sel == null || sel.length == 0)
                    return;
                LinkedList<File> files = new LinkedList<File>();
                addRecursiveFiles(files, sel);
                Main.pref.put("tagimages.lastdirectory", fc.getCurrentDirectory().getPath());
                GeoImageLayer.create(files, GpxLayer.this);
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
        });

        if (Main.applet)
            return new Component[] { new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)), new JSeparator(), color, line,
                new JMenuItem(new ConvertToDataLayerAction()), new JSeparator(),
                new JMenuItem(new RenameLayerAction(getAssociatedFile(), this)), new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this)) };
        return new Component[] { new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)), new JSeparator(),
                new JMenuItem(new LayerSaveAction(this)), new JMenuItem(new LayerSaveAsAction(this)), color, line,
                tagimage, importAudio, markersFromNamedTrackpoints, new JMenuItem(new ConvertToDataLayerAction()),
                new JMenuItem(new DownloadAlongTrackAction()), new JSeparator(),
                new JMenuItem(new RenameLayerAction(getAssociatedFile(), this)), new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this)) };
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

        if (data.tracks.size() > 0) {
            info.append("<table><thead align=\"center\"><tr><td colspan=\"5\">"
                    + trn("{0} track", "{0} tracks", data.tracks.size(), data.tracks.size())
                    + "</td></tr><tr><td>" + tr("Name") + "</td><td>"
                    + tr("Description") + "</td><td>" + tr("Timespan")
                    + "</td><td>" + tr("Length") + "</td><td>" + tr("URL")
                    + "</td></tr></thead>");

            for (GpxTrack trk : data.tracks) {
                WayPoint earliest = null, latest = null;

                info.append("<tr><td>");
                if (trk.attr.containsKey("name")) {
                    info.append(trk.attr.get("name"));
                }
                info.append("</td><td>");
                if (trk.attr.containsKey("desc")) {
                    info.append(" ").append(trk.attr.get("desc"));
                }
                info.append("</td><td>");

                for (Collection<WayPoint> seg : trk.trackSegs) {
                    for (WayPoint pnt : seg) {
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
                    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                    info.append(df.format(new Date((long) (earliest.time * 1000))) + " - "
                            + df.format(new Date((long) (latest.time * 1000))));
                    int diff = (int) (latest.time - earliest.time);
                    info.append(" (" + (diff / 3600) + ":" + ((diff % 3600) / 60) + ")");
                }

                info.append("</td><td>");
                info.append(new DecimalFormat("#0.00").format(trk.length() / 1000) + "km");
                info.append("</td><td>");
                if (trk.attr.containsKey("url")) {
                    info.append(trk.attr.get("url"));
                }
                info.append("</td></tr>");
            }

            info.append("</table><br><br>");

        }

        info.append(tr("Length: ") + new DecimalFormat("#0.00").format(data.length() / 1000) + "km");
        info.append("<br>");

        info.append(trn("{0} route, ", "{0} routes, ", data.routes.size(), data.routes.size())).append(
                trn("{0} waypoint", "{0} waypoints", data.waypoints.size(), data.waypoints.size())).append("<br>");

        return info.append("</html>").toString();
    }

    @Override
    public boolean isMergable(Layer other) {
        return other instanceof GpxLayer;
    }

    @Override
    public void mergeFrom(Layer from) {
        data.mergeFrom(((GpxLayer) from).data);
        computeCacheInSync = false;
    }

    private static Color[] colors = new Color[256];
    static {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.getHSBColor(i / 300.0f, 1, 1);
        }
    }

    // lookup array to draw arrows without doing any math
    private static int ll0 = 9;
    private static int sl4 = 5;
    private static int sl9 = 3;
    private static int[][] dir = { { +sl4, +ll0, +ll0, +sl4 }, { -sl9, +ll0, +sl9, +ll0 }, { -ll0, +sl4, -sl4, +ll0 },
        { -ll0, -sl9, -ll0, +sl9 }, { -sl4, -ll0, -ll0, -sl4 }, { +sl9, -ll0, -sl9, -ll0 },
        { +ll0, -sl4, +sl4, -ll0 }, { +ll0, +sl9, +ll0, -sl9 }, { +sl4, +ll0, +ll0, +sl4 },
        { -sl9, +ll0, +sl9, +ll0 }, { -ll0, +sl4, -sl4, +ll0 }, { -ll0, -sl9, -ll0, +sl9 } };

    // the different color modes
    enum colorModes {
        none, velocity, dilution
    }

    @Override
    public void paint(Graphics g, MapView mv) {

        /****************************************************************
         ********** STEP 1 - GET CONFIG VALUES **************************
         ****************************************************************/
        // Long startTime = System.currentTimeMillis();
        Color neutralColor = getColor(getName());
        // also draw lines between points belonging to different segments
        boolean forceLines = Main.pref.getBoolean("draw.rawgps.lines.force");
        // draw direction arrows on the lines
        boolean direction = Main.pref.getBoolean("draw.rawgps.direction");
        // don't draw lines if longer than x meters
        int lineWidth = Main.pref.getInteger("draw.rawgps.linewidth",0);

        int maxLineLength;
        if (this.isLocalFile) {
            maxLineLength = Main.pref.getInteger("draw.rawgps.max-line-length.local", -1);
        } else {
            maxLineLength = Main.pref.getInteger("draw.rawgps.max-line-length", 200);
        }
        // draw line between points, global setting
        boolean lines = (Main.pref.getBoolean("draw.rawgps.lines", true) || (Main.pref
                .getBoolean("draw.rawgps.lines.localfiles") && this.isLocalFile));
        String linesKey = "draw.rawgps.lines.layer " + getName();
        // draw lines, per-layer setting
        if (Main.pref.hasKey(linesKey)) {
            lines = Main.pref.getBoolean(linesKey);
        }
        // paint large dots for points
        boolean large = Main.pref.getBoolean("draw.rawgps.large");
        boolean hdopcircle = Main.pref.getBoolean("draw.rawgps.hdopcircle", true);
        // color the lines
        colorModes colored = colorModes.none;
        try {
            colored = colorModes.values()[Main.pref.getInteger("draw.rawgps.colors", 0)];
        } catch (Exception e) {
        }
        // paint direction arrow with alternate math. may be faster
        boolean alternatedirection = Main.pref.getBoolean("draw.rawgps.alternatedirection");
        // don't draw arrows nearer to each other than this
        int delta = Main.pref.getInteger("draw.rawgps.min-arrow-distance", 0);
        // allows to tweak line coloring for different speed levels.
        int colorTracksTune = Main.pref.getInteger("draw.rawgps.colorTracksTune", 45);

        if(lineWidth != 0)
        {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setStroke(new BasicStroke(lineWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        }

        /****************************************************************
         ********** STEP 2a - CHECK CACHE VALIDITY **********************
         ****************************************************************/
        if (computeCacheInSync
                && ((computeCacheMaxLineLengthUsed != maxLineLength) || (!neutralColor.equals(computeCacheColorUsed))
                        || (computeCacheColored != colored) || (computeCacheColorTracksTune != colorTracksTune))) {
            // System.out.println("(re-)computing gpx line styles, reason: CCIS=" +
            // computeCacheInSync + " CCMLLU=" + (computeCacheMaxLineLengthUsed != maxLineLength) +
            // " CCCU=" + (!neutralColor.equals(computeCacheColorUsed)) + " CCC=" +
            // (computeCacheColored != colored));
            computeCacheMaxLineLengthUsed = maxLineLength;
            computeCacheInSync = false;
            computeCacheColorUsed = neutralColor;
            computeCacheColored = colored;
            computeCacheColorTracksTune = colorTracksTune;
        }

        /****************************************************************
         ********** STEP 2b - RE-COMPUTE CACHE DATA *********************
         ****************************************************************/
        if (!computeCacheInSync) { // don't compute if the cache is good
            WayPoint oldWp = null;
            for (GpxTrack trk : data.tracks) {
                if (!forceLines) { // don't draw lines between segments, unless forced to
                    oldWp = null;
                }
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint trkPnt : segment) {
                        LatLon c = trkPnt.getCoor();
                        if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                            continue;
                        }
                        trkPnt.customColoring = neutralColor;
                        if (oldWp != null) {
                            double dist = c.greatCircleDistance(oldWp.getCoor());

                            switch (colored) {
                                case velocity:
                                    double dtime = trkPnt.time - oldWp.time;
                                    double vel = dist / dtime;
                                    double velColor = vel / colorTracksTune * 255;
                                    // Bad case first
                                    if (dtime <= 0 || vel < 0 || velColor > 255) {
                                        trkPnt.customColoring = colors[255];
                                    } else {
                                        trkPnt.customColoring = colors[(int) (velColor)];
                                    }
                                    break;

                                case dilution:
                                    if (trkPnt.attr.get("hdop") != null) {
                                        float hdop = ((Float) trkPnt.attr.get("hdop")).floatValue();
                                        if (hdop < 0) {
                                            hdop = 0;
                                        }
                                        int hdoplvl = Math.round(hdop * Main.pref.getInteger("hdop.factor", 25));
                                        // High hdop is bad, but high values in colors are green.
                                        // Therefore inverse the logic
                                        int hdopcolor = 255 - (hdoplvl > 255 ? 255 : hdoplvl);
                                        trkPnt.customColoring = colors[hdopcolor];
                                    }
                                    break;
                            }

                            if (maxLineLength == -1 || dist <= maxLineLength) {
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

        /****************************************************************
         ********** STEP 3a - DRAW LINES ********************************
         ****************************************************************/
        if (lines) {
            Point old = null;
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint trkPnt : segment) {
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
                } // end for segment
            } // end for trk
        } // end if lines

        /****************************************************************
         ********** STEP 3b - DRAW NICE ARROWS **************************
         ****************************************************************/
        if (lines && direction && !alternatedirection) {
            Point old = null;
            Point oldA = null; // last arrow painted
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint trkPnt : segment) {
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
                } // end for segment
            } // end for trk
        } // end if lines

        /****************************************************************
         ********** STEP 3c - DRAW FAST ARROWS **************************
         ****************************************************************/
        if (lines && direction && alternatedirection) {
            Point old = null;
            Point oldA = null; // last arrow painted
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint trkPnt : segment) {
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
                } // end for segment
            } // end for trk
        } // end if lines


        /****************************************************************
         ********** STEP 3d - DRAW LARGE POINTS AND HDOP CIRCLE *********
         ****************************************************************/
        if (large || hdopcircle) {
            g.setColor(neutralColor);
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint trkPnt : segment) {
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
                            g.fillRect(screen.x-1, screen.y-1, 3, 3);
                        }
                    } // end for trkpnt
                } // end for segment
            } // end for trk
        } // end if large || hdopcircle

        /****************************************************************
         ********** STEP 3e - DRAW SMALL POINTS FOR LINES ***************
         ****************************************************************/
        if (!large && lines) {
            g.setColor(neutralColor);
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint trkPnt : segment) {
                        LatLon c = trkPnt.getCoor();
                        if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                            continue;
                        }
                        if (!trkPnt.drawLine) {
                            Point screen = mv.getPoint(trkPnt.getEastNorth());
                            g.drawRect(screen.x, screen.y, 0, 0);
                        }
                    } // end for trkpnt
                } // end for segment
            } // end for trk
        } // end if large

        /****************************************************************
         ********** STEP 3f - DRAW SMALL POINTS INSTEAD OF LINES ********
         ****************************************************************/
        if (!large && !lines) {
            g.setColor(neutralColor);
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint trkPnt : segment) {
                        LatLon c = trkPnt.getCoor();
                        if (Double.isNaN(c.lat()) || Double.isNaN(c.lon())) {
                            continue;
                        }
                        Point screen = mv.getPoint(trkPnt.getEastNorth());
                        g.setColor(trkPnt.customColoring);
                        g.drawRect(screen.x, screen.y, 0, 0);
                    } // end for trkpnt
                } // end for segment
            } // end for trk
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

        public void actionPerformed(ActionEvent e) {
            JPanel msg = new JPanel(new GridBagLayout());
            msg
            .add(
                    new JLabel(
                            tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>If you want to upload traces, look here:")),
                            GBC.eol());
            msg.add(new UrlLabel(tr("http://www.openstreetmap.org/traces")), GBC.eop());
            if (!ConditionalOptionPaneUtil.showConfirmationDialog("convert_to_data", Main.parent, msg, tr("Warning"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION))
                return;
            DataSet ds = new DataSet();
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    List<Node> nodes = new ArrayList<Node>();
                    for (WayPoint p : segment) {
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
            msg.add(buffer, GBC.eol());
            msg.add(new JLabel(tr("Maximum area per request:")), GBC.eol());
            s = new String[area.length];
            for (int i = 0; i < area.length; ++i) {
                s[i] = tr("{0} sq km", area[i]);
            }
            JList maxRect = new JList(s);
            msg.add(maxRect, GBC.eol());

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

            /*
             * Find the average latitude for the data we're contemplating, so we can know how many
             * metres per degree of longitude we have.
             */
            double latsum = 0;
            int latcnt = 0;

            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint p : segment) {
                        latsum += p.getCoor().lat();
                        latcnt++;
                    }
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
            for (GpxTrack trk : data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint p : segment) {
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

            msg
            .add(
                    new JLabel(
                            tr(
                                    "<html>This action will require {0} individual<br>download requests. Do you wish<br>to continue?</html>",
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
        String uri = "file:".concat(wavFile.getAbsolutePath());
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
                if (track.trackSegs == null) {
                    continue;
                }
                for (Collection<WayPoint> seg : track.trackSegs) {
                    for (WayPoint w : seg) {
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
                if (track.trackSegs == null) {
                    continue;
                }
                for (Collection<WayPoint> seg : track.trackSegs) {
                    for (WayPoint w : seg) {
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
                if (track.trackSegs == null) {
                    continue;
                }
                for (Collection<WayPoint> seg : track.trackSegs) {
                    for (WayPoint w : seg) {
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
                if (track.trackSegs == null) {
                    continue;
                }
                for (Collection<WayPoint> seg : track.trackSegs) {
                    for (WayPoint w : seg) {
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
            String name;
            if (w.attr.containsKey("name")) {
                name = w.getString("name");
            } else if (w.attr.containsKey("desc")) {
                name = w.getString("desc");
            } else {
                name = AudioMarker.inventName(offset);
            }
            AudioMarker am = AudioMarker.create(w.getCoor(), name, uri, ml, w.time, offset);
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
            if (track.trackSegs == null) {
                continue;
            }
            for (Collection<WayPoint> seg : track.trackSegs) {
                WayPoint R = null;
                for (WayPoint S : seg) {
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
        WayPoint best = new WayPoint(Main.proj.eastNorth2latlon(bestEN));
        best.time = bestTime;
        return best;
    }
}
