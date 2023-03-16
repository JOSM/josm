// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.TimeZone;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Set the timezone for a test
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(Timezone.TimezoneExtension.class)
public @interface Timezone {
    class TimezoneExtension implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) {
            // All tests use the same timezone.
            TimeZone.setDefault(DateUtils.UTC);
        }
    }
}
