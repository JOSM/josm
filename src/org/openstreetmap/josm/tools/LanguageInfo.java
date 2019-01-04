// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;

/**
 * This is a utility class that provides information about locales and allows to convert locale codes.
 */
public final class LanguageInfo {

    private LanguageInfo() {
        // Hide default constructor for utils classes
    }

    /**
     * Type of the locale to use
     * @since 5915
     */
    public enum LocaleType {
        /** The current default language */
        DEFAULT,
        /** The current default language, but not english */
        DEFAULTNOTENGLISH,
        /** The base language (i.e. pt for pt_BR) */
        BASELANGUAGE,
        /** The standard english texts */
        ENGLISH
    }

    /**
     * Replies the wiki language prefix for the given locale. The wiki language
     * prefix has the form 'Xy:' where 'Xy' is a ISO 639 language code in title
     * case (or Xy_AB: for sub languages).
     *
     * @param type the type
     * @return the wiki language prefix or {@code null} for {@link LocaleType#BASELANGUAGE}, when
     * base language is identical to default or english
     * @since 5915
     */
    public static String getWikiLanguagePrefix(LocaleType type) {
        return getWikiLanguagePrefix(Locale.getDefault(), type);
    }

    static String getWikiLanguagePrefix(Locale locale, LocaleType type) {
        if (type == LocaleType.ENGLISH)
          return "";

        String code = getJOSMLocaleCode(locale);
        if (type == LocaleType.BASELANGUAGE) {
            if (code.matches("[^_]+_[^_]+")) {
                code = code.substring(0, 2);
                if ("en".equals(code))
                    return null;
            } else {
                return null;
            }
        } else if (type == LocaleType.DEFAULTNOTENGLISH && "en".equals(code)) {
            return null;
        } else if (code.matches(".+@.+")) {
          return code.substring(0, 1).toUpperCase(Locale.ENGLISH) + code.substring(1, 2)
          + '-' + code.substring(3, 4).toUpperCase(Locale.ENGLISH) + code.substring(4) + ':';
        }
        return code.substring(0, 1).toUpperCase(Locale.ENGLISH) + code.substring(1) + ':';
    }

    /**
     * Replies the wiki language prefix for the current locale.
     *
     * @return the wiki language prefix
     * @see Locale#getDefault()
     * @see #getWikiLanguagePrefix(LocaleType)
     */
    public static String getWikiLanguagePrefix() {
        return getWikiLanguagePrefix(LocaleType.DEFAULT);
    }

    /**
     * Replies the JOSM locale code for the default locale.
     *
     * @return the JOSM locale code for the default locale
     * @see #getJOSMLocaleCode(Locale)
     */
    public static String getJOSMLocaleCode() {
        return getJOSMLocaleCode(Locale.getDefault());
    }

    /**
     * Replies the locale code used by JOSM for a given locale.
     *
     * In most cases JOSM uses the 2-character ISO 639 language code ({@link Locale#getLanguage()}
     * to identify the locale of a localized resource, but in some cases it may use the
     * programmatic name for locales, as replied by {@link Locale#toString()}.
     *
     * For unknown country codes and variants this function already does fallback to
     * internally known translations.
     *
     * @param locale the locale. Replies "en" if null.
     * @return the JOSM code for the given locale
     */
    public static String getJOSMLocaleCode(Locale locale) {
        if (locale == null) return "en";
        for (String full : getLanguageCodes(locale)) {
            if ("iw_IL".equals(full))
                return "he";
            else if ("in".equals(full))
                return "id";
            else if (I18n.hasCode(full)) // catch all non-single codes
                return full;
        }

        // return single code as fallback
        return locale.getLanguage();
    }

    /**
     * Replies the locale code used by Java for a given locale.
     *
     * In most cases JOSM and Java uses the same codes, but for some exceptions this is needed.
     *
     * @param localeName the locale. Replies "en" if null.
     * @return the Java code for the given locale
     * @since 8232
     */
    public static String getJavaLocaleCode(String localeName) {
        if (localeName == null) return "en";
        if ("ca@valencia".equals(localeName)) {
            localeName = "ca__valencia";
        } else if ("he".equals(localeName)) {
            localeName = "iw_IL";
        } else if ("id".equals(localeName)) {
            localeName = "in";
        }
        return localeName;
    }

    /**
     * Replies the display string used by JOSM for a given locale.
     *
     * In most cases returns text replied by {@link Locale#getDisplayName()}, for some
     * locales an override is used (i.e. when unsupported by Java).
     *
     * @param locale the locale. Replies "en" if null.
     * @return the display string for the given locale
     * @since 8232
     */
    public static String getDisplayName(Locale locale) {
        return locale.getDisplayName();
    }

    /**
     * Replies the locale used by Java for a given language code.
     *
     * Accepts JOSM and Java codes as input.
     *
     * @param localeName the locale code.
     * @return the resulting locale
     */
    public static Locale getLocale(String localeName) {
        int country = localeName.indexOf('_');
        int variant = localeName.indexOf('@');
        if (variant < 0 && country >= 0)
            variant = localeName.indexOf('_', country+1);
        Locale l;
        if (variant > 0 && country > 0) {
            l = new Locale(localeName.substring(0, country), localeName.substring(country+1, variant), localeName.substring(variant + 1));
        } else if (variant > 0) {
            l = new Locale(localeName.substring(0, variant), "", localeName.substring(variant + 1));
        } else if (country > 0) {
            l = new Locale(localeName.substring(0, country), localeName.substring(country + 1));
        } else {
            l = new Locale(localeName);
        }
        return l;
    }

    /**
     * Check if a new language is better than a previous existing. Can be used in classes where
     * multiple user supplied language marked strings appear and the best one is searched. Following
     * priorities: current language, english, any other
     *
     * @param oldLanguage the language code of the existing string
     * @param newLanguage the language code of the new string
     * @return true if new one is better
     * @since 8091
     */
    public static boolean isBetterLanguage(String oldLanguage, String newLanguage) {
        if (oldLanguage == null)
            return true;
        String want = getJOSMLocaleCode();
        return want.equals(newLanguage) || (!want.equals(oldLanguage) && newLanguage.startsWith("en"));
    }

    /**
     * Replies the language prefix for use in XML elements (with a dot appended).
     *
     * @return the XML language prefix
     * @see #getJOSMLocaleCode()
     */
    public static String getLanguageCodeXML() {
        String code = getJOSMLocaleCode();
        code = code.replace('@', '-');
        return code+'.';
    }

    /**
     * Replies the language prefix for use in manifests (with an underscore appended).
     *
     * @return the manifest language prefix
     * @see #getJOSMLocaleCode()
     */
    public static String getLanguageCodeManifest() {
        String code = getJOSMLocaleCode();
        code = code.replace('@', '-');
        return code+'_';
    }

    /**
     * Replies a list of language codes for local names. Prefixes range from very specific
     * to more generic.
     * <ul>
     *   <li>lang_COUNTRY@variant  of the current locale</li>
     *   <li>lang@variant  of the current locale</li>
     *   <li>lang_COUNTRY of the current locale</li>
     *   <li>lang of the current locale</li>
     * </ul>
     *
     * @param l the locale to use, <code>null</code> for default locale
     * @return list of codes
     * @since 8283
     */
    public static Collection<String> getLanguageCodes(Locale l) {
        Collection<String> list = new LinkedList<>();
        if (l == null)
            l = Locale.getDefault();
        String lang = l.getLanguage();
        String c = l.getCountry();
        String v = l.getVariant();
        if (c.isEmpty())
            c = null;
        if (v != null && !v.isEmpty()) {
            if (c != null)
                list.add(lang+'_'+c+'@'+v);
            list.add(lang+'@'+v);
        }
        if (c != null)
            list.add(lang+'_'+c);
        list.add(lang);
        return list;
    }
}
