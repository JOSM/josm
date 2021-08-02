// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.util.Locale;

import javax.swing.ActionMap;
import javax.swing.filechooser.FileFilter;

/**
 * Abstract class to allow different file chooser implementations.
 * @since 7578
 */
public abstract class AbstractFileChooser {

    /** The locale for both implementations */
    protected static volatile Locale locale;

    /**
     * Sets the default locale for all implementations.
     * @param l locale
     */
    public static void setDefaultLocale(Locale l) {
        locale = l;
    }

    /**
     * Adds a filter to the list of user choosable file filters.
     * For information on setting the file selection mode, see
     * {@link #setFileSelectionMode setFileSelectionMode}.
     *
     * @param filter the <code>FileFilter</code> to add to the choosable file
     *               filter list
     *
     * @see #getChoosableFileFilters
     * @see #setFileSelectionMode
     */
    public abstract void addChoosableFileFilter(FileFilter filter);

    /**
     * Gets the list of user choosable file filters.
     *
     * @return a <code>FileFilter</code> array containing all the choosable
     *         file filters
     *
     * @see #addChoosableFileFilter
     */
    public abstract FileFilter[] getChoosableFileFilters();

    /**
     * Returns the current directory.
     *
     * @return the current directory
     * @see #setCurrentDirectory
     */
    public abstract File getCurrentDirectory();

    /**
     * Returns the currently selected file filter.
     *
     * @return the current file filter
     * @see #setFileFilter
     * @see #addChoosableFileFilter
     */
    public abstract FileFilter getFileFilter();

    /**
     * Returns the selected file. This can be set either by the
     * programmer via <code>setSelectedFile</code> or by a user action, such as
     * either typing the filename into the UI or selecting the
     * file from a list in the UI.
     *
     * @return the selected file
     * @see #setSelectedFile
     */
    public abstract File getSelectedFile();

    /**
     * Returns a list of selected files if the file chooser is
     * set to allow multiple selection.
     * @return a list of selected files if the file chooser is
     * set to allow multiple selection, or an empty array otherwise.
     */
    public abstract File[] getSelectedFiles();

    /**
     * Returns true if multiple files can be selected.
     * @return true if multiple files can be selected
     * @see #setMultiSelectionEnabled
     */
    public abstract boolean isMultiSelectionEnabled();

    /**
     * Determines whether the <code>AcceptAll FileFilter</code> is used
     * as an available choice in the choosable filter list.
     * If false, the <code>AcceptAll</code> file filter is removed from
     * the list of available file filters.
     * If true, the <code>AcceptAll</code> file filter will become the
     * the actively used file filter.
     * @param b whether the <code>AcceptAll FileFilter</code> is used
     * as an available choice in the choosable filter list
     *
     * @see #setFileFilter
     */
    public abstract void setAcceptAllFileFilterUsed(boolean b);

    /**
     * Sets the current directory. Passing in <code>null</code> sets the
     * file chooser to point to the user's default directory.
     * This default depends on the operating system. It is
     * typically the "My Documents" folder on Windows, and the user's
     * home directory on Unix.
     *
     * If the file passed in as <code>currentDirectory</code> is not a
     * directory, the parent of the file will be used as the currentDirectory.
     * If the parent is not traversable, then it will walk up the parent tree
     * until it finds a traversable directory, or hits the root of the
     * file system.
     *
     * @param dir the current directory to point to
     * @see #getCurrentDirectory
     */
    public abstract void setCurrentDirectory(File dir);

    /**
     * Sets the string that goes in the <code>JFileChooser</code> window's
     * title bar.
     *
     * @param title the new <code>String</code> for the title bar
     */
    public abstract void setDialogTitle(String title);

    /**
     * Sets the current file filter. The file filter is used by the
     * file chooser to filter out files from the user's view.
     *
     * @param filter the new current file filter to use
     * @see #getFileFilter
     */
    public abstract void setFileFilter(FileFilter filter);

    /**
     * Sets the <code>JFileChooser</code> to allow the user to just
     * select files, just select
     * directories, or select both files and directories.  The default is
     * <code>JFilesChooser.FILES_ONLY</code>.
     *
     * @param selectionMode the type of files to be displayed:
     * <ul>
     * <li>JFileChooser.FILES_ONLY
     * <li>JFileChooser.DIRECTORIES_ONLY
     * <li>JFileChooser.FILES_AND_DIRECTORIES
     * </ul>
     *
     * @throws IllegalArgumentException if <code>mode</code> is an illegal file selection mode
     */
    public abstract void setFileSelectionMode(int selectionMode);

    /**
     * Sets the file chooser to allow multiple file selections.
     *
     * @param multiple true if multiple files may be selected
     * @see #isMultiSelectionEnabled
     */
    public abstract void setMultiSelectionEnabled(boolean multiple);

    /**
     * Sets the selected file. If the file's parent directory is
     * not the current directory, changes the current directory
     * to be the file's parent directory.
     *
     * @param file the selected file
     * @see #getSelectedFile
     */
    public abstract void setSelectedFile(File file);

    /**
     * Pops up an "Open File" file chooser dialog. Note that the
     * text that appears in the approve button is determined by
     * the L&amp;F.
     *
     * @param    parent  the parent component of the dialog,
     *                  can be <code>null</code>;
     *                  see <code>showDialog</code> for details
     * @return   the return state of the file chooser on popdown:
     * <ul>
     * <li>JFileChooser.CANCEL_OPTION
     * <li>JFileChooser.APPROVE_OPTION
     * <li>JFileChooser.ERROR_OPTION if an error occurs or the
     *                  dialog is dismissed
     * </ul>
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public abstract int showOpenDialog(Component parent);

    /**
     * Pops up a "Save File" file chooser dialog. Note that the
     * text that appears in the approve button is determined by
     * the L&amp;F.
     *
     * @param    parent  the parent component of the dialog,
     *                  can be <code>null</code>;
     *                  see <code>showDialog</code> for details
     * @return   the return state of the file chooser on popdown:
     * <ul>
     * <li>JFileChooser.CANCEL_OPTION
     * <li>JFileChooser.APPROVE_OPTION
     * <li>JFileChooser.ERROR_OPTION if an error occurs or the
     *                  dialog is dismissed
     * </ul>
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public abstract int showSaveDialog(Component parent);

    /**
     * Gets the list of action names.
     *
     * @return a <code>ActionMap</code> array containing all the action names
     * @since 18113
     */
    public abstract ActionMap getActionMap();
}
