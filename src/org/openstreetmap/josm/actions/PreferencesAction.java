// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MainApplication;
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

    private PreferencesAction(String name, String icon, String tooltip,
                              Class<? extends TabPreferenceSetting> tab, Class<? extends SubPreferenceSetting> subTab) {
        super(name, icon, tooltip, null, false, "preference_" + Utils.<Class<?>>firstNonNull(tab, subTab).getName(), false);
        this.tab = tab;
        this.subTab = subTab;
    }

    /**
     * Returns a new {@code PreferenceAction} opening preferences dialog directly to the given tab, with default icon.
     * @param name The action name
     * @param tooltip The action tooltip
     * @param tab The preferences tab to select
     * @return The created action
     */
    public static PreferencesAction forPreferenceTab(String name, String tooltip, Class<? extends TabPreferenceSetting> tab) {
        return forPreferenceTab(name, tooltip, tab, "preference");
    }

    /**
     * Returns a new {@code PreferenceAction} opening preferences dialog directly to the given tab, with custom icon.
     * @param name The action name
     * @param tooltip The action tooltip
     * @param tab The preferences tab to select
     * @param icon The action icon
     * @return The created action
     * @since 6969
     */
    public static PreferencesAction forPreferenceTab(String name, String tooltip, Class<? extends TabPreferenceSetting> tab, String icon) {
        CheckParameterUtil.ensureParameterNotNull(tab);
        return new PreferencesAction(name, icon, tooltip, tab, null);
    }

    /**
     * Returns a new {@code PreferenceAction} opening preferences dialog directly to the given subtab, with default icon.
     * @param name The action name
     * @param tooltip The action tooltip
     * @param subTab The preferences subtab to select
     * @return The created action
     */
    public static PreferencesAction forPreferenceSubTab(String name, String tooltip, Class<? extends SubPreferenceSetting> subTab) {
        return forPreferenceSubTab(name, tooltip, subTab, "preference");
    }

    /**
     * Returns a new {@code PreferenceAction} opening preferences dialog directly to the given subtab, with custom icon.
     * @param name The action name
     * @param tooltip The action tooltip
     * @param subTab The preferences subtab to select
     * @param icon The action icon
     * @return The created action
     * @since 6969
     */
    public static PreferencesAction forPreferenceSubTab(String name, String tooltip, Class<? extends SubPreferenceSetting> subTab, String icon) {
        CheckParameterUtil.ensureParameterNotNull(subTab);
        return new PreferencesAction(name, icon, tooltip, null, subTab);
    }

    /**
     * Create the preference action with "Preferences" as label.
     */
    public PreferencesAction() {
        super(tr("Preferences..."), "preference", tr("Open a preferences dialog for global settings."),
                Shortcut.registerShortcut("system:preferences", tr("Preferences"), KeyEvent.VK_F12, Shortcut.DIRECT), true, false);
        setHelpId(ht("/Action/Preferences"));
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
        final PreferenceDialog p = new PreferenceDialog(MainApplication.getMainFrame());
        if (tab != null) {
            p.selectPreferencesTabByClass(tab);
        } else if (subTab != null) {
            p.selectSubPreferencesTabByClass(subTab);
        }
        p.setVisible(true);
    }
}
