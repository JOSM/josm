// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MemoryManagerTest;
import org.openstreetmap.josm.tools.Utils;

/**
 * Default actions taken by {@link JOSMTestRules}. Automatically registered.
 * Functionality that this provides may be moved to other classes.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JosmDefaults.DefaultsExtension.class)
public @interface JosmDefaults {
    /**
     * Default actions taken by {@link JOSMTestRules}. Automatically registered.
     * Functionality that this provides may be moved to other classes.
     */
    class DefaultsExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {
        private static final boolean PERFORM_MEMORY_CLEANUP =
                !Boolean.parseBoolean(System.getProperty("test.without.gc", Boolean.FALSE.toString()));
        private static final int JAVA_VERSION = Utils.getJavaVersion();

        @Override
        public void beforeAll(ExtensionContext extensionContext) throws Exception {
            cleanUpFromJosmFixture();
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            // Assume anonymous user
            if (!UserIdentityManager.getInstance().isAnonymous()) {
                UserIdentityManager.getInstance().setAnonymous();
            }
            User.clearUserMap();
            // Setup callbacks
            DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
            OsmConnection.setOAuthAccessTokenFetcher(OAuthAuthorizationWizard::obtainAccessToken);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            MemoryManagerTest.resetState(AnnotationUtils.findFirstParentAnnotation(context, MemoryManagerLeaks.class).isPresent());

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
        }

        @Override
        public void afterAll(ExtensionContext extensionContext) throws Exception {
            // Parts of JOSM uses weak references - destroy them.
            memoryCleanup();
        }

        /**
         * Clean up what test not using these test rules may have broken.
         * See {@link org.openstreetmap.josm.JOSMFixture} for the most common reason why this needs to be called.
         */
        private static void cleanUpFromJosmFixture() {
            MemoryManagerTest.resetState(true);
            JOSMTestRules.cleanLayerEnvironment();
            Preferences.main().resetToInitialState();
            Preferences.main().enableSaveOnPut(false);
            memoryCleanup();
        }

        /**
         * Call {@link System#gc()}
         * Warning: This is a very expensive method! GC is expensive!
         * For reference, a test run without gc will take ~7 minutes.
         * A test run with gc will take 20 minutes (if run before/after each test method) or 10 minutes (if run before/after each test class).
         * <p>
         * If you want to do a test run without manual calls to gc, add `-Dtest.without.gc=true` to the arguments.
         */
        public static void memoryCleanup() {
            if (PERFORM_MEMORY_CLEANUP) {
                System.gc();
                // Finalization was deprecated in Java 18
                if (JAVA_VERSION <= 17) {
                    System.runFinalization();
                }
            }
        }
    }
}
