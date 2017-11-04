// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;
import oauth.signpost.OAuthConsumer;

/**
 * Unit tests for class {@link OAuthToken}.
 */
public class OAuthTokenTest {

    /**
     * Unit test of method {@link OAuthToken#createToken}.
     */
    @Test
    public void testCreateToken() {
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
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(OAuthToken.class).usingGetClass().verify();
    }
}
