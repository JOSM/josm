// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.io.NoteReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link NoteComment}.
 */
class NoteTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link Note#toString} method.
     */
    @Test
    void testToString() {
        Note note = new Note(LatLon.ZERO);
        assertEquals("Note 0: null", note.toString());
        note.addComment(new NoteComment(Instant.now(), null, "foo", null, true));
        assertEquals("Note 0: foo", note.toString());
    }

    /**
     * Unit test of {@link Note#updateWith} method.
     */
    @Test
    void testUpdateWith() {
        Note n1 = new Note(LatLon.ZERO);
        n1.setId(1);
        Note n2 = new Note(LatLon.ZERO);
        n1.setId(2);
        assertNotEquals(n1, n2);
        n1.updateWith(n2);
        assertEquals(n1, n2);
    }

    /**
     * Unit test of methods {@link Note#equals} and {@link Note#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(Note.class).usingGetClass()
            .withIgnoredFields("latLon", "createdAt", "closedAt", "state", "comments")
            .suppress(Warning.NONFINAL_FIELDS)
            .withPrefabValues(LatLon.class, LatLon.ZERO, new LatLon(1, 1))
            .withPrefabValues(NoteComment.class,
                    new NoteComment(Instant.now(), null, "foo", null, true),
                    new NoteComment(Instant.now(), null, "bar", null, false))
            .verify();
    }

    @Test
    void testSorting() throws Exception {
        List<Note> list = new NoteReader("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<osm version=\"0.6\" generator=\"OpenStreetMap server\">\n" +
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
                "<note lon=\"23.2663071\" lat=\"50.7173607\">\n" +
                "  <id>1396945</id>\n" +
                "  <url>https://www.openstreetmap.org/api/0.6/notes/1396945</url>\n" +
                "  <comment_url>https://www.openstreetmap.org/api/0.6/notes/1396945/comment</comment_url>\n" +
                "  <close_url>https://www.openstreetmap.org/api/0.6/notes/1396945/close</close_url>\n" +
                "  <date_created>2018-05-17 15:41:06 UTC</date_created>\n" +
                "  <status>open</status>\n" +
                "  <comments>\n" +
                "  </comments>\n" +
                "</note>\n" +
                "</osm>\n").parse();
        // Non-regression test for ticket #20824
        list.sort(Note.DATE_COMPARATOR);
        list.sort(Note.DEFAULT_COMPARATOR);
        list.sort(Note.LAST_ACTION_COMPARATOR);
        list.sort(Note.USER_COMPARATOR);
    }
}
