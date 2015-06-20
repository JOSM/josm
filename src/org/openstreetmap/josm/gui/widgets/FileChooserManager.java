// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.io.File;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.preferences.BooleanProperty;

/**
 * A chained utility class used to create and open {@link AbstractFileChooser} dialogs.<br>
 * Use only this class if you need to control specifically your AbstractFileChooser dialog.<br>
 * <p>
 * A simpler usage is to call the {@link DiskAccessAction#createAndOpenFileChooser} methods.
 *
 * @since 5438 (creation)
 * @since 7578 (rename)
 */
public class FileChooserManager {

    /**
     * Property to enable use of native file dialogs.
     */
    public static final BooleanProperty PROP_USE_NATIVE_FILE_DIALOG = new BooleanProperty("use.native.file.dialog",
            // Native dialogs do not support file filters, so do not set them as default, except for OS X where they never worked
            Main.isPlatformOsx());

    private final boolean open;
    private final String lastDirProperty;
    private final String curDir;

    private AbstractFileChooser fc;

    /**
     * Creates a new {@code FileChooserManager}.
     * @param open If true, "Open File" dialogs will be created. If false, "Save File" dialogs will be created.
     * @see #createFileChooser
     */
    public FileChooserManager(boolean open) {
        this(open, null);
    }

    /**
     * Creates a new {@code FileChooserManager}.
     * @param open If true, "Open File" dialogs will be created. If false, "Save File" dialogs will be created.
     * @param lastDirProperty The name of the property used to get the last directory. This directory is used to initialize the AbstractFileChooser.
     *                        Then, if the user effectively chooses a file or a directory, this property will be updated to the directory path.
     * @see #createFileChooser
     */
    public FileChooserManager(boolean open, String lastDirProperty) {
        this(open, lastDirProperty, null);
    }

    /**
     * Creates a new {@code FileChooserManager}.
     * @param open If true, "Open File" dialogs will be created. If false, "Save File" dialogs will be created.
     * @param lastDirProperty The name of the property used to get the last directory. This directory is used to initialize the AbstractFileChooser.
     *                        Then, if the user effectively chooses a file or a directory, this property will be updated to the directory path.
     * @param defaultDir The default directory used to initialize the AbstractFileChooser if the {@code lastDirProperty} property value is missing.
     * @see #createFileChooser
     */
    public FileChooserManager(boolean open, String lastDirProperty, String defaultDir) {
        this.open = open;
        this.lastDirProperty = lastDirProperty == null || lastDirProperty.isEmpty() ? "lastDirectory" : lastDirProperty;
        this.curDir = Main.pref.get(this.lastDirProperty).isEmpty() ?
                defaultDir == null || defaultDir.isEmpty() ? "." : defaultDir
                : Main.pref.get(this.lastDirProperty);
    }

    /**
     * Replies the {@code AbstractFileChooser} that has been previously created.
     * @return The {@code AbstractFileChooser} that has been previously created, or {@code null} if it has not been created yet.
     * @see #createFileChooser
     */
    public final AbstractFileChooser getFileChooser() {
        return fc;
    }

    /**
     * Replies the initial directory used to construct the {@code AbstractFileChooser}.
     * @return The initial directory used to construct the {@code AbstractFileChooser}.
     */
    public final String getInitialDirectory() {
        return curDir;
    }

    /**
     * Creates a new {@link AbstractFileChooser} with default settings. All files will be accepted.
     * @return this
     */
    public final FileChooserManager createFileChooser() {
        return doCreateFileChooser(false, null, null, null, null, JFileChooser.FILES_ONLY, false);
    }

    /**
     * Creates a new {@link AbstractFileChooser} with given settings for a single {@code FileFilter}.
     *
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @param filter The only file filter that will be proposed by the dialog
     * @param selectionMode The selection mode that allows the user to:<br><ul>
     *                      <li>just select files ({@code JFileChooser.FILES_ONLY})</li>
     *                      <li>just select directories ({@code JFileChooser.DIRECTORIES_ONLY})</li>
     *                      <li>select both files and directories ({@code JFileChooser.FILES_AND_DIRECTORIES})</li></ul>
     * @return this
     * @see DiskAccessAction#createAndOpenFileChooser(boolean, boolean, String, FileFilter, int, String)
     */
    public final FileChooserManager createFileChooser(boolean multiple, String title, FileFilter filter, int selectionMode) {
        doCreateFileChooser(multiple, title, Collections.singleton(filter), filter, null, selectionMode, false);
        getFileChooser().setAcceptAllFileFilterUsed(false);
        return this;
    }

    /**
     * Creates a new {@link AbstractFileChooser} with given settings for a collection of {@code FileFilter}s.
     *
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @param filters The file filters that will be proposed by the dialog
     * @param defaultFilter The file filter that will be selected by default
     * @param selectionMode The selection mode that allows the user to:<br><ul>
     *                      <li>just select files ({@code JFileChooser.FILES_ONLY})</li>
     *                      <li>just select directories ({@code JFileChooser.DIRECTORIES_ONLY})</li>
     *                      <li>select both files and directories ({@code JFileChooser.FILES_AND_DIRECTORIES})</li></ul>
     * @return this
     * @see DiskAccessAction#createAndOpenFileChooser(boolean, boolean, String, Collection, FileFilter, int, String)
     */
    public final FileChooserManager createFileChooser(boolean multiple, String title, Collection<? extends FileFilter> filters,
            FileFilter defaultFilter, int selectionMode) {
        return doCreateFileChooser(multiple, title, filters, defaultFilter, null, selectionMode, false);
    }

    /**
     * Creates a new {@link AbstractFileChooser} with given settings for a file extension.
     *
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @param extension The file extension that will be selected as the default file filter
     * @param allTypes If true, all the files types known by JOSM will be proposed in the "file type" combobox.
     *                 If false, only the file filters that include {@code extension} will be proposed
     * @param selectionMode The selection mode that allows the user to:<br><ul>
     *                      <li>just select files ({@code JFileChooser.FILES_ONLY})</li>
     *                      <li>just select directories ({@code JFileChooser.DIRECTORIES_ONLY})</li>
     *                      <li>select both files and directories ({@code JFileChooser.FILES_AND_DIRECTORIES})</li></ul>
     * @return this
     * @see DiskAccessAction#createAndOpenFileChooser(boolean, boolean, String, FileFilter, int, String)
     */
    public final FileChooserManager createFileChooser(boolean multiple, String title, String extension, boolean allTypes, int selectionMode) {
        return doCreateFileChooser(multiple, title, null, null, extension, selectionMode, allTypes);
    }

    private FileChooserManager doCreateFileChooser(boolean multiple, String title, Collection<? extends FileFilter> filters,
            FileFilter defaultFilter, String extension, int selectionMode, boolean allTypes) {
        File file = new File(curDir);
        // Use native dialog is preference is set, unless an unsupported selection mode is specifically wanted
        if (PROP_USE_NATIVE_FILE_DIALOG.get() && NativeFileChooser.supportsSelectionMode(selectionMode)) {
            fc = new NativeFileChooser(file);
        } else {
            fc = new SwingFileChooser(file);
        }

        if (title != null) {
            fc.setDialogTitle(title);
        }

        fc.setFileSelectionMode(selectionMode);
        fc.setMultiSelectionEnabled(multiple);
        fc.setAcceptAllFileFilterUsed(false);

        if (filters != null) {
            for (FileFilter filter : filters) {
                fc.addChoosableFileFilter(filter);
            }
            if (defaultFilter != null) {
                fc.setFileFilter(defaultFilter);
            }
        } else if (open) {
            ExtensionFileFilter.applyChoosableImportFileFilters(fc, extension, allTypes);
        } else {
            ExtensionFileFilter.applyChoosableExportFileFilters(fc, extension, allTypes);
        }
        return this;
    }

    /**
     * Opens the {@code AbstractFileChooser} that has been created. Nothing happens if it has not been created yet.
     * @return the {@code AbstractFileChooser} if the user effectively choses a file or directory. {@code null} if the user cancelled the dialog.
     */
    public final AbstractFileChooser openFileChooser() {
        return openFileChooser(null);
    }

    /**
     * Opens the {@code AbstractFileChooser} that has been created and waits for the user to choose a file/directory, or cancel the dialog.<br>
     * Nothing happens if the dialog has not been created yet.<br>
     * When the user choses a file or directory, the {@code lastDirProperty} is updated to the chosen directory path.
     *
     * @param parent The Component used as the parent of the AbstractFileChooser. If null, uses {@code Main.parent}.
     * @return the {@code AbstractFileChooser} if the user effectively choses a file or directory. {@code null} if the user cancelled the dialog.
     */
    public AbstractFileChooser openFileChooser(Component parent) {
        if (fc != null) {
            if (parent == null) {
                parent = Main.parent;
            }

            int answer = open ? fc.showOpenDialog(parent) : fc.showSaveDialog(parent);
            if (answer != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir)) {
                Main.pref.put(lastDirProperty, fc.getCurrentDirectory().getAbsolutePath());
            }

            if (!open) {
                File file = fc.getSelectedFile();
                if (!SaveActionBase.confirmOverwrite(file)) {
                    return null;
                }
            }
        }
        return fc;
    }
}
