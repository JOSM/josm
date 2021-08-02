// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.io.File;

import javax.swing.ActionMap;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * File chooser based on the Swing's {@link JFileChooser} implementation.
 * @since 7578
 */
public class SwingFileChooser extends AbstractFileChooser {

    /** The JFileChooser which we use for this. */
    private final JFileChooser jFileChooser;

    /**
     * Constructs a new {@code SwingFileChooser}.
     * @param file the current file/directory to point to
     */
    public SwingFileChooser(File file) {
        jFileChooser = new JFileChooser(file);
    }

    @Override
    public void addChoosableFileFilter(FileFilter filter) {
        jFileChooser.addChoosableFileFilter(filter);
    }

    @Override
    public FileFilter[] getChoosableFileFilters() {
        return jFileChooser.getChoosableFileFilters();
    }

    @Override
    public File getCurrentDirectory() {
        return jFileChooser.getCurrentDirectory();
    }

    @Override
    public FileFilter getFileFilter() {
        return jFileChooser.getFileFilter();
    }

    @Override
    public File getSelectedFile() {
        return jFileChooser.getSelectedFile();
    }

    @Override
    public File[] getSelectedFiles() {
        return jFileChooser.getSelectedFiles();
    }

    @Override
    public boolean isMultiSelectionEnabled() {
        return jFileChooser.isMultiSelectionEnabled();
    }

    @Override
    public void setAcceptAllFileFilterUsed(boolean b) {
        jFileChooser.setAcceptAllFileFilterUsed(b);
    }

    @Override
    public void setCurrentDirectory(File f) {
        jFileChooser.setCurrentDirectory(f);
    }

    @Override
    public void setDialogTitle(String title) {
        jFileChooser.setDialogTitle(title);
    }

    @Override
    public void setFileFilter(FileFilter cff) {
        jFileChooser.setFileFilter(cff);
    }

    @Override
    public void setFileSelectionMode(int selectionMode) {
        jFileChooser.setFileSelectionMode(selectionMode);
    }

    @Override
    public void setMultiSelectionEnabled(boolean multiple) {
        jFileChooser.setMultiSelectionEnabled(multiple);
    }

    @Override
    public void setSelectedFile(File file) {
        jFileChooser.setSelectedFile(file);
    }

    @Override
    public int showOpenDialog(Component parent) {
        jFileChooser.setLocale(locale);
        return jFileChooser.showOpenDialog(parent);
    }

    @Override
    public int showSaveDialog(Component parent) {
        jFileChooser.setLocale(locale);
        return jFileChooser.showSaveDialog(parent);
    }

    @Override
    public ActionMap getActionMap() {
        return jFileChooser.getActionMap();
    }
}
