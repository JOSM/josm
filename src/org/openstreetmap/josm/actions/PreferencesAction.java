// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open the Preferences dialog.
 *
 * @author imi
 */
public class PreferencesAction extends JosmAction implements Runnable {

    /**
     * Create the preference action with "&Preferences" as label.
     */
    public PreferencesAction() {
        super(tr("Preferences..."), "preference", tr("Open a preferences page for global settings."),
        Shortcut.registerShortcut("system:preferences", tr("Preferences"), KeyEvent.VK_F12, Shortcut.GROUP_DIRECT), true);
        putValue("help", ht("/Action/Preferences"));
    }

    /**
     * Launch the preferences dialog.
     */
    public void actionPerformed(ActionEvent e) {
        run();
    }

    public void run() {
        PreferenceDialog prefDlg = new PreferenceDialog();
        prefDlg.setMinimumSize(new Dimension(400,300));
        JPanel prefPanel = new JPanel(new GridBagLayout());
        prefPanel.add(prefDlg, GBC.eol().fill(GBC.BOTH));

        JOptionPane pane = new JOptionPane(prefPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = pane.createDialog(Main.parent, tr("Preferences"));
        dlg.setResizable(true);
        dlg.setMinimumSize(new Dimension(500,400));

//      if (dlg.getWidth() > 600)
//          dlg.setSize(600, dlg.getHeight());
//      if (dlg.getHeight() > 600)
//          dlg.setSize(dlg.getWidth(),600);

        int JOSMWidth = Main.parent.getWidth();
        int JOSMHeight = Main.parent.getHeight();

        if (JOSMWidth > 2000 && JOSMWidth >  JOSMHeight * 2)
            // don't center on horizontal span monitor configurations (yes, can be selfish sometimes)
            JOSMWidth /= 2;

        int targetWidth = JOSMWidth / 2;
        if (targetWidth < 600) targetWidth = 600;
        if (targetWidth > 1200) targetWidth = 1200;
        int targetHeight = (JOSMHeight * 3) / 4;
        if (targetHeight < 600) targetHeight = 600;
        if (targetHeight > 1200) targetHeight = 1200;

        int targetX = Main.parent.getX() + JOSMWidth / 2 - targetWidth / 2;
        int targetY = Main.parent.getY() + JOSMHeight / 2 - targetHeight / 2;

        dlg.setBounds(targetX, targetY, targetWidth, targetHeight);

        dlg.setModal(true);
        dlg.setVisible(true);
        if (pane.getValue() instanceof Integer && (Integer)pane.getValue() == JOptionPane.OK_OPTION)
            prefDlg.ok();
        dlg.dispose();
    }
}
