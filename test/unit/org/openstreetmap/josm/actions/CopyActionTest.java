// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Unit tests for class {@link CopyAction}.
 */
public class CopyActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link CopyAction#getCopyString} method for a single way.
     */
    @Test
    public void testCopyStringWay() {
        final Way way = new Way(123L);
        assertEquals("way 123", CopyAction.getCopyString(Collections.singleton(way)));
    }

    /**
     * Test of {@link CopyAction#getCopyString} method for a way and a relation.
     */
    @Test
    public void testCopyStringWayRelation() {
        final Way way = new Way(123L);
        final Relation relation = new Relation(456);
        assertEquals("way 123,relation 456", CopyAction.getCopyString(Arrays.asList(way, relation)));
        assertEquals("relation 456,way 123", CopyAction.getCopyString(Arrays.asList(relation, way)));
    }
}
