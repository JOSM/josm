// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;

/**
 * Internationalisation support.
 *
 * @author Immanuel.Scholz
 */
public class I18n {
    private enum PluralMode { MODE_NOTONE, MODE_NONE, MODE_GREATERONE,
        MODE_CS, MODE_AR, MODE_PL, MODE_RO, MODE_RU, MODE_SK, MODE_SL}
    private static PluralMode pluralMode = PluralMode.MODE_NOTONE; /* english default */

    /* Localization keys for file chooser (and color chooser). */
    private static final String[] jFileChooserLocalizationKeys = new String[] {
        /* windows laf */
        "FileChooser.detailsViewActionLabelText",
        "FileChooser.detailsViewButtonAccessibleName",
        "FileChooser.detailsViewButtonToolTipText",
        "FileChooser.fileAttrHeaderText",
        "FileChooser.fileDateHeaderText",
        "FileChooser.fileNameHeaderText",
        "FileChooser.fileNameLabelText",
        "FileChooser.fileSizeHeaderText",
        "FileChooser.fileTypeHeaderText",
        "FileChooser.filesOfTypeLabelText",
        "FileChooser.homeFolderAccessibleName",
        "FileChooser.homeFolderToolTipText",
        "FileChooser.listViewActionLabelText",
        "FileChooser.listViewButtonAccessibleName",
        "FileChooser.listViewButtonToolTipText",
        "FileChooser.lookInLabelText",
        "FileChooser.newFolderAccessibleName",
        "FileChooser.newFolderActionLabelText",
        "FileChooser.newFolderToolTipText",
        "FileChooser.refreshActionLabelText",
        "FileChooser.saveInLabelText",
        "FileChooser.upFolderAccessibleName",
        "FileChooser.upFolderToolTipText",
        "FileChooser.viewMenuLabelText",

        /* gtk laf */
        "FileChooser.acceptAllFileFilterText",
        "FileChooser.cancelButtonText",
        "FileChooser.cancelButtonToolTipText",
        "FileChooser.deleteFileButtonText",
        "FileChooser.filesLabelText",
        "FileChooser.filterLabelText",
        "FileChooser.foldersLabelText",
        "FileChooser.newFolderButtonText",
        "FileChooser.newFolderDialogText",
        "FileChooser.openButtonText",
        "FileChooser.openButtonToolTipText",
        "FileChooser.openDialogTitleText",
        "FileChooser.pathLabelText",
        "FileChooser.renameFileButtonText",
        "FileChooser.renameFileDialogText",
        "FileChooser.renameFileErrorText",
        "FileChooser.renameFileErrorTitle",
        "FileChooser.saveButtonText",
        "FileChooser.saveButtonToolTipText",
        "FileChooser.saveDialogTitleText",

        /* motif laf */
        //"FileChooser.cancelButtonText",
        //"FileChooser.cancelButtonToolTipText",
        "FileChooser.enterFileNameLabelText",
        //"FileChooser.filesLabelText",
        //"FileChooser.filterLabelText",
        //"FileChooser.foldersLabelText",
        "FileChooser.helpButtonText",
        "FileChooser.helpButtonToolTipText",
        //"FileChooser.openButtonText",
        //"FileChooser.openButtonToolTipText",
        //"FileChooser.openDialogTitleText",
        //"FileChooser.pathLabelText",
        //"FileChooser.saveButtonText",
        //"FileChooser.saveButtonToolTipText",
        //"FileChooser.saveDialogTitleText",
        "FileChooser.updateButtonText",
        "FileChooser.updateButtonToolTipText",

        /* color chooser */
        "GTKColorChooserPanel.blueText",
        "GTKColorChooserPanel.colorNameText",
        "GTKColorChooserPanel.greenText",
        "GTKColorChooserPanel.hueText",
        "GTKColorChooserPanel.nameText",
        "GTKColorChooserPanel.redText",
        "GTKColorChooserPanel.saturationText",
        "GTKColorChooserPanel.valueText"
    };
    private static HashMap<String, String> strings = null;
    private static HashMap<String, String[]> pstrings = null;
    private static HashMap<String, PluralMode> languages = new HashMap<String, PluralMode>();

    /**
     * Set by MainApplication. Changes here later will probably mess up everything, because
     * many strings are already loaded.
     */
    public static final String tr(String text, Object... objects) {
        return MessageFormat.format(gettext(text, null), objects);
    }

    public static final String tr(String text) {
        if (text == null)
            return null;
        return MessageFormat.format(gettext(text, null), (Object)null);
    }

    public static final String trc(String ctx, String text) {
        if (ctx == null)
            return tr(text);
        if (text == null)
            return null;
        return MessageFormat.format(gettext(text, ctx), (Object)null);
    }

    /* NOTE: marktr does NOT support context strings - use marktrc instead */
    public static final String marktr(String text) {
        return text;
    }

    public static final String marktrc(String context, String text) {
        return text;
    }

    public static final String trn(String text, String pluralText, long n, Object... objects) {
        return MessageFormat.format(gettextn(text, pluralText, null, n), objects);
    }

    public static final String trn(String text, String pluralText, long n) {
        return MessageFormat.format(gettextn(text, pluralText, null, n), (Object)null);
    }

    public static final String trnc(String ctx, String text, String pluralText, long n, Object... objects) {
        return MessageFormat.format(gettextn(text, pluralText, ctx, n), objects);
    }

    public static final String trnc(String ctx, String text, String pluralText, long n) {
        return MessageFormat.format(gettextn(text, pluralText, ctx, n), (Object)null);
    }

    private static final String gettext(String text, String ctx)
    {
        int i;
        if(ctx == null && text.startsWith("_:") && (i = text.indexOf("\n")) >= 0)
        {
            ctx = text.substring(2,i-1);
            text = text.substring(i+1);
        }
        if(strings != null)
        {
            String trans = strings.get(ctx == null ? text : "_:"+ctx+"\n"+text);
            if(trans != null)
                return trans;
        }
        if(pstrings != null) {
            String[] trans = pstrings.get(ctx == null ? text : "_:"+ctx+"\n"+text);
            if(trans != null)
                return trans[0];
        }
        return text;
    }

    private static final String gettextn(String text, String plural, String ctx, long num)
    {
        int i;
        if(ctx == null && text.startsWith("_:") && (i = text.indexOf("\n")) >= 0)
        {
            ctx = text.substring(2,i-1);
            text = text.substring(i+1);
        }
        if(pstrings != null)
        {
            i = pluralEval(num);
            String[] trans = pstrings.get(ctx == null ? text : "_:"+ctx+"\n"+text);
            if(trans != null && trans.length > i)
                return trans[i];
        }

        return num == 1 ? text : plural;
    }

    /**
     * Get a list of all available JOSM Translations.
     * @return an array of locale objects.
     */
    public static final Locale[] getAvailableTranslations() {
        Vector<Locale> v = new Vector<Locale>();
        if(Main.class.getResource("/data/en.lang") != null)
        {
            for (String loc : languages.keySet()) {
                if(Main.class.getResource("/data/"+loc+".lang") != null) {
                    int i = loc.indexOf('_');
                    if (i > 0) {
                        v.add(new Locale(loc.substring(0, i), loc.substring(i + 1)));
                    } else {
                        v.add(new Locale(loc));
                    }
                }
            }
        }
        v.add(Locale.ENGLISH);
        Locale[] l = new Locale[v.size()];
        l = v.toArray(l);
        Arrays.sort(l, new Comparator<Locale>() {
            public int compare(Locale o1, Locale o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        return l;
    }

    public static void init()
    {
        languages.put("ar", PluralMode.MODE_AR);
        languages.put("bg", PluralMode.MODE_NOTONE);
        languages.put("cs", PluralMode.MODE_CS);
        languages.put("da", PluralMode.MODE_NOTONE);
        languages.put("de", PluralMode.MODE_NOTONE);
        languages.put("el", PluralMode.MODE_NOTONE);
        languages.put("en_AU", PluralMode.MODE_NOTONE);
        languages.put("en_GB", PluralMode.MODE_NOTONE);
        languages.put("es", PluralMode.MODE_NOTONE);
        languages.put("et", PluralMode.MODE_NOTONE);
        languages.put("eu", PluralMode.MODE_NOTONE);
        languages.put("fi", PluralMode.MODE_NOTONE);
        languages.put("fr", PluralMode.MODE_GREATERONE);
        languages.put("gl", PluralMode.MODE_NOTONE);
        languages.put("is", PluralMode.MODE_NOTONE);
        languages.put("it", PluralMode.MODE_NOTONE);
        languages.put("iw_IL", PluralMode.MODE_NOTONE);
        languages.put("ja", PluralMode.MODE_NONE);
        languages.put("nb", PluralMode.MODE_NOTONE);
        languages.put("nl", PluralMode.MODE_NOTONE);
        languages.put("pl", PluralMode.MODE_PL);
        languages.put("pt_BR", PluralMode.MODE_GREATERONE);
        languages.put("ro", PluralMode.MODE_RO);
        languages.put("ru", PluralMode.MODE_RU);
        languages.put("sk", PluralMode.MODE_SK);
        languages.put("sl", PluralMode.MODE_SL);
        languages.put("sv", PluralMode.MODE_NOTONE);
        languages.put("tr", PluralMode.MODE_NONE);
        languages.put("uk", PluralMode.MODE_RU);
        languages.put("zh_CN", PluralMode.MODE_NONE);
        languages.put("zh_TW", PluralMode.MODE_NONE);

        /* try initial language settings, may be changed later again */
        if(!load(Locale.getDefault().toString())) {
            Locale.setDefault(Locale.ENGLISH);
        }
    }

    private static boolean load(String l)
    {
        if(l.equals("en") || l.equals("en_US"))
        {
            strings = null;
            pstrings = null;
            pluralMode = PluralMode.MODE_NOTONE;
            return true;
        }
        URL en = Main.class.getResource("/data/en.lang");
        if(en == null)
            return false;
        URL tr = Main.class.getResource("/data/"+l+".lang");
        if(tr == null)
        {
            int i = l.indexOf('_');
            if (i > 0) {
                l = l.substring(0, i);
            }
            tr = Main.class.getResource("/data/"+l+".lang");
            if(tr == null)
                return false;
        }

        HashMap<String, String> s = new HashMap<String, String>();
        HashMap<String, String[]> p = new HashMap<String, String[]>();
        /* file format:
           for all single strings:
           {
             unsigned short (2 byte) stringlength
             string
           }
           unsigned short (2 byte) 0xFFFF (marks end of single strings)
           for all multi strings:
           {
             unsigned char (1 byte) stringcount
             for stringcount
               unsigned short (2 byte) stringlength
               string
           }
         */
        try
        {
            InputStream ens = new BufferedInputStream(en.openStream());
            InputStream trs = new BufferedInputStream(tr.openStream());
            byte[] enlen = new byte[2];
            byte[] trlen = new byte[2];
            boolean multimode = false;
            byte[] str = new byte[4096];
            for(;;)
            {
                if(multimode)
                {
                    int ennum = ens.read();
                    int trnum = trs.read();
                    if((ennum == -1 && trnum != -1) || (ennum != -1 && trnum == -1)) /* files do not match */
                        return false;
                    if(ennum == -1) {
                        break;
                    }
                    String[] enstrings = new String[ennum];
                    String[] trstrings = new String[trnum];
                    for(int i = 0; i < ennum; ++i)
                    {
                        int val = ens.read(enlen);
                        if(val != 2) /* file corrupt */
                            return false;
                        val = (enlen[0] < 0 ? 256+enlen[0]:enlen[0])*256+(enlen[1] < 0 ? 256+enlen[1]:enlen[1]);
                        if(val > str.length) {
                            str = new byte[val];
                        }
                        int rval = ens.read(str, 0, val);
                        if(rval != val) /* file corrupt */
                            return false;
                        enstrings[i] = new String(str, 0, val, "utf-8");
                    }
                    for(int i = 0; i < trnum; ++i)
                    {
                        int val = trs.read(trlen);
                        if(val != 2) /* file corrupt */
                            return false;
                        val = (trlen[0] < 0 ? 256+trlen[0]:trlen[0])*256+(trlen[1] < 0 ? 256+trlen[1]:trlen[1]);
                        if(val > str.length) {
                            str = new byte[val];
                        }
                        int rval = trs.read(str, 0, val);
                        if(rval != val) /* file corrupt */
                            return false;
                        trstrings[i] = new String(str, 0, val, "utf-8");
                    }
                    if(trnum > 0) {
                        p.put(enstrings[0], trstrings);
                    }
                }
                else
                {
                    int enval = ens.read(enlen);
                    int trval = trs.read(trlen);
                    if(enval != trval) /* files do not match */
                        return false;
                    if(enval == -1) {
                        break;
                    }
                    if(enval != 2) /* files corrupt */
                        return false;
                    enval = (enlen[0] < 0 ? 256+enlen[0]:enlen[0])*256+(enlen[1] < 0 ? 256+enlen[1]:enlen[1]);
                    trval = (trlen[0] < 0 ? 256+trlen[0]:trlen[0])*256+(trlen[1] < 0 ? 256+trlen[1]:trlen[1]);
                    if(enval == 0xFFFF)
                    {
                        multimode = true;
                        if(trval != 0xFFFF) /* files do not match */
                            return false;
                    }
                    else
                    {
                        if(enval > str.length) {
                            str = new byte[enval];
                        }
                        if(trval > str.length) {
                            str = new byte[trval];
                        }
                        int val = ens.read(str, 0, enval);
                        if(val != enval) /* file corrupt */
                            return false;
                        String enstr = new String(str, 0, enval, "utf-8");
                        if(trval != 0)
                        {
                            val = trs.read(str, 0, trval);
                            if(val != trval) /* file corrupt */
                                return false;
                            String trstr = new String(str, 0, trval, "utf-8");
                            s.put(enstr, trstr);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            return false;
        }
        if(!s.isEmpty() && languages.containsKey(l))
        {
            strings = s;
            pstrings = p;
            pluralMode = languages.get(l);
            return true;
        }
        return false;
    }

    /**
     * Sets the default locale (see {@see Locale#setDefault(Locale)} to the local
     * given by <code>localName</code>.
     *
     * Ignored if localName is null. If the locale with name <code>localName</code>
     * isn't found the default local is set to <tt>en</tt> (english).
     *
     * @param localeName the locale name. Ignored if null.
     */
    public static void set(String localeName){
        if (localeName != null) {
            Locale l;
            if (localeName.equals("he")) {
                localeName = "iw_IL";
            }
            int i = localeName.indexOf('_');
            if (i > 0) {
                l = new Locale(localeName.substring(0, i), localeName.substring(i + 1));
            } else {
                l = new Locale(localeName);
            }
            if (load(localeName)) {
                Locale.setDefault(l);
            } else {
                if (!l.getLanguage().equals("en")) {
                    System.out.println(tr("Unable to find translation for the locale {0}. Reverting to {1}.",
                            l.getDisplayName(), Locale.getDefault().getDisplayName()));
                } else {
                    strings = null;
                    pstrings = null;
                }
            }
        }
    }

    /**
     * Localizations for file chooser dialog.
     * For some locales (e.g. de, fr) translations are provided
     * by Java, but not for others (e.g. ru, uk).
     */
    public static void fixJFileChooser() {
        Locale l = Locale.getDefault();

        JFileChooser.setDefaultLocale(l);
        JColorChooser.setDefaultLocale(l);
        for (String key : jFileChooserLocalizationKeys) {
            String us = UIManager.getString(key, Locale.US);
            String loc = UIManager.getString(key, l);
            // only provide custom translation if it is not already localized by Java
            if (us != null && us.equals(loc)) {
                UIManager.put(key, tr(us));
            }
        }
    }

    private static int pluralEval(long n)
    {
        switch(pluralMode)
        {
        case MODE_NOTONE: /* bg, da, de, el, en, en_GB, es, et, eu, fi, gl, is, it, iw_IL, nb, nl, sv */
            return ((n != 1) ? 1 : 0);
        case MODE_NONE: /* ja, tr, zh_CN, zh_TW */
            return 0;
        case MODE_GREATERONE: /* fr, pt_BR */
            return ((n > 1) ? 1 : 0);
        case MODE_CS:
            return ((n == 1) ? 0 : (((n >= 2) && (n <= 4)) ? 1 : 2));
        case MODE_AR:
            return ((n == 0) ? 0 : ((n == 1) ? 1 : ((n == 2) ? 2 : ((((n % 100) >= 3)
                    && ((n % 100) <= 10)) ? 3 : ((((n % 100) >= 11) && ((n % 100) <= 99)) ? 4 : 5)))));
        case MODE_PL:
            return ((n == 1) ? 0 : (((((n % 10) >= 2) && ((n % 10) <= 4))
                    && (((n % 100) < 10) || ((n % 100) >= 20))) ? 1 : 2));
        case MODE_RO:
            return ((n == 1) ? 0 : ((((n % 100) > 19) || (((n % 100) == 0) && (n != 0))) ? 2 : 1));
        case MODE_RU:
            return ((((n % 10) == 1) && ((n % 100) != 11)) ? 0 : (((((n % 10) >= 2)
                    && ((n % 10) <= 4)) && (((n % 100) < 10) || ((n % 100) >= 20))) ? 1 : 2));
        case MODE_SK:
            return ((n == 1) ? 1 : (((n >= 2) && (n <= 4)) ? 2 : 0));
        case MODE_SL:
            return (((n % 100) == 1) ? 1 : (((n % 100) == 2) ? 2 : ((((n % 100) == 3)
                    || ((n % 100) == 4)) ? 3 : 0)));
        }
        return 0;
    }
}
