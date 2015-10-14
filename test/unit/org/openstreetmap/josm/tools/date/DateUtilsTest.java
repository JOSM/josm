// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of {@link DateUtils} class.
 */
public class DateUtilsTest {

    /**
     * Test to parse date as returned for map data.
     */
    @Test
    public void testMapDate() {
        assertEquals(1344870637000L, DateUtils.fromString("2012-08-13T15:10:37Z").getTime());
    }

    /**
     * Test to parse date as returned for note data.
     */
    @Test
    public void testNoteDate() {
        assertEquals(1417298930000L, DateUtils.fromString("2014-11-29 22:08:50 UTC").getTime());
    }
}
