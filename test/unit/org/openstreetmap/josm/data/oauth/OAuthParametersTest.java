// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link OAuthParameters}.
 */
@BasicPreferences
class OAuthParametersTest {
    /**
     * Unit test of method {@link OAuthParameters#createDefault}.
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    void testCreateDefault() {
        OAuthParameters def = OAuthParameters.createDefault();
        assertNotNull(def);
        assertEquals(def, OAuthParameters.createDefault(Config.getUrls().getDefaultOsmApiUrl()));
        OAuthParameters dev = OAuthParameters.createDefault("https://api06.dev.openstreetmap.org/api");
        assertNotNull(dev);
        assertNotEquals(def, dev);
        Logging.setLogLevel(Logging.LEVEL_TRACE); // enable trace for line coverage
        assertEquals(def, OAuthParameters.createDefault("wrong_url"));
        OAuthParameters dev2 = new OAuthParameters(dev);
        assertEquals(dev, dev2);
    }

    /**
     * Unit test of methods {@link OAuthParameters#equals} and {@link OAuthParameters#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(OAuthParameters.class).usingGetClass().verify();
    }
}
