// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Override this test's assumed JOSM version (as reported by {@link Version}).
 * @see JOSMTestRules#assumeRevision(String)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ExtendWith(AssumeRevision.SetRevisionExtension.class)
public @interface AssumeRevision {
    /**
     * Returns overridden assumed JOSM version.
     * @return overridden assumed JOSM version
     */
    String value();

    /**
     * The extension that sets up and restores version information
     */
    class SetRevisionExtension implements BeforeEachCallback, AfterEachCallback {

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            Version lastVersion = context.getStore(ExtensionContext.Namespace.create(SetRevisionExtension.class))
                    .get(Version.class, Version.class);
            TestUtils.setPrivateStaticField(Version.class, "instance", lastVersion);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            context.getStore(ExtensionContext.Namespace.create(SetRevisionExtension.class))
                    .put(Version.class, Version.getInstance());
            final String revisionString = AnnotationUtils.findFirstParentAnnotation(context, AssumeRevision.class)
                    .map(AssumeRevision::value).orElseThrow(NullPointerException::new);
            final Version replacementVersion = new MockVersion(revisionString);
            TestUtils.setPrivateStaticField(Version.class, "instance", replacementVersion);
        }

        private static class MockVersion extends Version {
            MockVersion(final String propertiesString) {
                super.initFromRevisionInfo(
                        new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8))
                );
            }
        }
    }
}
