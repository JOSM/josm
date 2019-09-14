// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.downloadtasks.DownloadTaskList;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.gpx.DownloadAlongPanel;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Abstract superclass of DownloadAlongTrackAction and DownloadAlongWayAction
 * @since 6054
 */
public abstract class DownloadAlongAction extends JosmAction {

    /**
     * Sub classes must override this method.
     * @return the task to start or null if nothing to do
     */
    protected abstract PleaseWaitRunnable createTask();

    /**
     * Constructs a new {@code DownloadAlongAction}
     * @param name the action's text as displayed in the menu
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param registerInToolbar register this action for the toolbar preferences?
     */
    public DownloadAlongAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);
    }

    protected static void addToDownload(Area a, Rectangle2D r, Collection<Rectangle2D> results, double maxArea) {
        Area tmp = new Area(r);
        // intersect with sought-after area
        tmp.intersect(a);
        if (tmp.isEmpty()) {
            return;
        }
        Rectangle2D bounds = tmp.getBounds2D();
        if (bounds.getWidth() * bounds.getHeight() > maxArea) {
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
            addToDownload(tmp, r1, results, maxArea);
            addToDownload(tmp, r2, results, maxArea);
        } else {
            DataSet ds = MainApplication.getLayerManager().getEditDataSet();
            if (ds != null) {
                Collection<Bounds> existing = ds.getDataSourceBounds();
                if (existing != null) {
                    double p = LatLon.MAX_SERVER_PRECISION;
                    LatLon min = new LatLon(bounds.getY()+p, bounds.getX()+p);
                    LatLon max = new LatLon(bounds.getY()+bounds.getHeight()-p, bounds.getX()+bounds.getWidth()-p);
                    if (existing.stream().anyMatch(current -> (current.contains(min) && current.contains(max)))) {
                        return; // skip this one, already downloaded
                    }
                }
            }
            results.add(bounds);
        }
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
     * (upper and lower half), but we do not request the full upper and lower rectangle, only
     * the part of the upper/lower rectangle that actually has something in it.
     *
     * This functions calculates the rectangles, asks the user to continue and downloads
     * the areas if applicable.
     *
     * @param a download area hull
     * @param maxArea maximum area size for a single download
     * @param osmDownload Set to true if OSM data should be downloaded
     * @param gpxDownload Set to true if GPX data should be downloaded
     * @param title the title string for the confirmation dialog
     */
    protected static void confirmAndDownloadAreas(Area a, double maxArea, boolean osmDownload, boolean gpxDownload, String title) {
        List<Rectangle2D> toDownload = new ArrayList<>();
        addToDownload(a, a.getBounds(), toDownload, maxArea);
        if (toDownload.isEmpty()) {
            return;
        }
        JPanel msg = new JPanel(new GridBagLayout());
        msg.add(new JLabel(
                tr("<html>This action will require {0} individual<br>" + "download requests. Do you wish<br>to continue?</html>",
                        toDownload.size())), GBC.eol());
        if (!GraphicsEnvironment.isHeadless() && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                MainApplication.getMainFrame(), msg, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
            return;
        }
        final PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download data"));
        final Future<?> future = new DownloadTaskList(Config.getPref().getBoolean("download.along.zoom-after-download"))
                .download(false, toDownload, osmDownload, gpxDownload, monitor);
        waitFuture(future, monitor);
    }

    /**
     * Calculate list of points between two given points so that the distance between two consecutive points is below a limit.
     * @param p1 first point or null
     * @param p2 second point (must not be null)
     * @param bufferDist the maximum distance
     * @return a list of points with at least one point (p2) and maybe more.
     */
    protected static Collection<LatLon> calcBetweenPoints(LatLon p1, LatLon p2, double bufferDist) {
        ArrayList<LatLon> intermediateNodes = new ArrayList<>();
        intermediateNodes.add(p2);
        if (p1 != null && p2.greatCircleDistance(p1) > bufferDist) {
            Double d = p2.greatCircleDistance(p1) / bufferDist;
            int nbNodes = d.intValue();
            if (Logging.isDebugEnabled()) {
                Logging.debug(tr("{0} intermediate nodes to download.", nbNodes));
                Logging.debug(tr("between {0} {1} and {2} {3}", p2.lat(), p2.lon(), p1.lat(), p1.lon()));
            }
            double latStep = (p2.lat() - p1.lat()) / (nbNodes + 1);
            double lonStep = (p2.lon() - p1.lon()) / (nbNodes + 1);
            for (int i = 1; i <= nbNodes; i++) {
                LatLon intermediate = new LatLon(p1.lat() + i * latStep, p1.lon() + i * lonStep);
                intermediateNodes.add(intermediate);
                if (Logging.isTraceEnabled()) {
                    Logging.trace(tr("  adding {0} {1}", intermediate.lat(), intermediate.lon()));
                }
            }
        }
        return intermediateNodes;
    }

    /**
     * Create task that downloads areas along the given path using the values specified in the panel.
     * @param alongPath the path along which the areas are to be downloaded
     * @param panel the panel that was displayed to the user and now contains his selections
     * @param confirmTitle the title to display in the confirmation panel
     * @return the task or null if canceled by user
     */
    protected PleaseWaitRunnable createCalcTask(Path2D alongPath, DownloadAlongPanel panel, String confirmTitle) {
        /*
         * Find the average latitude for the data we're contemplating, so we can know how many
         * metres per degree of longitude we have.
         */
        double latsum = 0;
        int latcnt = 0;
        final PathIterator pit = alongPath.getPathIterator(null);
        final double[] res = new double[6];
        while (!pit.isDone()) {
            int type = pit.currentSegment(res);
            if (type == PathIterator.SEG_LINETO || type == PathIterator.SEG_MOVETO) {
                latsum += res[1];
                latcnt++;
            }
            pit.next();
        }
        if (latcnt == 0) {
            return null;
        }
        final double avglat = latsum / latcnt;
        final double scale = Math.cos(Utils.toRadians(avglat));

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
        final boolean displayProgress = totalTicks > 20_000 && bufferY < 0.01;

        class CalculateDownloadArea extends PleaseWaitRunnable {

            private final Path2D downloadPath = new Path2D.Double();
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
                confirmAndDownloadAreas(new Area(downloadPath), maxArea, panel.isDownloadOsmData(), panel.isDownloadGpxData(),
                        confirmTitle);
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
             * calculate area enclosing a single point
             */
            private void calcAreaForWayPoint(LatLon c) {
                r.setRect(c.lon() - bufferX, c.lat() - bufferY, 2 * bufferX, 2 * bufferY);
                downloadPath.append(r, false);
            }

            @Override
            protected void realRun() {
                progressMonitor.setTicksCount(totalTicks);
                PathIterator pit = alongPath.getPathIterator(null);
                double[] res = new double[6];
                LatLon previous = null;
                while (!pit.isDone()) {
                    int type = pit.currentSegment(res);
                    LatLon c = new LatLon(res[1], res[0]);
                    if (type == PathIterator.SEG_LINETO) {
                        tick();
                        for (LatLon d : calcBetweenPoints(previous, c, bufferDist)) {
                            calcAreaForWayPoint(d);
                        }
                        previous = c;
                    } else if (type == PathIterator.SEG_MOVETO) {
                        previous = c;
                        tick();
                        calcAreaForWayPoint(c);
                    }
                    pit.next();
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
