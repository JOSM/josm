// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.AllFormatsImporter;
import org.openstreetmap.josm.io.FileImporter;
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
        putValue("help", ht("/Action/Open"));

    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = createAndOpenFileChooser(true, true, null);
        if (fc == null)
            return;
        File[] files = fc.getSelectedFiles();
        OpenFileTask task = new OpenFileTask(Arrays.asList(files), fc.getFileFilter());
        task.setRecordHistory(true);
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
        private List<File> successfullyOpenedFiles = new ArrayList<File>();
        private FileFilter fileFilter;
        private boolean cancelled;
        private boolean recordHistory = false;

        public OpenFileTask(List<File> files, FileFilter fileFilter, String title) {
            super(title, false /* don't ignore exception */);
            this.files = new ArrayList<File>(files);
            this.fileFilter = fileFilter;
        }

        public OpenFileTask(List<File> files, FileFilter fileFilter) {
            this(files, fileFilter, tr("Opening files"));
        }

        /**
         * save filename in history (for list of recently opened files)
         * default: false
         */
        public void setRecordHistory(boolean recordHistory) {
            this.recordHistory = recordHistory;
        }

        public boolean isRecordHistory() {
            return recordHistory;
        }

        @Override
        protected void cancel() {
            this.cancelled = true;
        }

        @Override
        protected void finish() {
            // do nothing
        }

        protected void alertFilesNotMatchingWithImporter(Collection<File> files, FileImporter importer) {
            final StringBuffer msg = new StringBuffer();
            msg.append("<html>");
            msg.append(
                    trn(
                            "Cannot open {0} file with the file importer ''{1}''.",
                            "Cannot open {0} files with the file importer ''{1}''.",
                            files.size(),
                            files.size(),
                            importer.filter.getDescription()
                    )
            ).append("<br>");
            msg.append("<ul>");
            for (File f: files) {
                msg.append("<li>").append(f.getAbsolutePath()).append("</li>");
            }
            msg.append("</ul>");

            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    msg.toString(),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Action/OpenFile#ImporterCantImportFiles")
            );
        }

        protected void alertFilesWithUnknownImporter(Collection<File> files) {
            final StringBuffer msg = new StringBuffer();
            msg.append("<html>");
            msg.append(
                    trn(
                            "Cannot open {0} file because no suitable file importer is available.",
                            "Cannot open {0} files because no suitable file importer is available.",
                            files.size(),
                            files.size()
                    )
            ).append("<br>");
            msg.append("<ul>");
            for (File f: files) {
                msg.append("<li>").append(f.getAbsolutePath()).append("</li>");
            }
            msg.append("</ul>");

            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    msg.toString(),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Action/OpenFile#MissingImporterForFiles")
            );
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
             * If the filter hasn't been changed in the dialog, chosenImporter is null now.
             * When the filter has been set explicitly to AllFormatsImporter, treat this the same.
             */
            if (chosenImporter instanceof AllFormatsImporter) {
                chosenImporter = null;
            }
            getProgressMonitor().setTicksCount(files.size());

            if (chosenImporter != null) {
                // The importer was expicitely chosen, so use it.
                List<File> filesNotMatchingWithImporter = new LinkedList<File>();
                List<File> filesMatchingWithImporter = new LinkedList<File>();
                for (final File f : files) {
                    if (!chosenImporter.acceptFile(f)) {
                        if (f.isDirectory()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    JOptionPane.showMessageDialog(Main.parent, tr(
                                            "<html>Cannot open directory ''{0}''.<br>Please select a file.</html>",
                                            f.getAbsolutePath()), tr("Open file"), JOptionPane.ERROR_MESSAGE);
                                }
                            });
                            // TODO when changing to Java 6: Don't cancel the
                            // task here but use different modality. (Currently 2 dialogs
                            // would block each other.)
                            return;
                        } else {
                            filesNotMatchingWithImporter.add(f);
                        }
                    } else {
                        filesMatchingWithImporter.add(f);
                    }
                }

                if (!filesNotMatchingWithImporter.isEmpty()) {
                    alertFilesNotMatchingWithImporter(filesNotMatchingWithImporter, chosenImporter);
                }
                if (!filesMatchingWithImporter.isEmpty()) {
                    importData(chosenImporter, filesMatchingWithImporter);
                }
            } else {
                // find appropriate importer
                MultiMap<FileImporter, File> map = new MultiMap<FileImporter, File>();
                List<File> filesWithUnknownImporter = new LinkedList<File>();
                FILES: for (File f : files) {
                    for (FileImporter importer : ExtensionFileFilter.importers) {
                        if (importer.acceptFile(f)) {
                            map.put(importer, f);
                            continue FILES;
                        }
                    }
                    filesWithUnknownImporter.add(f);
                }
                if (!filesWithUnknownImporter.isEmpty()) {
                    alertFilesWithUnknownImporter(filesWithUnknownImporter);
                }
                List<FileImporter> ims = new ArrayList<FileImporter>(map.keySet());
                Collections.sort(ims);
                Collections.reverse(ims);

                Set<String> fileHistory = new LinkedHashSet<String>();

                for (FileImporter importer : ims) {
                    List<File> files = new ArrayList<File>(map.get(importer));
                    importData(importer, files);

                    if (recordHistory && !importer.isBatchImporter()) {
                        for (File f : files) {
                            fileHistory.add(f.getPath());
                        }
                    }
                }

                if (recordHistory) {
                    Collection<String> oldFileHistory = Main.pref.getCollection("file-open.history");
                    fileHistory.addAll(oldFileHistory);
                    int maxsize = Math.max(0, Main.pref.getInteger("file-open.history.max-size", 15));
                    Collection<String> trimmedFileHistory = new ArrayList<String>(Math.min(maxsize, fileHistory.size()));
                    int i = 0;
                    for (String s : fileHistory) {
                        if (++i > maxsize) {
                            break;
                        }
                        trimmedFileHistory.add(s);
                    }
                    Main.pref.putCollection("file-open.history", trimmedFileHistory);
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
                    msg = trn("Opening {0} file...", "Opening {0} files...", files.size(), files.size());
                }
                getProgressMonitor().setCustomText(msg);
                getProgressMonitor().indeterminateSubTask(msg);
                if (importer.importDataHandleExceptions(files, getProgressMonitor().createSubTaskMonitor(files.size(), false))) {
                    successfullyOpenedFiles.addAll(files);
                }
            } else {
                for (File f : files) {
                    if (cancelled) return;
                    getProgressMonitor().indeterminateSubTask(tr("Opening file ''{0}'' ...", f.getAbsolutePath()));
                    if (importer.importDataHandleExceptions(f, getProgressMonitor().createSubTaskMonitor(1, false))) {
                        successfullyOpenedFiles.add(f);
                    }
                }
            }
        }

        public List<File> getSuccessfullyOpenedFiles() {
            return successfullyOpenedFiles;
        }
    }
}
