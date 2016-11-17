// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

/**
 * Unit tests of {@link FileFilterAllFiles} class.
 */
public class FileFilterAllFilesTest {

    /**
     * Unit test of method {@link FileFilterAllFiles#getInstance}.
     */
    @Test
    public void testFileFilterAllFiles() {
        assertTrue(FileFilterAllFiles.getInstance().accept(new File(".")));
        assertNotNull(FileFilterAllFiles.getInstance().getDescription());
    }
}
