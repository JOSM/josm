// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.util.Locale;
import static org.openstreetmap.josm.tools.I18n.tr;

public class LanguageInfo {

    /**
     * Replies the wiki language prefix for the given locale. The wiki language
     * prefix has the form 'Xy:' where 'Xy' is a ISO 639 language code in title
     * case.
     *
     * @param locale  the locale
     * @return the wiki language prefix
     */
    static public String getWikiLanguagePrefix(Locale locale) {
        String code = getJOSMLocaleCode(locale);
        if (code.length() == 2) {
            if (code.equals("en")) return "";
        } else if (code.equals("zh_TW") || code.equals("zh_CN")) {
            /* do nothing */
        } else if (code.matches("[^_]+_[^_]+")) {
            code = code.substring(0,2);
            if (code.equals("en")) return "";
        } else {
            System.err.println(tr("Warning: failed to derive wiki language prefix from JOSM locale code ''{0}''. Using default code ''en''.", code));
            return "";
        }
        return code.substring(0,1).toUpperCase() + code.substring(1) + ":";
    }

    /**
     * Replies the wiki language prefix for the current locale.
     *
     * @return the wiki language prefix
     * @see Locale#getDefault()
     * @see #getWikiLanguagePrefix(Locale)
     */
    static public String getWikiLanguagePrefix() {
        return getWikiLanguagePrefix(Locale.getDefault());
    }

    /**
     * Replies the JOSM locale code for the default locale.
     *
     * @return the JOSM locale code for the default locale
     * @see #getJOSMLocaleCode(Locale)
     */
    static public String getJOSMLocaleCode() {
        return getJOSMLocaleCode(Locale.getDefault());
    }

    /**
     * Replies the locale code used by JOSM for a given locale.
     *
     * In most cases JOSM uses the 2-character ISO 639 language code ({@see Locale#getLanguage()}
     * to identify the locale of a localized resource, but in some cases it may use the
     * programmatic name for locales, as replied by {@see Locale#toString()}.
     *
     * @param locale the locale. Replies "en" if null.
     * @return the JOSM code for the given locale
     */
    static public String getJOSMLocaleCode(Locale locale) {
        if (locale == null) return "en";
        String full = locale.toString();
        if (full.equals("iw_IL"))
            return "he";
        else if (full.equals("in"))
            return "id";
        else if (I18n.hasCode(full)) /* catch all non-single codes */
            return full;

        /* return single code */
        return locale.getLanguage();
    }

    /**
     * Replies the locale code used by Java for a given locale.
     *
     * In most cases JOSM and Java uses the same codes, but for some exceptions this is needed.
     *
     * @param code the locale code.
     * @return the resulting locale
     */
    static public Locale getLocale(String localeName) {
        if (localeName.equals("he")) {
            localeName = "iw_IL";
        }
        else if (localeName.equals("id")) {
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

    static public String getLanguageCodeXML()
    {
        return getJOSMLocaleCode()+".";
    }
    static public String getLanguageCodeManifest()
    {
        return getJOSMLocaleCode()+"_";
    }
}
