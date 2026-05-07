// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for class {@link AddTagsDialog}.
 */
@BasicPreferences
class AddTagsDialogTest {
    /**
     * Unit test of {@link AddTagsDialog#parseUrlTagsToKeyValues}
     */
    @Test
    void testParseUrlTagsToKeyValues() {
        Map<String, String> strings = AddTagsDialog.parseUrlTagsToKeyValues("wikipedia:de=Residenzschloss Dresden|name:en=Dresden Castle");
        assertEquals(2, strings.size());
        assertEquals("Residenzschloss Dresden", strings.get("wikipedia:de"));
        assertEquals("Dresden Castle", strings.get("name:en"));
    }

    /**
     * Unit test for issue #14490 "Support for escaping pipe character in remote control addtags parameters"
     * A single URL section (value of addtags=...) with a single escaped pipe sysmbol
     */
    @Test
    void testParseUrlTagsToKeyValues_OneKeyValuePair_OneEscapedPipe() {
        Map<String, String> strings = AddTagsDialog.parseUrlTagsToKeyValues("gtfs:route_id=de:mvv-muenchen:19-210\\|210");
        assertEquals(1, strings.size());
        assertEquals("de:mvv-muenchen:19-210|210", strings.get("gtfs:route_id"));
    }

    /**
     * Unit test for issue #14490 "Support for escaping pipe character in remote control addtags parameters"
     * A single URL section (value of addtags=...) with two escaped pipe sysmbols
     */
    @Test
    void testParseUrlTagsToKeyValues_OneKeyValuePair_TwoEscapedPipe() {
        Map<String, String> strings = AddTagsDialog.parseUrlTagsToKeyValues("gtfs:route_id=de:mvv-muenchen:19-210\\|210\\|RegionalBus:1179_3");
        assertEquals(1, strings.size());
        assertEquals("de:mvv-muenchen:19-210|210|RegionalBus:1179_3", strings.get("gtfs:route_id"));
    }

    /**
     * Unit test for issue #14490 "Support for escaping pipe character in remote control addtags parameters"
     * Two URL sections (values of addtags=...) with two escaped pipe sysmbols each
     */
    @Test
    void testParseUrlTagsToKeyValues_TwoKeyValuePairs_FourEscapedPipe() {
        Map<String, String> strings = AddTagsDialog.parseUrlTagsToKeyValues("gtfs:route_id=de:mvv-muenchen:19-210\\|210\\|"
        + "RegionalBus:1179_3|gtfs:trip_id:sample=de:mvv-muenchen:19-210\\|210\\|RegionalBus:1179-1-1-H-0-We#3-320-333");
        assertEquals(2, strings.size());
        assertEquals("de:mvv-muenchen:19-210|210|RegionalBus:1179_3", strings.get("gtfs:route_id"));
        assertEquals("de:mvv-muenchen:19-210|210|RegionalBus:1179-1-1-H-0-We#3-320-333", strings.get("gtfs:trip_id:sample"));
    }

    /**
     * Unit test for issue #14490 "Support for escaping pipe character in remote control addtags parameters"
     * Two URL sections (values of addtags=...) with four unescaped pipe sysmbols in the value parts
     * Same as the test above, but without escapeing the pipe symbols
     * This is how it worked before the patch, even if the pipe symbols would have been escaped
     */
    @Test
    void testParseUrlTagsToKeyValues_PipeInValue_NoEscapedPipe() {
        Map<String, String> strings = AddTagsDialog.parseUrlTagsToKeyValues("gtfs:route_id=de:mvv-muenchen:19-210|210|"
        + "RegionalBus:1179_3|gtfs:trip_id:sample=de:mvv-muenchen:19-210|210|RegionalBus:1179-1-1-H-0-We#3-320-333");
        assertEquals(0, strings.size());
    }
}
