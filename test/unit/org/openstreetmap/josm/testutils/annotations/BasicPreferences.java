// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.commons.util.ReflectionUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * Allow tests to use JOSM preferences (see {@link JOSMTestRules#preferences()}).
 * This is often enough for basic tests.
 *
 * @author Taylor Smock
 * @see FullPreferences
 * @since 18037
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(BasicPreferences.BasicPreferencesExtension.class)
public @interface BasicPreferences {

    /**
     * Initialize basic preferences. This is often more than enough for basic tests.
     * @author Taylor Smock
     */
    class BasicPreferencesExtension implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            AnnotationUtils.resetStaticClass(Config.class);

            // Ensure that config tests are consistent
            resetConfigVariables(null);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            if (context.getElement().isPresent() && context.getElement().get().isAnnotationPresent(BasicPreferences.class)) {
                this.afterAll(context);
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Preferences pref = Preferences.main();
            // Disable saving on put, just to avoid overwriting pref files
            pref.enableSaveOnPut(false);
            pref.resetToDefault();
            Config.setPreferencesInstance(pref);
            Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
            Config.setUrlsProvider(JosmUrls.getInstance());
            // Force an invalid URL just to avoid accidents
            Config.getPref().put("osm-server.url", "http://invalid");

            // Store the pref for other extensions
            context.getStore(Namespace.create(BasicPreferencesExtension.class)).put("preferences", pref);

            resetConfigVariables(pref);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            if (AnnotationUtils.elementIsAnnotated(context.getElement(), BasicPreferences.class) || Config.getPref() == null) {
                this.beforeAll(context);
            }
        }

        /**
         * Reset all {@link AbstractProperty} preferences to the new preference
         * @param pref The preference to use
         * @throws ReflectiveOperationException if reflection fails
         */
        private void resetConfigVariables(Preferences pref) throws ReflectiveOperationException {
            final Field configField = AbstractProperty.class.getDeclaredField("preferences");
            configField.setAccessible(true);
            for (Path classPath : ReflectionUtils.getAllClasspathRootDirectories()) {
                for (Class<?> clazz : ReflectionUtils.findAllClassesInClasspathRoot(classPath.toUri(), ClassFilter.of(tClazz -> true))) {
                    try {
                        for (Field field : clazz.getDeclaredFields()) {
                            if (ReflectionUtils.isStatic(field) && AbstractProperty.class.isAssignableFrom(field.getType())) {
                                field.setAccessible(true);
                                configField.set(field.get(null), pref);
                            }
                        }
                    } catch (NoClassDefFoundError noClassDefFoundError) {
                        Logging.warn("{0}: No class definition found", clazz.getName());
                    }
                }
            }
        }
    }
}