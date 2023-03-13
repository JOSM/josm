// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link OsmUtils}.
 */
class OsmUtilsTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link OsmUtils#createPrimitive}
     */
    @Test
    void testCreatePrimitive() {
        final OsmPrimitive p = OsmUtils.createPrimitive("way name=Foo railway=rail");
        assertInstanceOf(Way.class, p);
        assertEquals(2, p.getKeys().size());
        assertEquals("Foo", p.get("name"));
        assertEquals("rail", p.get("railway"));
    }

    /**
     * Unit test of {@link OsmUtils#createPrimitive}
     */
    @Test
    void testArea() {
        final OsmPrimitive p = OsmUtils.createPrimitive("area name=Foo railway=rail");
        assertEquals(OsmPrimitiveType.WAY, p.getType());
        assertEquals(p.getKeys(), OsmUtils.createPrimitive("way name=Foo railway=rail").getKeys());
    }

    /**
     * Unit test of {@link OsmUtils#createPrimitive}
     */
    @Test
    void testCreatePrimitiveFail() {
        assertThrows(IllegalArgumentException.class, () -> OsmUtils.createPrimitive("noway name=Foo"));
    }

    /**
     * Unit test of {@link OsmUtils#splitMultipleValues}
     */
    @Test
    void testSplitMultipleValues() {
        // examples from https://wiki.openstreetmap.org/wiki/Semi-colon_value_separator
        assertEquals(Arrays.asList("B500", "B550"), OsmUtils.splitMultipleValues("B500;B550").collect(Collectors.toList()));
        assertEquals(Arrays.asList("B500", "B550"), OsmUtils.splitMultipleValues("B500 ; B550").collect(Collectors.toList()));
        assertEquals(Arrays.asList("Tu-Fr 08:00-18:00", "Mo 09:00-18:00", "Sa 09:00-12:00", "closed Aug"),
                OsmUtils.splitMultipleValues("Tu-Fr 08:00-18:00;Mo 09:00-18:00;Sa 09:00-12:00;closed Aug").collect(Collectors.toList()));
    }

    /**
     * Unit test of {@link OsmUtils#isTrue}, {@link OsmUtils#isFalse}, {@link OsmUtils#getOsmBoolean}
     */
    @Test
    void testTrueFalse() {
        assertFalse(OsmUtils.isTrue(null));
        assertFalse(OsmUtils.isFalse(null));
        assertNull(OsmUtils.getOsmBoolean(null));
        assertTrue(OsmUtils.isTrue("yes"));
        assertFalse(OsmUtils.isFalse("yes"));
        assertEquals(Boolean.TRUE, OsmUtils.getOsmBoolean("yes"));
        assertTrue(OsmUtils.isFalse("no"));
        assertFalse(OsmUtils.isTrue("no"));
        assertEquals(Boolean.FALSE, OsmUtils.getOsmBoolean("no"));
        assertNull(OsmUtils.getOsmBoolean("foobar"));
    }
}
