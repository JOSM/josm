// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationMemberConflictResolver} class.
 */
public class RelationMemberConflictResolverTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test for {@link RelationMemberConflictResolver#RelationMemberConflictResolver}.
     */
    @Test
    public void testRelationMemberConflictResolver() {
        assertNotNull(new RelationMemberConflictResolver(null));
    }
}
