// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.APIDataSet;

/**
 * Unit tests for class {@link FixDataHook}.
 */
public class FixDataHookTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link FixDataHook#checkUpload} method.
     */
    @Test
    public void testCheckUpload() {
        new FixDataHook().checkUpload(new APIDataSet());
    }
}
