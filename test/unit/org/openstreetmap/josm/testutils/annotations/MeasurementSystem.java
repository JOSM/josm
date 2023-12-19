// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Set up the system of measurement
 * @author Taylor Smock
 * @since 18893
 * @see JOSMTestRules#metricSystem()
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@BasicPreferences
@ExtendWith(MeasurementSystem.SystemOfMeasurementExtension.class)
public @interface MeasurementSystem {
    /**
     * The measurement system to use. See {@link SystemOfMeasurement#ALL_SYSTEMS} for all currently implemented measurement systems.
     * Currently known measurement systems are:
     * <ul>
     *     <li>Chinese</li>
     *     <li>Imperial</li>
     *     <li>Metric</li>
     *     <li>Nautical Mile</li>
     * </ul>
     * @return The measurement system name.
     */
    String value() default "Metric";

    class SystemOfMeasurementExtension implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext extensionContext) throws Exception {
            final String system = AnnotationUtils.findFirstParentAnnotation(extensionContext, MeasurementSystem.class)
                    .map(MeasurementSystem::value)
                    .orElse(SystemOfMeasurement.METRIC.getName());
            SystemOfMeasurement.setSystemOfMeasurement(SystemOfMeasurement.ALL_SYSTEMS.get(system));
        }
    }
}
