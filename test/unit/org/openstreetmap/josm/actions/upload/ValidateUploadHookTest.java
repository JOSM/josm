// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ValidateUploadHook}.
 */
public class ValidateUploadHookTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().fakeAPI().timeout(20000);

    /**
     * Test of {@link ValidateUploadHook#checkUpload} method.
     */
    @Test
    public void testCheckUpload() {
        assertTrue(new ValidateUploadHook().checkUpload(new APIDataSet()));
    }
}
