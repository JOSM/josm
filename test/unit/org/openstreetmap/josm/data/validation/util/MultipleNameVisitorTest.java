// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Unit tests for class {@link MultipleNameVisitor}.
 */
public class MultipleNameVisitorTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

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
