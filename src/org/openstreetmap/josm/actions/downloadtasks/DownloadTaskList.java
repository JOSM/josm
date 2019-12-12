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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class encapsulates the downloading of several bounding boxes that would otherwise be too
 * large to download in one go. Error messages will be collected for all downloads and displayed as
 * a list in the end.
 * @author xeen
 * @since 6053
 */
public class DownloadTaskList {
    private final List<DownloadTask> tasks = new LinkedList<>();
    private final List<Future<?>> taskFutures = new LinkedList<>();
    private final boolean zoomAfterDownload;
    private ProgressMonitor progressMonitor;

    /**
     * Constructs a new {@code DownloadTaskList}. Zooms to each download area.
     */
    public DownloadTaskList() {
        this(true);
    }

    /**
     * Constructs a new {@code DownloadTaskList}.
     * @param zoomAfterDownload whether to zoom to each download area
     * @since 15205
     */
    public DownloadTaskList(boolean zoomAfterDownload) {
        this.zoomAfterDownload = zoomAfterDownload;
    }

    private void addDownloadTask(ProgressMonitor progressMonitor, DownloadTask dt, Rectangle2D td, int i, int n) {
        ProgressMonitor childProgress = progressMonitor.createSubTaskMonitor(1, false);
        childProgress.setCustomText(tr("Download {0} of {1} ({2} left)", i, n, n - i));
        dt.setZoomAfterDownload(zoomAfterDownload);
        Future<?> future = dt.download(new DownloadParams(), new Bounds(td), childProgress);
        taskFutures.add(future);
        tasks.add(dt);
    }

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param rects The List of Rectangle2D to download
     * @param osmData Set to true if OSM data should be downloaded
     * @param gpxData Set to true if GPX data should be downloaded
     * @param progressMonitor The progress monitor
     * @return The Future representing the asynchronous download task
     */
    public Future<?> download(boolean newLayer, List<Rectangle2D> rects, boolean osmData, boolean gpxData, ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
        if (newLayer) {
            Layer l = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            MainApplication.getLayerManager().addLayer(l);
            MainApplication.getLayerManager().setActiveLayer(l);
        }

        int n = (osmData && gpxData ? 2 : 1)*rects.size();
        progressMonitor.beginTask(null, n);
        int i = 0;
        for (Rectangle2D td : rects) {
            i++;
            if (osmData) {
                addDownloadTask(progressMonitor, new DownloadOsmTask(), td, i, n);
            }
            if (gpxData) {
                addDownloadTask(progressMonitor, new DownloadGpsTask(), td, i, n);
            }
        }
        progressMonitor.addCancelListener(() -> {
            for (DownloadTask dt : tasks) {
                dt.cancel();
            }
        });
        return MainApplication.worker.submit(new PostDownloadProcessor(osmData));
    }

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param areas The Collection of Areas to download
     * @param osmData Set to true if OSM data should be downloaded
     * @param gpxData Set to true if GPX data should be downloaded
     * @param progressMonitor The progress monitor
     * @return The Future representing the asynchronous download task
     */
    public Future<?> download(boolean newLayer, Collection<Area> areas, boolean osmData, boolean gpxData, ProgressMonitor progressMonitor) {
        progressMonitor.beginTask(tr("Updating data"));
        try {
            List<Rectangle2D> rects = areas.stream().map(Area::getBounds2D).collect(Collectors.toList());
            return download(newLayer, rects, osmData, gpxData, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Replies the set of ids of all complete, non-new primitives (i.e. those with !primitive.incomplete)
     * @param ds data set
     *
     * @return the set of ids of all complete, non-new primitives
     */
    protected Set<OsmPrimitive> getCompletePrimitives(DataSet ds) {
        return ds.allPrimitives().stream().filter(p -> !p.isIncomplete() && !p.isNew()).collect(Collectors.toSet());
    }

    /**
     * Updates the local state of a set of primitives (given by a set of primitive ids) with the
     * state currently held on the server.
     *
     * @param potentiallyDeleted a set of ids to check update from the server
     */
    protected void updatePotentiallyDeletedPrimitives(Set<OsmPrimitive> potentiallyDeleted) {
        final List<OsmPrimitive> toSelect = new ArrayList<>();
        for (OsmPrimitive primitive : potentiallyDeleted) {
            if (primitive != null) {
                toSelect.add(primitive);
            }
        }
        EventQueue.invokeLater(() -> UpdateSelectionAction.updatePrimitives(toSelect));
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
        ButtonSpec[] options = {
                new ButtonSpec(
                        tr("Check on the server"),
                        new ImageProvider("ok"),
                        tr("Click to check whether objects in your local dataset are deleted on the server"),
                        null /* no specific help topic */),
                new ButtonSpec(
                        tr("Ignore"),
                        new ImageProvider("cancel"),
                        tr("Click to abort and to resume editing"),
                        null /* no specific help topic */),
        };

        String message = "<html>" + trn(
                "There is {0} object in your local dataset which "
                + "might be deleted on the server.<br>If you later try to delete or "
                + "update this the server is likely to report a conflict.",
                "There are {0} objects in your local dataset which "
                + "might be deleted on the server.<br>If you later try to delete or "
                + "update them the server is likely to report a conflict.",
                potentiallyDeleted.size(), potentiallyDeleted.size())
                + "<br>"
                + trn("Click <strong>{0}</strong> to check the state of this object on the server.",
                "Click <strong>{0}</strong> to check the state of these objects on the server.",
                potentiallyDeleted.size(),
                options[0].text) + "<br>"
                + tr("Click <strong>{0}</strong> to ignore." + "</html>", options[1].text);

        int ret = HelpAwareOptionPane.showOptionDialog(
                MainApplication.getMainFrame(),
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
        return tasks.stream()
                .filter(t -> t instanceof DownloadOsmTask)
                .map(t -> ((DownloadOsmTask) t).getDownloadedData())
                .filter(Objects::nonNull)
                .flatMap(ds -> ds.allPrimitives().stream())
                .collect(Collectors.toSet());
    }

    class PostDownloadProcessor implements Runnable {

        private final boolean osmData;

        PostDownloadProcessor(boolean osmData) {
            this.osmData = osmData;
        }

        /**
         * Grabs and displays the error messages after all download threads have finished.
         */
        @Override
        public void run() {
            progressMonitor.finishTask();

            // wait for all download tasks to finish
            //
            for (Future<?> future : taskFutures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    Logging.error(e);
                    return;
                }
            }
            Set<Object> errors = tasks.stream().flatMap(t -> t.getErrorObjects().stream()).collect(Collectors.toSet());
            if (!errors.isEmpty()) {
                final Collection<String> items = new ArrayList<>();
                for (Object error : errors) {
                    if (error instanceof String) {
                        items.add((String) error);
                    } else if (error instanceof Exception) {
                        items.add(ExceptionUtil.explainException((Exception) error));
                    }
                }

                GuiHelper.runInEDT(() -> {
                    if (items.size() == 1 && PostDownloadHandler.isNoDataErrorMessage(items.iterator().next())) {
                        new Notification(items.iterator().next()).setIcon(JOptionPane.WARNING_MESSAGE).show();
                    } else {
                        JOptionPane.showMessageDialog(MainApplication.getMainFrame(), "<html>"
                                + tr("The following errors occurred during mass download: {0}",
                                        Utils.joinAsHtmlUnorderedList(items)) + "</html>",
                                tr("Errors during download"), JOptionPane.ERROR_MESSAGE);
                    }
                });

                return;
            }

            // FIXME: this is a hack. We assume that the user canceled the whole download if at
            // least one task was canceled or if it failed
            //
            for (DownloadTask task : tasks) {
                if (task instanceof AbstractDownloadTask) {
                    AbstractDownloadTask<?> absTask = (AbstractDownloadTask<?>) task;
                    if (absTask.isCanceled() || absTask.isFailed())
                        return;
                }
            }
            final DataSet editDataSet = MainApplication.getLayerManager().getEditDataSet();
            if (editDataSet != null && osmData) {
                final List<DownloadOsmTask> osmTasks = tasks.stream()
                        .filter(t -> t instanceof DownloadOsmTask).map(t -> (DownloadOsmTask) t)
                        .filter(t -> t.getDownloadedData() != null)
                        .collect(Collectors.toList());
                final Set<Bounds> tasksBounds = osmTasks.stream()
                        .flatMap(t -> t.getDownloadedData().getDataSourceBounds().stream())
                        .collect(Collectors.toSet());
                final Set<Bounds> layerBounds = new LinkedHashSet<>(editDataSet.getDataSourceBounds());
                final Set<OsmPrimitive> myPrimitives = new LinkedHashSet<>();
                if (layerBounds.equals(tasksBounds)) {
                    // the full edit layer is updated (we have downloaded again all its current bounds)
                    myPrimitives.addAll(getCompletePrimitives(editDataSet));
                    for (DownloadOsmTask task : osmTasks) {
                        // myPrimitives.removeAll(ds.allPrimitives()) will do the same job but much slower
                        task.getDownloadedData().allPrimitives().forEach(myPrimitives::remove);
                    }
                } else {
                    // partial update, only check what has been downloaded
                    for (DownloadOsmTask task : osmTasks) {
                        myPrimitives.addAll(task.searchPotentiallyDeletedPrimitives(editDataSet));
                    }
                }
                if (!myPrimitives.isEmpty()) {
                    GuiHelper.runInEDT(() -> handlePotentiallyDeletedPrimitives(myPrimitives));
                }
            }
        }
    }
}
