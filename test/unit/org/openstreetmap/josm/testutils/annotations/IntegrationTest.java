// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Mark this test as an <a href="https://en.wikipedia.org/wiki/Integration_testing">Integration</a> test
 * @author Taylor Smock
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Tag("IntegrationTest")
public @interface IntegrationTest {
}
