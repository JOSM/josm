// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import javax.swing.JCheckBoxMenuItem;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

public class PreferenceToggleAction extends JosmAction implements PreferenceChangedListener {

    private final JCheckBoxMenuItem checkbox;
    private final String prefKey;
    private final boolean prefDefault;

    public PreferenceToggleAction(String name, String tooltip, String prefKey, boolean prefDefault) {
        super(name, null, tooltip, null, false);
        putValue("toolbar", "toggle-" + prefKey);
        this.prefKey = prefKey;
        this.prefDefault = prefDefault;
        this.checkbox = new JCheckBoxMenuItem(this);
        this.checkbox.setSelected(Main.pref.getBoolean(prefKey, prefDefault));
        Main.pref.addPreferenceChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Main.pref.put(prefKey, checkbox.isSelected());
    }

    public JCheckBoxMenuItem getCheckbox() {
        return checkbox;
    }

    @Override
    public void preferenceChanged(Preferences.PreferenceChangeEvent e) {
        if (prefKey.equals(e.getKey())) {
            checkbox.setSelected(Main.pref.getBoolean(prefKey, prefDefault));
        }
    }
}
