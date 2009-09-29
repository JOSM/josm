// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open an URL input dialog and load data from the given URL.
 *
 * @author imi
 */
public class OpenLocationAction extends JosmAction {

    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenLocationAction() {
        super(tr("Open Location..."), "openlocation", tr("Open an URL."),
                Shortcut.registerShortcut("system:open_location", tr("File: {0}", tr("Open Location...")), KeyEvent.VK_L, Shortcut.GROUP_MENU), true);
    }

    public void actionPerformed(ActionEvent e) {

        JCheckBox layer = new JCheckBox(tr("Separate Layer"));
        layer.setSelected(Main.pref.getBoolean("download.newlayer"));
        JPanel all = new JPanel(new GridBagLayout());
        all.add(new JLabel(tr("Enter URL to download:")), GBC.eol());
        JTextField urltext = new JTextField(40);
        all.add(urltext, GBC.eol());
        all.add(layer, GBC.eol());
        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("Download Location"),
                new String[] {tr("Download URL"), tr("Cancel")}
        );
        dialog.setContent(all);
        dialog.setButtonIcons(new String[] {"download.png", "cancel.png"});
        dialog.showDialog();
        if (dialog.getValue() != 1) return;
        openUrl(layer.isSelected(), urltext.getText());
    }

    /**
     * Open the given file.
     */
    public void openUrl(boolean new_layer, String url) {
        new DownloadOsmTask().loadUrl(new_layer, url, null);
    }

}
