// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import nl.jqno.equalsverifier.EqualsVerifier;
import oauth.signpost.OAuthConsumer;

/**
 * Unit tests for class {@link OAuthToken}.
 */
@BasicPreferences
class OAuthTokenTest {

    /**
     * Unit test of method {@link OAuthToken#createToken}.
     */
    @Test
    void testCreateToken() {
        OAuthConsumer defCon = OAuthParameters.createDefault().buildConsumer();
        assertNotNull(defCon);
        OAuthToken defTok = OAuthToken.createToken(defCon);
        assertNotNull(defTok);
        assertEquals(defCon.getToken(), defTok.getKey());
        assertEquals(defCon.getTokenSecret(), defTok.getSecret());
        assertEquals(defTok, new OAuthToken(defTok));
    }

    /**
     * Unit test of methods {@link OAuthToken#equals} and {@link OAuthToken#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(OAuthToken.class).usingGetClass().verify();
    }
}
