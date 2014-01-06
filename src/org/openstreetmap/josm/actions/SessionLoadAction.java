// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.NavigatableComponent.ViewportData;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.FileFilterAllFiles;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionImporter;
import org.openstreetmap.josm.io.session.SessionReader;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Loads a JOSM session
 * @since 4668
 */
public class SessionLoadAction extends DiskAccessAction {

    /**
     * Constructs a new {@code SessionLoadAction}.
     */
    public SessionLoadAction() {
        super(tr("Load Session"), "open", tr("Load a session from file."), null, true, "load-session", true);
        putValue("help", ht("/Action/SessionLoad"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = createAndOpenFileChooser(true, false, tr("Open session"),
                Arrays.asList(SessionImporter.FILE_FILTER, FileFilterAllFiles.getInstance()),
                SessionImporter.FILE_FILTER, JFileChooser.FILES_ONLY, "lastDirectory");
        if (fc == null) return;
        File file = fc.getSelectedFile();
        boolean zip = file.getName().toLowerCase().endsWith(".joz");
        Main.worker.submit(new Loader(file, zip));
    }

    /**
     * JOSM session loader
     */
    public static class Loader extends PleaseWaitRunnable {

        private boolean canceled;
        private File file;
        private final URI uri;
        private final InputStream is;
        private final boolean zip;
        private List<Layer> layers;
        private Layer active;
        private List<Runnable> postLoadTasks;
        private ViewportData viewport;

        /**
         * Constructs a new {@code Loader} for local session file.
         * @param file The JOSM session file
         * @param zip {@code true} if the file is a session archive file (*.joz)
         */
        public Loader(File file, boolean zip) {
            super(tr("Loading session ''{0}''", file.getName()));
            CheckParameterUtil.ensureParameterNotNull(file, "file");
            this.file = file;
            this.uri = null;
            this.is = null;
            this.zip = zip;
        }

        /**
         * Constructs a new {@code Loader} for session file input stream (may be a remote file).
         * @param is The input stream to session file
         * @param uri The file URI
         * @param zip {@code true} if the file is a session archive file (*.joz)
         * @since 6245
         */
        public Loader(InputStream is, URI uri, boolean zip) {
            super(tr("Loading session ''{0}''", uri));
            CheckParameterUtil.ensureParameterNotNull(is, "is");
            CheckParameterUtil.ensureParameterNotNull(uri, "uri");
            this.file = null;
            this.uri = uri;
            this.is = is;
            this.zip = zip;
        }

        @Override
        public void cancel() {
            Thread.dumpStack();
            canceled = true;
        }

        @Override
        protected void finish() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (canceled) return;
                    if (!layers.isEmpty()) {
                        Layer firstLayer = layers.iterator().next();
                        boolean noMap = Main.map == null;
                        if (noMap) {
                            Main.main.createMapFrame(firstLayer, viewport);
                        }
                        for (Layer l : layers) {
                            if (canceled) return;
                            Main.main.addLayer(l);
                        }
                        if (active != null) {
                            Main.map.mapView.setActiveLayer(active);
                        }
                        if (noMap) {
                            Main.map.setVisible(true);
                        }
                    }
                    for (Runnable task : postLoadTasks) {
                        if (canceled) return;
                        if (task == null) {
                            continue;
                        }
                        task.run();
                    }
                }
            });
        }

        @Override
        protected void realRun() {
            try {
                ProgressMonitor monitor = getProgressMonitor();
                SessionReader reader = new SessionReader();
                boolean tempFile = false;
                try {
                    if (file == null) {
                        // Download and write entire joz file as a temp file on disk as we need random access later
                        file = File.createTempFile("session_", ".joz", Utils.getJosmTempDir());
                        tempFile = true;
                        FileOutputStream out = new FileOutputStream(file);
                        try {
                            Utils.copyStream(is, out);
                        } finally {
                            Utils.close(out);
                        }
                    }
                    reader.loadSession(file, zip, monitor);
                    layers = reader.getLayers();
                    active = reader.getActive();
                    postLoadTasks = reader.getPostLoadTasks();
                    viewport = reader.getViewport();
                } finally {
                    if (tempFile) {
                        if (!file.delete()) {
                            file.deleteOnExit();
                        }
                        file = null;
                    }
                }
            } catch (IllegalDataException e) {
                Main.error(e);
                HelpAwareOptionPane.showMessageDialogInEDT(
                        Main.parent,
                        tr("<html>Could not load session file ''{0}''.<br>Error is:<br>{1}</html>", uri != null ? uri : file.getName(), e.getMessage()),
                        tr("Data Error"),
                        JOptionPane.ERROR_MESSAGE,
                        null
                        );
                cancel();
            } catch (IOException e) {
                Main.error(e);
                HelpAwareOptionPane.showMessageDialogInEDT(
                        Main.parent,
                        tr("<html>Could not load session file ''{0}''.<br>Error is:<br>{1}</html>", uri != null ? uri : file.getName(), e.getMessage()),
                        tr("IO Error"),
                        JOptionPane.ERROR_MESSAGE,
                        null
                        );
                cancel();
            } catch (RuntimeException e) {
                cancel();
                throw e;
            } catch (Error e) {
                cancel();
                throw e;
            }
        }
    }
}

