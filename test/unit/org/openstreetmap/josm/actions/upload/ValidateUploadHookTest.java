// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ValidateUploadHook}.
 */
class ValidateUploadHookTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().fakeAPI().timeout(30000);

    /**
     * Test of {@link ValidateUploadHook#checkUpload} method.
     */
    @Test
    void testCheckUpload() {
        assertTrue(new ValidateUploadHook().checkUpload(new APIDataSet()));
    }
}
