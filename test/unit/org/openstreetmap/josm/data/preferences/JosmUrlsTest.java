// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link JosmUrls} class.
 */
class JosmUrlsTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().devAPI();

    /**
     * Unit test of {@link JosmUrls#getBaseUserUrl}.
     */
    @Test
    void testGetBaseUserUrl() {
        assertEquals("https://api06.dev.openstreetmap.org/user", Config.getUrls().getBaseUserUrl());
    }
}
