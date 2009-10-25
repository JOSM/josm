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
        } else if (code.matches("[^_]+_[^_]+")) {
            code = code.substring(0,2);
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
     * Replies the local code used by JOSM for a given locale.
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
        /* list of non-single codes supported by josm */
        else if (full.equals("en_GB"))
            return full;

        return locale.getLanguage();
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
