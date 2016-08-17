// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.Graphics2D;
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
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.gpx.ChooseTrackVisibilityAction;
import org.openstreetmap.josm.gui.layer.gpx.ConvertToDataLayerAction;
import org.openstreetmap.josm.gui.layer.gpx.CustomizeDrawingAction;
import org.openstreetmap.josm.gui.layer.gpx.DownloadAlongTrackAction;
import org.openstreetmap.josm.gui.layer.gpx.DownloadWmsAlongTrackAction;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper;
import org.openstreetmap.josm.gui.layer.gpx.ImportAudioAction;
import org.openstreetmap.josm.gui.layer.gpx.ImportImagesAction;
import org.openstreetmap.josm.gui.layer.gpx.MarkersFromNamedPointsAction;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.date.DateUtils;

public class GpxLayer extends Layer {

    /** GPX data */
    public GpxData data;
    private final boolean isLocalFile;
    // used by ChooseTrackVisibilityAction to determine which tracks to show/hide
    public boolean[] trackVisibility = new boolean[0];

    private final List<GpxTrack> lastTracks = new ArrayList<>(); // List of tracks at last paint
    private int lastUpdateCount;

    private final GpxDrawHelper drawHelper;

    /**
     * Constructs a new {@code GpxLayer} without name.
     * @param d GPX data
     */
    public GpxLayer(GpxData d) {
        this(d, null, false);
    }

    /**
     * Constructs a new {@code GpxLayer} with a given name.
     * @param d GPX data
     * @param name layer name
     */
    public GpxLayer(GpxData d, String name) {
        this(d, name, false);
    }

    /**
     * Constructs a new {@code GpxLayer} with a given name, thah can be attached to a local file.
     * @param d GPX data
     * @param name layer name
     * @param isLocal whether data is attached to a local file
     */
    public GpxLayer(GpxData d, String name, boolean isLocal) {
        super(d.getString(GpxConstants.META_NAME));
        data = d;
        drawHelper = new GpxDrawHelper(data, getColorProperty());
        SystemOfMeasurement.addSoMChangeListener(drawHelper);
        ensureTrackVisibilityLength();
        setName(name);
        isLocalFile = isLocal;
    }

    @Override
    protected ColorProperty getBaseColorProperty() {
        return GpxDrawHelper.DEFAULT_COLOR;
    }

    /**
     * Returns a human readable string that shows the timespan of the given track
     * @param trk The GPX track for which timespan is displayed
     * @return The timespan as a string
     */
    public static String getTimespanForTrack(GpxTrack trk) {
        Date[] bounds = GpxData.getMinMaxTimeForTrack(trk);
        String ts = "";
        if (bounds != null) {
            DateFormat df = DateUtils.getDateFormat(DateFormat.SHORT);
            String earliestDate = df.format(bounds[0]);
            String latestDate = df.format(bounds[1]);

            if (earliestDate.equals(latestDate)) {
                DateFormat tf = DateUtils.getTimeFormat(DateFormat.SHORT);
                ts += earliestDate + ' ';
                ts += tf.format(bounds[0]) + " - " + tf.format(bounds[1]);
            } else {
                DateFormat dtf = DateUtils.getDateTimeFormat(DateFormat.SHORT, DateFormat.MEDIUM);
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
        StringBuilder info = new StringBuilder(48).append("<html>");

        if (data.attr.containsKey("name")) {
            info.append(tr("Name: {0}", data.get(GpxConstants.META_NAME))).append("<br>");
        }

        if (data.attr.containsKey("desc")) {
            info.append(tr("Description: {0}", data.get(GpxConstants.META_DESC))).append("<br>");
        }

        if (!data.tracks.isEmpty()) {
            info.append("<table><thead align='center'><tr><td colspan='5'>")
                .append(trn("{0} track", "{0} tracks", data.tracks.size(), data.tracks.size()))
                .append("</td></tr><tr align='center'><td>").append(tr("Name")).append("</td><td>")
                .append(tr("Description")).append("</td><td>").append(tr("Timespan"))
                .append("</td><td>").append(tr("Length")).append("</td><td>").append(tr("URL"))
                .append("</td></tr></thead>");

            for (GpxTrack trk : data.tracks) {
                info.append("<tr><td>");
                if (trk.getAttributes().containsKey(GpxConstants.GPX_NAME)) {
                    info.append(trk.get(GpxConstants.GPX_NAME));
                }
                info.append("</td><td>");
                if (trk.getAttributes().containsKey(GpxConstants.GPX_DESC)) {
                    info.append(' ').append(trk.get(GpxConstants.GPX_DESC));
                }
                info.append("</td><td>");
                info.append(getTimespanForTrack(trk));
                info.append("</td><td>");
                info.append(SystemOfMeasurement.getSystemOfMeasurement().getDistText(trk.length()));
                info.append("</td><td>");
                if (trk.getAttributes().containsKey("url")) {
                    info.append(trk.get("url"));
                }
                info.append("</td></tr>");
            }
            info.append("</table><br><br>");
        }

        info.append(tr("Length: {0}", SystemOfMeasurement.getSystemOfMeasurement().getDistText(data.length()))).append("<br>")
            .append(trn("{0} route, ", "{0} routes, ", data.routes.size(), data.routes.size()))
            .append(trn("{0} waypoint", "{0} waypoints", data.waypoints.size(), data.waypoints.size())).append("<br></html>");

        final JScrollPane sp = new JScrollPane(new HtmlPanel(info.toString()));
        sp.setPreferredSize(new Dimension(sp.getPreferredSize().width+20, 370));
        SwingUtilities.invokeLater(() -> sp.getVerticalScrollBar().setValue(0));
        return sp;
    }

    @Override
    public boolean isInfoResizable() {
        return true;
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                LayerListDialog.getInstance().createMergeLayerAction(this),
                SeparatorLayerAction.INSTANCE,
                new LayerSaveAction(this),
                new LayerSaveAsAction(this),
                new CustomizeColor(this),
                new CustomizeDrawingAction(this),
                new ImportImagesAction(this),
                new ImportAudioAction(this),
                new MarkersFromNamedPointsAction(this),
                new ConvertToDataLayerAction.FromGpxLayer(this),
                new DownloadAlongTrackAction(data),
                new DownloadWmsAlongTrackAction(data),
                SeparatorLayerAction.INSTANCE,
                new ChooseTrackVisibilityAction(this),
                new RenameLayerAction(getAssociatedFile(), this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this) };
    }

    /**
     * Determines if data is attached to a local file.
     * @return {@code true} if data is attached to a local file, {@code false} otherwise
     */
    public boolean isLocalFile() {
        return isLocalFile;
    }

    @Override
    public String getToolTipText() {
        StringBuilder info = new StringBuilder(48).append("<html>");

        if (data.attr.containsKey(GpxConstants.META_NAME)) {
            info.append(tr("Name: {0}", data.get(GpxConstants.META_NAME))).append("<br>");
        }

        if (data.attr.containsKey(GpxConstants.META_DESC)) {
            info.append(tr("Description: {0}", data.get(GpxConstants.META_DESC))).append("<br>");
        }

        info.append(trn("{0} track, ", "{0} tracks, ", data.tracks.size(), data.tracks.size()))
            .append(trn("{0} route, ", "{0} routes, ", data.routes.size(), data.routes.size()))
            .append(trn("{0} waypoint", "{0} waypoints", data.waypoints.size(), data.waypoints.size())).append("<br>")
            .append(tr("Length: {0}", SystemOfMeasurement.getSystemOfMeasurement().getDistText(data.length())))
            .append("<br></html>");
        return info.toString();
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
            Date[] t = GpxData.getMinMaxTimeForTrack(trk);

            if (t == null) continue;
            long tm = t[1].getTime();
            trackVisibility[i] = (tm == 0 && showWithoutDate) || (from <= tm && tm <= to);
            i++;
        }
    }

    @Override
    public void mergeFrom(Layer from) {
        data.mergeFrom(((GpxLayer) from).data);
        drawHelper.dataChanged();
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        lastUpdateCount = sumUpdateCount();
        lastTracks.clear();
        lastTracks.addAll(data.tracks);

        List<WayPoint> visibleSegments = listVisibleSegments(box);
        if (!visibleSegments.isEmpty()) {
            drawHelper.readPreferences(getName());
            drawHelper.drawAll(g, mv, visibleSegments);
            if (Main.getLayerManager().getActiveLayer() == this) {
                drawHelper.drawColorBar(g, mv);
            }
        }
    }

    private List<WayPoint> listVisibleSegments(Bounds box) {
        WayPoint last = null;
        LinkedList<WayPoint> visibleSegments = new LinkedList<>();

        ensureTrackVisibilityLength();
        for (Collection<WayPoint> segment : data.getLinesIterable(trackVisibility)) {

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
    private void ensureTrackVisibilityLength() {
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
        data.resetEastNorthCache();
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
        return SaveActionBase.createAndOpenSaveFileChooser(tr("Save GPX file"), GpxImporter.getFileFilter());
    }

    @Override
    public LayerPositionStrategy getDefaultLayerPosition() {
        return LayerPositionStrategy.AFTER_LAST_DATA_LAYER;
    }

    @Override
    public void destroy() {
        SystemOfMeasurement.removeSoMChangeListener(drawHelper);
    }
}
