// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.downloadtasks.DownloadTaskList;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Abstract superclass of DownloadAlongTrackAction and DownloadAlongWayAction
 * @since 6054
 */
public abstract class DownloadAlongAction extends JosmAction {

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
            addToDownload(a, r1, results, maxArea);
            addToDownload(a, r2, results, maxArea);
        } else {
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
     * (upper and lower half), but we donot request the full upper and lower rectangle, only
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
     * @param progressMonitor the progress monitor
     */
    protected static void confirmAndDownloadAreas(Area a, double maxArea, boolean osmDownload, boolean gpxDownload, String title,
            ProgressMonitor progressMonitor) {
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
        final Future<?> future = new DownloadTaskList().download(false, toDownload, osmDownload, gpxDownload, monitor);
        waitFuture(future, monitor);
    }
}
