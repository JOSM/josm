// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link RelationMemberConflictResolver} class.
 */
public class RelationMemberConflictResolverTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test for {@link RelationMemberConflictResolver#RelationMemberConflictResolver}.
     */
    @Test
    public void testRelationMemberConflictResolver() {
        assertNotNull(new RelationMemberConflictResolver(null));
    }
}
