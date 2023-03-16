// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.testutils.mockers.EDTAssertionMocker;

/**
 * Ensure that assertions in the edt are caught.
 * The default mocker is {@link EDTAssertionMocker}. If you want to use a different one,
 * you must use {@link org.junit.jupiter.api.extension.RegisterExtension} with
 * {@link AssertionsExtension#setMocker(Runnable)}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AssertionsInEDT.AssertionsExtension.class)
public @interface AssertionsInEDT {
    class AssertionsExtension implements BeforeEachCallback {
        private Runnable edtAssertionMockingRunnable = EDTAssertionMocker::new;
        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            this.edtAssertionMockingRunnable.run();
        }

        /**
         * Re-raise AssertionErrors thrown in the EDT where they would have normally been swallowed.
         * @param edtAssertionMockingRunnable Runnable for initializing this functionality
         *
         * @return this instance, for easy chaining
         */
        public AssertionsExtension setMocker(Runnable edtAssertionMockingRunnable) {
            this.edtAssertionMockingRunnable = edtAssertionMockingRunnable;
            return this;
        }
    }
}
