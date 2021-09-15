// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
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
@StaticClassCleanup(ElemStyles.class)
public @interface MapStyles {
    /**
     * Initialize and reset mappaintstyles
     * @author Taylor Smock
     */
    class MapStylesExtension implements AfterEachCallback, BeforeEachCallback {
        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            final Method clearMethod = ElemStyles.class.getDeclaredMethod("clear");
            clearMethod.setAccessible(true);
            clearMethod.invoke(MapPaintStyles.getStyles());
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            MapPaintStyles.readFromPreferences();
        }
    }
}
