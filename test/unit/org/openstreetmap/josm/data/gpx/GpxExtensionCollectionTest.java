// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Unit tests of {@link GpxExtensionCollection}.
 */
class GpxExtensionCollectionTest {
    @BeforeEach
    void before() {
        Logging.clearLastErrorAndWarnings();
    }

    @Test
    void testCloseChildWithEmptyStack() {
        GpxExtensionCollection collection = new GpxExtensionCollection();
        collection.closeChild("foo", "bar");
        collection.closeChild("foo", "bar");
        List<String> logs = Logging.getLastErrorAndWarnings();
        assertEquals(2, logs.size());
        assertTrue(logs.get(0).endsWith("W: Can't close child 'foo', no element in stack."));
        assertTrue(logs.get(1).endsWith("W: Can't close child 'foo', no element in stack."));
    }

    @Test
    void testCloseChildWithAnotherInStack() {
        GpxExtensionCollection collection = new GpxExtensionCollection();
        collection.openChild(null, "baz", new AttributesImpl());
        collection.closeChild("foo", "bar");
        List<String> logs = Logging.getLastErrorAndWarnings();
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).endsWith("W: Couldn't close child 'foo', closed 'baz' instead."));
    }
}
