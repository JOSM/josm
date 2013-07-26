// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
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
import org.openstreetmap.josm.io.session.SessionReader;

public class SessionLoadAction extends DiskAccessAction {

    public SessionLoadAction() {
        super(tr("Load Session"), "open", tr("Load a session from file."), null, true, "load-session", true);
        putValue("help", ht("/Action/SessionLoad"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ExtensionFileFilter ff = new ExtensionFileFilter("jos,joz", "jos", tr("Session file (*.jos, *.joz)"));
        JFileChooser fc = createAndOpenFileChooser(true, false, tr("Open session"), Arrays.asList(ff, FileFilterAllFiles.getInstance()), ff, JFileChooser.FILES_ONLY, "lastDirectory");
        if (fc == null) return;
        File file = fc.getSelectedFile();
        boolean zip = file.getName().toLowerCase().endsWith(".joz");
        Main.worker.submit(new Loader(file, zip));
    }

    public static class Loader extends PleaseWaitRunnable {

        private boolean canceled;
        private File file;
        private boolean zip;
        private List<Layer> layers;
        private List<Runnable> postLoadTasks;
        private ViewportData viewport;

        public Loader(File file, boolean zip) {
            super(tr("Loading session ''{0}''", file.getName()));
            this.file = file;
            this.zip = zip;
        }

        @Override
        protected void cancel() {
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
                reader.loadSession(file, zip, monitor);
                layers = reader.getLayers();
                postLoadTasks = reader.getPostLoadTasks();
                viewport = reader.getViewport();
            } catch (IllegalDataException e) {
                e.printStackTrace();
                HelpAwareOptionPane.showMessageDialogInEDT(
                        Main.parent,
                        tr("<html>Could not load session file ''{0}''.<br>Error is:<br>{1}</html>", file.getName(), e.getMessage()),
                        tr("Data Error"),
                        JOptionPane.ERROR_MESSAGE,
                        null
                        );
                cancel();
            } catch (IOException e) {
                e.printStackTrace();
                HelpAwareOptionPane.showMessageDialogInEDT(
                        Main.parent,
                        tr("<html>Could not load session file ''{0}''.<br>Error is:<br>{1}</html>", file.getName(), e.getMessage()),
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

