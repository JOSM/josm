// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link MultipleNameVisitor}.
 */
public class MultipleNameVisitorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform();

    /**
     * Non-regression test for bug #11967.
     */
    @Test
    public void testTicket11967() {
        MultipleNameVisitor visitor = new MultipleNameVisitor();
        visitor.visit(Arrays.asList(new Way(), new Way()));
        assertEquals("2 ways", visitor.toString());
    }
}
