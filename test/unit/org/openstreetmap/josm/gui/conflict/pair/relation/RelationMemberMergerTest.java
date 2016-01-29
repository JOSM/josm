// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link RelationMemberMerger} class.
 */
public class RelationMemberMergerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link RelationMemberMerger#RelationMemberMerger}.
     */
    @Test
    public void testRelationMemberMerger() {
        assertNotNull(new RelationMemberMerger());
    }
}
