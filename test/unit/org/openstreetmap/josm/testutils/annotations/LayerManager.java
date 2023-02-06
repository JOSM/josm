// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Clear the main {@link org.openstreetmap.josm.gui.layer.LayerManager} between tests.
 * <br />
 * You shouldn't have to register this -- it should be run automatically by the JUnit 5 test environment.
 * See <a href="https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic">
 *     Automatic Extension Registration
 * </a> for more information.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(LayerManager.LayerManagerExtension.class)
public @interface LayerManager {
    class LayerManagerExtension implements BeforeEachCallback, AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) {
            JOSMTestRules.cleanLayerEnvironment();
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            this.afterEach(context);
        }
    }
}
