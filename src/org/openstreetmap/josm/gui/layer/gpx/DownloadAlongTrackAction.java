package org.openstreetmap.josm.gui.layer.gpx;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTaskList;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.GBC;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Action that issues a series of download requests to the API, following the GPX track.
 *
 * @author fred
 */
public class DownloadAlongTrackAction extends AbstractAction {
    static final int NEAR_TRACK = 0;
    static final int NEAR_WAYPOINTS = 1;
    static final int NEAR_BOTH = 2;
    
    private static final String PREF_DOWNLOAD_ALONG_TRACK_DISTANCE = "gpxLayer.downloadAlongTrack.distance";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_AREA = "gpxLayer.downloadAlongTrack.area";
    private static final String PREF_DOWNLOAD_ALONG_TRACK_NEAR = "gpxLayer.downloadAlongTrack.near";

    
    private final Integer[] dist = {5000, 500, 50};
    private final Integer[] area = {20, 10, 5, 1};
    
    private final GpxData data;

    public DownloadAlongTrackAction(GpxData data) {
        super(tr("Download from OSM along this track"), ImageProvider.get("downloadalongtrack"));
        this.data = data;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        /*
         * build selection dialog
         */
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JLabel(tr("Download everything within:")), GBC.eol());
        String[] s = new String[dist.length];
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
        JList downloadNear = new JList(new String[]{tr("track only"), tr("waypoints only"), tr("track and waypoints")});
        downloadNear.setSelectedIndex(Main.pref.getInteger(PREF_DOWNLOAD_ALONG_TRACK_NEAR, 0));
        msg.add(downloadNear, GBC.eol());
        int ret = JOptionPane.showConfirmDialog(Main.parent, msg, tr("Download from OSM along this track"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        switch (ret) {
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                return;
            default:
        // continue
        }
        Main.pref.putInteger(PREF_DOWNLOAD_ALONG_TRACK_DISTANCE, buffer.getSelectedIndex());
        Main.pref.putInteger(PREF_DOWNLOAD_ALONG_TRACK_AREA, maxRect.getSelectedIndex());
        final int near = downloadNear.getSelectedIndex();
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
        final int buffer_dist = dist[i < 0 ? 0 : i];
        i = maxRect.getSelectedIndex();
        final double max_area = area[i < 0 ? 0 : i] / 10000.0 / scale;
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
                confirmAndDownloadAreas(a, max_area, progressMonitor);
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

    /**
     * Area "a" contains the hull that we would like to download data for. however we
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
     *
     * This functions calculates the rectangles, asks the user to continue and downloads
     * the areas if applicable.
     */
    private void confirmAndDownloadAreas(Area a, double max_area, ProgressMonitor progressMonitor) {
        List<Rectangle2D> toDownload = new ArrayList<Rectangle2D>();
        addToDownload(a, a.getBounds(), toDownload, max_area);
        if (toDownload.isEmpty()) {
            return;
        }
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JLabel(tr("<html>This action will require {0} individual<br>" + "download requests. Do you wish<br>to continue?</html>", toDownload.size())), GBC.eol());
        if (toDownload.size() > 1) {
            int ret = JOptionPane.showConfirmDialog(Main.parent, msg, tr("Download from OSM along this track"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            switch (ret) {
                case JOptionPane.CANCEL_OPTION:
                case JOptionPane.CLOSED_OPTION:
                    return;
                default:
            // continue
            }
        }
        final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download data"));
        final Future<?> future = new DownloadOsmTaskList().download(false, toDownload, monitor);
        Main.worker.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                monitor.close();
            }
        });
    }
    
     private static void addToDownload(Area a, Rectangle2D r, Collection<Rectangle2D> results, double max_area) {
        Area tmp = new Area(r);
        // intersect with sought-after area
        tmp.intersect(a);
        if (tmp.isEmpty()) {
             return;
         }
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
    
}
