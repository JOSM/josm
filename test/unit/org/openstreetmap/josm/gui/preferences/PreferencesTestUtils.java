// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.junit.Assert.assertEquals;

/**
 * Various utils, useful for preferences unit tests.
 */
public final class PreferencesTestUtils {

    private PreferencesTestUtils() {
        // Hide constructor for utility classes
    }

    /**
     * Generic test for {@link PreferenceSetting#addGui(PreferenceTabbedPane)}.
     * @param factory setting factory to test
     * @param parentClass optional parent setting, can be {@code null}
     */
    public static void doTestPreferenceSettingAddGui(
            PreferenceSettingFactory factory, Class<? extends DefaultTabPreferenceSetting> parentClass) {
        doTestPreferenceSettingAddGui(factory, parentClass, 1);
    }

    /**
     * Generic test for {@link PreferenceSetting#addGui(PreferenceTabbedPane)}.
     * @param factory setting factory to test
     * @param parentClass optional parent setting, can be {@code null}
     * @param increment expected tab number increment
     */
    public static void doTestPreferenceSettingAddGui(
            PreferenceSettingFactory factory, Class<? extends DefaultTabPreferenceSetting> parentClass, int increment) {
        PreferenceSetting setting = factory.createPreferenceSetting();
        PreferenceTabbedPane tabPane = new PreferenceTabbedPane();
        tabPane.buildGui();
        int tabs = parentClass != null ? tabPane.getSetting(parentClass).getTabPane().getTabCount() : -1;
        setting.addGui(tabPane);
        if (parentClass != null) {
            assertEquals(tabs + increment, tabPane.getSetting(parentClass).getTabPane().getTabCount());
            assertEquals(tabPane.getSetting(parentClass), ((SubPreferenceSetting) setting).getTabPreferenceSetting(tabPane));
        }
        setting.ok();
    }
}
