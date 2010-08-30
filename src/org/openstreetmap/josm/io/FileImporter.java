// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public abstract class FileImporter implements Comparable<FileImporter> {

    public final ExtensionFileFilter filter;

    public FileImporter(ExtensionFileFilter filter) {
        this.filter = filter;
    }

    public boolean acceptFile(File pathname) {
        return filter.acceptName(pathname.getName());
    }

    /**
     * A batch importer is a file importer that prefers to read multiple files at the same time.
     */
    public boolean isBatchImporter() {
        return false;
    }

    /**
     * Needs to be implemented if isBatchImporter() returns false.
     * @throws IllegalDataException
     */
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        throw new IOException(tr("Could not import ''{0}''.", file.getName()));
    }

    /**
     * Needs to be implemented if isBatchImporter() returns true.
     * @throws IllegalDataException
     */
    public void importData(List<File> files, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        throw new IOException(tr("Could not import files."));
    }

    /**
     * Wrapper to give meaningful output if things go wrong.
     */
    public void importDataHandleExceptions(File f, ProgressMonitor progressMonitor) {
        try {
            System.out.println("Open file: " + f.getAbsolutePath() + " (" + f.length() + " bytes)");
            importData(f, progressMonitor);
        } catch (Exception e) {
            e.printStackTrace();
            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    tr("<html>Could not read file ''{0}''.<br>Error is:<br>{1}</html>", f.getName(), e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE, null
            );
        }
    }
    public void importDataHandleExceptions(List<File> files, ProgressMonitor progressMonitor) {
        try {
            System.out.println("Open "+files.size()+" files");
            importData(files, progressMonitor);
        } catch (Exception e) {
            e.printStackTrace();
            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    tr("<html>Could not read files.<br>Error is:<br>{0}</html>", e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE, null
            );
        }
    }

    /**
     * If multiple files (with multiple file formats) are selected,
     * they are opened in the order of their priorities.
     * Highest priority comes first.
     */
    public double getPriority() {
        return 0;
    }

    public int compareTo(FileImporter other) {
        return (new Double(this.getPriority())).compareTo(other.getPriority());
    }

}
