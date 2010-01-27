// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URLDecoder;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.gui.download.DownloadUrlDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that opens a connection to the osm server and downloads map data.
 *
 * An dialog is displayed asking the user to specify an URL to download from.
 *
 * @author Matthias Julius <matthias@julius-net.net>
 */
public class DownloadUrlAction extends JosmAction {

    public DownloadUrlAction() {
        super(tr("Download from URL..."), "downloadurl", tr("Download map data from any URL."),
                Shortcut.registerShortcut("file:downloadUrl", tr("File: {0}", tr("Download from URL...")), KeyEvent.VK_D, Shortcut.GROUP_NONE), true);
        putValue("help", ht("/Action/DownloadURL"));
    }

    public void actionPerformed(ActionEvent e) {
        DownloadUrlDialog dialog = DownloadUrlDialog.getInstance();
        dialog.restoreSettings();
        dialog.setVisible(true);
        if (! dialog.isCanceled()) {
            dialog.rememberSettings();
            String url = dialog.getUrl();
            try {
                DownloadTask osmTask = new DownloadOsmTask();
                osmTask.loadUrl(dialog.isNewLayerRequired(), URLDecoder.decode(url, "UTF-8"), null);
            } catch (Exception ex) {
                System.out.println("Download URL Error:");
                ex.printStackTrace();
                return;
            }
        }
    }
}
