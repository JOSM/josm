// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.AllFormatsImporter;
import org.openstreetmap.josm.io.FileImporter;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.MultiMap;
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
        OpenFileTask task = new OpenFileTask(Arrays.asList(files), fc.getFileFilter());
        Main.worker.submit(task);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(! Main.applet);
    }

    /**
     * Open a list of files. The complete list will be passed to batch importers.
     * @param fileList A list of files
     */
    static public void openFiles(List<File> fileList) {
        OpenFileTask task = new OpenFileTask(fileList, null);
        Main.worker.submit(task);
    }

    static public class OpenFileTask extends PleaseWaitRunnable {
        private List<File> files;
        private FileFilter fileFilter;
        private boolean cancelled;

        public OpenFileTask(List<File> files, FileFilter fileFilter) {
            super(tr("Opening files"), false /* don't ignore exception */);
            this.files = new ArrayList<File>(files);
            this.fileFilter = fileFilter;
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

            /**
             * Find the importer with the chosen file filter
             */
            FileImporter chosenImporter = null;
            for (FileImporter importer : ExtensionFileFilter.importers) {
                if (fileFilter == importer.filter) {
                    chosenImporter = importer;
                }
            }
            /**
             * If the filter wasn't changed in the dialog, chosenImporter is null now.
             * When the filter was explicitly set to AllFormatsImporter, treat this the same.
             */
            if (chosenImporter instanceof AllFormatsImporter) {
                chosenImporter = null;
            }
            getProgressMonitor().setTicks(files.size());

            if (chosenImporter != null) { // The importer was expicitely chosen, so use it.
                //System.err.println("Importer: " +chosenImporter.getClass().getName());
                for (File f : files) {
                    if (!chosenImporter.acceptFile(f)) {
                        if (f.isDirectory()) {
                            JOptionPane.showMessageDialog(
                                    Main.parent,
                                    tr("<html>Cannot open directory.<br>Please select a file!"),
                                    tr("Open file"),
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            return;
                        } else
                            throw new IllegalStateException();
                    }
                }
                importData(chosenImporter, files);
            }
            else {    // find apropriate importer
                MultiMap<FileImporter, File> map = new MultiMap<FileImporter, File>();
                while (! files.isEmpty()) {
                    File f = files.get(0);
                    for (FileImporter importer : ExtensionFileFilter.importers) {
                        if (importer.acceptFile(f)) {
                            map.add(importer, f);
                            files.remove(f);
                        }
                    }
                    if (files.contains(f))
                        throw new RuntimeException(); // no importer found
                }
                List<FileImporter> ims = new ArrayList<FileImporter>(map.keySet());
                Collections.sort(ims);
                Collections.reverse(ims);
                for (FileImporter importer : ims) {
                    //System.err.println("Using "+importer.getClass().getName());
                    List<File> files = map.get(importer);
                    //System.err.println("for files: "+files);
                    importData(importer, files);
                }
            }
        }

        public void importData(FileImporter importer, List<File> files) {
            if (importer.isBatchImporter()) {
                if (cancelled) return;
                String msg;
                if (files.size() == 1) {
                    msg = tr("Opening 1 file...");
                } else {
                    msg = tr("Opening {0} files...", files.size());
                }
                getProgressMonitor().indeterminateSubTask(msg);
                importer.importDataHandleExceptions(files);
                getProgressMonitor().worked(files.size());
            } else {
                for (File f : files) {
                    if (cancelled) return;
                    getProgressMonitor().indeterminateSubTask(tr("Opening file ''{0}'' ...", f.getAbsolutePath()));
                    importer.importDataHandleExceptions(f);
                    getProgressMonitor().worked(1);
                }
            }
        }
    }
}
