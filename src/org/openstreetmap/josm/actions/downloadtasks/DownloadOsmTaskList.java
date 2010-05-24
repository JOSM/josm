// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.EventQueue;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor.CancelListener;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This class encapsulates the downloading of several bounding boxes that would otherwise be too
 * large to download in one go. Error messages will be collected for all downloads and displayed as
 * a list in the end.
 * @author xeen
 *
 */
public class DownloadOsmTaskList {
    private List<DownloadTask> osmTasks = new LinkedList<DownloadTask>();
    private List<Future<?>> osmTaskFutures = new LinkedList<Future<?>>();
    private ProgressMonitor progressMonitor;

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param The List of Rectangle2D to download
     */
    public Future<?> download(boolean newLayer, List<Rectangle2D> rects, ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
        if (newLayer) {
            Layer l = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            Main.main.addLayer(l);
            Main.map.mapView.setActiveLayer(l);
        }

        progressMonitor.beginTask(null, rects.size());
        int i = 0;
        for (Rectangle2D td : rects) {
            i++;
            DownloadTask dt = new DownloadOsmTask();
            ProgressMonitor childProgress = progressMonitor.createSubTaskMonitor(1, false);
            childProgress.setCustomText(tr("Download {0} of {1} ({2} left)", i, rects.size(), rects.size() - i));
            Future<?> future = dt.download(false, new Bounds(td), childProgress);
            osmTaskFutures.add(future);
            osmTasks.add(dt);
        }
        progressMonitor.addCancelListener(new CancelListener() {
            public void operationCanceled() {
                for (DownloadTask dt : osmTasks) {
                    dt.cancel();
                }
            }
        });
        return Main.worker.submit(new PostDownloadProcessor());
    }

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param The Collection of Areas to download
     */
    public Future<?> download(boolean newLayer, Collection<Area> areas, ProgressMonitor progressMonitor) {
        progressMonitor.beginTask(tr("Updating data"));
        try {
            List<Rectangle2D> rects = new ArrayList<Rectangle2D>(areas.size());
            for (Area a : areas) {
                rects.add(a.getBounds2D());
            }

            return download(newLayer, rects, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Replies the set of ids of all complete, non-new primitives (i.e. those with !
     * primitive.incomplete)
     *
     * @return the set of ids of all complete, non-new primitives
     */
    protected Set<OsmPrimitive> getCompletePrimitives(DataSet ds) {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (OsmPrimitive primitive : ds.allPrimitives()) {
            if (!primitive.isIncomplete() && !primitive.isNew()) {
                ret.add(primitive);
            }
        }
        return ret;
    }

    /**
     * Updates the local state of a set of primitives (given by a set of primitive ids) with the
     * state currently held on the server.
     *
     * @param potentiallyDeleted a set of ids to check update from the server
     */
    protected void updatePotentiallyDeletedPrimitives(Set<OsmPrimitive> potentiallyDeleted) {
        final ArrayList<OsmPrimitive> toSelect = new ArrayList<OsmPrimitive>();
        for (OsmPrimitive primitive : potentiallyDeleted) {
            if (primitive != null) {
                toSelect.add(primitive);
            }
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new UpdateSelectionAction().updatePrimitives(toSelect);
            }
        });
    }

    /**
     * Processes a set of primitives (given by a set of their ids) which might be deleted on the
     * server. First prompts the user whether he wants to check the current state on the server. If
     * yes, retrieves the current state on the server and checks whether the primitives are indeed
     * deleted on the server.
     *
     * @param potentiallyDeleted a set of primitives (given by their ids)
     */
    protected void handlePotentiallyDeletedPrimitives(Set<OsmPrimitive> potentiallyDeleted) {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Check on the server"),
                        ImageProvider.get("ok"),
                        tr("Click to check whether objects in your local dataset are deleted on the server"),
                        null  /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("Ignore"),
                        ImageProvider.get("cancel"),
                        tr("Click to abort and to resume editing"),
                        null /* no specific help topic */
                ),
        };

        String message = "<html>"
            + trn("There is {0} object in your local dataset which "
                    + "might be deleted on the server. If you later try to delete or "
                    + "update this the server is likely to report a conflict.",
                    "There are {0} objects in your local dataset which "
                    + "might be deleted on the server. If you later try to delete or "
                    + "update them the server is likely to report a conflict.", potentiallyDeleted.size(), potentiallyDeleted.size())
                    + "<br>"
                    + trn("Click <strong>{0}</strong> to check the state of this object on the server.",
                            "Click <strong>{0}</strong> to check the state of these objects on the server.",
                            potentiallyDeleted.size(),
                            options[0].text) + "<br>"
                            + tr("Click <strong>{0}</strong> to ignore." + "</html>", options[1].text);

        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                message,
                tr("Deleted or moved objects"),
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0],
                ht("/Action/UpdateData#SyncPotentiallyDeletedObjects")
        );
        if (ret != 0 /* OK */)
            return;

        updatePotentiallyDeletedPrimitives(potentiallyDeleted);
    }

    /**
     * Replies the set of primitive ids which have been downloaded by this task list
     *
     * @return the set of primitive ids which have been downloaded by this task list
     */
    public Set<OsmPrimitive> getDownloadedPrimitives() {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (DownloadTask task : osmTasks) {
            if (task instanceof DownloadOsmTask) {
                DataSet ds = ((DownloadOsmTask) task).getDownloadedData();
                if (ds != null) {
                    ret.addAll(ds.allPrimitives());
                }
            }
        }
        return ret;
    }

    class PostDownloadProcessor implements Runnable {
        /**
         * Grabs and displays the error messages after all download threads have finished.
         */
        public void run() {
            progressMonitor.finishTask();

            // wait for all download tasks to finish
            //
            for (Future<?> future : osmTaskFutures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            LinkedHashSet<Object> errors = new LinkedHashSet<Object>();
            for (DownloadTask dt : osmTasks) {
                errors.addAll(dt.getErrorObjects());
            }
            if (!errors.isEmpty()) {
                StringBuffer sb = new StringBuffer();
                for (Object error : errors) {
                    if (error instanceof String) {
                        sb.append("<li>").append(error).append("</li>").append("<br>");
                    } else if (error instanceof Exception) {
                        sb.append("<li>").append(ExceptionUtil.explainException((Exception) error)).append("</li>")
                        .append("<br>");
                    }
                }
                sb.insert(0, "<ul>");
                sb.append("</ul>");

                JOptionPane.showMessageDialog(Main.parent, "<html>"
                        + tr("The following errors occurred during mass download: {0}", sb.toString()) + "</html>",
                        tr("Errors during Download"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            // FIXME: this is a hack. We assume that the user canceled the whole download if at
            // least
            // one task was canceled or if it failed
            //
            for (DownloadTask task : osmTasks) {
                if (task instanceof DownloadOsmTask) {
                    DownloadOsmTask osmTask = (DownloadOsmTask) task;
                    if (osmTask.isCanceled() || osmTask.isFailed())
                        return;
                }
            }
            final OsmDataLayer editLayer = Main.map.mapView.getEditLayer();
            if (editLayer != null) {
                Set<OsmPrimitive> myPrimitives = getCompletePrimitives(editLayer.data);
                for (DownloadTask task : osmTasks) {
                    if (task instanceof DownloadOsmTask) {
                        DataSet ds = ((DownloadOsmTask) task).getDownloadedData();
                        if (ds != null) {
                            myPrimitives.removeAll(ds.allPrimitives());
                        }
                    }
                }
                if (!myPrimitives.isEmpty()) {
                    handlePotentiallyDeletedPrimitives(myPrimitives);
                }
            }
        }
    }
}
