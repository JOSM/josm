// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Open a file chooser dialog and select an file to import. Then call the gpx-import driver. Finally
 * open an internal frame into the main window with the gpx data shown.
 *
 * @author imi
 */
public class OpenFileAction extends DiskAccessAction {

    /**
     * The {@link ExtensionFileFilter} matching .url files
     */
    public static final ExtensionFileFilter urlFileFilter = new ExtensionFileFilter("url", "url", tr("URL Files") + " (*.url)");

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenFileAction() {
        super(tr("Open..."), "open", tr("Open a file."),
                Shortcut.registerShortcut("system:open", tr("File: {0}", tr("Open...")), KeyEvent.VK_O, Shortcut.CTRL));
        putValue("help", ht("/Action/Open"));
    }

    @Override
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
        openFiles(fileList, false);
    }

    static public void openFiles(List<File> fileList, boolean recordHistory) {
        OpenFileTask task = new OpenFileTask(fileList, null);
        task.setRecordHistory(recordHistory);
        Main.worker.submit(task);
    }

    static public class OpenFileTask extends PleaseWaitRunnable {
        private List<File> files;
        private List<File> successfullyOpenedFiles = new ArrayList<File>();
        private FileFilter fileFilter;
        private boolean canceled;
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
            this.canceled = true;
        }

        @Override
        protected void finish() {
            // do nothing
        }

        protected void alertFilesNotMatchingWithImporter(Collection<File> files, FileImporter importer) {
            final StringBuilder msg = new StringBuilder();
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
                    HelpUtil.ht("/Action/Open#ImporterCantImportFiles")
            );
        }

        protected void alertFilesWithUnknownImporter(Collection<File> files) {
            final StringBuilder msg = new StringBuilder();
            msg.append("<html>");
            msg.append(
                    trn(
                            "Cannot open {0} file because file does not exist or no suitable file importer is available.",
                            "Cannot open {0} files because files do not exist or no suitable file importer is available.",
                            files.size(),
                            files.size()
                    )
            ).append("<br>");
            msg.append("<ul>");
            for (File f: files) {
                msg.append("<li>");
                msg.append(f.getAbsolutePath());
                msg.append(" (<i>");
                msg.append(f.exists() ? tr("no importer") : tr("does not exist"));
                msg.append("</i>)</li>");
            }
            msg.append("</ul>");

            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    msg.toString(),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Action/Open#MissingImporterForFiles")
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
                // The importer was explicitly chosen, so use it.
                List<File> filesNotMatchingWithImporter = new LinkedList<File>();
                List<File> filesMatchingWithImporter = new LinkedList<File>();
                for (final File f : files) {
                    if (!chosenImporter.acceptFile(f)) {
                        if (f.isDirectory()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
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
                MultiMap<FileImporter, File> importerMap = new MultiMap<FileImporter, File>();
                List<File> filesWithUnknownImporter = new LinkedList<File>();
                List<File> urlFiles = new LinkedList<File>();
                FILES: for (File f : files) {
                    for (FileImporter importer : ExtensionFileFilter.importers) {
                        if (importer.acceptFile(f)) {
                            importerMap.put(importer, f);
                            continue FILES;
                        }
                    }
                    if (urlFileFilter.accept(f)) {
                        urlFiles.add(f);
                    } else {
                        filesWithUnknownImporter.add(f);
                    }
                }
                if (!filesWithUnknownImporter.isEmpty()) {
                    alertFilesWithUnknownImporter(filesWithUnknownImporter);
                }
                List<FileImporter> importers = new ArrayList<FileImporter>(importerMap.keySet());
                Collections.sort(importers);
                Collections.reverse(importers);

                Set<String> fileHistory = new LinkedHashSet<String>();
                Set<String> failedAll = new HashSet<String>();

                for (FileImporter importer : importers) {
                    List<File> files = new ArrayList<File>(importerMap.get(importer));
                    importData(importer, files);
                    // suppose all files will fail to load
                    List<File> failedFiles = new ArrayList<File>(files);

                    if (recordHistory && !importer.isBatchImporter()) {
                        // remove the files which didn't fail to load from the failed list
                        failedFiles.removeAll(successfullyOpenedFiles);
                        for (File f : successfullyOpenedFiles) {
                            fileHistory.add(f.getCanonicalPath());
                        }
                        for (File f : failedFiles) {
                            failedAll.add(f.getCanonicalPath());
                        }
                    }
                }

                for (File urlFile: urlFiles) {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(urlFile));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Matcher m = Pattern.compile(".*(http://.*)").matcher(line);
                            if (m.matches()) {
                                String url = m.group(1);
                                Main.main.menu.openLocation.openUrl(false, url);
                            }
                        }
                        Utils.close(reader);
                    } catch (Exception e) {
                        Main.error(e);
                    }
                }

                if (recordHistory) {
                    Collection<String> oldFileHistory = Main.pref.getCollection("file-open.history");
                    fileHistory.addAll(oldFileHistory);
                    // remove the files which failed to load from the list
                    fileHistory.removeAll(failedAll);
                    int maxsize = Math.max(0, Main.pref.getInteger("file-open.history.max-size", 15));
                    Main.pref.putCollectionBounded("file-open.history", maxsize, fileHistory);
                }
            }
        }

        public void importData(FileImporter importer, List<File> files) {
            if (importer.isBatchImporter()) {
                if (canceled) return;
                String msg = trn("Opening {0} file...", "Opening {0} files...", files.size(), files.size());
                getProgressMonitor().setCustomText(msg);
                getProgressMonitor().indeterminateSubTask(msg);
                if (importer.importDataHandleExceptions(files, getProgressMonitor().createSubTaskMonitor(files.size(), false))) {
                    successfullyOpenedFiles.addAll(files);
                }
            } else {
                for (File f : files) {
                    if (canceled) return;
                    getProgressMonitor().indeterminateSubTask(tr("Opening file ''{0}'' ...", f.getAbsolutePath()));
                    if (importer.importDataHandleExceptions(f, getProgressMonitor().createSubTaskMonitor(1, false))) {
                        successfullyOpenedFiles.add(f);
                    }
                }
            }
        }

        /**
         * Replies the list of files that have been successfully opened.
         * @return The list of files that have been successfully opened.
         */
        public List<File> getSuccessfullyOpenedFiles() {
            return successfullyOpenedFiles;
        }
    }
}
