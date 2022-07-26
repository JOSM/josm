// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Declare static classes to clean up.
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Repeatable(StaticClassCleanup.StaticClassesCleanup.class)
@ExtendWith(StaticClassCleanup.StaticClassCleanupExtension.class)
public @interface StaticClassCleanup {
    /** The class to reset */
    Class<?> value();
    @Documented
    @Retention(RUNTIME)
    @Target({ TYPE, METHOD })
    @interface StaticClassesCleanup {
        StaticClassCleanup[] value();
    }

    /**
     * Clean up static classes
     */
    class StaticClassCleanupExtension implements AfterEachCallback, AfterAllCallback {
        private static void resetAnnotations(Collection<StaticClassCleanup> annotations) {
            assertAll(annotations.stream().map(StaticClassCleanup::value)
                    .map(clazz -> (Executable) () -> assertDoesNotThrow(() -> AnnotationUtils.resetStaticClass(clazz))));
        }

        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            final List<StaticClassCleanup> annotations = context.getElement()
                    .map(element -> AnnotationSupport.findRepeatableAnnotations(element, StaticClassCleanup.class))
                    .orElseGet(Collections::emptyList);
            resetAnnotations(annotations);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            if (context.getTestMethod().isPresent()) {
                final List<StaticClassCleanup> annotations =
                        AnnotationSupport.findRepeatableAnnotations(context.getRequiredTestMethod(),
                                StaticClassCleanup.class);
                resetAnnotations(annotations);
            }
        }
    }
}
