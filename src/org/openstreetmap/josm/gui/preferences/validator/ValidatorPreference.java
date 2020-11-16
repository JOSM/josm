// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.ExtensibleTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;

/**
 * Preference settings for the validator.
 *
 * @author frsantos
 */
public final class ValidatorPreference extends ExtensibleTabPreferenceSetting {

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
                tr("An OSM data validator that checks for common errors made by users and editor programs."), false);
    }

    @Override
    public boolean ok() {
        return false;
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Validator");
    }
}
