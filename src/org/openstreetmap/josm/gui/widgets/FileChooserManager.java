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
import org.openstreetmap.josm.spi.preferences.Config;

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

    private boolean multiple;
    private String title;
    private Collection<? extends FileFilter> filters;
    private FileFilter defaultFilter;
    private int selectionMode = JFileChooser.FILES_ONLY;
    private String extension;
    private boolean allTypes;
    private File file;

    private AbstractFileChooser fc;

    /**
     * Creates a new {@code FileChooserManager} with default values.
     * @see #createFileChooser
     */
    public FileChooserManager() {
        this(false, null, null);
    }

    /**
     * Creates a new {@code FileChooserManager}.
     * @param open If true, "Open File" dialogs will be created. If false, "Save File" dialogs will be created.
     * @see #createFileChooser
     */
    public FileChooserManager(boolean open) {
        this(open, null);
    }

    // CHECKSTYLE.OFF: LineLength

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
        this.curDir = Config.getPref().get(this.lastDirProperty).isEmpty() ?
                defaultDir == null || defaultDir.isEmpty() ? "." : defaultDir
                : Config.getPref().get(this.lastDirProperty);
    }

    // CHECKSTYLE.ON: LineLength

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
        return doCreateFileChooser();
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
        multiple(multiple);
        title(title);
        filters(Collections.singleton(filter));
        defaultFilter(filter);
        selectionMode(selectionMode);

        doCreateFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
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
        multiple(multiple);
        title(title);
        filters(filters);
        defaultFilter(defaultFilter);
        selectionMode(selectionMode);
        return doCreateFileChooser();
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
        multiple(multiple);
        title(title);
        extension(extension);
        allTypes(allTypes);
        selectionMode(selectionMode);
        return doCreateFileChooser();
    }

    /**
     * Builder method to set {@code multiple} property.
     * @param value If true, makes the dialog allow multiple file selections
     * @return this
     */
    public FileChooserManager multiple(boolean value) {
        multiple = value;
        return this;
    }

    /**
     * Builder method to set {@code title} property.
     * @param value The string that goes in the dialog window's title bar
     * @return this
     */
    public FileChooserManager title(String value) {
        title = value;
        return this;
    }

    /**
     * Builder method to set {@code filters} property.
     * @param value The file filters that will be proposed by the dialog
     * @return this
     */
    public FileChooserManager filters(Collection<? extends FileFilter> value) {
        filters = value;
        return this;
    }

    /**
     * Builder method to set {@code defaultFilter} property.
     * @param value The file filter that will be selected by default
     * @return this
     */
    public FileChooserManager defaultFilter(FileFilter value) {
        defaultFilter = value;
        return this;
    }

    /**
     * Builder method to set {@code selectionMode} property.
     * @param value The selection mode that allows the user to:<br><ul>
     *                      <li>just select files ({@code JFileChooser.FILES_ONLY})</li>
     *                      <li>just select directories ({@code JFileChooser.DIRECTORIES_ONLY})</li>
     *                      <li>select both files and directories ({@code JFileChooser.FILES_AND_DIRECTORIES})</li></ul>
     * @return this
     */
    public FileChooserManager selectionMode(int value) {
        selectionMode = value;
        return this;
    }

    /**
     * Builder method to set {@code extension} property.
     * @param value The file extension that will be selected as the default file filter
     * @return this
     */
    public FileChooserManager extension(String value) {
        extension = value;
        return this;
    }

    /**
     * Builder method to set {@code allTypes} property.
     * @param value If true, all the files types known by JOSM will be proposed in the "file type" combobox.
     *              If false, only the file filters that include {@code extension} will be proposed
     * @return this
     */
    public FileChooserManager allTypes(boolean value) {
        allTypes = value;
        return this;
    }

    /**
     * Builder method to set {@code file} property.
     * @param value {@link File} object with default filename
     * @return this
     */
    public FileChooserManager file(File value) {
        file = value;
        return this;
    }

    /**
     * Builds {@code FileChooserManager} object using properties set by builder methods or default values.
     * @return this
     */
    public FileChooserManager doCreateFileChooser() {
        File f = new File(curDir);
        // Use native dialog is preference is set, unless an unsupported selection mode is specifically wanted
        if (PROP_USE_NATIVE_FILE_DIALOG.get() && NativeFileChooser.supportsSelectionMode(selectionMode)) {
            fc = new NativeFileChooser(f);
        } else {
            fc = new SwingFileChooser(f);
        }

        if (title != null) {
            fc.setDialogTitle(title);
        }

        fc.setFileSelectionMode(selectionMode);
        fc.setMultiSelectionEnabled(multiple);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setSelectedFile(this.file);

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
     * Opens the {@code AbstractFileChooser} that has been created.
     * @return the {@code AbstractFileChooser} if the user effectively choses a file or directory. {@code null} if the user cancelled the dialog.
     */
    public final AbstractFileChooser openFileChooser() {
        return openFileChooser(null);
    }

    /**
     * Opens the {@code AbstractFileChooser} that has been created and waits for the user to choose a file/directory, or cancel the dialog.<br>
     * When the user choses a file or directory, the {@code lastDirProperty} is updated to the chosen directory path.
     *
     * @param parent The Component used as the parent of the AbstractFileChooser. If null, uses {@code Main.parent}.
     * @return the {@code AbstractFileChooser} if the user effectively choses a file or directory. {@code null} if the user cancelled the dialog.
     */
    public AbstractFileChooser openFileChooser(Component parent) {
        if (fc == null)
            doCreateFileChooser();

        if (parent == null) {
            parent = Main.parent;
        }

        int answer = open ? fc.showOpenDialog(parent) : fc.showSaveDialog(parent);
        if (answer != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir)) {
            Config.getPref().put(lastDirProperty, fc.getCurrentDirectory().getAbsolutePath());
        }

        if (!open && !FileChooserManager.PROP_USE_NATIVE_FILE_DIALOG.get() &&
            !SaveActionBase.confirmOverwrite(fc.getSelectedFile())) {
            return null;
        }
        return fc;
    }

    /**
     * Opens the file chooser dialog, then checks if filename has the given extension.
     * If not, adds the extension and asks for overwrite if filename exists.
     *
     * @return the {@code File} or {@code null} if the user cancelled the dialog.
     */
    public File getFileForSave() {
        return SaveActionBase.checkFileAndConfirmOverWrite(openFileChooser(), extension);
    }
}
