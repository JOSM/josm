// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Abstract base class for {@link PreferenceSetting} implementations.
 *
 * Handles the flag that indicates if a PreferenceSetting is and expert option or not.
 * @since 4968
 */
public abstract class DefaultPreferenceSetting implements PreferenceSetting {

    private final boolean isExpert;

    /**
     * Constructs a new DefaultPreferenceSetting.
     *
     * (Not an expert option by default.)
     */
    protected DefaultPreferenceSetting() {
        this(false);
    }

    /**
     * Constructs a new DefaultPreferenceSetting.
     *
     * @param isExpert true, if it is an expert option
     */
    protected DefaultPreferenceSetting(boolean isExpert) {
        this.isExpert = isExpert;
    }

    @Override
    public boolean isExpert() {
        return isExpert;
    }

    /**
     * Saves state from a {@link JCheckBox} to a boolean preference.
     * @param prefName preference name
     * @param cb check box
     * @since 13050
     */
    protected static void saveBoolean(String prefName, JCheckBox cb) {
        Config.getPref().putBoolean(prefName, cb.isSelected());
    }

    /**
     * Saves text from a {@link JTextField} to a double preference.
     * @param prefName preference name
     * @param tf text field
     * @since 13050
     */
    protected static void saveDouble(String prefName, JTextField tf) {
        String text = tf.getText();
        try {
            Config.getPref().putDouble(prefName, JosmDecimalFormatSymbolsProvider.parseDouble(text));
        } catch (NumberFormatException e) {
            Logging.warn("Unable to save '" + text + "' as a double value for preference " + prefName);
            Logging.trace(e);
        }
    }

    /**
     * Saves text from a {@link JTextField} to an integer preference.
     * @param prefName preference name
     * @param tf text field
     * @since 13050
     */
    protected static void saveInt(String prefName, JTextField tf) {
        String text = tf.getText();
        try {
            Config.getPref().putInt(prefName, Integer.parseInt(text));
        } catch (NumberFormatException e) {
            Logging.warn("Unable to save '" + text + "' as an integer value for preference " + prefName);
            Logging.trace(e);
        }
    }
}
