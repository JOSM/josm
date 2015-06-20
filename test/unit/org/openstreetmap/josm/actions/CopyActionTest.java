// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

public class CopyActionTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testCopyStringWay() throws Exception {
        final Way way = new Way(123L);
        assertThat(CopyAction.getCopyString(Collections.singleton(way)), is("way 123"));
    }

    @Test
    public void testCopyStringWayRelation() throws Exception {
        final Way way = new Way(123L);
        final Relation relation = new Relation(456);
        assertThat(CopyAction.getCopyString(Arrays.asList(way, relation)), is("way 123,relation 456"));
        assertThat(CopyAction.getCopyString(Arrays.asList(relation, way)), is("relation 456,way 123"));
    }
}
