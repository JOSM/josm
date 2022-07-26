// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.data.SystemOfMeasurement;

/**
 * Use the metric system
 *
 * @author Taylor Smock
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@BasicPreferences
@ExtendWith(MeasurementSystemAnnotation.MeasurementSystemExtension.class)
public @interface MeasurementSystemAnnotation {
    /**
     * An enum wrapping some {@link SystemOfMeasurement} static values
     * @author Taylor Smock
     */
    enum MeasurementSystem {
        CHINESE,
        IMPERIAL,
        METRIC,
        NAUTICAL_MILE
    }

    /**
     * The MeasurementSystem to use
     * @return The MeasurementSystem set by the annotation
     */
    MeasurementSystem value() default MeasurementSystem.METRIC;

    /**
     * Set the measurement system for each test
     * @author Taylor Smock
     */
    class MeasurementSystemExtension implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            SystemOfMeasurement.PROP_SYSTEM_OF_MEASUREMENT.put(null);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            SystemOfMeasurement measurement = context
              .getStore(ExtensionContext.Namespace.create(MeasurementSystemExtension.class))
              .get("measurement", SystemOfMeasurement.class);
            if (measurement == null) {
                this.afterAll(context);
            } else {
                SystemOfMeasurement.setSystemOfMeasurement(measurement);
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            SystemOfMeasurement measurement = this.setMeasurement(context);
            context.getStore(ExtensionContext.Namespace.create(MeasurementSystemExtension.class))
              .put("measurement", measurement);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            this.setMeasurement(context);
        }

        private SystemOfMeasurement setMeasurement(ExtensionContext context) {
            Optional<MeasurementSystemAnnotation> annotation =
              AnnotationUtils.findFirstParentAnnotation(context, MeasurementSystemAnnotation.class);
            if (annotation.isPresent()) {
                String measurementName = annotation.get().value().toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
                SystemOfMeasurement measurement = SystemOfMeasurement.ALL_SYSTEMS.entrySet().stream()
                  .filter(entry -> measurementName.equalsIgnoreCase(entry.getKey())).map(Map.Entry::getValue)
                  .findFirst().orElse(null);
                if (measurement != null) {
                    SystemOfMeasurement.setSystemOfMeasurement(measurement);
                }
                return measurement;
            }
            return null;
        }
    }
}
