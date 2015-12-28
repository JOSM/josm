// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import oauth.signpost.OAuthConsumer;

import org.junit.Test;

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
    }

    /**
     * Unit test of method {@link OAuthToken#equals}.
     */
    @Test
    public void testEquals() {
        OAuthToken tok = new OAuthToken(
                OAuthParameters.DEFAULT_JOSM_CONSUMER_KEY,
                OAuthParameters.DEFAULT_JOSM_CONSUMER_SECRET);
        OAuthToken tok2 = new OAuthToken(tok);
        assertEquals(tok, tok2);
    }
}
