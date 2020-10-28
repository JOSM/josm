// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of the {@code AbstractPrimitive} class.
 */
class AbstractPrimitiveTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link AbstractPrimitive#isUndeleted} method.
     */
    @Test
    void testIsUndeleted() {
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
    void testHasTagDifferent() {
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
