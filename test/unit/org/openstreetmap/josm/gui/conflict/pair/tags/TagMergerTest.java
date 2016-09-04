// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagMerger} class.
 */
public class TagMergerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link TagMerger#TagMerger}.
     */
    @Test
    public void testTagMerger() {
        assertNotNull(new TagMerger());
    }
}
