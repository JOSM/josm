// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test {@link StringSetting}.
 */
public class StringSettingTest {
    /**
     * This is a preference test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of methods {@link StringSetting#equals} and {@link StringSetting#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(StringSetting.class).usingGetClass()
            .withIgnoredFields("isNew", "time")
            .verify();
    }
}
