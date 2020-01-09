// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OsmUtilsTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    public void testCreatePrimitive() throws Exception {
        final OsmPrimitive p = OsmUtils.createPrimitive("way name=Foo railway=rail");
        assertTrue(p instanceof Way);
        assertEquals(2, p.keySet().size());
        assertEquals("Foo", p.get("name"));
        assertEquals("rail", p.get("railway"));
    }

    @Test
    public void testArea() throws Exception {
        final OsmPrimitive p = OsmUtils.createPrimitive("area name=Foo railway=rail");
        assertEquals(OsmPrimitiveType.WAY, p.getType());
        assertTrue(p.getKeys().equals(OsmUtils.createPrimitive("way name=Foo railway=rail").getKeys()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePrimitiveFail() throws Exception {
        OsmUtils.createPrimitive("noway name=Foo");
    }

    @Test
    public void testSplitMultipleValues() {
        // examples from https://wiki.openstreetmap.org/wiki/Semi-colon_value_separator
        assertEquals(Arrays.asList("B500", "B550"), OsmUtils.splitMultipleValues("B500;B550").collect(Collectors.toList()));
        assertEquals(Arrays.asList("B500", "B550"), OsmUtils.splitMultipleValues("B500 ; B550").collect(Collectors.toList()));
        assertEquals(Arrays.asList("Tu-Fr 08:00-18:00", "Mo 09:00-18:00", "Sa 09:00-12:00", "closed Aug"),
                OsmUtils.splitMultipleValues("Tu-Fr 08:00-18:00;Mo 09:00-18:00;Sa 09:00-12:00;closed Aug").collect(Collectors.toList()));
    }
}
