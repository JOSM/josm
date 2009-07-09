// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.util.Locale;

public class LanguageInfo {
    static public String getLanguageCodeWiki()
    {
        String languageCode = getLanguageCode();
        if(languageCode.equals("en"))
            return "";
        else if(languageCode.equals("pt_BR"))
            return "Pt:";
        return languageCode.substring(0,1).toUpperCase() + languageCode.substring(1) + ":";
    }
    static public String getLanguageCode()
    {
        String full = Locale.getDefault().toString();
        if (full.equals("iw_IL"))
            return "he";
        /* list of non-single codes supported by josm */
        else if (full.equals("en_GB"))
            return full;
        return Locale.getDefault().getLanguage();
    }
    static public String getLanguageCodeXML()
    {
        return getLanguageCode()+".";
    }
    static public String getLanguageCodeManifest()
    {
        return getLanguageCode()+"_";
    }
}
