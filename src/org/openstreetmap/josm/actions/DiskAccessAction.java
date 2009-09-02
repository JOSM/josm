// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import javax.swing.JFileChooser;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Helper class for all actions that access the disk
 */
abstract public class DiskAccessAction extends JosmAction {

    public DiskAccessAction(String name, String iconName, String tooltip, Shortcut shortcut) {
        super(name, iconName, tooltip, shortcut, true);
    }

    public static JFileChooser createAndOpenFileChooser(boolean open, boolean multiple, String title) {
        return createAndOpenFileChooser(open, multiple, title, null);
    }

    public static JFileChooser createAndOpenFileChooser(boolean open, boolean multiple, String title, String extension) {
        String curDir = Main.pref.get("lastDirectory");
        if (curDir.equals("")) {
            curDir = ".";
        }
        JFileChooser fc = new JFileChooser(new File(curDir));
        if (title != null) {
            fc.setDialogTitle(title);
        }

        fc.setMultiSelectionEnabled(multiple);
        fc.setAcceptAllFileFilterUsed(false);
        System.out.println("opening fc for extension " + extension);
        ExtensionFileFilter.applyChoosableImportFileFilters(fc, extension);

        int answer = open ? fc.showOpenDialog(Main.parent) : fc.showSaveDialog(Main.parent);
        if (answer != JFileChooser.APPROVE_OPTION)
            return null;

        if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir)) {
            Main.pref.put("lastDirectory", fc.getCurrentDirectory().getAbsolutePath());
        }

        if (!open) {
            File file = fc.getSelectedFile();
            if (file == null || (file.exists() && 1 !=
                new ExtendedDialog(Main.parent,
                        tr("Overwrite"),
                        tr("File exists. Overwrite?"),
                        new String[] {tr("Overwrite"), tr("Cancel")},
                        new String[] {"save_as.png", "cancel.png"}).getValue()))
                return null;
        }

        return fc;
    }
}
