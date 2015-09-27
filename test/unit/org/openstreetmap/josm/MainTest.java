// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MainTest {
    @Test
    public void testParamType() throws Exception {
        assertThat(Main.paramType("48.000,16.000,48.001,16.001"), is(Main.DownloadParamType.bounds));

    }
}
