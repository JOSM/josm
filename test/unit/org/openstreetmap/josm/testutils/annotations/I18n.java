// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * Enables the i18n module for this test.
 * @author Taylor Smock
 * @see org.openstreetmap.josm.testutils.JOSMTestRules#i18n(String)
 * @since 17914
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(I18n.I18nExtension.class)
public @interface I18n {

    /**
     * Get the language to use for i18n
     * @return The language (default "en").
     */
    String value() default "en";

    /**
     * Enables the i18n module for this test.
     * @author Taylor Smock
     * @see org.openstreetmap.josm.testutils.JOSMTestRules#i18n(String)
     */
    class I18nExtension implements AfterEachCallback, BeforeEachCallback {
        @Override
        public void beforeEach(ExtensionContext context) {
            String language = AnnotationSupport.findAnnotation(context.getElement(), I18n.class).map(I18n::value).orElse("en");
            if (!Locale.getDefault().equals(LanguageInfo.getLocale(language, false))) {
                org.openstreetmap.josm.tools.I18n.set(language);
            }
        }

        @Override
        public void afterEach(ExtensionContext context) {
            if (!Locale.ENGLISH.equals(Locale.getDefault())) {
                org.openstreetmap.josm.tools.I18n.set("en");
                org.openstreetmap.josm.tools.I18n.set(org.openstreetmap.josm.tools.I18n.getOriginalLocale().getLanguage());
                Locale.setDefault(Locale.ENGLISH);
            }
        }
    }
}
