// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * Unit tests for class {@link AddTagsDialog}.
 */
public class AddTagsDialogTest {

    /**
     * Unit test of {@link AddTagsDialog#parseUrlTagsToKeyValues}
     */
    @Test
    public void testParseUrlTagsToKeyValues() {
        String[][] strings = AddTagsDialog.parseUrlTagsToKeyValues("wikipedia:de=Residenzschloss Dresden|name:en=Dresden Castle");
        assertEquals("[[wikipedia:de, Residenzschloss Dresden], [name:en, Dresden Castle]]", Arrays.deepToString(strings));
    }
}
