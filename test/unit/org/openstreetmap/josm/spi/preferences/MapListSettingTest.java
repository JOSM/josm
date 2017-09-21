// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link MapListSetting}.
 */
public class MapListSettingTest {
    /**
     * This is a preference test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of methods {@link MapListSetting#equals} and {@link MapListSetting#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(MapListSetting.class).usingGetClass()
            .withIgnoredFields("isNew", "time")
            .verify();
    }
}
