// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.ExpertToggleAction.ExpertModeChangeListener;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeListener;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.layer.gpx.ChooseTrackVisibilityAction;
import org.openstreetmap.josm.gui.layer.gpx.ConvertFromGpxLayerAction;
import org.openstreetmap.josm.gui.layer.gpx.CustomizeDrawingAction;
import org.openstreetmap.josm.gui.layer.gpx.DownloadAlongTrackAction;
import org.openstreetmap.josm.gui.layer.gpx.DownloadWmsAlongTrackAction;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper;
import org.openstreetmap.josm.gui.layer.gpx.ImportAudioAction;
import org.openstreetmap.josm.gui.layer.gpx.ImportImagesAction;
import org.openstreetmap.josm.gui.layer.gpx.MarkersFromNamedPointsAction;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * A layer that displays data from a Gpx file / the OSM gpx downloads.
 */
public class GpxLayer extends Layer implements ExpertModeChangeListener {

    /** GPX data */
    public GpxData data;
    private final boolean isLocalFile;
    private boolean isExpertMode;
    /**
     * used by {@link ChooseTrackVisibilityAction} to determine which tracks to show/hide
     *
     * Call {@link #invalidate()} after each change!
     *
     * TODO: Make it private, make it respond to track changes.
     */
    public boolean[] trackVisibility = new boolean[0];
    /**
     * Added as field to be kept as reference.
     */
    private final GpxDataChangeListener dataChangeListener = e -> this.invalidate();

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
     * Constructs a new {@code GpxLayer} with a given name, that can be attached to a local file.
     * @param d GPX data
     * @param name layer name
     * @param isLocal whether data is attached to a local file
     */
    public GpxLayer(GpxData d, String name, boolean isLocal) {
        super(d.getString(GpxConstants.META_NAME));
        data = d;
        data.addWeakChangeListener(dataChangeListener);
        trackVisibility = new boolean[data.getTracks().size()];
        Arrays.fill(trackVisibility, true);
        setName(name);
        isLocalFile = isLocal;
        ExpertToggleAction.addExpertModeChangeListener(this, true);
    }

    @Override
    protected NamedColorProperty getBaseColorProperty() {
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
        StringBuilder info = new StringBuilder(128)
                .append("<html><head><style>td { padding: 4px 16px; }</style></head><body>");

        if (data.attr.containsKey("name")) {
            info.append(tr("Name: {0}", data.get(GpxConstants.META_NAME))).append("<br>");
        }

        if (data.attr.containsKey("desc")) {
            info.append(tr("Description: {0}", data.get(GpxConstants.META_DESC))).append("<br>");
        }

        if (!data.getTracks().isEmpty()) {
            info.append("<table><thead align='center'><tr><td colspan='5'>")
                .append(trn("{0} track, {1} track segments", "{0} tracks, {1} track segments",
                        data.getTrackCount(), data.getTrackCount(),
                        data.getTrackSegsCount(), data.getTrackSegsCount()))
                .append("</td></tr><tr align='center'><td>").append(tr("Name"))
                .append("</td><td>").append(tr("Description"))
                .append("</td><td>").append(tr("Timespan"))
                .append("</td><td>").append(tr("Length"))
                .append("</td><td>").append(tr("Number of<br/>Segments"))
                .append("</td><td>").append(tr("URL"))
                .append("</td></tr></thead>");

            for (GpxTrack trk : data.getTracks()) {
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
                info.append(trk.getSegments().size());
                info.append("</td><td>");
                if (trk.getAttributes().containsKey("url")) {
                    info.append(trk.get("url"));
                }
                info.append("</td></tr>");
            }
            info.append("</table><br><br>");
        }

        info.append(tr("Length: {0}", SystemOfMeasurement.getSystemOfMeasurement().getDistText(data.length()))).append("<br>")
            .append(trn("{0} route, ", "{0} routes, ", data.getRoutes().size(), data.getRoutes().size()))
            .append(trn("{0} waypoint", "{0} waypoints", data.getWaypoints().size(), data.getWaypoints().size()))
            .append("<br></body></html>");

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
        List<Action> entries = new ArrayList<>(Arrays.asList(
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
                new ConvertFromGpxLayerAction(this),
                new DownloadAlongTrackAction(data),
                new DownloadWmsAlongTrackAction(data),
                SeparatorLayerAction.INSTANCE,
                new ChooseTrackVisibilityAction(this),
                new RenameLayerAction(getAssociatedFile(), this)));

        List<Action> expert = Arrays.asList(
                new CombineTracksToSegmentedTrackAction(this),
                new SplitTrackSegementsToTracksAction(this),
                new SplitTracksToLayersAction(this));

        if (isExpertMode && expert.stream().anyMatch(Action::isEnabled)) {
            entries.add(SeparatorLayerAction.INSTANCE);
            expert.stream().filter(Action::isEnabled).forEach(entries::add);
        }

        entries.add(SeparatorLayerAction.INSTANCE);
        entries.add(new LayerListPopup.InfoAction(this));
        return entries.toArray(new Action[0]);
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

        info.append(trn("{0} track", "{0} tracks", data.getTrackCount(), data.getTrackCount()))
            .append(trn(" ({0} segment)", " ({0} segments)", data.getTrackSegsCount(), data.getTrackSegsCount()))
            .append(", ")
            .append(trn("{0} route, ", "{0} routes, ", data.getRoutes().size(), data.getRoutes().size()))
            .append(trn("{0} waypoint", "{0} waypoints", data.getWaypoints().size(), data.getWaypoints().size())).append("<br>")
            .append(tr("Length: {0}", SystemOfMeasurement.getSystemOfMeasurement().getDistText(data.length())))
            .append("<br></html>");
        return info.toString();
    }

    @Override
    public boolean isMergable(Layer other) {
        return other instanceof GpxLayer;
    }

    /**
     * Shows/hides all tracks of a given date range by setting them to visible/invisible.
     * @param fromDate The min date
     * @param toDate The max date
     * @param showWithoutDate Include tracks that don't have any date set..
     */
    public void filterTracksByDate(Date fromDate, Date toDate, boolean showWithoutDate) {
        int i = 0;
        long from = fromDate.getTime();
        long to = toDate.getTime();
        for (GpxTrack trk : data.getTracks()) {
            Date[] t = GpxData.getMinMaxTimeForTrack(trk);

            if (t == null) continue;
            long tm = t[1].getTime();
            trackVisibility[i] = (tm == 0 && showWithoutDate) || (from <= tm && tm <= to);
            i++;
        }
        invalidate();
    }

    @Override
    public void mergeFrom(Layer from) {
        if (!(from instanceof GpxLayer))
            throw new IllegalArgumentException("not a GpxLayer: " + from);
        mergeFrom((GpxLayer) from, false, false);
    }

    /**
     * Merges the given GpxLayer into this layer and can remove timewise overlapping parts of the given track
     * @param from The GpxLayer that gets merged into this one
     * @param cutOverlapping whether overlapping parts of the given track should be removed
     * @param connect whether the tracks should be connected on cuts
     * @since 14338
     */
    public void mergeFrom(GpxLayer from, boolean cutOverlapping, boolean connect) {
        data.mergeFrom(from.data, cutOverlapping, connect);
        invalidate();
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
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        // unused - we use a painter so this is not called.
    }

    @Override
    protected LayerPainter createMapViewPainter(MapViewEvent event) {
        return new GpxDrawHelper(this);
    }

    /**
     * Action to merge tracks into a single segmented track
     *
     * @since 13210
     */
    public static class CombineTracksToSegmentedTrackAction extends AbstractAction {
        private final transient GpxLayer layer;

        /**
         * Create a new CombineTracksToSegmentedTrackAction
         * @param layer The layer with the data to work on.
         */
        public CombineTracksToSegmentedTrackAction(GpxLayer layer) {
            // FIXME: icon missing, create a new icon for this action
            //new ImageProvider(..."gpx_tracks_to_segmented_track").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Collect segments of all tracks and combine in a single track."));
            putValue(NAME, tr("Combine tracks of this layer"));
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            layer.data.combineTracksToSegmentedTrack();
            layer.invalidate();
        }

        @Override
        public boolean isEnabled() {
            return layer.data.getTrackCount() > 1;
        }
    }

    /**
     * Action to split track segments into a multiple tracks with one segment each
     *
     * @since 13210
     */
    public static class SplitTrackSegementsToTracksAction extends AbstractAction {
        private final transient GpxLayer layer;

        /**
         * Create a new SplitTrackSegementsToTracksAction
         * @param layer The layer with the data to work on.
         */
        public SplitTrackSegementsToTracksAction(GpxLayer layer) {
            // FIXME: icon missing, create a new icon for this action
            //new ImageProvider(..."gpx_segmented_track_to_tracks").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Split multiple track segments of one track into multiple tracks."));
            putValue(NAME, tr("Split track segments to tracks"));
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            layer.data.splitTrackSegmentsToTracks();
            layer.invalidate();
        }

        @Override
        public boolean isEnabled() {
            return layer.data.getTrackSegsCount() > layer.data.getTrackCount();
        }
    }

    /**
     * Action to split tracks of one gpx layer into multiple gpx layers,
     * the result is one GPX track per gpx layer.
     *
     * @since 13210
     */
    public static class SplitTracksToLayersAction extends AbstractAction {
        private final transient GpxLayer layer;

        /**
         * Create a new SplitTrackSegementsToTracksAction
         * @param layer The layer with the data to work on.
         */
        public SplitTracksToLayersAction(GpxLayer layer) {
            // FIXME: icon missing, create a new icon for this action
            //new ImageProvider(..."gpx_split_tracks_to_layers").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Split the tracks of this layer to one new layer each."));
            putValue(NAME, tr("Split tracks to new layers"));
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            layer.data.splitTracksToLayers();
            // layer is not modified by this action
        }

        @Override
        public boolean isEnabled() {
            return layer.data.getTrackCount() > 1;
        }
    }

    @Override
    public void expertChanged(boolean isExpert) {
        this.isExpertMode = isExpert;
    }
}
