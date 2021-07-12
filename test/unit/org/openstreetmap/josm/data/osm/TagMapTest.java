// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the {@code TagMap} class.
 */
class TagMapTest {

    /**
     * Unit test for {@link TagMap#toString}
     */
    @Test
    void testToString() {
        assertEquals("TagMap[]", new TagMap().toString());
        assertEquals("TagMap[key=val]", new TagMap(new String[]{"key", "val"}).toString());
        TagMap foobar = new TagMap(new String[]{"key", "val", "foo", "bar"});
        assertEquals("TagMap[key=val,foo=bar]", foobar.toString());
        assertEquals("TagMap[key=val,foo=bar]", new TagMap(foobar).toString());
        TagCollection coll = new TagCollection(Arrays.asList(new Tag("a", "b")));
        assertEquals("TagMap[a=b]", new TagMap(coll).toString());
    }
}
