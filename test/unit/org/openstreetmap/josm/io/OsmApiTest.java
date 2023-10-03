// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

/**
 * Unit tests of {@link OsmApi} class.
 */
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
}
