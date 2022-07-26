// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Clean up layers before/after the tests
 * @author Taylor Smock
 * @see JOSMTestRules#cleanLayerEnvironment()
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@ExtendWith(LayerEnvironment.LayerEnvironmentExtension.class)
public @interface LayerEnvironment {
    /**
     * Clean up layers
     * @author Taylor Smock
     *
     */
    class LayerEnvironmentExtension implements BeforeEachCallback, AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            beforeEach(context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            JOSMTestRules.cleanLayerEnvironment();
        }
    }
}
