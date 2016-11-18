// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
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
     * Test of {@link OsmLinkPaster#parseLatLon(String)}
     */
    @Test
    public void testParseLatLon() {
        assertEquals(new LatLon(51.71873, 8.76164),
                OsmLinkPaster.parseLatLon("https://www.openstreetmap.org/#map=17/51.71873/8.76164"));
        assertNull(OsmLinkPaster.parseLatLon("http://www.openstreetmap.org/"));
        assertNull(OsmLinkPaster.parseLatLon("foo-bar"));
    }
}
