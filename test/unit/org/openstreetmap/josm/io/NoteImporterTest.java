// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit tests of {@link NoteImporter} class.
 */
public class NoteImporterTest {

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12531">Bug #12531</a>.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket12531() throws Exception {
        assertNull(Main.map);
        assertTrue(new NoteImporter().importDataHandleExceptions(
                new File(TestUtils.getRegressionDataFile(12531, "notes.osn")), null));
    }
}
