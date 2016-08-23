// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link OsmLinkPaster}
 * @author Michael Zangl
 */
public class OsmLinkPasterTest {
    /**
     * No dependencies
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link OsmLinkPaster#parseIds(String)}
     */
    @Test
    public void testParseIds() {
        assertArrayEquals(new Object[] {new SimplePrimitiveId(1234, OsmPrimitiveType.NODE) },
                OsmLinkPaster.parseIds("http://www.openstreetmap.org/node/1234").toArray());
        assertArrayEquals(new Object[] {new SimplePrimitiveId(1234, OsmPrimitiveType.WAY) },
                OsmLinkPaster.parseIds("http://www.openstreetmap.org/way/1234").toArray());
        assertArrayEquals(new Object[] {new SimplePrimitiveId(1234, OsmPrimitiveType.RELATION) },
                OsmLinkPaster.parseIds("http://www.openstreetmap.org/relation/1234").toArray());

        assertArrayEquals(new Object[] {new SimplePrimitiveId(1234, OsmPrimitiveType.NODE) },
                OsmLinkPaster.parseIds("http://www.osm.org/node/1234").toArray());
        assertArrayEquals(new Object[] {new SimplePrimitiveId(1234, OsmPrimitiveType.WAY) },
                OsmLinkPaster.parseIds("http://osm.org/way/1234").toArray());
        assertArrayEquals(new Object[] {new SimplePrimitiveId(1234, OsmPrimitiveType.RELATION) },
                OsmLinkPaster.parseIds("https://www.openstreetmap.org/relation/1234").toArray());

        assertArrayEquals(new Object[0], OsmLinkPaster.parseIds("http://www.openstreetmap.org/xx/1234").toArray());
        assertArrayEquals(new Object[0], OsmLinkPaster.parseIds("http://www.openstreetmap.org/way/1234x").toArray());
        assertArrayEquals(new Object[0], OsmLinkPaster.parseIds("").toArray());
    }
}
