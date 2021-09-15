// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Use presets in this test. Only runs per class OR per method, but not both.
 *
 * @author Taylor Smock
 * @see JOSMTestRules#presets()
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@ExtendWith(Presets.PresetsExtension.class)
@StaticClassCleanup(TaggingPresets.class)
public @interface Presets {

    /** {@code true} to clear presets between tests. Alternatively, re-annotate specific tests with this annotation. */
    boolean value() default false;

    /**
     * Initialize the presets
     * @author Taylor Smock
     *
     */
    class PresetsExtension implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            // If @Main was called, the necessary vars for destroy to work have been initialized
            if (AnnotationUtils.findFirstParentAnnotation(context, Main.class).isPresent()) {
                TaggingPresets.destroy();
            }
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            if (context.getElement().isPresent() && context.getElement().get().isAnnotationPresent(Presets.class)
                    || AnnotationUtils.findFirstParentAnnotation(context, Presets.class).map(Presets::value).orElse(false)) {
                this.afterAll(context);
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            TaggingPresets.readFromPreferences();
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            if (context.getElement().isPresent() && context.getElement().get().isAnnotationPresent(Presets.class)
                    || AnnotationUtils.findFirstParentAnnotation(context, Presets.class).map(Presets::value).orElse(false)) {
                this.beforeAll(context);
            }
        }
    }
}
