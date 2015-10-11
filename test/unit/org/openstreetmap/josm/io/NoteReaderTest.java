// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
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
public class NoteReaderTest {

    /**
     * Test to read the first note of OSM database.
     * @throws SAXException if any SAX parsing error occurs
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testNoteReader() throws SAXException, IOException {
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
        assertEquals(DateUtils.fromString("2013-04-24 08:08:51 UTC"), n.getClosedAt());
        assertEquals(DateUtils.fromString("2013-04-24 08:07:02 UTC"), n.getCreatedAt());
        assertEquals(4, n.getId());
        assertEquals(new LatLon(36.7232991, 68.86415), n.getLatLon());
        assertEquals(State.closed, n.getState());
        List<NoteComment> comments = n.getComments();
        assertEquals(2, comments.size());

        NoteComment c1 = comments.get(0);
        assertEquals(c1, n.getFirstComment());
        assertEquals(DateUtils.fromString("2013-04-24 08:07:02 UTC"), c1.getCommentTimestamp());
        assertEquals(Action.opened, c1.getNoteAction());
        assertEquals("test", c1.getText());
        assertEquals(User.createOsmUser(1626, "FredB"), c1.getUser());

        NoteComment c2 = comments.get(1);
        assertEquals(Action.closed, c2.getNoteAction());
        assertEquals("", c2.getText());
    }
}
