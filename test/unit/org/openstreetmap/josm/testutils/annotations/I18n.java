// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * Enables the i18n module for this test.
 * @author Taylor Smock
 * @see org.openstreetmap.josm.testutils.JOSMTestRules#i18n(String)
 * @since 17914
 */
@Documented
@Inherited
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
            String language = AnnotationUtils.findFirstParentAnnotation(context, I18n.class).map(I18n::value).orElse("en");
            if (!Locale.getDefault().equals(LanguageInfo.getLocale(language, false))) {
                org.openstreetmap.josm.tools.I18n.set(language);
                // We want to have a consistent "country", so we don't use a locale with a country code from the original locale.
                // Unless someone specified it via the <lang>_<country> syntax.
                if (!language.contains("_")) {
                    Locale.setDefault(LanguageInfo.getLocale(language, false));
                }
            }
        }

        @Override
        public void afterEach(ExtensionContext context) {
            Locale original = org.openstreetmap.josm.tools.I18n.getOriginalLocale();
            if (original == null) {
                org.openstreetmap.josm.tools.I18n.set("en");
            } else if (!original.equals(Locale.getDefault())) {
                org.openstreetmap.josm.tools.I18n.set(original.getLanguage());
                Locale.setDefault(original);
            }
        }
    }
}
