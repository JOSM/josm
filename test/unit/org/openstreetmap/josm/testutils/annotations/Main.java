// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.WindowlessMapViewStateMocker;
import org.openstreetmap.josm.testutils.mockers.WindowlessNavigatableComponentMocker;

/**
 * Use the {@link MainApplication#main}, {@code Main.contentPanePrivate}, {@code Main.mainPanel}, global variables in this test.
 * @author Taylor Smock
 * @see JOSMTestRules#main()
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@BasicPreferences
@HTTP // Prevent MOTD from throwing
@ExtendWith(Main.MainExtension.class)
@StaticClassCleanup(MainApplication.class)
public @interface Main {
    /**
     * Get the class to use as the mocker for the map view
     * @return The mocker class for the map view
     */
    Class<?> mapViewStateMocker() default WindowlessMapViewStateMocker.class;

    /**
     * Get the class to use for the navigable component
     * @return The class to use for the navigable component.
     */
    Class<?> navigableComponentMocker() default WindowlessNavigatableComponentMocker.class;

    /**
     * Initialize the MainApplication
     * @author Taylor Smock
     */
    class MainExtension implements BeforeEachCallback, AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            synchronized (MainExtension.class) {
                MainApplication.getLayerManager().resetState();
            }
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            Optional<Main> annotation = AnnotationSupport.findAnnotation(context.getElement(), Main.class);
            Class<?> mapViewStateMocker = null;
            Class<?> navigableComponentMocker = null;
            if (annotation.isPresent()) {
                mapViewStateMocker = annotation.get().mapViewStateMocker();
                navigableComponentMocker = annotation.get().navigableComponentMocker();
            }

            // apply mockers to MapViewState and NavigableComponent whether we're headless or not
            // as we generally don't create the josm main window even in non-headless mode.
            if (mapViewStateMocker != null) {
                mapViewStateMocker.getConstructor().newInstance();
            }
            if (navigableComponentMocker != null) {
                navigableComponentMocker.getConstructor().newInstance();
            }

            synchronized (MainExtension.class) {
                new MainApplication();
                JOSMFixture.initContentPane();
                JOSMFixture.initMainPanel(true);
                JOSMFixture.initToolbar();
                JOSMFixture.initMainMenu();
            }
        }
    }
}
