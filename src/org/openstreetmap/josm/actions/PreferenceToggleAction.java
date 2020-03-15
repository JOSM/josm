// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;

/**
 * User action to toggle a custom boolean preference value.
 *
 * A user action will just change a preference value. To take any real action,
 * register another {@link PreferenceChangedListener} for the given preference key.
 */
public class PreferenceToggleAction extends JosmAction implements PreferenceChangedListener {

    private final JCheckBoxMenuItem checkbox;
    private final BooleanProperty pref;

    /**
     * Create a new PreferenceToggleAction.
     * @param name the (translated) title
     * @param tooltip tooltip text
     * @param prefKey the preference key to toggle
     * @param prefDefault default value for the preference entry
     */
    public PreferenceToggleAction(String name, String tooltip, String prefKey, boolean prefDefault) {
        super(name, null, tooltip, null, false);
        setToolbarId("toggle-" + prefKey);
        this.pref = new BooleanProperty(prefKey, prefDefault);
        checkbox = new JCheckBoxMenuItem(this);
        checkbox.setSelected(pref.get());
        Preferences.main().addWeakKeyPreferenceChangeListener(prefKey, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        pref.put(checkbox.isSelected());
    }

    /**
     * Get the checkbox that can be used for this action. It can only be used at one place.
     * @return The checkbox.
     */
    public JCheckBoxMenuItem getCheckbox() {
        return checkbox;
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        checkbox.setSelected(pref.get());
    }
}
