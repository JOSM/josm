// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetNameTemplateList;
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
@BasicPreferences
@ExtendWith(Presets.PresetsExtension.class)
@StaticClassCleanup(TaggingPresets.class)
@StaticClassCleanup(TaggingPresetNameTemplateList.class)
public @interface Presets {

    /** {@code true} to clear presets between tests. Alternatively, re-annotate specific tests with this annotation. */
    boolean value() default false;

    /**
     * Initialize the presets
     * @author Taylor Smock
     *
     */
    class PresetsExtension implements BeforeAllCallback, BeforeEachCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            // If @Main was called, do the full initialization (note: @Main only gets run beforeEach)
            if (AnnotationUtils.findFirstParentAnnotation(context, Main.class).isPresent()
                && MainApplication.getMenu() != null) {
                TaggingPresets.initialize();
            } else {
                TaggingPresets.readFromPreferences();
            }
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
