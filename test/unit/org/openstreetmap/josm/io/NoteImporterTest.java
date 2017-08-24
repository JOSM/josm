// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests of {@link NoteImporter} class.
 */
public class NoteImporterTest {

    /**
     * Use the test rules to remove any layers and reset state.
     */
    @Rule
    public final JOSMTestRules rules = new JOSMTestRules();

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12531">Bug #12531</a>.
     */
    @Test
    public void testTicket12531() {
        MainApplication.getLayerManager().resetState();
        assertNull(MainApplication.getMap());
        assertTrue(new NoteImporter().importDataHandleExceptions(
                new File(TestUtils.getRegressionDataFile(12531, "notes.osn")), null));
    }
}
