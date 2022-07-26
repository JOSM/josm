// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Set up loading of NTV2 grit shift files to support projections that need them.
 * @author Taylor Smock
 * @see JOSMTestRules#projectionNadGrids()
 * @since xxx
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(ProjectionNadGrids.ProjectionNadGridsExtension.class)
@StaticClassCleanup(NTV2GridShiftFileWrapper.class)
public @interface ProjectionNadGrids {
    /**
     * Set up loading of NTV2 grit shift files to support projections that need them.
     * Use {@link ProjectionNadGrids} instead.
     * @author Taylor Smock
     * @see JOSMTestRules#projectionNadGrids()
     */
    class ProjectionNadGridsExtension implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            MainApplication.setupNadGridSources();
        }
    }
}
