// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.swing.ProgressMonitorExecutor;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ImageProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests for class {@link ExitAction}.
 */
public final class ExitActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().main();

    /**
     * System.exit rule
     */
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    /**
     * Unit test of {@link ExitAction#actionPerformed}
     */
    @Test
    public void testActionPerformed() {
        TestUtils.assumeWorkingJMockit();
        exit.expectSystemExitWithStatus(0);

        boolean[] workerShutdownCalled = {false};
        boolean[] workerShutdownNowCalled = {false};
        boolean[] imageProviderShutdownCalled = {false};

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
            private void shutdown(Invocation invocation) {
                imageProviderShutdownCalled[0] = true;
            }
        };

        // No layer

        new ExitAction().actionPerformed(null);

        assertTrue(workerShutdownCalled[0]);
        assertTrue(workerShutdownNowCalled[0]);
        assertTrue(imageProviderShutdownCalled[0]);
    }
}
