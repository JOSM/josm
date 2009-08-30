// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.FileImporter;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open a file chooser dialog and select an file to import. Then call the gpx-import driver. Finally
 * open an internal frame into the main window with the gpx data shown.
 *
 * @author imi
 */
public class OpenFileAction extends DiskAccessAction {

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenFileAction() {
        super(tr("Open..."), "open", tr("Open a file."),
                Shortcut.registerShortcut("system:open", tr("File: {0}", tr("Open...")), KeyEvent.VK_O, Shortcut.GROUP_MENU));
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = createAndOpenFileChooser(true, true, null);
        if (fc == null)
            return;
        File[] files = fc.getSelectedFiles();
        for (int i = files.length; i > 0; --i) {
            openFile(files[i-1]);
        }
    }

    /**
     * Open the given file.
     */
    public void openFile(File file) {
        try {
            System.out.println("Open file: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
            for (FileImporter importer : ExtensionFileFilter.importers)
                if (importer.acceptFile(file)) {
                    importer.importData(file);
                }
        } catch (IOException x) {
            x.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Could not read file ''{0}\''. Error is: <br>{1}</html>", file.getName(), x.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );

        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(! Main.applet);
    }
}
