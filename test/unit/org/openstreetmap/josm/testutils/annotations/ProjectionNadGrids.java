// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Paths;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Utils;

/**
 * Use NAD projections in tests.
 *
 * @author Taylor Smock
 * @see JOSMTestRules#projectionNadGrids()
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@ExtendWith(ProjectionNadGrids.NadGridsExtension.class)
public @interface ProjectionNadGrids {
    class NadGridsExtension implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext extensionContext) throws Exception {
            if (Utils.isBlank(Utils.getSystemProperty("PROJ_LIB"))) {
                Utils.updateSystemProperty("PROJ_LIB", Paths.get("nodist", "data", "projection").toString());
            }
            MainApplication.setupNadGridSources();
        }
    }
}
