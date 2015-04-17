// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Locale;

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
        if(type == LocaleType.ENGLISH)
          return "";

        String code = getJOSMLocaleCode();
        if(type == LocaleType.BASELANGUAGE) {
            if(code.matches("[^_]+_[^_]+")) {
                code = code.substring(0,2);
                if ("en".equals(code))
                    return null;
            } else {
                return null;
            }
        } else if(type == LocaleType.DEFAULTNOTENGLISH && "en".equals(code))
            return null;
        return code.substring(0,1).toUpperCase() + code.substring(1) + ":";
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
     * @param locale the locale. Replies "en" if null.
     * @return the JOSM code for the given locale
     */
    public static String getJOSMLocaleCode(Locale locale) {
        if (locale == null) return "en";
        String full = locale.toString();
        if ("iw_IL".equals(full))
            return "he";
        else if ("in".equals(full))
            return "id";
        else if ("ca__valencia".equals(full))
            return "ca@valencia";
        else if (I18n.hasCode(full)) // catch all non-single codes
            return full;

        // return single code
        return locale.getLanguage();
    }

    /**
     * Replies the locale code used by Java for a given locale.
     *
     * In most cases JOSM and Java uses the same codes, but for some exceptions this is needed.
     *
     * @param localeName the locale code.
     * @return the resulting locale
     */
    public static Locale getLocale(String localeName) {
        if ("ca@valencia".equals(localeName) || "ca__valencia".equals(localeName)) {
            return new Locale("ca", "", "valencia");
        }
        if ("he".equals(localeName)) {
            localeName = "iw_IL";
        }
        else if ("id".equals(localeName)) {
            localeName = "in";
        }
        Locale l;
        int i = localeName.indexOf('_');
        if (i > 0) {
            l = new Locale(localeName.substring(0, i), localeName.substring(i + 1));
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
        return getJOSMLocaleCode()+".";
    }

    /**
     * Replies the language prefix for use in manifests (with an underscore appended).
     *
     * @return the manifest language prefix
     * @see #getJOSMLocaleCode()
     */
    public static String getLanguageCodeManifest() {
        return getJOSMLocaleCode()+"_";
    }
}
