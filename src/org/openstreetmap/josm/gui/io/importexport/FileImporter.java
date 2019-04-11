// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.ImportCancelException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * Abstract file importer.
 * @since 1637
 * @since 10386 (signature)
 */
public abstract class FileImporter implements Comparable<FileImporter> {

    /**
     * The extension file filter used to accept files.
     */
    public final ExtensionFileFilter filter;

    private boolean enabled;

    /**
     * Constructs a new {@code FileImporter} with the given extension file filter.
     * @param filter The extension file filter
     */
    public FileImporter(ExtensionFileFilter filter) {
        this.filter = filter;
        this.enabled = true;
    }

    /**
     * Determines if this file importer accepts the given file.
     * @param pathname The file to test
     * @return {@code true} if this file importer accepts the given file, {@code false} otherwise
     */
    public boolean acceptFile(File pathname) {
        return filter.acceptName(pathname.getName());
    }

    /**
     * A batch importer is a file importer that prefers to read multiple files at the same time.
     * @return {@code true} if this importer is a batch importer
     */
    public boolean isBatchImporter() {
        return false;
    }

    /**
     * Needs to be implemented if isBatchImporter() returns false.
     * @param file file to import
     * @param progressMonitor progress monitor
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if invalid data is read
     */
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        throw new IOException(tr("Could not import ''{0}''.", file.getName()));
    }

    /**
     * Needs to be implemented if isBatchImporter() returns true.
     * @param files files to import
     * @param progressMonitor progress monitor
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if invalid data is read
     */
    public void importData(List<File> files, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        throw new IOException(tr("Could not import files."));
    }

    /**
     * Wrapper to {@link #importData(File, ProgressMonitor)} to give meaningful output if things go wrong.
     * @param f data file to import
     * @param progressMonitor progress monitor
     * @return true if data import was successful
     */
    public boolean importDataHandleExceptions(File f, ProgressMonitor progressMonitor) {
        try {
            Logging.info("Open file: " + f.getAbsolutePath() + " (" + f.length() + " bytes)");
            importData(f, progressMonitor);
            return true;
        } catch (IllegalDataException | IllegalStateException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ImportCancelException) {
                displayCancel(cause);
            } else {
                displayError(f, e);
            }
            return false;
        } catch (IOException e) {
            displayError(f, e);
            return false;
        } catch (RuntimeException | LinkageError e) { // NOPMD
            BugReportExceptionHandler.handleException(e);
            return false;
        }
    }

    private static void displayError(File f, Exception e) {
        Logging.error(e);
        HelpAwareOptionPane.showMessageDialogInEDT(
                MainApplication.getMainFrame(),
                tr("<html>Could not read file ''{0}''.<br>Error is:<br>{1}</html>",
                        f.getName(), Utils.escapeReservedCharactersHTML(e.getMessage())),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE, null
        );
    }

    private static void displayCancel(final Throwable t) {
        GuiHelper.runInEDTAndWait(() -> {
            Notification note = new Notification(t.getMessage());
            note.setIcon(JOptionPane.INFORMATION_MESSAGE);
            note.setDuration(Notification.TIME_SHORT);
            note.show();
        });
    }

    /**
     * Wrapper to {@link #importData(List, ProgressMonitor)} to give meaningful output if things go wrong.
     * @param files data files to import
     * @param progressMonitor progress monitor
     * @return true if data import was successful
     */
    public boolean importDataHandleExceptions(List<File> files, ProgressMonitor progressMonitor) {
        try {
            Logging.info("Open "+files.size()+" files");
            importData(files, progressMonitor);
            return true;
        } catch (IOException | IllegalDataException e) {
            Logging.error(e);
            HelpAwareOptionPane.showMessageDialogInEDT(
                    MainApplication.getMainFrame(),
                    tr("<html>Could not read files.<br>Error is:<br>{0}</html>", Utils.escapeReservedCharactersHTML(e.getMessage())),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE, null
            );
            return false;
        }
    }

    /**
     * If multiple files (with multiple file formats) are selected,
     * they are opened in the order of their priorities.
     * Highest priority comes first.
     * @return priority
     */
    public double getPriority() {
        return 0;
    }

    @Override
    public int compareTo(FileImporter other) {
        return Double.compare(this.getPriority(), other.getPriority());
    }

    /**
     * Returns the enabled state of this {@code FileImporter}. When enabled, it is listed and usable in "File-&gt;Open" dialog.
     * @return true if this {@code FileImporter} is enabled
     * @since 5459
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled state of the {@code FileImporter}. When enabled, it is listed and usable in "File-&gt;Open" dialog.
     * @param enabled true to enable this {@code FileImporter}, false to disable it
     * @since 5459
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
