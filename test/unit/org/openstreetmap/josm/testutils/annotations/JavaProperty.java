// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This stores and resets java system properties between tests
 * @author Taylor Smock
 * @since xxx
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD})
@Inherited
@ExtendWith(JavaProperty.JavaPropertyExtension.class)
public @interface JavaProperty {

    /**
     * Store and restore system preferences
     */
    class JavaPropertyExtension implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
        /**
         * Get the store
         * @param context The context to use
         * @return The store to use
         */
        private static ExtensionContext.Store getStore(final ExtensionContext context) {
            return context.getStore(ExtensionContext.Namespace.create(JavaPropertyExtension.class));
        }

        /**
         * Store the current properties
         * @param type the runtime type
         * @param context The context to use
         */
        private static void storeProperties(final String type, final ExtensionContext context) {
            final ExtensionContext.Store store = getStore(context);
            final Properties oldProperties = new Properties();
            final Properties currentProperties = System.getProperties();
            currentProperties.stringPropertyNames()
                    .forEach(property -> oldProperties.setProperty(property, currentProperties.getProperty(property)));
            store.put(type, oldProperties);
        }

        private static void restoreProperties(final String type, final ExtensionContext context) {
            final ExtensionContext.Store store = getStore(context);
            final Properties oldProperties = store.getOrDefault(type, Properties.class, new Properties());
            final Properties currentProperties = System.getProperties();
            final Set<String> propertiesToRemove = new HashSet<>();
            for (String property : currentProperties.stringPropertyNames()) {
                if (!oldProperties.stringPropertyNames().contains(property)) {
                    propertiesToRemove.add(property);
                } else {
                    currentProperties.setProperty(property, oldProperties.getProperty(property));
                }
            }

            for (String property : oldProperties.stringPropertyNames()) {
                if (!currentProperties.stringPropertyNames().contains(property)) {
                    currentProperties.setProperty(property, oldProperties.getProperty(property));
                }
            }

            propertiesToRemove.forEach(currentProperties::remove);
        }

        @Override
        public void afterAll(final ExtensionContext context) throws Exception {
            restoreProperties("all", context);
        }

        @Override
        public void afterEach(final ExtensionContext context) throws Exception {
            restoreProperties("each", context);
        }

        @Override
        public void beforeAll(final ExtensionContext context) throws Exception {
            storeProperties("all", context);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            storeProperties("each", context);
        }
    }
}
