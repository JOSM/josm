// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This class encapsulates the downloading of several bounding boxes that would otherwise be too
 * large to download in one go. Error messages will be collected for all downloads and displayed
 * as a list in the end.
 * @author xeen
 *
 */
public class DownloadOsmTaskList implements Runnable {
    private List<DownloadTask> osmTasks = new LinkedList<DownloadTask>();

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param The List of Rectangle2D to download
     */
    public void download(boolean newLayer, List<Rectangle2D> rects) {
        if(newLayer) {
            Layer l = new OsmDataLayer(new DataSet(), tr("Data Layer"), null);
            Main.main.addLayer(l);
            Main.map.mapView.setActiveLayer(l);
        }

        int i = 0;
        for(Rectangle2D td : rects) {
            i++;
            DownloadTask dt = new DownloadOsmTask();
            dt.download(null, td.getMinY(), td.getMinX(), td.getMaxY(), td.getMaxX(), true,
                    tr("Download {0} of {1} ({2} left)", i, rects.size(), rects.size()-i));
            osmTasks.add(dt);
        }

        // If we try to get the error message now the download task will never have been started
        // and we'd be stuck in a classical dead lock. Instead attach this to the worker and once
        // run() gets called all downloadTasks have finished and we can grab the error messages.
        Main.worker.execute(this);
    }

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param The Collection of Areas to download
     */
    public void download(boolean newLayer, Collection<Area> areas) {
        List<Rectangle2D> rects = new LinkedList<Rectangle2D>();
        for(Area a : areas)
            rects.add(a.getBounds2D());

        download(newLayer, rects);
    }

    /**
     * Grabs and displays the error messages after all download threads have finished.
     */
    public void run() {
        String errors = "";

        for(DownloadTask dt : osmTasks) {
            String err = dt.getErrorMessage();
            if(err.equals(""))
                continue;
            errors += "* " + err + "\r\n";
        }

        osmTasks.clear();
        if(errors.equals(""))
            return;

        JOptionPane.showMessageDialog(Main.parent,
                tr("The following errors occured during mass download:") + "\r\n" + errors,
                tr("Errors during Download"),
                JOptionPane.ERROR_MESSAGE);
    }
}
