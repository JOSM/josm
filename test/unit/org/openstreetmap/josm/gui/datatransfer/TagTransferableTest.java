// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.datatransfer.data.TagTransferData;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagTransferable} class.
 */
public class TagTransferableTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link TagTransferable#isDataFlavorSupported} method.
     */
    @Test
    public void testIsDataFlavorSupported() {
        TagTransferable tt = new TagTransferable(null);
        assertTrue(tt.isDataFlavorSupported(TagTransferData.FLAVOR));
        assertTrue(tt.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertFalse(tt.isDataFlavorSupported(DataFlavor.imageFlavor));
        assertFalse(tt.isDataFlavorSupported(null));
    }

    /**
     * Test of {@link RelationMemberTransferable#getTransferData} method - nominal case.
     * @throws Exception if an error occurs
     */
    @Test
    public void testGetTransferDataNominal() throws Exception {
        Map<String, String> tags = new HashMap<>();
        tags.put("foo", "bar");
        TagTransferable tt = new TagTransferable(new TagTransferData(tags));
        assertEquals("foo=bar", tt.getTransferData(DataFlavor.stringFlavor));
        assertEquals(tags, ((TagTransferData) tt.getTransferData(TagTransferData.FLAVOR)).getTags());
    }

    /**
     * Test of {@link TagTransferable#getTransferData} method - error case.
     * @throws UnsupportedFlavorException always
     * @throws IOException never
     */
    @Test(expected = UnsupportedFlavorException.class)
    public void testGetTransferDataError() throws UnsupportedFlavorException, IOException {
        new TagTransferable(null).getTransferData(null);
    }
}
