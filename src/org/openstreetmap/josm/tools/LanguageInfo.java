// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.ArrayList;
import java.util.List;
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
        ENGLISH,
        /** The locale prefix on the OSM wiki */
        OSM_WIKI,
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
        if (type == LocaleType.ENGLISH) {
            return "";
        } else if (type == LocaleType.OSM_WIKI && Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
            return "";
        } else if (type == LocaleType.OSM_WIKI && Locale.SIMPLIFIED_CHINESE.equals(locale)) {
            return "Zh-hans:";
        } else if (type == LocaleType.OSM_WIKI && Locale.TRADITIONAL_CHINESE.equals(locale)) {
            return "Zh-hant:";
        }

        String code = getJOSMLocaleCode(locale);

        if (type == LocaleType.OSM_WIKI) {
            if (code.matches("[^_@]+[_@][^_]+")) {
                code = code.substring(0, 2);
                if ("en".equals(code)) {
                    return "";
                }
            }
            switch (code) {
                case "nb":          /* OSM-Wiki has "no", but no "nb" */
                    return "No:";
                case "sr@latin":    /* OSM-Wiki has "Sr-latn" and not Sr-latin */
                    return "Sr-latn:";
                case "de":
                case "es":
                case "fr":
                case "it":
                case "nl":
                case "ru":
                case "ja":
                    return code.toUpperCase(Locale.ENGLISH) + ":";
                default:
                    return code.substring(0, 1).toUpperCase(Locale.ENGLISH) + code.substring(1) + ":";
            }
        }

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
            return code.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + code.charAt(1)
                    + '-'
                    + code.substring(3, 4).toUpperCase(Locale.ENGLISH)
                    + code.substring(4)
                    + ':';
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
     * <p>
     * In most cases JOSM uses the 2-character ISO 639 language code ({@link Locale#getLanguage()}
     * to identify the locale of a localized resource, but in some cases it may use the
     * programmatic name for locales, as replied by {@link Locale#toString()}.
     * <p>
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
     * Replies the OSM locale codes for the default locale.
     *
     * @param prefix a prefix like {@code name:}.
     * @return the OSM locale codes for the default locale
     * @see #getOSMLocaleCodes(String, Locale)
     * @since 19045
     */
    public static String[] getOSMLocaleCodes(String prefix) {
        return getOSMLocaleCodes(prefix, Locale.getDefault());
    }

    /**
     * Replies the locale codes used by OSM for a given locale.
     * <p>
     * In most cases OSM uses the 2-character ISO 639 language code ({@link Locale#getLanguage()}
     * to identify the locale of a localized resource, but in some cases it may use the
     * programmatic name for locales, as replied by {@link Locale#toString()}.
     * <p>
     * For unknown country codes and variants this function already does fallback to
     * internally known translations.
     *
     * @param prefix a prefix like {@code name:}.
     * @param locale the locale. Replies "en" if null.
     * @return the OSM codes for the given locale
     * @since 19045
     */
    public static String[] getOSMLocaleCodes(String prefix, Locale locale) {
        if (prefix == null) {
            prefix = "";
        }
        String main = getJOSMLocaleCode(locale);
        switch (main) {
            case "zh_CN":
                return new String[]{prefix+"zh-Hans-CN", prefix+"zh-Hans", prefix+"zh"};
            case "zh_TW":
                return new String[]{prefix+"zh-Hant-TW", prefix+"zh-Hant", prefix+"zh"};
            default:
                ArrayList<String> r = new ArrayList<>();
                for (String s : LanguageInfo.getLanguageCodes(null)) {
                    r.add(prefix + s);
                }
                return r.toArray(String[]::new);
        }
    }

    /**
     * Replies the locale code used by Java for a given locale.
     * <p>
     * In most cases JOSM and Java uses the same codes, but for some exceptions this is needed.
     *
     * @param localeName the locale. Replies "en" if null.
     * @return the Java code for the given locale
     * @since 8232
     */
    public static String getJavaLocaleCode(String localeName) {
        if (localeName == null) return "en";
        switch (localeName) {
            case "ca@valencia":
                return "ca__valencia";
            case "sr@latin":
                return "sr__latin";
            case "he":
                return "iw_IL";
            case "id":
                return "in";
            default:
                return localeName;
        }
    }

    /**
     * Replies the display string used by JOSM for a given locale.
     * <p>
     * In most cases returns text replied by {@link Locale#getDisplayName()}, for some
     * locales an override is used (i.e. when unsupported by Java).
     *
     * @param locale the locale. Replies "en" if null.
     * @return the display string for the given locale
     * @since 8232
     */
    public static String getDisplayName(Locale locale) {
        String currentCountry = Locale.getDefault().getCountry();
        String localeCountry = locale.getCountry();
        // Don't display locale country if country has been forced to current country at JOSM startup
        if (currentCountry.equals(localeCountry) && !I18n.hasCode(getLanguageCodes(locale).get(0))) {
            return new Locale(locale.getLanguage(), "", locale.getVariant()).getDisplayName();
        }
        return locale.getDisplayName();
    }

    /**
     * Replies the locale used by Java for a given language code.
     * <p>
     * Accepts JOSM and Java codes as input.
     *
     * @param localeName the locale code.
     * @return the resulting locale
     */
    public static Locale getLocale(String localeName) {
        return getLocale(localeName, false);
    }

    /**
     * Replies the locale used by Java for a given language code.
     * <p>
     * Accepts JOSM, Java and POSIX codes as input.
     *
     * @param localeName the locale code.
     * @param useDefaultCountry if {@code true}, the current locale country will be used if no country is specified
     * @return the resulting locale
     * @since 15547
     */
    public static Locale getLocale(String localeName, boolean useDefaultCountry) {
        final int encoding = localeName.indexOf('.');
        if (encoding > 0) {
            localeName = localeName.substring(0, encoding);
        }
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
            l = new Locale(localeName, useDefaultCountry ? Locale.getDefault().getCountry() : "");
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
    public static List<String> getLanguageCodes(Locale l) {
        List<String> list = new ArrayList<>(4);
        if (l == null)
            l = Locale.getDefault();
        String lang = l.getLanguage();
        String c = l.getCountry();
        String v = l.getVariant();
        if (c.isEmpty())
            c = null;
        if (!Utils.isEmpty(v)) {
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
