// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    public void testBuildRequestWaysString() {
        MultiFetchOverpassObjectReader reader = new MultiFetchOverpassObjectReader();
        reader.append(Arrays.asList(new Way(123), new Way(126), new Way(130)));
        String requestString = reader.buildComplexRequestString();
        assertEquals("(way(id:123,126,130);>;);out meta;", requestString);
    }

    /**
     * Test {@link MultiFetchOverpassObjectReader#buildRequestString}
     */
    @Test
    public void testBuildRequestRelationsString() {
        MultiFetchOverpassObjectReader reader = new MultiFetchOverpassObjectReader();
        reader.append(Arrays.asList(new Relation(123), new Relation(126), new Relation(130)));
        reader.setRecurseDownRelations(true);
        String requestString = reader.buildComplexRequestString();
        assertEquals("relation(id:123,126,130);>>;out meta;", requestString);
        reader.setRecurseDownRelations(false);
        requestString = reader.buildComplexRequestString();
        assertEquals("relation(id:123,126,130);out meta;", requestString);
    }

    /**
     * Test {@link MultiFetchOverpassObjectReader#buildRequestString}
     */
    @Test
    public void testBuildComplexString() {
        MultiFetchOverpassObjectReader reader = new MultiFetchOverpassObjectReader();
        reader.setRecurseDownRelations(true);
        reader.append(Arrays.asList(new Relation(123), new Relation(126), new Relation(130), new Way(88), new Way(99),
                new Node(1)));
        String requestString = reader.buildComplexRequestString();
        assertEquals("(relation(id:123,126,130);>>;(way(id:88,99);>;);node(1););out meta;", requestString);
        reader.setRecurseDownRelations(false);
        requestString = reader.buildComplexRequestString();
        assertEquals("(relation(id:123,126,130);(way(id:88,99);>;);node(1););out meta;", requestString);
    }

}
