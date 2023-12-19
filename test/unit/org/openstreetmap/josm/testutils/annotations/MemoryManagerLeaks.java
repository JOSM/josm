// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Allow the memory manager to contain items after execution of the test cases.
 * @author Taylor Smock
 * @since 18893
 * @see JOSMTestRules#memoryManagerLeaks()
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@BasicPreferences
public @interface MemoryManagerLeaks {
}
