// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Use projections in tests (Mercator).
 * @author Taylor Smock
 * @see JOSMTestRules#projection()
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ExtendWith(Projection.ProjectionExtension.class)
@StaticClassCleanup(ProjectionRegistry.class)
public @interface Projection {
    /**
     * The value to use for the projection. Defaults to EPSG:3857 (Mercator).
     * @return The value to use to get the projection from {@link Projections#getProjectionByCode}.
     */
    String projectionCode() default "EPSG:3857";

    /**
     * Use projections in tests. Use {@link Projection} preferentially.
     * @author Taylor Smock
     *
     */
    class ProjectionExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            ProjectionRegistry.clearProjectionChangeListeners();
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            // Needed in order to run prior to Territories
            beforeEach(context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            Optional<Projection> annotation = AnnotationSupport.findAnnotation(context.getElement(), Projection.class);
            if (annotation.isPresent()) {
                ProjectionRegistry.setProjection(Projections.getProjectionByCode(annotation.get().projectionCode()));
            } else {
                ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
            }
        }

    }
}
