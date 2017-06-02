// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of the {@code AbstractPrimitive} class.
 */
public class AbstractPrimitiveTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link AbstractPrimitive#isUndeleted} method.
     */
    @Test
    public void testIsUndeleted() {
        AbstractPrimitive p = new Node(1);
        p.setVisible(false);
        p.setDeleted(false);
        assertFalse(p.isVisible());
        assertFalse(p.isDeleted());
        assertTrue(p.isUndeleted());

        p.setVisible(false);
        p.setDeleted(true);
        assertFalse(p.isVisible());
        assertTrue(p.isDeleted());
        assertFalse(p.isUndeleted());

        p.setVisible(true);
        p.setDeleted(false);
        assertTrue(p.isVisible());
        assertFalse(p.isDeleted());
        assertFalse(p.isUndeleted());

        p.setVisible(true);
        p.setDeleted(true);
        assertTrue(p.isVisible());
        assertTrue(p.isDeleted());
        assertFalse(p.isUndeleted());
    }

    /**
     * Unit test of {@link AbstractPrimitive#hasTagDifferent} methods.
     */
    @Test
    public void testHasTagDifferent() {
        AbstractPrimitive p = new Node();

        assertFalse(p.hasTagDifferent("foo", "bar"));
        assertFalse(p.hasTagDifferent("foo", "bar", "baz"));
        assertFalse(p.hasTagDifferent("foo", Collections.singleton("bar")));

        p.put("foo", "bar");
        assertTrue(p.hasTagDifferent("foo", "baz"));
        assertFalse(p.hasTagDifferent("foo", "bar"));
        assertFalse(p.hasTagDifferent("foo", "bar", "baz"));
        assertFalse(p.hasTagDifferent("foo", Collections.singleton("bar")));

        p.put("foo", "foo");
        assertTrue(p.hasTagDifferent("foo", "bar"));
        assertTrue(p.hasTagDifferent("foo", "bar", "baz"));
        assertTrue(p.hasTagDifferent("foo", Collections.singleton("bar")));
    }
}
