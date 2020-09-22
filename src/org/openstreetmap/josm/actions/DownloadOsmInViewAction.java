// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.Future;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that downloads the OSM data within the current view from the server.
 *
 * No interaction is required.
 */
public final class DownloadOsmInViewAction extends JosmAction {

    /**
     * Creates a new {@code DownloadOsmInViewAction}.
     */
    public DownloadOsmInViewAction() {
        super(tr("Download in current view"), "download_in_view", tr("Download map data from the OSM server in current view"),
                Shortcut.registerShortcut("file:downloadosminview",
                tr("Download in current view"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), false,
                "dialogs/download_in_view", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Bounds bounds = MainApplication.getMap().mapView.getRealBounds();
        DownloadOsmInViewTask task = new DownloadOsmInViewTask();
        task.setZoomAfterDownload(false);
        Future<?> future = task.download(bounds);
        MainApplication.worker.submit(new PostDownloadHandler(task, future));
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveLayer() != null
                && !NetworkManager.isOffline(OnlineResource.OSM_API));
    }

    private static class DownloadOsmInViewTask extends DownloadOsmTask {
        Future<?> download(Bounds downloadArea) {
            return download(new DownloadTask(new DownloadParams(), new BoundingBoxDownloader(downloadArea), null, false), downloadArea);
        }
    }
}
