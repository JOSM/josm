// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Helper class for all actions that access the disk.
 * @since 78
 */
public abstract class DiskAccessAction extends JosmAction {

    /**
     * Constructs a new {@code DiskAccessAction}.
     *
     * @param name The action's text as displayed on the menu (if it is added to a menu)
     * @param iconName The filename of the icon to use
     * @param tooltip A longer description of the action that will be displayed in the tooltip
     * @param shortcut A ready-created shortcut object or {@code null} if you don't want a shortcut
     * @since 1084
     */
    protected DiskAccessAction(String name, String iconName, String tooltip, Shortcut shortcut) {
        super(name, iconName, tooltip, shortcut, true);
    }

    /**
     * Constructs a new {@code DiskAccessAction}.
     *
     * @param name The action's text as displayed on the menu (if it is added to a menu)
     * @param iconName The filename of the icon to use
     * @param tooltip  A longer description of the action that will be displayed in the tooltip
     * @param shortcut A ready-created shortcut object or null if you don't want a shortcut
     * @param register Register this action for the toolbar preferences?
     * @param toolbarId Identifier for the toolbar preferences. The iconName is used, if this parameter is null
     * @param installAdapters False, if you don't want to install layer changed and selection changed adapters
     * @since 5438
     */
    protected DiskAccessAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register,
            String toolbarId, boolean installAdapters) {
        super(name, iconName, tooltip, shortcut, register, toolbarId, installAdapters);
    }

    /**
     * Creates a new {@link AbstractFileChooser} and makes it visible.
     * @param open If true, pops up an "Open File" dialog. If false, pops up a "Save File" dialog
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @return The {@code AbstractFileChooser}.
     * @since 1646
     */
    public static AbstractFileChooser createAndOpenFileChooser(boolean open, boolean multiple, String title) {
        return createAndOpenFileChooser(open, multiple, title, null);
    }

    /**
     * Creates a new {@link AbstractFileChooser} and makes it visible.
     * @param open If true, pops up an "Open File" dialog. If false, pops up a "Save File" dialog
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @param extension The file extension that will be selected as the default file filter
     * @return The {@code AbstractFileChooser}.
     * @since 2020
     */
    public static AbstractFileChooser createAndOpenFileChooser(boolean open, boolean multiple, String title, String extension) {
        return createAndOpenFileChooser(open, multiple, title, extension, JFileChooser.FILES_ONLY, true, null);
    }

    /**
     * Creates a new {@link AbstractFileChooser} and makes it visible.
     * @param open If true, pops up an "Open File" dialog. If false, pops up a "Save File" dialog
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @param extension The file extension that will be selected as the default file filter
     * @param selectionMode The selection mode that allows the user to:<br><ul>
     *                      <li>just select files ({@code JFileChooser.FILES_ONLY})</li>
     *                      <li>just select directories ({@code JFileChooser.DIRECTORIES_ONLY})</li>
     *                      <li>select both files and directories ({@code JFileChooser.FILES_AND_DIRECTORIES})</li></ul>
     * @param allTypes If true, all the files types known by JOSM will be proposed in the "file type" combobox.
     *                 If false, only the file filters that include {@code extension} will be proposed
     * @param lastDirProperty The name of the property used to setup the AbstractFileChooser initial directory.
     *        This property will then be updated to the new "last directory" chosen by the user.
     *        If null, the default property "lastDirectory" will be used.
     * @return The {@code AbstractFileChooser}.
     * @since 5438
     */
    public static AbstractFileChooser createAndOpenFileChooser(boolean open, boolean multiple, String title, String extension,
            int selectionMode, boolean allTypes, String lastDirProperty) {
        return new FileChooserManager(open, lastDirProperty)
            .createFileChooser(multiple, title, extension, allTypes, selectionMode).openFileChooser();
    }

    /**
     * Creates a new {@link AbstractFileChooser} for a single {@link FileFilter} and makes it visible.
     * @param open If true, pops up an "Open File" dialog. If false, pops up a "Save File" dialog
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @param filter The only file filter that will be proposed by the dialog
     * @param selectionMode The selection mode that allows the user to:<br><ul>
     *                      <li>just select files ({@code JFileChooser.FILES_ONLY})</li>
     *                      <li>just select directories ({@code JFileChooser.DIRECTORIES_ONLY})</li>
     *                      <li>select both files and directories ({@code JFileChooser.FILES_AND_DIRECTORIES})</li></ul>
     * @param lastDirProperty The name of the property used to setup the AbstractFileChooser initial directory.
     * This property will then be updated to the new "last directory" chosen by the user
     * @return The {@code AbstractFileChooser}.
     * @since 5438
     */
    public static AbstractFileChooser createAndOpenFileChooser(boolean open, boolean multiple, String title, FileFilter filter,
            int selectionMode, String lastDirProperty) {
        return new FileChooserManager(open, lastDirProperty).createFileChooser(multiple, title, filter, selectionMode).openFileChooser();
    }

    /**
     * Creates a new {@link AbstractFileChooser} for several {@link FileFilter}s and makes it visible.
     * @param open If true, pops up an "Open File" dialog. If false, pops up a "Save File" dialog
     * @param multiple If true, makes the dialog allow multiple file selections
     * @param title The string that goes in the dialog window's title bar
     * @param filters The file filters that will be proposed by the dialog
     * @param defaultFilter The file filter that will be selected by default
     * @param selectionMode The selection mode that allows the user to:<br><ul>
     *                      <li>just select files ({@code JFileChooser.FILES_ONLY})</li>
     *                      <li>just select directories ({@code JFileChooser.DIRECTORIES_ONLY})</li>
     *                      <li>select both files and directories ({@code JFileChooser.FILES_AND_DIRECTORIES})</li></ul>
     * @param lastDirProperty The name of the property used to setup the JFileChooser initial directory.
     * This property will then be updated to the new "last directory" chosen by the user
     * @return The {@code AbstractFileChooser}.
     * @since 5438
     */
    public static AbstractFileChooser createAndOpenFileChooser(boolean open, boolean multiple, String title,
            Collection<? extends FileFilter> filters, FileFilter defaultFilter, int selectionMode, String lastDirProperty) {
        return new FileChooserManager(open, lastDirProperty).createFileChooser(multiple, title, filters, defaultFilter, selectionMode)
                .openFileChooser();
    }
}
