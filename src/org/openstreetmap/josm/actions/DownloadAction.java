// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that opens a connection to the osm server and downloads map data.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * @author imi
 */
public class DownloadAction extends JosmAction {

    /**
     * Constructs a new {@code DownloadAction}.
     */
    public DownloadAction() {
        super(tr("Download data"), "download", tr("Download map data from a server of your choice"),
              Shortcut.registerShortcut("file:download", tr("File: {0}", tr("Download data")), KeyEvent.VK_DOWN, Shortcut.CTRL_SHIFT),
              true);
        putValue("help", ht("/Action/Download"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DownloadDialog dialog = DownloadDialog.getInstance();
        dialog.restoreSettings();
        dialog.setVisible(true);
    }
}
