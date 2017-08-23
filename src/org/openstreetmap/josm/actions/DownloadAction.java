// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.AbstractDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that opens a connection to the osm server and downloads map data.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * @author imi
 */
public class DownloadAction extends JosmAction {

    /**
     * Constructs a new {@code DownloadAction}.
     */
    public DownloadAction() {
        super(tr("Download from OSM..."), "download", tr("Download map data from the OSM server."),
              Shortcut.registerShortcut("file:download", tr("File: {0}", tr("Download from OSM...")), KeyEvent.VK_DOWN, Shortcut.CTRL_SHIFT),
              true);
        putValue("help", ht("/Action/Download"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DownloadDialog dialog = DownloadDialog.getInstance();
        dialog.restoreSettings();
        dialog.setVisible(true);

        if (dialog.isCanceled()) {
            return;
        }

        dialog.rememberSettings();

        Optional<Bounds> selectedArea = dialog.getSelectedDownloadArea();
        if (!selectedArea.isPresent()) {
            JOptionPane.showMessageDialog(
                    dialog,
                    tr("Please select a download area first."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        final Bounds area = selectedArea.get();
        final boolean zoom = dialog.isZoomToDownloadedDataRequired();
        final List<Pair<AbstractDownloadTask<?>, Future<?>>> tasks = new ArrayList<>();

        if (dialog.isDownloadOsmData()) {
            DownloadOsmTask task = new DownloadOsmTask();
            task.setZoomAfterDownload(zoom && !dialog.isDownloadGpxData() && !dialog.isDownloadNotes());
            Future<?> future = task.download(dialog.isNewLayerRequired(), area, null);
            Main.worker.submit(new PostDownloadHandler(task, future));
            if (zoom) {
                tasks.add(new Pair<>(task, future));
            }
        }

        if (dialog.isDownloadGpxData()) {
            DownloadGpsTask task = new DownloadGpsTask();
            task.setZoomAfterDownload(zoom && !dialog.isDownloadOsmData() && !dialog.isDownloadNotes());
            Future<?> future = task.download(dialog.isNewLayerRequired(), area, null);
            Main.worker.submit(new PostDownloadHandler(task, future));
            if (zoom) {
                tasks.add(new Pair<>(task, future));
            }
        }

        if (dialog.isDownloadNotes()) {
            DownloadNotesTask task = new DownloadNotesTask();
            task.setZoomAfterDownload(zoom && !dialog.isDownloadOsmData() && !dialog.isDownloadGpxData());
            Future<?> future = task.download(false, area, null);
            Main.worker.submit(new PostDownloadHandler(task, future));
            if (zoom) {
                tasks.add(new Pair<>(task, future));
            }
        }

        if (zoom && tasks.size() > 1) {
            Main.worker.submit(() -> {
                ProjectionBounds bounds = null;
                // Wait for completion of download jobs
                for (Pair<AbstractDownloadTask<?>, Future<?>> p : tasks) {
                    try {
                        p.b.get();
                        ProjectionBounds b = p.a.getDownloadProjectionBounds();
                        if (bounds == null) {
                            bounds = b;
                        } else if (b != null) {
                            bounds.extend(b);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logging.warn(ex);
                    }
                }
                // Zoom to the larger download bounds
                MapFrame map = MainApplication.getMap();
                if (map != null && bounds != null) {
                    final ProjectionBounds pb = bounds;
                    GuiHelper.runInEDTAndWait(() -> map.mapView.zoomTo(new ViewportData(pb)));
                }
            });
        }
    }
}
