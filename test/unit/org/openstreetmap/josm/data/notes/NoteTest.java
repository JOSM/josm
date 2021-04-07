// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
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
}
