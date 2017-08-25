// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Preference settings for the validator.
 *
 * @author frsantos
 */
public final class ValidatorPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code ValidatorPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ValidatorPreference();
        }
    }

    private ValidatorPreference() {
        super(/* ICON(preferences/) */ "validator", tr("Data validator"),
                tr("An OSM data validator that checks for common errors made by users and editor programs."),
                false, new JTabbedPane());
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.createPreferenceTab(this).add(getTabPane(), GBC.eol().fill(GBC.BOTH));
    }

    @Override
    public boolean ok() {
        return false;
    }
}
