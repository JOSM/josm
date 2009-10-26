// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Action that opens a connection to the osm server and downloads map data.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * @author imi
 */
public class DownloadAction extends JosmAction {

    public DownloadDialog dialog;

    public DownloadAction() {
        super(tr("Download from OSM..."), "download", tr("Download map data from the OSM server."),
                Shortcut.registerShortcut("file:download", tr("File: {0}", tr("Download from OSM...")), KeyEvent.VK_D, Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY), true);
        putValue("help", ht("/Action/Download"));
    }

    /**
     * Creates the download dialog
     * @return the downlaod dialog
     */
    protected ExtendedDialog createUploadDialog() {
        dialog = new DownloadDialog();
        JPanel downPanel = new JPanel(new BorderLayout());
        downPanel.add(dialog, BorderLayout.CENTER);

        final String prefName = dialog.getClass().getName()+ ".geometry";
        final WindowGeometry wg = WindowGeometry.centerInWindow(Main.parent,
                new Dimension(1000,600));

        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("Download"),
                new String[] {tr("OK"), tr("Cancel")});
        dialog.setContent(downPanel, false /* don't use a scroll pane inside the dialog */);
        dialog.setButtonIcons(new String[] {"ok", "cancel"});
        dialog.setRememberWindowGeometry(prefName, wg);
        return dialog;
    }

    public void actionPerformed(ActionEvent e) {
        ExtendedDialog dlg = createUploadDialog();
        boolean finish = false;
        while (!finish) {
            dlg.showDialog();
            Main.pref.put("download.newlayer", dialog.newLayer.isSelected());
            if (dlg.getValue() == 1 /* OK */) {
                Main.pref.put("download.tab", Integer.toString(dialog.getSelectedTab()));
                for (DownloadTask task : dialog.downloadTasks) {
                    Main.pref.put("download."+task.getPreferencesSuffix(), task.getCheckBox().isSelected());
                    if (task.getCheckBox().isSelected()) {
                        // asynchronously launch the download task ...
                        Future<?> future = task.download(this, dialog.minlat, dialog.minlon, dialog.maxlat, dialog.maxlon, null);
                        // ... and the continuation when the download task is finished
                        Main.worker.submit(new PostDownloadHandler(task, future));
                        finish = true;
                    }
                }
            } else {
                finish = true;
            }
            if (!finish) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Please select at least one task to download"),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }

        dialog = null;
        dlg.dispose();
    }
}
