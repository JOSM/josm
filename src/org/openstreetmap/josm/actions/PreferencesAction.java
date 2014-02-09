// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Open the Preferences dialog.
 *
 * @author imi
 */
public class PreferencesAction extends JosmAction implements Runnable {

    private final Class<? extends TabPreferenceSetting> tab;
    private final Class<? extends SubPreferenceSetting> subTab;

    private PreferencesAction(String name, String tooltip,
                              Class<? extends TabPreferenceSetting> tab, Class<? extends SubPreferenceSetting> subTab) {
        super(name, "preference", tooltip, null, false, "preference_" + Utils.<Class>firstNonNull(tab, subTab).getName(), false);
        this.tab = tab;
        this.subTab = subTab;
    }

    public static PreferencesAction forPreferenceTab(String name, String tooltip, Class<? extends TabPreferenceSetting> tab) {
        CheckParameterUtil.ensureParameterNotNull(tab);
        return new PreferencesAction(name, tooltip, tab, null);
    }

    public static PreferencesAction forPreferenceSubTab(String name, String tooltip, Class<? extends SubPreferenceSetting> subTab) {
        CheckParameterUtil.ensureParameterNotNull(subTab);
        return new PreferencesAction(name, tooltip, null, subTab);
    }

    /**
     * Create the preference action with "Preferences" as label.
     */
    public PreferencesAction() {
        super(tr("Preferences..."), "preference", tr("Open a preferences dialog for global settings."),
                Shortcut.registerShortcut("system:preferences", tr("Preferences"), KeyEvent.VK_F12, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/Preferences"));
        this.tab = null;
        this.subTab = null;
    }

    /**
     * Launch the preferences dialog.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    @Override
    public void run() {
        final PreferenceDialog p = new PreferenceDialog(Main.parent);
        if (tab != null) {
            p.selectPreferencesTabByClass(tab);
        } else if( subTab != null) {
            p.selectSubPreferencesTabByClass(subTab);
        }
        p.setVisible(true);
    }
}
