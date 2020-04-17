// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link AddTagsDialog}.
 */
public class AddTagsDialogTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link AddTagsDialog#parseUrlTagsToKeyValues}
     */
    @Test
    public void testParseUrlTagsToKeyValues() {
        Map<String, String> strings = AddTagsDialog.parseUrlTagsToKeyValues("wikipedia:de=Residenzschloss Dresden|name:en=Dresden Castle");
        assertEquals(2, strings.size());
        assertEquals("Residenzschloss Dresden", strings.get("wikipedia:de"));
        assertEquals("Dresden Castle", strings.get("name:en"));
    }
}
