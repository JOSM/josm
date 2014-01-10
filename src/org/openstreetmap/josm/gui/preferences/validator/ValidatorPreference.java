// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JTabbedPane;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
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
        super("validator", tr("Data validator"),
                tr("An OSM data validator that checks for common errors made by users and editor programs."),
                false, new JTabbedPane());
    }

    /** The preferences prefix */
    public static final String PREFIX = "validator";

    /** The preferences key for error layer */
    public static final String PREF_LAYER = PREFIX + ".layer";

    /** The preferences key for enabled tests */
    public static final String PREF_SKIP_TESTS = PREFIX + ".skip";

    /** The preferences key for enabled tests */
    public static final String PREF_USE_IGNORE = PREFIX + ".ignore";

    /** The preferences key for enabled tests before upload*/
    public static final String PREF_SKIP_TESTS_BEFORE_UPLOAD = PREFIX + ".skipBeforeUpload";

    /** The preferences key for ignored severity other on upload */
    public static final String PREF_OTHER_UPLOAD = PREFIX + ".otherUpload";

    /** The preferences for ignored severity other */
    public static final BooleanProperty PREF_OTHER = new BooleanProperty(PREFIX + ".other", false);

    /**
     * The preferences key for enabling the permanent filtering
     * of the displayed errors in the tree regarding the current selection
     */
    public static final String PREF_FILTER_BY_SELECTION = PREFIX + ".selectionFilter";

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.createPreferenceTab(this).add(getTabPane(), GBC.eol().fill(GBC.BOTH));
    }

    @Override
    public boolean ok() {
        return false;
    }
}
