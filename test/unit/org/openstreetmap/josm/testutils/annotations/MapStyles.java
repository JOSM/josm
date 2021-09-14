// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;

/**
 * Initialize map paint styles from preferences
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@FullPreferences
@ExtendWith(MapStyles.MapStylesExtension.class)
@StaticClassCleanup(MapPaintStyles.class)
public @interface MapStyles {
    /**
     * Initialize and reset mappaintstyles
     * @author Taylor Smock
     */
    class MapStylesExtension implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            MapPaintStyles.readFromPreferences();
        }
    }
}
