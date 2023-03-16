// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.mockers.WindowlessMapViewStateMocker;
import org.openstreetmap.josm.testutils.mockers.WindowlessNavigatableComponentMocker;

/**
 * The annotation for mocking map view and navigable components
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HTTP
@LayerManager
@ExtendWith(Main.MainExtension.class)
public @interface Main {
    /**
     * If a specific mocker is required, use {@link org.junit.jupiter.api.extension.RegisterExtension}.
     */
    class MainExtension implements BeforeEachCallback {
        /**
         * The mocker for the map view state
         */
        private Runnable mapViewStateMockingRunnable = WindowlessMapViewStateMocker::new;
        /**
         * The mocker for navigable components
         */
        private Runnable navigableComponentMockingRunnable = WindowlessNavigatableComponentMocker::new;

        /**
         * Set the specific map view mocker
         *
         * @param mapViewStateMockingRunnable The new mocker
         * @return this, for easy chaining
         */
        public MainExtension setMapViewMocker(Runnable mapViewStateMockingRunnable) {
            this.mapViewStateMockingRunnable = mapViewStateMockingRunnable;
            return this;
        }

        /**
         * Set the navigable component mocker
         *
         * @param navigableComponentMockingRunnable The new mocker
         * @return this, for easy chaining
         */
        public MainExtension setNavigableComponentMocker(Runnable navigableComponentMockingRunnable) {
            this.navigableComponentMockingRunnable = navigableComponentMockingRunnable;
            return this;
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            TestUtils.assumeWorkingJMockit();
            // apply mockers to MapViewState and NavigableComponent whether we're headless or not
            // as we generally don't create the josm main window even in non-headless mode.
            if (this.mapViewStateMockingRunnable != null) {
                this.mapViewStateMockingRunnable.run();
            }
            if (this.navigableComponentMockingRunnable != null) {
                this.navigableComponentMockingRunnable.run();
            }

            new MainApplication();
            JOSMFixture.initContentPane();
            JOSMFixture.initMainPanel(true);
            JOSMFixture.initToolbar();
            JOSMFixture.initMainMenu();
        }
    }
}
