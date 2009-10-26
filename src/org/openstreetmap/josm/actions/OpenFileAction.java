// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.FileImporter;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * Open a file chooser dialog and select an file to import. Then call the gpx-import driver. Finally
 * open an internal frame into the main window with the gpx data shown.
 *
 * @author imi
 */
public class OpenFileAction extends DiskAccessAction {

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenFileAction() {
        super(tr("Open..."), "open", tr("Open a file."),
                Shortcut.registerShortcut("system:open", tr("File: {0}", tr("Open...")), KeyEvent.VK_O, Shortcut.GROUP_MENU));
        putValue("help", ht("/Action/OpenFile"));
        
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = createAndOpenFileChooser(true, true, null);
        if (fc == null)
            return;
        File[] files = fc.getSelectedFiles();
        OpenFileTask task = new OpenFileTask(Arrays.asList(files));
        Main.worker.submit(task);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(! Main.applet);
    }

    static public void openFile(File f) throws IOException, IllegalDataException {
        for (FileImporter importer : ExtensionFileFilter.importers)
            if (importer.acceptFile(f)) {
                importer.importData(f);
            }
    }

    static public class OpenFileTask extends PleaseWaitRunnable {
        private List<File> files;
        private boolean cancelled;

        public OpenFileTask(List<File> files) {
            super(tr("Opening files"), false /* don't ignore exception */);
            this.files = files;
        }
        @Override
        protected void cancel() {
            this.cancelled = true;
        }

        @Override
        protected void finish() {
            // do nothing
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            if (files == null || files.isEmpty()) return;
            getProgressMonitor().setTicks(files.size());
            for (File f : files) {
                if (cancelled) return;
                getProgressMonitor().subTask(tr("Opening file ''{0}'' ...", f.getAbsolutePath()));
                try {
                    System.out.println("Open file: " + f.getAbsolutePath() + " (" + f.length() + " bytes)");
                    openFile(f);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("<html>Could not read file ''{0}\''.<br> Error is: <br>{1}</html>", f.getName(), e.getMessage()),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                getProgressMonitor().worked(1);
            }
        }
    }
}