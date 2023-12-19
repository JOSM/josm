// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

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
@ExtendWith(TaggingPresets.TaggingPresetsExtension.class)
public @interface TaggingPresets {

    class TaggingPresetsExtension implements BeforeEachCallback {
        private static int expectedHashcode = 0;

        @Override
        public void beforeEach(ExtensionContext extensionContext) {
            setup();
        }

        /**
         * Setup the tagging presets
         */
        public static synchronized void setup() {
            final Collection<TaggingPreset> oldPresets = org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.getTaggingPresets();
            if (oldPresets.isEmpty() || expectedHashcode != oldPresets.hashCode()) {
                org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.readFromPreferences();
                expectedHashcode = org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.getTaggingPresets().hashCode();
            }
        }
    }
}
