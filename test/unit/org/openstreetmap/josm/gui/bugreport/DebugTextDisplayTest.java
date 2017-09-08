// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit test of {@link DebugTextDisplay} class.
 */
public class DebugTextDisplayTest {
    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test {@link DebugTextDisplay#getCodeText}
     */
    @Test
    public void testGetCodeText() {
        assertEquals("test", new DebugTextDisplay("test").getCodeText());
    }

    /**
     * Test {@link DebugTextDisplay#copyToClipboard}
     */
    @Test
    public void testCopyToClipboard() {
        new DebugTextDisplay("copy").copyToClipboard();
        assertEquals(String.format("{{{%ncopy%n}}}"), ClipboardUtils.getClipboardStringContent());
    }
}
