// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DateUtilsTest {
    @Test
    public void testMapDate() throws Exception {
        assertThat(DateUtils.fromString("2012-08-13T15:10:37Z").getTime(), is(1344870637000L));

    }

    @Test
    public void testNoteDate() throws Exception {
        assertThat(DateUtils.fromString("2014-11-29 22:08:50 UTC").getTime(), is(1417298930000L));
    }
}