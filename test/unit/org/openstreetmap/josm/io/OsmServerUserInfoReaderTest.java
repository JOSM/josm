// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.tools.XmlUtils;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test of {@link OsmServerUserInfoReader}
 */
class OsmServerUserInfoReaderTest {

    /**
     * Unit test of {@link OsmServerUserInfoReader#buildFromXML}
     */
    @Test
    void testBuildFromXML() throws Exception {
        // from https://wiki.openstreetmap.org/wiki/API_v0.6#Details_of_the_logged-in_user
        String xml = "<osm version=\"0.6\" generator=\"OpenStreetMap server\">\n" +
                "\t<user display_name=\"Max Muster\" account_created=\"2006-07-21T19:28:26Z\" id=\"1234\">\n" +
                "\t\t<contributor-terms agreed=\"true\" pd=\"true\"/>\n" +
                "\t\t<img href=\"https://www.openstreetmap.org/attachments/users/images/000/000/1234/original/someLongURLOrOther.JPG\"/>\n" +
                "\t\t<roles></roles>\n" +
                "\t\t<changesets count=\"4182\"/>\n" +
                "\t\t<traces count=\"513\"/>\n" +
                "\t\t<blocks>\n" +
                "\t\t\t<received count=\"0\" active=\"0\"/>\n" +
                "\t\t</blocks>\n" +
                "\t\t<home lat=\"49.4733718952806\" lon=\"8.89285988577866\" zoom=\"3\"/>\n" +
                "\t\t<description>The description of your profile</description>\n" +
                "\t\t<languages>\n" +
                "\t\t\t<lang>de-DE</lang>\n" +
                "\t\t\t<lang>de</lang>\n" +
                "\t\t\t<lang>en-US</lang>\n" +
                "\t\t\t<lang>en</lang>\n" +
                "\t\t</languages>\n" +
                "\t\t<messages>\n" +
                "\t\t\t<received count=\"1\" unread=\"0\"/>\n" +
                "\t\t\t<sent count=\"0\"/>\n" +
                "\t\t</messages>\n" +
                "\t</user>\n" +
                "</osm>";
        Document document = XmlUtils.parseSafeDOM(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        UserInfo userInfo = OsmServerUserInfoReader.buildFromXML(document);
        assertEquals("Max Muster", userInfo.getDisplayName());
        assertEquals(1234, userInfo.getId());
        assertEquals(Instant.parse("2006-07-21T19:28:26Z"), userInfo.getAccountCreated());
    }
}
