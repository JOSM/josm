// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link FileFilterAllFiles} class.
 */
class FileFilterAllFilesTest {

    /**
     * Unit test of method {@link FileFilterAllFiles#getInstance}.
     */
    @Test
    void testFileFilterAllFiles() {
        assertTrue(FileFilterAllFiles.getInstance().accept(new File(".")));
        assertNotNull(FileFilterAllFiles.getInstance().getDescription());
    }
}
