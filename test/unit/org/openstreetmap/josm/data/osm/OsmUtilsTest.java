// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

public class OsmUtilsTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

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
}
