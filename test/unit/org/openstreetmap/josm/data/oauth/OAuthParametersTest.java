// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openstreetmap.josm.io.OsmApi;

/**
 * Unit tests for class {@link OAuthParameters}.
 */
public class OAuthParametersTest {

    /**
     * Unit test of method {@link OAuthParameters#createDefault}.
     */
    @Test
    public void testCreateDefault() {
        OAuthParameters def = OAuthParameters.createDefault();
        assertNotNull(def);
        assertEquals(def, OAuthParameters.createDefault(OsmApi.DEFAULT_API_URL));
        OAuthParameters dev = OAuthParameters.createDefault("http://api06.dev.openstreetmap.org/api");
        assertNotNull(dev);
        assertNotEquals(def, dev);
        assertEquals(def, OAuthParameters.createDefault("wrong_url"));
    }

    /**
     * Unit test of method {@link OAuthParameters#equals}.
     */
    @Test
    public void testEquals() {
        OAuthParameters dev = OAuthParameters.createDefault("http://master.apis.dev.openstreetmap.org/api");
        OAuthParameters dev2 = new OAuthParameters(dev);
        assertEquals(dev, dev2);
    }
}
