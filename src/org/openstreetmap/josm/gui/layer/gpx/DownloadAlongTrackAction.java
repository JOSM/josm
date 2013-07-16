// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAlongAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

/**
 * Action that issues a series of download requests to the API, following the GPX track.
 *
 * @author fred
 */
public class DownloadAlongTrackAction extends DownloadAlongAction {

    static final int NEAR_TRACK = 0;
    static final int NEAR_WAYPOINTS = 1;
    static final int NEAR_BOTH = 2;

    private static final String PREF_DOWNLOAD_ALONG_TRACK_OSM = "downloadAlongTrack.download.osm";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_GPS = "downloadAlongTrack.download.gps";

    private static final String PREF_DOWNLOAD_ALONG_TRACK_DISTANCE = "downloadAlongTrack.distance";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_AREA = "downloadAlongTrack.area";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_NEAR = "downloadAlongTrack.near";

    private final GpxData data;

    /**
     * Constructs a new {@code DownloadAlongTrackAction}
     * @param data The GPX data used to download along
     */
    public DownloadAlongTrackAction(GpxData data) {
        super(tr("Download from OSM along this track"), "downloadalongtrack", null, null, true);
        this.data = data;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final DownloadAlongPanel panel = new DownloadAlongPanel(
                PREF_DOWNLOAD_ALONG_TRACK_OSM, PREF_DOWNLOAD_ALONG_TRACK_GPS,
                PREF_DOWNLOAD_ALONG_TRACK_DISTANCE, PREF_DOWNLOAD_ALONG_TRACK_AREA, PREF_DOWNLOAD_ALONG_TRACK_NEAR);

        if (0 != panel.showInDownloadDialog(tr("Download from OSM along this track"), HelpUtil.ht("/Action/DownloadAlongTrack"))) {
            return;
        }

        final int near = panel.getNear();

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
        final double buffer_dist = panel.getDistance();
        final double max_area = panel.getArea() / 10000.0 / scale;
        final double buffer_y = buffer_dist / 100000.0;
        final double buffer_x = buffer_y / scale;
        final int totalTicks = latcnt;
        // guess if a progress bar might be useful.
        final boolean displayProgress = totalTicks > 2000 && buffer_y < 0.01;

        class CalculateDownloadArea extends PleaseWaitRunnable {

            private Area a = new Area();
            private boolean cancel = false;
            private int ticks = 0;
            private Rectangle2D r = new Rectangle2D.Double();

            public CalculateDownloadArea() {
                super(tr("Calculating Download Area"), displayProgress ? null : NullProgressMonitor.INSTANCE, false);
            }

            @Override
            protected void cancel() {
                cancel = true;
            }

            @Override
            protected void finish() {
            }

            @Override
            protected void afterFinish() {
                if (cancel) {
                    return;
                }
                confirmAndDownloadAreas(a, max_area, panel.isDownloadOsmData(), panel.isDownloadGpxData(),
                        tr("Download from OSM along this track"), progressMonitor);
            }

            /**
             * increase tick count by one, report progress every 100 ticks
             */
            private void tick() {
                ticks++;
                if (ticks % 100 == 0) {
                    progressMonitor.worked(100);
                }
            }

            /**
             * calculate area for single, given way point and return new LatLon if the
             * way point has been used to modify the area.
             */
            private LatLon calcAreaForWayPoint(WayPoint p, LatLon previous) {
                tick();
                LatLon c = p.getCoor();
                if (previous == null || c.greatCircleDistance(previous) > buffer_dist) {
                    // we add a buffer around the point.
                    r.setRect(c.lon() - buffer_x, c.lat() - buffer_y, 2 * buffer_x, 2 * buffer_y);
                    a.add(new Area(r));
                    return c;
                }
                return previous;
            }

            @Override
            protected void realRun() {
                progressMonitor.setTicksCount(totalTicks);
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
                                if (cancel) {
                                    return;
                                }
                                previous = calcAreaForWayPoint(p, previous);
                            }
                        }
                    }
                }
                if (near == NEAR_WAYPOINTS || near == NEAR_BOTH) {
                    for (WayPoint p : data.waypoints) {
                        if (cancel) {
                            return;
                        }
                        previous = calcAreaForWayPoint(p, previous);
                    }
                }
            }
        }
        Main.worker.submit(new CalculateDownloadArea());
    }
}
