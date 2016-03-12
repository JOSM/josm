// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.actions.PasteTagsAction.TagPaster;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.WayData;

/**
 * Unit tests for class {@link PasteTagsAction}.
 */
public class PasteTagsActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static boolean isHeterogeneousSource(PrimitiveData... t) {
        return new TagPaster(Arrays.asList(t), null).isHeterogeneousSource();
    }

    /**
     * Unit test of {@link TagPaster#isHeterogeneousSource}.
     */
    @Test
    public void testTagPasterIsHeterogeneousSource() {
        // 0 item
        assertFalse(isHeterogeneousSource());
        // 1 item
        assertFalse(isHeterogeneousSource(new NodeData()));
        assertFalse(isHeterogeneousSource(new WayData()));
        assertFalse(isHeterogeneousSource(new RelationData()));
        // 2 items of same type
        assertFalse(isHeterogeneousSource(new NodeData(), new NodeData()));
        assertFalse(isHeterogeneousSource(new WayData(), new WayData()));
        assertFalse(isHeterogeneousSource(new RelationData(), new RelationData()));
        // 2 items of different type
        assertTrue(isHeterogeneousSource(new NodeData(), new WayData()));
        assertTrue(isHeterogeneousSource(new NodeData(), new RelationData()));
        assertTrue(isHeterogeneousSource(new WayData(), new RelationData()));
    }
}
