// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
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
        for(Area a : areas) {
            rects.add(a.getBounds2D());
        }

        download(newLayer, rects);
    }

    /**
     * Grabs and displays the error messages after all download threads have finished.
     */
    public void run() {
        String errors = "";

        for(DownloadTask dt : osmTasks) {
            String err = dt.getErrorMessage();
            if(err.equals("")) {
                continue;
            }
            errors += "* " + err + "\r\n";
        }

        if(! errors.equals("")) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("The following errors occured during mass download:") + "\r\n" + errors,
                    tr("Errors during Download"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Set<Long> myPrimitiveIds = Main.main.editLayer().data.getPrimitiveIds();
        Set<Long> downloadedIds = getDownloadedIds();
        myPrimitiveIds.removeAll(downloadedIds);
        myPrimitiveIds.remove(new Long(0));
        if (! myPrimitiveIds.isEmpty()) {
            handlePotentiallyDeletedPrimitives(myPrimitiveIds);
        }
    }

    protected void checkPotentiallyDeletedPrimitives(Set<Long> potentiallyDeleted) {
        DataSet ds =  Main.main.editLayer().data;
        ArrayList<OsmPrimitive> toSelect = new ArrayList<OsmPrimitive>();
        for (Long id : potentiallyDeleted) {
            OsmPrimitive primitive = ds.getPrimitiveById(id);
            if (primitive != null) {
                toSelect.add(primitive);
            }
        }
        ds.setSelected(toSelect);
        EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        new UpdateSelectionAction().actionPerformed(new ActionEvent(this, 0, ""));
                    }
                }
        );
    }

    protected void handlePotentiallyDeletedPrimitives(Set<Long> potentiallyDeleted) {
        String [] options = {
                "Check individually",
                "Ignore"
        };

        String message = tr("<html>"
                +  "There are {0} primitives in your local dataset which<br>"
                + "might be deleted on the server. If you later try to delete or<br>"
                + "update them on the server the server is likely to report a<br>"
                + "conflict.<br>"
                + "<br>"
                + "Click <strong>{1}</strong> to check these primitives individually.<br>"
                + "Click <strong>{2}</strong> to ignore.<br>"
                + "</html>",
                potentiallyDeleted.size(), options[0], options[1]
        );

        int ret = JOptionPane.showOptionDialog(
                Main.parent,
                message,
                tr("Deleted or moved primitives"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        );
        switch(ret) {
        case JOptionPane.CLOSED_OPTION: return;
        case JOptionPane.NO_OPTION: return;
        case JOptionPane.YES_OPTION: checkPotentiallyDeletedPrimitives(potentiallyDeleted); break;
        }
    }

    protected boolean wasDownloaded(long id, DataSet ds) {
        OsmPrimitive primitive = ds.getPrimitiveById(id);
        return primitive != null;
    }

    public boolean wasDownloaded(long id) {
        for (DownloadTask task : osmTasks) {
            if(task instanceof DownloadOsmTask) {
                DataSet ds = ((DownloadOsmTask)task).getDownloadedData();
                if(wasDownloaded(id,ds)) return true;
            }
        }
        return false;
    }


    public Set<Long> getDownloadedIds() {
        HashSet<Long> ret = new HashSet<Long>();
        for (DownloadTask task : osmTasks) {
            if(task instanceof DownloadOsmTask) {
                DataSet ds = ((DownloadOsmTask)task).getDownloadedData();
                ret.addAll(ds.getPrimitiveIds());
            }
        }
        return ret;
    }
}
