// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.tools.GBC;
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

    public DownloadDialog dialog;

    public DownloadAction() {
        super(tr("Download from OSM..."), "download", tr("Download map data from the OSM server."),
                Shortcut.registerShortcut("file:download", tr("File: {0}", tr("Download from OSM...")), KeyEvent.VK_D, Shortcut.GROUPS_ALT1+Shortcut.GROUP_HOTKEY), true);
    }

    public void actionPerformed(ActionEvent e) {
        dialog = new DownloadDialog();

        JPanel downPanel = new JPanel(new GridBagLayout());
        downPanel.add(dialog, GBC.eol().fill(GBC.BOTH));

        JOptionPane pane = new JOptionPane(downPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = pane.createDialog(Main.parent, tr("Download"));
        dlg.setResizable(true);
        dialog.setOptionPane(pane);

        if (dlg.getWidth() > 1000) {
            dlg.setSize(1000, dlg.getHeight());
        }
        if (dlg.getHeight() > 600) {
            dlg.setSize(dlg.getWidth(),600);
        }

        boolean finish = false;
        while (!finish) {
            dlg.setVisible(true);
            Main.pref.put("download.newlayer", dialog.newLayer.isSelected());
            if (pane.getValue() instanceof Integer && (Integer)pane.getValue() == JOptionPane.OK_OPTION) {
                Main.pref.put("download.tab", Integer.toString(dialog.getSelectedTab()));
                for (DownloadTask task : dialog.downloadTasks) {
                    Main.pref.put("download."+task.getPreferencesSuffix(), task.getCheckBox().isSelected());
                    if (task.getCheckBox().isSelected()) {
                        task.download(this, dialog.minlat, dialog.minlon, dialog.maxlat, dialog.maxlon, null);
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
