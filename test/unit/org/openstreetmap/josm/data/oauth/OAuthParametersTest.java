// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link OAuthParameters}.
 */
public class OAuthParametersTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of method {@link OAuthParameters#createDefault}.
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void testCreateDefault() {
        OAuthParameters def = OAuthParameters.createDefault();
        assertNotNull(def);
        assertEquals(def, OAuthParameters.createDefault(OsmApi.DEFAULT_API_URL));
        OAuthParameters dev = OAuthParameters.createDefault("http://api06.dev.openstreetmap.org/api");
        assertNotNull(dev);
        assertNotEquals(def, dev);
        Logging.setLogLevel(Logging.LEVEL_TRACE); // enable trace for line coverage
        assertEquals(def, OAuthParameters.createDefault("wrong_url"));
        OAuthParameters dev2 = new OAuthParameters(dev);
        assertEquals(dev, dev2);
    }

    /**
     * Unit test of method {@link OAuthParameters#createFromPreferences}.
     * @deprecated to remove end of 2017
     */
    @Test
    @Deprecated
    public void testCreateFromPreferences() {
        assertNotNull(OAuthParameters.createFromPreferences(Main.pref));
    }

    /**
     * Unit test of methods {@link OAuthParameters#equals} and {@link OAuthParameters#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(OAuthParameters.class).usingGetClass().verify();
    }
}
