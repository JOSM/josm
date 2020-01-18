// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests of {@link MultiFetchOverpassObjectReader}.
 */
public class MultiFetchOverpassObjectReaderTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test {@link MultiFetchOverpassObjectReader#buildRequestString}
     */
    @Test
    public void testBuildRequestString() {
        String requestString = new MultiFetchOverpassObjectReader()
                .buildRequestString(OsmPrimitiveType.WAY, new TreeSet<>(Arrays.asList(130L, 123L, 126L)));
        assertEquals("interpreter?data=" + Utils.encodeUrl("(way(123);>;way(126);>;way(130);>;);out meta;"), requestString);
    }

}
