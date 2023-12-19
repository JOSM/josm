// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.support.AnnotationSupport;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Allow tests to use JOSM preferences (see {@link JOSMTestRules#preferences()}).
 * This is often enough for basic tests. There are two modes:
 * <ul>
 *     <li>Between test classes (usually enough) if annotated at the class level ({@link ElementType#TYPE})</li>
 *     <li>Between test methods if annotated at the method level ({@link ElementType#METHOD})</li>
 *     <li>Between test method if annotated at the class level <i>and</i> the annotated value is {@code true}</li>
 * </ul>
 *
 * @author Taylor Smock
 * @see FullPreferences
 * @since 18037
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
@ExtendWith(BasicPreferences.BasicPreferencesExtension.class)
public @interface BasicPreferences {
    /**
     * Clear preferences between tests
     * @return {@code true} if the preferences should be cleared between tests
     */
    boolean value() default false;

    /**
     * Initialize basic preferences. This is often more than enough for basic tests.
     * @author Taylor Smock
     */
    class BasicPreferencesExtension implements AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            AnnotationUtils.resetStaticClass(Config.class);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            if (AnnotationSupport.isAnnotated(context.getElement(), BasicPreferences.class)) {
                this.afterAll(context);
            }
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            Preferences pref = Preferences.main();
            // Disable saving on put, just to avoid overwriting pref files
            pref.enableSaveOnPut(false);
            pref.resetToInitialState();
            pref.enableSaveOnPut(false);
            @SuppressWarnings("unchecked")
            final Map<String, Setting<?>> defaultsMap = (Map<String, Setting<?>>) TestUtils.getPrivateField(pref, "defaultsMap");
            defaultsMap.clear();
            Config.setPreferencesInstance(pref);
            Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
            Config.setUrlsProvider(JosmUrls.getInstance());
            // Force an invalid URL just to avoid accidents
            Config.getPref().put("osm-server.url", "http://invalid");

            // Store the pref for other extensions
            context.getStore(Namespace.create(BasicPreferencesExtension.class)).put("preferences", pref);
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            if (AnnotationSupport.isAnnotated(context.getElement(), BasicPreferences.class) || Config.getPref() == null
            || AnnotationUtils.findFirstParentAnnotation(context, BasicPreferences.class).map(BasicPreferences::value).orElse(false)) {
                this.beforeAll(context);
            }
        }
    }
}
