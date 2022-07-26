// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;

/**
 * Unit tests of {@link Preferences}.
 */
@OsmApiType(OsmApiType.APIType.FAKE)
class PreferencesTest {
    /**
     * Test {@link Preferences#toXML}.
     */
    @Test
    void testToXml() {
        assertEquals(String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
            "<preferences xmlns='http://josm.openstreetmap.de/preferences-1.0' version='%d'>%n" +
            "  <tag key='osm-server.url' value='http://fake.xxx/api'/>%n" +
            "</preferences>%n", Version.getInstance().getVersion()),
                Preferences.main().toXML(true));
    }
}
