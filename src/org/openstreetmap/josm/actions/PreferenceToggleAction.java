// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.ImageProvider;

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
     * @param pref the preference to toggle
     */
    public PreferenceToggleAction(String name, String tooltip, BooleanProperty pref) {
        this(name, null, tooltip, pref);
    }

    /**
     * Create a new PreferenceToggleAction.
     * @param name the (translated) title
     * @param icon icon to display e.g. in menu
     * @param tooltip tooltip text
     * @param pref the preference to toggle
     * @since 17021
     */
    public PreferenceToggleAction(String name, ImageProvider icon, String tooltip, BooleanProperty pref) {
        super(name, icon, tooltip, null, false, null, true);
        setToolbarId("toggle-" + pref.getKey());
        this.pref = pref;
        checkbox = new JCheckBoxMenuItem(this);
        checkbox.setSelected(pref.get());
        Preferences.main().addWeakKeyPreferenceChangeListener(pref.getKey(), this);
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
