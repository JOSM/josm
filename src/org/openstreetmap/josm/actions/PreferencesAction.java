// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;
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
        super(tr("Preferences..."), "preference", tr("Open a preferences dialog for global settings."),
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
        new PreferenceDialog(Main.parent).setVisible(true);
    }
}
