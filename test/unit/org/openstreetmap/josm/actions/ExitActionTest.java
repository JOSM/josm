// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.SaveLayersDialog;
import org.openstreetmap.josm.gui.progress.swing.ProgressMonitorExecutor;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ImageProvider;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests for class {@link ExitAction}.
 */
final class ExitActionTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Unit test of {@link ExitAction#actionPerformed}
     */
    @Test
    @ExpectSystemExitWithStatus(0)
    void testActionPerformed() {
        TestUtils.assumeWorkingJMockit();

        boolean[] workerShutdownCalled = {false};
        boolean[] workerShutdownNowCalled = {false};
        boolean[] imageProviderShutdownCalledNowFalse = {false};
        boolean[] imageProviderShutdownCalledNowTrue = {false};
        boolean[] jcsCacheManagerShutdownCalled = {false};
        boolean[] saveLayersDialogCloseDialogCalled = {false};

        // critically we don't proceed into the actual implementation in any of these mock methods -
        // that would be quite annoying for tests following this one which were expecting to use any
        // of these
        new MockUp<ProgressMonitorExecutor>() {
            @Mock
            private void shutdown(Invocation invocation) {
                if (invocation.getInvokedInstance() == MainApplication.worker) {
                    workerShutdownCalled[0] = true;
                }
            }

            @Mock
            private void shutdownNow(Invocation invocation) {
                if (invocation.getInvokedInstance() == MainApplication.worker) {
                    // regular shutdown should have been called first
                    assertTrue(workerShutdownCalled[0]);
                    workerShutdownNowCalled[0] = true;
                }
            }
        };
        new MockUp<ImageProvider>() {
            @Mock
            private void shutdown(Invocation invocation, boolean now) {
                if (now) {
                    // should have already been called with now = false
                    assertTrue(imageProviderShutdownCalledNowFalse[0]);
                    imageProviderShutdownCalledNowTrue[0] = true;
                } else {
                    imageProviderShutdownCalledNowFalse[0] = true;
                }
            }
        };
        new MockUp<JCSCacheManager>() {
            @Mock
            private void shutdown(Invocation invocation) {
                jcsCacheManagerShutdownCalled[0] = true;
            }
        };
        new MockUp<SaveLayersDialog>() {
            @Mock
            private void closeDialog(Invocation invocation) {
                saveLayersDialogCloseDialogCalled[0] = true;
            }
        };

        // No layer

        try {
            new ExitAction().actionPerformed(null);
        } finally {
            // ExpectedSystemExit presumably works using an exception, so executing anything after the
            // previous line requires it to be put in a finally block
            assertTrue(workerShutdownCalled[0]);
            assertTrue(workerShutdownNowCalled[0]);
            assertTrue(imageProviderShutdownCalledNowFalse[0]);
            assertTrue(imageProviderShutdownCalledNowTrue[0]);
            assertTrue(jcsCacheManagerShutdownCalled[0]);
            assertTrue(saveLayersDialogCloseDialogCalled[0]);
        }
    }
}
