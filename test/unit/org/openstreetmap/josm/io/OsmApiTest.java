// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link OsmApi} class.
 */
@BasicPreferences
@org.openstreetmap.josm.testutils.annotations.OsmApi
class OsmApiTest {
    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12675">Bug #12675</a>.
     * @throws IllegalDataException if an error occurs
     */
    @Test
    void testTicket12675() throws IllegalDataException {
        OsmApi api = OsmApi.getOsmApi();
        Changeset cs = new Changeset();
        cs.setUser(User.getAnonymous());
        cs.setId(38038262);
        String xml = api.toXml(cs);
        assertEquals("<?xml version='1.0' encoding='UTF-8'?>\n"+
                     "<osm version='0.6' generator='JOSM'>\n"+
                     "  <changeset id='38038262' user='&lt;anonymous&gt;' uid='-1' open='false'>\n"+
                     "  </changeset>\n"+
                     "</osm>\n", xml.replace("\r", ""));
        Changeset cs2 = OsmChangesetParser.parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                NullProgressMonitor.INSTANCE).iterator().next();
        assertEquals(User.getAnonymous(), cs2.getUser());
    }

    /**
     * Unit test of {@link OsmApi#normalizeApiUrl}: white space and trailing slashes are removed.
     */
    @Test
    void testNormalizeApiUrl() {
        assertEquals("https://example.com/api", OsmApi.normalizeApiUrl("https://example.com/api"));
        assertEquals("https://example.com/api", OsmApi.normalizeApiUrl("https://example.com/api/"));
        assertEquals("https://example.com/api", OsmApi.normalizeApiUrl("https://example.com/api///"));
        assertEquals("https://example.com/api", OsmApi.normalizeApiUrl("  https://example.com/api/\t"));
        assertEquals("https://example.com", OsmApi.normalizeApiUrl("https://example.com/"));
        assertEquals("", OsmApi.normalizeApiUrl("   "));
        assertNull(OsmApi.normalizeApiUrl(null));
        // the slashes of the scheme must not be eaten
        assertEquals("https://", OsmApi.normalizeApiUrl("https://"));
    }

    /**
     * A URL which only differs by a trailing slash is the same API, and is stored without the slash.
     */
    @Test
    void testGetOsmApiIgnoresTrailingSlash() {
        OsmApi api = OsmApi.getOsmApi("https://example.com/api/");
        assertEquals("https://example.com/api", api.getServerUrl());
        assertSame(api, OsmApi.getOsmApi("https://example.com/api"));
        assertSame(api, OsmApi.getOsmApi(" https://example.com/api// "));
    }

    /**
     * An API URL stored with a trailing slash is used without it, so that code comparing it to a known URL or
     * deriving another URL from it works. Non-regression test for the OAuth and "History (web)" breakage.
     */
    @Test
    void testTrailingSlashInPreference() {
        Config.getPref().put("osm-server.url", "https://example.com/api/");
        assertEquals("https://example.com/api", OsmApi.getOsmApi().getServerUrl());
        assertEquals("https://example.com/api/", OsmApi.getOsmApi().getBaseUrl());
    }
}
