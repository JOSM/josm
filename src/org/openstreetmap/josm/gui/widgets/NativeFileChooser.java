// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Utils;

/**
 * File chooser based on the AWT's {@link FileDialog} implementation,
 * which looks like more a native file chooser than the Swing implementation.
 * @since 7578
 */
public class NativeFileChooser extends AbstractFileChooser {

    /** The instance of the fileDialog */
    private final FileDialog fileDialog;
    private FileFilter fileFilter;
    private final List<FileFilter> fileFilters = new ArrayList<>();
    private int selectionMode;

    /**
     * Constructs a new {@code NativeFileChooser}.
     * @param file the current file/directory to point to
     */
    public NativeFileChooser(File file) {
        fileDialog = new FileDialog(MainApplication.getMainFrame());
        if (file != null) {
            fileDialog.setDirectory(file.getAbsolutePath());
            if (file.isFile()) {
                fileDialog.setFile(file.toString());
            }
        }
    }

    @Override
    public void addChoosableFileFilter(FileFilter filter) {
        // TODO implement this after Oracle fixes JDK-4811090 / JDK-6192906
        // https://bugs.openjdk.java.net/browse/JDK-4811090 : Extend awt filedialog
        // https://bugs.openjdk.java.net/browse/JDK-6192906 : Add more features to java.awt.FileDialog
        fileFilters.add(filter);
    }

    @Override
    public FileFilter[] getChoosableFileFilters() {
        // TODO implement this after Oracle fixes JDK-4811090 / JDK-6192906
        // https://bugs.openjdk.java.net/browse/JDK-4811090 : Extend awt filedialog
        // https://bugs.openjdk.java.net/browse/JDK-6192906 : Add more features to java.awt.FileDialog
        return fileFilters.toArray(new FileFilter[0]);
    }

    @Override
    public File getCurrentDirectory() {
        return new File(fileDialog.getDirectory());
    }

    @Override
    public FileFilter getFileFilter() {
        return fileFilter;
    }

    @Override
    public File getSelectedFile() {
        return new File(fileDialog.getDirectory() + fileDialog.getFile());
    }

    @Override
    public File[] getSelectedFiles() {
        return fileDialog.getFiles();
    }

    @Override
    public boolean isMultiSelectionEnabled() {
        return fileDialog.isMultipleMode();
    }

    @Override
    public void setAcceptAllFileFilterUsed(boolean b) {
        // TODO implement this after Oracle fixes JDK-4811090 / JDK-6192906
        // https://bugs.openjdk.java.net/browse/JDK-4811090 : Extend awt filedialog
        // https://bugs.openjdk.java.net/browse/JDK-6192906 : Add more features to java.awt.FileDialog
    }

    @Override
    public void setCurrentDirectory(File f) {
        fileDialog.setDirectory(f.toString());
    }

    @Override
    public void setDialogTitle(String title) {
        fileDialog.setTitle(title);
    }

    @Override
    public void setFileFilter(final FileFilter cff) {
        FilenameFilter filter = (directory, fileName) -> cff.accept(new File(directory.getAbsolutePath() + fileName));
        fileDialog.setFilenameFilter(filter);
        fileFilter = cff;
    }

    @Override
    public void setFileSelectionMode(int selectionMode) {
        // CHECKSTYLE.OFF: LineLength
        // TODO implement this after Oracle fixes JDK-6192906 / JDK-6699863 / JDK-6927978 / JDK-7125172:
        // https://bugs.openjdk.java.net/browse/JDK-6192906 : Add more features to java.awt.FileDialog
        // https://bugs.openjdk.java.net/browse/JDK-6699863 : awt filedialog cannot select directories
        // https://bugs.openjdk.java.net/browse/JDK-6927978 : Directory Selection standard dialog support
        // https://bugs.openjdk.java.net/browse/JDK-7125172 : FileDialog objects don't allow directory AND files selection simultaneously

        // There is however a basic support for directory selection on OS X, with Java >= 7u40:
        // http://stackoverflow.com/questions/1224714/how-can-i-make-a-java-filedialog-accept-directories-as-its-filetype-in-os-x/1224744#1224744
        // https://bugs.openjdk.java.net/browse/JDK-7161437 : [macosx] awt.FileDialog doesn't respond appropriately for mac when selecting folders
        // CHECKSTYLE.ON: LineLength
        this.selectionMode = selectionMode;
    }

    @Override
    public void setMultiSelectionEnabled(boolean multiple) {
        fileDialog.setMultipleMode(multiple);
    }

    @Override
    public void setSelectedFile(File file) {
        if (file == null) return;
        fileDialog.setDirectory(file.getParent());
        fileDialog.setFile(file.getName());
    }

    @Override
    public int showOpenDialog(Component parent) {
        boolean appleProperty = PlatformManager.isPlatformOsx() && selectionMode == JFileChooser.DIRECTORIES_ONLY;
        if (appleProperty) {
            Utils.updateSystemProperty("apple.awt.fileDialogForDirectories", "true");
        }
        try {
            fileDialog.setLocale(locale);
            fileDialog.setMode(FileDialog.LOAD);
            fileDialog.setVisible(true);
            return fileDialog.getFile() == null ? JFileChooser.CANCEL_OPTION : JFileChooser.APPROVE_OPTION;
        } finally {
            if (appleProperty) {
                Utils.updateSystemProperty("apple.awt.fileDialogForDirectories", "false");
            }
        }
    }

    @Override
    public int showSaveDialog(Component parent) {
        fileDialog.setLocale(locale);
        fileDialog.setMode(FileDialog.SAVE);
        fileDialog.setVisible(true);
        return fileDialog.getFile() == null ? JFileChooser.CANCEL_OPTION : JFileChooser.APPROVE_OPTION;
    }

    /**
     * Determines if the selection mode is suuported by the native file chooser.
     * @param selectionMode the selection mode
     * @return {@code true} if the selection mode is supported, {@code false} otherwise
     */
    public static boolean supportsSelectionMode(int selectionMode) {
        switch (selectionMode) {
        case JFileChooser.FILES_AND_DIRECTORIES:
            // CHECKSTYLE.OFF: LineLength
            // https://bugs.openjdk.java.net/browse/JDK-7125172 : FileDialog objects don't allow directory AND files selection simultaneously
            return false;
        case JFileChooser.DIRECTORIES_ONLY:
            // http://stackoverflow.com/questions/1224714/how-can-i-make-a-java-filedialog-accept-directories-as-its-filetype-in-os-x/1224744#1224744
            // CHECKSTYLE.ON: LineLength
            return PlatformManager.isPlatformOsx();
        case JFileChooser.FILES_ONLY:
        default:
            return true;
        }
    }
}
