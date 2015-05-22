// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.io.OnlineResource;

/**
 * Action that downloads the notes within the current view from the server.
 *
 * No interaction is required.
 */
public final class DownloadNotesInViewAction extends JosmAction {

    private DownloadNotesInViewAction(String iconName) {
        super(tr("Download notes in current view"), iconName, tr("Download notes in current view"), null, false);
    }

    public static DownloadNotesInViewAction newActionWithNoteIcon() {
        return new DownloadNotesInViewAction("dialogs/notes/note_open");
    }

    public static DownloadNotesInViewAction newActionWithDownloadIcon() {
        return new DownloadNotesInViewAction("download");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Bounds bounds = Main.map.mapView.getRealBounds();
        DownloadNotesTask task = new DownloadNotesTask();
        Future<?> future = task.download(false, bounds, null);
        Main.worker.submit(new PostDownloadHandler(task, future));
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.map != null && Main.map.mapView != null && !Main.isOffline(OnlineResource.OSM_API));
    }
}
