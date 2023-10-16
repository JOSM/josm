// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Use map styles in tests.
 *
 * @author Taylor Smock
 * @see JOSMTestRules#mapStyles()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@BasicPreferences
@ExtendWith(MapPaintStyles.MapPaintStylesExtension.class)
public @interface MapPaintStyles {
    class MapPaintStylesExtension implements BeforeEachCallback {
        private static int lastHashcode;

        @Override
        public void beforeEach(ExtensionContext extensionContext) throws Exception {
            setup();
        }

        public static void setup() {
            final ElemStyles styles = org.openstreetmap.josm.gui.mappaint.MapPaintStyles.getStyles();
            if (styles.getStyleSources().hashCode() != lastHashcode || styles.getStyleSources().isEmpty()) {
                org.openstreetmap.josm.gui.mappaint.MapPaintStyles.readFromPreferences();
                lastHashcode = org.openstreetmap.josm.gui.mappaint.MapPaintStyles.getStyles().getStyleSources().hashCode();
            }
        }
    }
}
