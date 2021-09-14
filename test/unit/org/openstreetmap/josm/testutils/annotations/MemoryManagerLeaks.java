// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.tools.MemoryManagerTest;

/**
 * Use to ensure that memory leaks are thrown.
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(MemoryManagerLeaks.MemoryManagerLeaksExtension.class)
public @interface MemoryManagerLeaks {
    /**
     * Set to {@code true} to ignore leaks
     * @return {@code false} to throw an exception if there are leaks
     */
    boolean value() default false;

    /**
     * The extension that ensures that leaks are thrown (by default)
     * @author Taylor Smock
     */
    class MemoryManagerLeaksExtension implements AfterAllCallback, AfterEachCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            this.afterEach(context);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            Optional<MemoryManagerLeaks> annotation = AnnotationUtils.findFirstParentAnnotation(context, MemoryManagerLeaks.class);
            annotation.ifPresent(memoryManagerLeaks -> MemoryManagerTest.resetState(memoryManagerLeaks.value()));
        }
    }
}
