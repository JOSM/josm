// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import javax.swing.JCheckBoxMenuItem;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class PreferenceToggleAction extends JosmAction implements PreferenceChangedListener {

    protected final JCheckBoxMenuItem checkbox;
    protected final BooleanProperty property;

    public PreferenceToggleAction(String name, String tooltip, String prefKey, boolean prefDefault) {
        this(name, tooltip, new BooleanProperty(prefKey, prefDefault));
    }

    public PreferenceToggleAction(String name, String tooltip, BooleanProperty property) {
        super(name, null, tooltip, null, false);
        CheckParameterUtil.ensureParameterNotNull(property, "property");
        putValue("toolbar", "toggle-" + property.getKey());
        this.property = property;
        this.checkbox = new JCheckBoxMenuItem(this);
        this.checkbox.setSelected(property.get());
        Main.pref.addPreferenceChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        property.put(checkbox.isSelected());
    }

    public JCheckBoxMenuItem getCheckbox() {
        return checkbox;
    }

    @Override
    public void preferenceChanged(Preferences.PreferenceChangeEvent e) {
        if (property.getKey().equals(e.getKey())) {
            checkbox.setSelected(property.get());
        }
    }
}
