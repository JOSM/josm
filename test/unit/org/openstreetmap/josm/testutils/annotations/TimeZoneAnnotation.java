// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.TimeZone;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Set the default timezone
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(TimeZoneAnnotation.TimeZoneExtension.class)
public @interface TimeZoneAnnotation {
    /**
     * Set the default timezone
     * @author Taylor Smock
     */
    class TimeZoneExtension implements AfterAllCallback, AfterEachCallback, BeforeEachCallback {
        @Override
        public void afterAll(ExtensionContext context) {
            TimeZone.setDefault(null);
            TimeZone.getDefault().getDisplayName();
        }

        @Override
        public void afterEach(ExtensionContext context) {
            this.afterAll(context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            TimeZone.setDefault(DateUtils.UTC);
        }
    }
}
