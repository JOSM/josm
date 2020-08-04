// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.geom.Path2D;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.actions.DownloadAlongAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Utils;

/**
 * Action that issues a series of download requests to the API, following the GPX track.
 *
 * @author fred
 * @since 5715
 */
public class DownloadAlongTrackAction extends DownloadAlongAction implements Layer.LayerAction, Layer.MultiLayerAction {

    private static final int NEAR_TRACK = 0;
    private static final int NEAR_WAYPOINTS = 1;
    private static final int NEAR_BOTH = 2;

    private static final String PREF_DOWNLOAD_ALONG_TRACK_OSM = "downloadAlongTrack.download.osm";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_GPS = "downloadAlongTrack.download.gps";

    private static final String PREF_DOWNLOAD_ALONG_TRACK_DISTANCE = "downloadAlongTrack.distance";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_AREA = "downloadAlongTrack.area";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_NEAR = "downloadAlongTrack.near";

    private final transient Collection<GpxData> data;

    /**
     * Constructs a new {@code DownloadAlongTrackAction}
     * @param data The GPX data used to download along
     */
    public DownloadAlongTrackAction(Collection<GpxData> data) {
        super(tr("Download from OSM along this track"), "downloadalongtrack", null, null, false);
        this.data = data;
    }

    @Override
    protected PleaseWaitRunnable createTask() {
        final DownloadAlongPanel panel = new DownloadAlongPanel(
                PREF_DOWNLOAD_ALONG_TRACK_OSM, PREF_DOWNLOAD_ALONG_TRACK_GPS,
                PREF_DOWNLOAD_ALONG_TRACK_DISTANCE, PREF_DOWNLOAD_ALONG_TRACK_AREA, PREF_DOWNLOAD_ALONG_TRACK_NEAR);

        int ret = panel.showInDownloadDialog(tr("Download from OSM along this track"), HelpUtil.ht("/Action/DownloadAlongTrack"));
        if (0 != ret && 1 != ret) {
            return null;
        }

        final int near = panel.getNear();

        // Convert the GPX data into a Path2D.
        Path2D gpxPath = new Path2D.Double();
        if (near == NEAR_TRACK || near == NEAR_BOTH) {
            data.stream().flatMap(GpxData::getTrackSegmentsStream).forEach(segment -> {
                boolean first = true;
                for (WayPoint p : segment.getWayPoints()) {
                    if (first) {
                        gpxPath.moveTo(p.lon(), p.lat());
                        first = false;
                    } else {
                        gpxPath.lineTo(p.lon(), p.lat());
                    }
                }
            });
        }
        if (near == NEAR_WAYPOINTS || near == NEAR_BOTH) {
            data.stream().flatMap(d -> d.getWaypoints().stream()).forEach(p -> {
                gpxPath.moveTo(p.lon(), p.lat());
                gpxPath.closePath();
            });
        }
        return createCalcTask(gpxPath, panel, tr("Download from OSM along this track"), 1 == ret);
    }

    @Override
    public Component createMenuComponent() {
        return new JMenuItem(this);
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return !Utils.filteredCollection(layers, GpxLayer.class).isEmpty();
    }

    @Override
    public Action getMultiLayerAction(List<Layer> layers) {
        final List<GpxData> gpxData = Utils.filteredCollection(layers, GpxLayer.class)
                .stream().map(layer -> layer.data)
                .collect(Collectors.toList());
        return new DownloadAlongTrackAction(gpxData);
    }
}
