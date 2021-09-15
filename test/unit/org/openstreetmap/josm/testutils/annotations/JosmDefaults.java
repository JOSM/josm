// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;

/**
 * The default annotations for tests. This should come after other {@code @ExtendWith} calls, but should be added <i>last</i>.
 * This helps ensure that we know what is actually required for the test to finish. This is just used for commonly needed items,
 * which may look for additional issues or perform generic cleanups.
 *
 * Current defaults:
 * <ul>
 *   <li>{@link MemoryManagerLeaks} (defaults)</li>
 * </ul>
 *
 * In addition, this also waits for various threads to finish (specifically, the EDT and worker threads).
 *
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@MemoryManagerLeaks
@LayerEnvironment
@ExtendWith(JosmDefaults.JosmDefaultsExtension.class)
public @interface JosmDefaults {
    /**
     * This performs default cleanups
     * @author Taylor Smock
     * @since xxx
     */
    class JosmDefaultsExtension implements AfterAllCallback, AfterEachCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            this.afterEach(context);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            // Get Timeout, if set
            long timeOut = AnnotationUtils.findFirstParentAnnotation(context, Timeout.class)
                    .map(timeout -> timeout.unit().toMillis(timeout.value())).orElse(10_000L);
            // Sync EDT and worker threads
            TestUtils.syncEDTAndWorkerThreads();

            assertTrue(ForkJoinPool.commonPool().awaitQuiescence(timeOut, TimeUnit.MILLISECONDS));

            Window[] windows = Window.getWindows();
            if (windows.length != 0) {
                Logging.info(
                    "Attempting to close {0} windows left open by tests: {1}",
                    windows.length,
                    Arrays.toString(windows)
                );
            }

            GuiHelper.runInEDTAndWait(() -> {
                for (Window window : windows) {
                    window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                    window.dispose();
                }
            });

            // Parts of JOSM uses weak references - destroy them.
            System.gc();
        }
    }
}
