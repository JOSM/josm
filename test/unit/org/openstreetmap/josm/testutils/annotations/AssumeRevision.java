// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.testutils.JOSMTestRules;


/**
 * Override this test's assumed JOSM version (as reported by {@link Version}).
 * @author Taylor Smock
 * @see JOSMTestRules#assumeRevision(String)
 * @since xxx
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ExtendWith(AssumeRevision.AssumeRevisionExtension.class)
public @interface AssumeRevision {
    /**
     * Returns overridden assumed JOSM version.
     * @return overridden assumed JOSM version
     */
    String value();

    /**
     * Override the JOSM revision information. Use {@link AssumeRevision} instead of directly using this extension.
     * @author Taylor Smock
     * @since xxx
     */
    class AssumeRevisionExtension implements BeforeEachCallback, AfterEachCallback {

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            Store store = context.getStore(Namespace.create(AssumeRevisionExtension.class));
            Version originalVersion = store.getOrDefault(Version.class, Version.class, null);
            if (originalVersion != null) {
                TestUtils.setPrivateStaticField(Version.class, "instance", originalVersion);
            }
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            Optional<AssumeRevision> annotation = AnnotationUtils.findFirstParentAnnotation(context, AssumeRevision.class);
            if (annotation.isPresent()) {
                Store store = context.getStore(Namespace.create(AssumeRevisionExtension.class));
                if (store.get(Version.class, Version.class) == null) {
                    store.put(Version.class, Version.getInstance());
                }
                final Version replacementVersion = new MockVersion(annotation.get().value());
                TestUtils.setPrivateStaticField(Version.class, "instance", replacementVersion);
            }
        }
    }

}
