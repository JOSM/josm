// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Use presets in tests.
 *
 * @author Taylor Smock
 * @see JOSMTestRules#presets()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@BasicPreferences
@Territories
@ExtendWith(TaggingPresets.TaggingPresetsExtension.class)
public @interface TaggingPresets {

    class TaggingPresetsExtension implements BeforeEachCallback, BeforeAllCallback {
        private static int expectedHashcode = 0;
        private static Locale lastLocale;

        @Override
        public void beforeAll(ExtensionContext extensionContext) throws Exception {
            setup();
        }

        @Override
        public void beforeEach(ExtensionContext extensionContext) {
            setup();
        }

        /**
         * Set up the tagging presets
         */
        public static synchronized void setup() {
            final Collection<TaggingPreset> oldPresets = org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.getTaggingPresets();
            if (oldPresets.isEmpty() || expectedHashcode != oldPresets.hashCode() || !Objects.equals(lastLocale, Locale.getDefault())) {
                org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.readFromPreferences();
                expectedHashcode = org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.getTaggingPresets().hashCode();
                lastLocale = Locale.getDefault();
            }
        }
    }
}
