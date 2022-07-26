// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.EDTAssertionMocker;

import mockit.MockUp;

/**
 * Raise assertions in the EDT.
 * @author Taylor Smock
 * @see JOSMTestRules#assertionsInEDT
 * @since xxx
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface AssertionsInEDT {
    /**
     * Get the mocker to use for the EDT
     * @return The mock
     */
    Class<? extends MockUp<?>> value() default EDTAssertionMocker.class;
    /**
     * Initialize the mocker for the EDT
     * @author Taylor Smock
     *
     */
    class AssertionsInEDTExtension implements BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            Optional<AssertionsInEDT> annotation = AnnotationSupport.findAnnotation(context.getElement(), AssertionsInEDT.class);
            if (annotation.isPresent()) {
                Class<? extends MockUp<?>> clazz = annotation.get().value();
                clazz.getConstructor().newInstance();
            }
        }
    }
}
