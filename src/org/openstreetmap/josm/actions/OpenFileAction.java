// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.importexport.AllFormatsImporter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Open a file chooser dialog and select a file to import.
 *
 * @author imi
 * @since 1146
 */
public class OpenFileAction extends DiskAccessAction {

    /**
     * The {@link ExtensionFileFilter} matching .url files
     */
    public static final ExtensionFileFilter URL_FILE_FILTER = new ExtensionFileFilter("url", "url", tr("URL Files") + " (*.url)");

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenFileAction() {
        super(tr("Open..."), "open", tr("Open a file."),
                Shortcut.registerShortcut("system:open", tr("File: {0}", tr("Open...")), KeyEvent.VK_O, Shortcut.CTRL));
        setHelpId(ht("/Action/Open"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractFileChooser fc = createAndOpenFileChooser(true, true, null);
        if (fc == null)
            return;
        File[] files = fc.getSelectedFiles();
        OpenFileTask task = new OpenFileTask(Arrays.asList(files), fc.getFileFilter());
        task.setRecordHistory(true);
        MainApplication.worker.submit(task);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(true);
    }

    /**
     * Open a list of files. The complete list will be passed to batch importers.
     * Filenames will not be saved in history.
     * @param fileList A list of files
     * @return the future task
     * @since 11986 (return task)
     */
    public static Future<?> openFiles(List<File> fileList) {
        return openFiles(fileList, false);
    }

    /**
     * Open a list of files. The complete list will be passed to batch importers.
     * @param fileList A list of files
     * @param recordHistory {@code true} to save filename in history (default: false)
     * @return the future task
     * @since 11986 (return task)
     */
    public static Future<?> openFiles(List<File> fileList, boolean recordHistory) {
        OpenFileTask task = new OpenFileTask(fileList, null);
        task.setRecordHistory(recordHistory);
        return MainApplication.worker.submit(task);
    }

    /**
     * Task to open files.
     */
    public static class OpenFileTask extends PleaseWaitRunnable {
        private final List<File> files;
        private final List<File> successfullyOpenedFiles = new ArrayList<>();
        private final Set<String> fileHistory = new LinkedHashSet<>();
        private final Set<String> failedAll = new HashSet<>();
        private final FileFilter fileFilter;
        private boolean canceled;
        private boolean recordHistory;

        /**
         * Constructs a new {@code OpenFileTask}.
         * @param files files to open
         * @param fileFilter file filter
         * @param title message for the user
         */
        public OpenFileTask(final List<File> files, final FileFilter fileFilter, final String title) {
            super(title, false /* don't ignore exception */);
            this.fileFilter = fileFilter;
            this.files = new ArrayList<>(files.size());
            for (final File file : files) {
                if (file.exists()) {
                    this.files.add(PlatformManager.getPlatform().resolveFileLink(file));
                } else if (file.getParentFile() != null) {
                    // try to guess an extension using the specified fileFilter
                    final File[] matchingFiles = file.getParentFile().listFiles((dir, name) ->
                            name.startsWith(file.getName()) && fileFilter != null && fileFilter.accept(new File(dir, name)));
                    if (matchingFiles != null && matchingFiles.length == 1) {
                        // use the unique match as filename
                        this.files.add(matchingFiles[0]);
                    } else {
                        // add original filename for error reporting later on
                        this.files.add(file);
                    }
                } else {
                    String message = tr("Unable to locate file  ''{0}''.", file.getPath());
                    Logging.warn(message);
                    new Notification(message).show();
                }
            }
        }

        /**
         * Constructs a new {@code OpenFileTask}.
         * @param files files to open
         * @param fileFilter file filter
         */
        public OpenFileTask(List<File> files, FileFilter fileFilter) {
            this(files, fileFilter, tr("Opening files"));
        }

        /**
         * Sets whether to save filename in history (for list of recently opened files).
         * @param recordHistory {@code true} to save filename in history (default: false)
         */
        public void setRecordHistory(boolean recordHistory) {
            this.recordHistory = recordHistory;
        }

        /**
         * Determines if filename must be saved in history (for list of recently opened files).
         * @return {@code true} if filename must be saved in history
         */
        public boolean isRecordHistory() {
            return recordHistory;
        }

        @Override
        protected void cancel() {
            this.canceled = true;
        }

        @Override
        protected void finish() {
            MapFrame map = MainApplication.getMap();
            if (map != null) {
                map.repaint();
            }
        }

        protected void alertFilesNotMatchingWithImporter(Collection<File> files, FileImporter importer) {
            final StringBuilder msg = new StringBuilder(128).append("<html>").append(
                    trn("Cannot open {0} file with the file importer ''{1}''.",
                        "Cannot open {0} files with the file importer ''{1}''.",
                        files.size(),
                        files.size(),
                        Utils.escapeReservedCharactersHTML(importer.filter.getDescription())
                    )
            ).append("<br><ul>");
            for (File f: files) {
                msg.append("<li>").append(f.getAbsolutePath()).append("</li>");
            }
            msg.append("</ul></html>");

            HelpAwareOptionPane.showMessageDialogInEDT(
                    MainApplication.getMainFrame(),
                    msg.toString(),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE,
                    ht("/Action/Open#ImporterCantImportFiles")
            );
        }

        protected void alertFilesWithUnknownImporter(Collection<File> files) {
            final StringBuilder msg = new StringBuilder(128).append("<html>").append(
                    trn("Cannot open {0} file because file does not exist or no suitable file importer is available.",
                        "Cannot open {0} files because files do not exist or no suitable file importer is available.",
                        files.size(),
                        files.size()
                    )
            ).append("<br><ul>");
            for (File f: files) {
                msg.append("<li>").append(f.getAbsolutePath()).append(" (<i>")
                   .append(f.exists() ? tr("no importer") : tr("does not exist"))
                   .append("</i>)</li>");
            }
            msg.append("</ul></html>");

            HelpAwareOptionPane.showMessageDialogInEDT(
                    MainApplication.getMainFrame(),
                    msg.toString(),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE,
                    ht("/Action/Open#MissingImporterForFiles")
            );
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            if (files == null || files.isEmpty()) return;

            /**
             * Find the importer with the chosen file filter
             */
            FileImporter chosenImporter = null;
            if (fileFilter != null) {
                for (FileImporter importer : ExtensionFileFilter.getImporters()) {
                    if (fileFilter.equals(importer.filter)) {
                        chosenImporter = importer;
                    }
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
                List<File> filesNotMatchingWithImporter = new LinkedList<>();
                List<File> filesMatchingWithImporter = new LinkedList<>();
                for (final File f : files) {
                    if (!chosenImporter.acceptFile(f)) {
                        if (f.isDirectory()) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr(
                                    "<html>Cannot open directory ''{0}''.<br>Please select a file.</html>",
                                    f.getAbsolutePath()), tr("Open file"), JOptionPane.ERROR_MESSAGE));
                            // TODO when changing to Java 6: Don't cancel the task here but use different modality. (Currently 2 dialogs
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
                MultiMap<FileImporter, File> importerMap = new MultiMap<>();
                List<File> filesWithUnknownImporter = new LinkedList<>();
                List<File> urlFiles = new LinkedList<>();
                FILES: for (File f : files) {
                    for (FileImporter importer : ExtensionFileFilter.getImporters()) {
                        if (importer.acceptFile(f)) {
                            importerMap.put(importer, f);
                            continue FILES;
                        }
                    }
                    if (URL_FILE_FILTER.accept(f)) {
                        urlFiles.add(f);
                    } else {
                        filesWithUnknownImporter.add(f);
                    }
                }
                if (!filesWithUnknownImporter.isEmpty()) {
                    alertFilesWithUnknownImporter(filesWithUnknownImporter);
                }
                List<FileImporter> importers = new ArrayList<>(importerMap.keySet());
                Collections.sort(importers);
                Collections.reverse(importers);

                for (FileImporter importer : importers) {
                    importData(importer, new ArrayList<>(importerMap.get(importer)));
                }

                Pattern urlPattern = Pattern.compile(".*(https?://.*)");
                for (File urlFile: urlFiles) {
                    try (BufferedReader reader = Files.newBufferedReader(urlFile.toPath(), StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Matcher m = urlPattern.matcher(line);
                            if (m.matches()) {
                                String url = m.group(1);
                                MainApplication.getMenu().openLocation.openUrl(false, url);
                            }
                        }
                    } catch (IOException | PatternSyntaxException | IllegalStateException | IndexOutOfBoundsException e) {
                        Logging.error(e);
                    }
                }
            }

            if (recordHistory) {
                Collection<String> oldFileHistory = Config.getPref().getList("file-open.history");
                fileHistory.addAll(oldFileHistory);
                // remove the files which failed to load from the list
                fileHistory.removeAll(failedAll);
                int maxsize = Math.max(0, Config.getPref().getInt("file-open.history.max-size", 15));
                PreferencesUtils.putListBounded(Config.getPref(), "file-open.history", maxsize, new ArrayList<>(fileHistory));
            }
        }

        /**
         * Import data files with the given importer.
         * @param importer file importer
         * @param files data files to import
         */
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
            if (recordHistory && !importer.isBatchImporter()) {
                for (File f : files) {
                    try {
                        if (successfullyOpenedFiles.contains(f)) {
                            fileHistory.add(f.getCanonicalPath());
                        } else {
                            failedAll.add(f.getCanonicalPath());
                        }
                    } catch (IOException e) {
                        Logging.warn(e);
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
