// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import org.junit.jupiter.api.extension.AfterEachCallback;
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
    class DefaultsExtension implements BeforeEachCallback, AfterEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            cleanUpFromJosmFixture();
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
            MemoryManagerTest.resetState(false);

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

        /**
         * Clean up what test not using these test rules may have broken.
         */
        private void cleanUpFromJosmFixture() {
            MemoryManagerTest.resetState(true);
            JOSMTestRules.cleanLayerEnvironment();
            Preferences.main().resetToInitialState();
            Preferences.main().enableSaveOnPut(false);
            System.gc();
        }
    }
}
