// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import org.openstreetmap.josm.actions.DownloadAlongAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.tools.Utils;

/**
 * Action that issues a series of download requests to the API, following the GPX track.
 *
 * @author fred
 * @since 5715
 */
public class DownloadAlongTrackAction extends DownloadAlongAction {

    private static final int NEAR_TRACK = 0;
    private static final int NEAR_WAYPOINTS = 1;
    private static final int NEAR_BOTH = 2;

    private static final String PREF_DOWNLOAD_ALONG_TRACK_OSM = "downloadAlongTrack.download.osm";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_GPS = "downloadAlongTrack.download.gps";

    private static final String PREF_DOWNLOAD_ALONG_TRACK_DISTANCE = "downloadAlongTrack.distance";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_AREA = "downloadAlongTrack.area";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_NEAR = "downloadAlongTrack.near";

    private final transient GpxData data;

    /**
     * Constructs a new {@code DownloadAlongTrackAction}
     * @param data The GPX data used to download along
     */
    public DownloadAlongTrackAction(GpxData data) {
        super(tr("Download from OSM along this track"), "downloadalongtrack", null, null, false);
        this.data = data;
    }

    PleaseWaitRunnable createTask() {
        final DownloadAlongPanel panel = new DownloadAlongPanel(
                PREF_DOWNLOAD_ALONG_TRACK_OSM, PREF_DOWNLOAD_ALONG_TRACK_GPS,
                PREF_DOWNLOAD_ALONG_TRACK_DISTANCE, PREF_DOWNLOAD_ALONG_TRACK_AREA, PREF_DOWNLOAD_ALONG_TRACK_NEAR);

        if (0 != panel.showInDownloadDialog(tr("Download from OSM along this track"), HelpUtil.ht("/Action/DownloadAlongTrack"))) {
            return null;
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
                        latsum += p.lat();
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
        if (latcnt == 0) {
            return null;
        }
        double avglat = latsum / latcnt;
        double scale = Math.cos(Utils.toRadians(avglat));
        /*
         * Compute buffer zone extents and maximum bounding box size. Note that the maximum we
         * ever offer is a bbox area of 0.002, while the API theoretically supports 0.25, but as
         * soon as you touch any built-up area, that kind of bounding box will download forever
         * and then stop because it has more than 50k nodes.
         */
        final double bufferDist = panel.getDistance();
        final double maxArea = panel.getArea() / 10000.0 / scale;
        final double bufferY = bufferDist / 100000.0;
        final double bufferX = bufferY / scale;
        final int totalTicks = latcnt;
        // guess if a progress bar might be useful.
        final boolean displayProgress = totalTicks > 2000 && bufferY < 0.01;

        class CalculateDownloadArea extends PleaseWaitRunnable {

            private final Area a = new Area();
            private boolean cancel;
            private int ticks;
            private final Rectangle2D r = new Rectangle2D.Double();

            CalculateDownloadArea() {
                super(tr("Calculating Download Area"), displayProgress ? null : NullProgressMonitor.INSTANCE, false);
            }

            @Override
            protected void cancel() {
                cancel = true;
            }

            @Override
            protected void finish() {
                // Do nothing
            }

            @Override
            protected void afterFinish() {
                if (cancel) {
                    return;
                }
                confirmAndDownloadAreas(a, maxArea, panel.isDownloadOsmData(), panel.isDownloadGpxData(),
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
                if (previous == null || c.greatCircleDistance(previous) > bufferDist) {
                    // we add a buffer around the point.
                    r.setRect(c.lon() - bufferX, c.lat() - bufferY, 2 * bufferX, 2 * bufferY);
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

        return new CalculateDownloadArea();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PleaseWaitRunnable task = createTask();
        if (task != null) {
            MainApplication.worker.submit(task);
        }
    }
}
