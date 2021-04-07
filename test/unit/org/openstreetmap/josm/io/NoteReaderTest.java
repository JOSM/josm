// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.Note.State;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.notes.NoteComment.Action;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link NoteReader} class.
 */
class NoteReaderTest {

    /**
     * Test to read the first note of OSM database.
     * @throws SAXException if any SAX parsing error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testNoteReader() throws SAXException, IOException {
        List<Note> list = new NoteReader(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<osm version=\"0.6\" generator=\"OpenStreetMap server\">\n"+
            "<note lon=\"68.86415\" lat=\"36.7232991\">\n"+
            "  <id>4</id>\n"+
            "  <url>http://api.openstreetmap.org/api/0.6/notes/4</url>\n"+
            "  <reopen_url>http://api.openstreetmap.org/api/0.6/notes/4/reopen</reopen_url>\n"+
            "  <date_created>2013-04-24 08:07:02 UTC</date_created>\n"+
            "  <status>closed</status>\n"+
            "  <date_closed>2013-04-24 08:08:51 UTC</date_closed>\n"+
            "  <comments>\n"+
            "    <comment>\n"+
            "      <date>2013-04-24 08:07:02 UTC</date>\n"+
            "      <uid>1626</uid>\n"+
            "      <user>FredB</user>\n"+
            "      <user_url>http://www.openstreetmap.org/user/FredB</user_url>\n"+
            "      <action>opened</action>\n"+
            "      <text>test</text>\n"+
            "      <html>&lt;p&gt;test&lt;/p&gt;</html>\n"+
            "    </comment>\n"+
            "    <comment>\n"+
            "      <date>2013-04-24 08:08:51 UTC</date>\n"+
            "      <uid>1626</uid>\n"+
            "      <user>FredB</user>\n"+
            "      <user_url>http://www.openstreetmap.org/user/FredB</user_url>\n"+
            "      <action>closed</action>\n"+
            "      <text></text>\n"+
            "      <html>&lt;p&gt;&lt;/p&gt;</html>\n"+
            "    </comment>\n"+
            "  </comments>\n"+
            "</note>\n"+
            "</osm>").parse();

        assertEquals(1, list.size());
        Note n = list.get(0);
        assertEquals(DateUtils.parseInstant("2013-04-24 08:08:51 UTC"), n.getClosedAt());
        assertEquals(DateUtils.parseInstant("2013-04-24 08:07:02 UTC"), n.getCreatedAt());
        assertEquals(4, n.getId());
        assertEquals(new LatLon(36.7232991, 68.86415), n.getLatLon());
        assertEquals(State.CLOSED, n.getState());
        List<NoteComment> comments = n.getComments();
        assertEquals(2, comments.size());

        NoteComment c1 = comments.get(0);
        assertEquals(c1, n.getFirstComment());
        assertEquals(DateUtils.parseInstant("2013-04-24 08:07:02 UTC"), c1.getCommentTimestamp());
        assertEquals(Action.OPENED, c1.getNoteAction());
        assertEquals("test", c1.getText());
        assertEquals(User.createOsmUser(1626, "FredB"), c1.getUser());

        NoteComment c2 = comments.get(1);
        assertEquals(Action.CLOSED, c2.getNoteAction());
        assertEquals("", c2.getText());
    }

    /**
     * Non-regression test for bug #12393.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket12393() throws Exception {
        // CHECKSTYLE.OFF: LineLength
        new NoteReader(
            "<note id=\"233775\" lat=\"48.2411985\" lon=\"-122.3744820\" created_at=\"2014-08-31T17:13:29Z\" closed_at=\"2015-09-06T23:35:14Z\">"+
            "<comment action=\"opened\" timestamp=\"2014-08-31T17:13:29Z\" uid=\"7247\" user=\"goldfndr\">Jump Start Espresso | 26930</comment>"+
            "<comment action=\"hidden\" timestamp=\"2015-09-06T23:34:26Z\" uid=\"355617\" user=\"pnorman\"></comment>"+
            "<comment action=\"reopened\" timestamp=\"2015-09-06T23:34:38Z\" uid=\"355617\" user=\"pnorman\"></comment>"+
            "<comment action=\"closed\" timestamp=\"2015-09-06T23:35:14Z\" uid=\"355617\" user=\"pnorman\">mapped, but inadvertently hid the note</comment>"+
            "</note>").parse();
        // CHECKSTYLE.ON: LineLength
    }
}
