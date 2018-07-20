// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.Test;
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlException;
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlParser;

/**
 * Unit tests of {@link ChangesetQueryUrlParser} class
 */
public class ChangesetQueryUrlParserTest {

    /**
     * Basic unit test of {@link ChangesetQueryUrlParser#parse}
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testParseBasic() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();

        // OK
        parser.parse("");

        // should be OK
        ChangesetQuery q = parser.parse(null);
        assertNotNull(q);

        // should be OK
        q = parser.parse("");
        assertNotNull(q);
    }

    private static void shouldFail(String s) {
        try {
            new ChangesetQueryUrlParser().parse(s);
            fail("should throw exception");
        } catch (ChangesetQueryUrlException e) {
            // OK
        }
    }

    /**
     * Parse "uid="
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testUid() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        ChangesetQuery q;

        // OK
        q = parser.parse("uid=1234");
        assertNotNull(q);

        shouldFail("uid=0");
        shouldFail("uid=-1");
        shouldFail("uid=abc");
    }

    /**
     * Parse "display_name="
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testDisplayName() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        ChangesetQuery q;

        // OK
        q = parser.parse("display_name=abcd");
        assertNotNull(q);
        assertEquals("abcd", q.getUserName());
    }

    /**
     * Parse "open="
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testOpen() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        ChangesetQuery q;

        // OK
        q = parser.parse("open=true");
        assertNotNull(q);
        assertEquals(Boolean.TRUE, q.getRestrictionToOpen());

        // OK
        q = parser.parse("open=false");
        assertNotNull(q);
        assertEquals(Boolean.FALSE, q.getRestrictionToOpen());

        // illegal value for open
        shouldFail("open=abcd");
    }

    /**
     * Parse "closed="
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testClosed() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        ChangesetQuery q;

        // OK
        q = parser.parse("closed=true");
        assertNotNull(q);
        assertEquals(Boolean.TRUE, q.getRestrictionToClosed());

        // OK
        q = parser.parse("closed=false");
        assertNotNull(q);
        assertEquals(Boolean.FALSE, q.getRestrictionToClosed());

        // illegal value for open
        shouldFail("closed=abcd");
    }

    /**
     * Check we can't have both an uid and a display name
     */
    @Test
    public void testUidAndDisplayName() {
        shouldFail("uid=1&display_name=abcd");
    }

    /**
     * Parse "time="
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testTime() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        ChangesetQuery q;

        // OK
        q = parser.parse("time=2009-12-25T10:00:00Z");
        assertNotNull(q);
        assertNotNull(q.getClosedAfter());
        OffsetDateTime cal = q.getClosedAfter().toInstant().atOffset(ZoneOffset.UTC);
        assertEquals(OffsetDateTime.of(2009, 12, 25, 10, 0, 0, 0, ZoneOffset.UTC), cal);

        // OK
        q = parser.parse("time=2009-12-25T10:00:00Z,2009-11-25T10:00:00Z");
        assertNotNull(q);
        assertNotNull(q.getClosedAfter());
        assertNotNull(q.getCreatedBefore());

        // should fail
        shouldFail("time=asdf");
    }

    /**
     * Parse "bbox="
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testBbox() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        ChangesetQuery q;

        // OK
        q = parser.parse("bbox=-1,-1,1,1");
        assertNotNull(q);
        assertNotNull(q.getBounds());

        // should fail
        shouldFail("bbox=-91,-1,1,1");
        shouldFail("bbox=-1,-181,1,1");
        shouldFail("bbox=-1,-1,91,1");
        shouldFail("bbox=-1,-1,1,181");
        shouldFail("bbox=-1,-1,1");
    }

    /**
     * Parse "changesets="
     * @throws ChangesetQueryUrlException never
     */
    @Test
    public void testChangesetIds() throws ChangesetQueryUrlException {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        ChangesetQuery q;

        // OK
        q = parser.parse("changesets=1,2,3");
        assertNotNull(q);
        assertTrue(q.getAdditionalChangesetIds().containsAll(Arrays.asList(1L, 2L, 3L)));
        assertEquals(3, q.getAdditionalChangesetIds().size());

        // OK
        q = parser.parse("changesets=1,2,3,4,1");
        assertNotNull(q);
        assertTrue(q.getAdditionalChangesetIds().containsAll(Arrays.asList(1L, 2L, 3L, 4L)));
        assertEquals(4, q.getAdditionalChangesetIds().size());

        // OK
        q = parser.parse("changesets=");
        assertNotNull(q);
        assertEquals(0, q.getAdditionalChangesetIds().size());

        // should fail
        shouldFail("changesets=foo");
    }
}
