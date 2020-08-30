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
        PreferenceSetting setting = factory.createPreferenceSetting();
        PreferenceTabbedPane tabPane = new PreferenceTabbedPane();
        tabPane.buildGui();
        int tabs = parentClass != null ? tabPane.getSetting(parentClass).getTabPane().getTabCount() : -1;
        setting.addGui(tabPane);
        if (parentClass != null) {
            assertEquals(tabs + 1, tabPane.getSetting(parentClass).getTabPane().getTabCount());
            assertEquals(tabPane.getSetting(parentClass), ((SubPreferenceSetting) setting).getTabPreferenceSetting(tabPane));
        }
        setting.ok();
    }
}
