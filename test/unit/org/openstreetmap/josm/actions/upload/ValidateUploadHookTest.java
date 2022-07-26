// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;

/**
 * Unit tests for class {@link ValidateUploadHook}.
 */
@OsmApiType(OsmApiType.APIType.FAKE)
@Timeout(30)
class ValidateUploadHookTest {
    /**
     * Test of {@link ValidateUploadHook#checkUpload} method.
     */
    @Test
    void testCheckUpload() {
        assertTrue(new ValidateUploadHook().checkUpload(new APIDataSet()));
    }
}
