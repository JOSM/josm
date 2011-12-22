// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionReader;

public class SessionLoadAction extends JosmAction {
    public SessionLoadAction() {
        super(tr("Load Session"), "open", tr("Load a session from file."), null, false);
        putValue("help", ht("/Action/SessionLoad"));
    }

    public void actionPerformed(ActionEvent e) {
        String curDir = Main.pref.get("lastDirectory");
        if (curDir.equals("")) {
            curDir = ".";
        }
        JFileChooser fc = new JFileChooser(new File(curDir));
        fc.setDialogTitle(tr("Open session"));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(true);
        FileFilter ff = new ExtensionFileFilter("jos,joz", "jos", tr("Session file (*.jos, *.joz)"));
        fc.addChoosableFileFilter(ff);
        int answer = fc.showOpenDialog(Main.parent);
        if (answer != JFileChooser.APPROVE_OPTION)
            return;

        if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir)) {
            Main.pref.put("lastDirectory", fc.getCurrentDirectory().getAbsolutePath());
        }
        File file = fc.getSelectedFile();
        boolean zip = true;
        if (file.getName().toLowerCase().endsWith(".jos")) {
            zip = false;
        }
        Main.worker.submit(new Loader(file, zip));
    }

    public class Loader extends PleaseWaitRunnable {

        private boolean canceled;
        private File file;
        private boolean zip;
        private List<Layer> layers;
        private List<Runnable> postLoadTasks;

        public Loader(File file, boolean zip) {
            super(tr("Loading session ''{0}''", file.getName()));
            this.file = file;
            this.zip = zip;
        }

        @Override
        protected void cancel() {
            Thread.currentThread().dumpStack();
            canceled = true;
        }

        @Override
        protected void finish() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (canceled) return;
                    for (Layer l : layers) {
                        if (canceled) return;
                        Main.main.addLayer(l);
                    }
                    for (Runnable task : postLoadTasks) {
                        if (canceled) return;
                        if (task == null) continue;
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

