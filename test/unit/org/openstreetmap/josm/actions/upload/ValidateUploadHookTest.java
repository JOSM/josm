// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.APIDataSet;

/**
 * Unit tests for class {@link ValidateUploadHook}.
 */
public class ValidateUploadHookTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link ValidateUploadHook#checkUpload} method.
     */
    @Test
    public void testCheckUpload() {
        new ValidateUploadHook().checkUpload(new APIDataSet());
    }
}
