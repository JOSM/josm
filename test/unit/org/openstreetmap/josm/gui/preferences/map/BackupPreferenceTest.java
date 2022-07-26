// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link BackupPreference} class.
 */
@BasicPreferences
class BackupPreferenceTest {
    /**
     * Unit test of {@link BackupPreference#BackupPreference}.
     */
    @Test
    void testBackupPreference() {
        assertNotNull(new BackupPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link BackupPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new BackupPreference.Factory(), null);
    }
}
