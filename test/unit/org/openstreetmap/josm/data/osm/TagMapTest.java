// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of the {@code TagMap} class.
 */
public class TagMapTest {

    /**
     * Unit test for {@link TagMap#toString}
     */
    @Test
    public void testToString() {
        assertEquals("TagMap[]", new TagMap().toString());
        assertEquals("TagMap[key=val]", new TagMap(new String[]{"key", "val"}).toString());
        assertEquals("TagMap[key=val,foo=bar]", new TagMap(new String[]{"key", "val", "foo", "bar"}).toString());
    }
}
