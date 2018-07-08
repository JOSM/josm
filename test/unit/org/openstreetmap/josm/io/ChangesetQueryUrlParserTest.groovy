// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io

import static groovy.test.GroovyAssert.shouldFail

import java.time.OffsetDateTime
import java.time.ZoneOffset

import org.junit.Test
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlException
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlParser

class ChangesetQueryUrlParserTest {

    @Test
    public void testConstructor() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
    }

    @Test
    public void testParseBasic() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();

        // OK
        parser.parse ""

        // should be OK
        ChangesetQuery q = parser.parse(null)
        assert q != null

        // should be OK
        q = parser.parse("")
        assert q != null
    }

    @Test
    public void testUid() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("uid=1234")
        assert q != null

        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("uid=0")
        }

        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("uid=-1")
        }

        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("uid=abc")
        }
    }

    @Test
    public void testDisplayName() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("display_name=abcd")
        assert q != null
        assert q.@userName == "abcd"
    }

    @Test
    public void testOpen() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("open=true")
        assert q != null
        assert q.@open == true

        // OK
        q = parser.parse("open=false")
        assert q != null
        assert q.@open == false

        // illegal value for open
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("open=abcd")
        }
    }

    @Test
    public void testClosed() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("closed=true")
        assert q != null
        assert q.@closed == true

        // OK
        q = parser.parse("closed=false")
        assert q != null
        assert q.@closed == false

        // illegal value for open
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("closed=abcd")
        }
    }

    @Test
    public void testUidAndDisplayName() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // we can't have both an uid and a display name
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("uid=1&display_name=abcd")
        }
    }

    @Test
    public void testTime() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("time=2009-12-25T10:00:00Z")
        assert q != null
        assert q.@closedAfter != null
        def cal = q.@closedAfter.toInstant().atOffset(ZoneOffset.UTC)
        assert cal == OffsetDateTime.of(2009, 12, 25, 10, 0, 0, 0, ZoneOffset.UTC)

        // OK
        q = parser.parse("time=2009-12-25T10:00:00Z,2009-11-25T10:00:00Z")
        assert q!= null
        assert q.@closedAfter != null
        assert q.@createdBefore != null

        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("time=asdf")
        }
    }

    @Test
    public void testBbox() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("bbox=-1,-1,1,1")
        assert q != null
        assert q.@bounds != null

        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("bbox=-91,-1,1,1")
        }

        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("bbox=-1,-181,1,1")
        }

        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("bbox=-1,-1,91,1")
        }
        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("bbox=-1,-1,1,181")
        }
        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("bbox=-1,-1,1")
        }
    }

    @Test
    public void testChangesetIds() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("changesets=1,2,3")
        assert q != null
        assert q.@changesetIds.containsAll(Arrays.asList(1L, 2L, 3L))
        assert q.@changesetIds.size() == 3

        // OK
        q = parser.parse("changesets=1,2,3,4,1")
        assert q != null
        assert q.@changesetIds.containsAll(Arrays.asList(1L, 2L, 3L, 4L))
        assert q.@changesetIds.size() == 4

        // OK
        q = parser.parse("changesets=")
        assert q != null
        assert q.@changesetIds.size() == 0

        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("changesets=foo")
        }
    }
}
