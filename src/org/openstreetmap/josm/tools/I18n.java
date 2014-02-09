// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.UIManager;

import org.openstreetmap.gui.jmapviewer.FeatureAdapter.TranslationAdapter;
import org.openstreetmap.josm.Main;

/**
 * Internationalisation support.
 *
 * @author Immanuel.Scholz
 */
public final class I18n {

    private I18n() {
        // Hide default constructor for utils classes
    }

    private enum PluralMode { MODE_NOTONE, MODE_NONE, MODE_GREATERONE,
        MODE_CS/*, MODE_AR*/, MODE_PL/*, MODE_RO*/, MODE_RU, MODE_SK/*, MODE_SL*/}
    private static PluralMode pluralMode = PluralMode.MODE_NOTONE; /* english default */
    private static String loadedCode = "en";

    /* Localization keys for file chooser (and color chooser). */
    private static final String[] javaInternalMessageKeys = new String[] {
        /* JFileChooser windows laf */
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

        /* JFileChooser gtk laf */
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

        /* JFileChooser motif laf */
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

        /* gtk color chooser */
        "GTKColorChooserPanel.blueText",
        "GTKColorChooserPanel.colorNameText",
        "GTKColorChooserPanel.greenText",
        "GTKColorChooserPanel.hueText",
        "GTKColorChooserPanel.nameText",
        "GTKColorChooserPanel.redText",
        "GTKColorChooserPanel.saturationText",
        "GTKColorChooserPanel.valueText",

        /* JOptionPane */
        "OptionPane.okButtonText",
        "OptionPane.yesButtonText",
        "OptionPane.noButtonText",
        "OptionPane.cancelButtonText"
    };
    private static Map<String, String> strings = null;
    private static Map<String, String[]> pstrings = null;
    private static Map<String, PluralMode> languages = new HashMap<String, PluralMode>();

    /**
     * Translates some text for the current locale.
     * These strings are collected by a script that runs on the source code files.
     * After translation, the localizations are distributed with the main program.
     * <br>
     * For example, {@code tr("JOSM''s default value is ''{0}''.", val)}.
     * <br>
     * Use {@link #trn} for distinguishing singular from plural text, i.e.,
     * do not use {@code tr(size == 1 ? "singular" : "plural")} nor
     * {@code size == 1 ? tr("singular") : tr("plural")}
     *
     * @param text the text to translate.
     * Must be a string literal. (No constants or local vars.)
     * Can be broken over multiple lines.
     * An apostrophe ' must be quoted by another apostrophe.
     * @param objects the parameters for the string.
     * Mark occurrences in {@code text} with {@code {0}}, {@code {1}}, ...
     * @return the translated string.
     * @see #trn
     * @see #trc
     * @see #trnc
     */
    public static final String tr(String text, Object... objects) {
        if (text == null) return null;
        return MessageFormat.format(gettext(text, null), objects);
    }

    /**
     * Translates some text in a context for the current locale.
     * There can be different translations for the same text within different contexts.
     *
     * @param context string that helps translators to find an appropriate
     * translation for {@code text}.
     * @param text the text to translate.
     * @return the translated string.
     * @see #tr
     * @see #trn
     * @see #trnc
     */
    public static final String trc(String context, String text) {
        if (context == null)
            return tr(text);
        if (text == null)
            return null;
        return MessageFormat.format(gettext(text, context), (Object)null);
    }

    public static final String trc_lazy(String context, String text) {
        if (context == null)
            return tr(text);
        if (text == null)
            return null;
        return MessageFormat.format(gettext_lazy(text, context), (Object)null);
    }

    /**
     * Marks a string for translation (such that a script can harvest
     * the translatable strings from the source files).
     *
     * For example, {@code
     * String[] options = new String[] {marktr("up"), marktr("down")};
     * lbl.setText(tr(options[0]));}
     * @param text the string to be marked for translation.
     * @return {@code text} unmodified.
     */
    public static final String marktr(String text) {
        return text;
    }

    public static final String marktrc(String context, String text) {
        return text;
    }

    /**
     * Translates some text for the current locale and distinguishes between
     * {@code singularText} and {@code pluralText} depending on {@code n}.
     * <br>
     * For instance, {@code trn("There was an error!", "There were errors!", i)} or
     * {@code trn("Found {0} error in {1}!", "Found {0} errors in {1}!", i, Integer.toString(i), url)}.
     *
     * @param singularText the singular text to translate.
     * Must be a string literal. (No constants or local vars.)
     * Can be broken over multiple lines.
     * An apostrophe ' must be quoted by another apostrophe.
     * @param pluralText the plural text to translate.
     * Must be a string literal. (No constants or local vars.)
     * Can be broken over multiple lines.
     * An apostrophe ' must be quoted by another apostrophe.
     * @param n a number to determine whether {@code singularText} or {@code pluralText} is used.
     * @param objects the parameters for the string.
     * Mark occurrences in {@code singularText} and {@code pluralText} with {@code {0}}, {@code {1}}, ...
     * @return the translated string.
     * @see #tr
     * @see #trc
     * @see #trnc
     */
    public static final String trn(String singularText, String pluralText, long n, Object... objects) {
        return MessageFormat.format(gettextn(singularText, pluralText, null, n), objects);
    }

    /**
     * Translates some text in a context for the current locale and distinguishes between
     * {@code singularText} and {@code pluralText} depending on {@code n}.
     * There can be different translations for the same text within different contexts.
     *
     * @param context string that helps translators to find an appropriate
     * translation for {@code text}.
     * @param singularText the singular text to translate.
     * Must be a string literal. (No constants or local vars.)
     * Can be broken over multiple lines.
     * An apostrophe ' must be quoted by another apostrophe.
     * @param pluralText the plural text to translate.
     * Must be a string literal. (No constants or local vars.)
     * Can be broken over multiple lines.
     * An apostrophe ' must be quoted by another apostrophe.
     * @param n a number to determine whether {@code singularText} or {@code pluralText} is used.
     * @param objects the parameters for the string.
     * Mark occurrences in {@code singularText} and {@code pluralText} with {@code {0}}, {@code {1}}, ...
     * @return the translated string.
     * @see #tr
     * @see #trc
     * @see #trn
     */
    public static final String trnc(String context, String singularText, String pluralText, long n, Object... objects) {
        return MessageFormat.format(gettextn(singularText, pluralText, context, n), objects);
    }

    private static final String gettext(String text, String ctx, boolean lazy)
    {
        int i;
        if(ctx == null && text.startsWith("_:") && (i = text.indexOf('\n')) >= 0)
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
            i = pluralEval(1);
            String[] trans = pstrings.get(ctx == null ? text : "_:"+ctx+"\n"+text);
            if(trans != null && trans.length > i)
                return trans[i];
        }
        return lazy ? gettext(text, null) : text;
    }

    private static final String gettext(String text, String ctx) {
        return gettext(text, ctx, false);
    }


    /* try without context, when context try fails */
    private static final String gettext_lazy(String text, String ctx) {
        return gettext(text, ctx, true);
    }

    private static final String gettextn(String text, String plural, String ctx, long num)
    {
        int i;
        if(ctx == null && text.startsWith("_:") && (i = text.indexOf('\n')) >= 0)
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

    public static String escape(String msg) {
        if (msg == null) return null;
        return msg.replace("\'", "\'\'").replace("{", "\'{\'").replace("}", "\'}\'");
    }

    private static URL getTranslationFile(String lang) {
        return Main.class.getResource("/data/"+lang+".lang");
    }

    /**
     * Get a list of all available JOSM Translations.
     * @return an array of locale objects.
     */
    public static final Locale[] getAvailableTranslations() {
        Collection<Locale> v = new ArrayList<Locale>(languages.size());
        if(getTranslationFile("en") != null)
        {
            for (String loc : languages.keySet()) {
                if(getTranslationFile(loc) != null) {
                    v.add(LanguageInfo.getLocale(loc));
                }
            }
        }
        v.add(Locale.ENGLISH);
        Locale[] l = new Locale[v.size()];
        l = v.toArray(l);
        Arrays.sort(l, new Comparator<Locale>() {
            @Override
            public int compare(Locale o1, Locale o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        return l;
    }

    public static boolean hasCode(String code)
    {
        return languages.containsKey(code);
    }

    public static void init()
    {
        //languages.put("ar", PluralMode.MODE_AR);
        languages.put("bg", PluralMode.MODE_NOTONE);
        languages.put("ca", PluralMode.MODE_NOTONE);
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
        //languages.put("he", PluralMode.MODE_NOTONE);
        languages.put("hu", PluralMode.MODE_NOTONE);
        languages.put("id", PluralMode.MODE_NONE);
        //languages.put("is", PluralMode.MODE_NOTONE);
        languages.put("it", PluralMode.MODE_NOTONE);
        languages.put("ja", PluralMode.MODE_NONE);
        languages.put("nb", PluralMode.MODE_NOTONE);
        languages.put("nl", PluralMode.MODE_NOTONE);
        languages.put("pl", PluralMode.MODE_PL);
        languages.put("pt", PluralMode.MODE_NOTONE);
        languages.put("pt_BR", PluralMode.MODE_GREATERONE);
        //languages.put("ro", PluralMode.MODE_RO);
        languages.put("ru", PluralMode.MODE_RU);
        languages.put("sk", PluralMode.MODE_SK);
        //languages.put("sl", PluralMode.MODE_SL);
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

    public static void addTexts(File source)
    {
        if(loadedCode.equals("en"))
            return;
        FileInputStream fis = null;
        JarInputStream jar = null;
        FileInputStream fisTrans = null;
        JarInputStream jarTrans = null;
        String enfile = "data/en.lang";
        String langfile = "data/"+loadedCode+".lang";
        try
        {
            ZipEntry e;
            fis = new FileInputStream(source);
            jar = new JarInputStream(fis);
            boolean found = false;
            while(!found && (e = jar.getNextEntry()) != null)
            {
                String name = e.getName();
                if(name.equals(enfile))
                    found = true;
            }
            if(found)
            {
                fisTrans = new FileInputStream(source);
                jarTrans = new JarInputStream(fisTrans);
                found = false;
                while(!found && (e = jarTrans.getNextEntry()) != null)
                {
                    String name = e.getName();
                    if(name.equals(langfile))
                        found = true;
                }
                if(found)
                    load(jar, jarTrans, true);
            }
        } catch(IOException e) {
            // Ignore
        } finally {
            Utils.close(jar);
            Utils.close(fis);
            Utils.close(jarTrans);
            Utils.close(fisTrans);
        }
    }

    private static boolean load(String l)
    {
        if(l.equals("en") || l.equals("en_US"))
        {
            strings = null;
            pstrings = null;
            loadedCode = "en";
            pluralMode = PluralMode.MODE_NOTONE;
            return true;
        }
        URL en = getTranslationFile("en");
        if(en == null)
            return false;
        URL tr = getTranslationFile(l);
        if(tr == null || !languages.containsKey(l))
        {
            int i = l.indexOf('_');
            if (i > 0) {
                l = l.substring(0, i);
            }
            tr = getTranslationFile(l);
            if(tr == null || !languages.containsKey(l))
                return false;
        }
        InputStream enStream = null;
        InputStream trStream = null;
        try {
            enStream = en.openStream();
            trStream = tr.openStream();
            if (load(enStream, trStream, false)) {
                pluralMode = languages.get(l);
                loadedCode = l;
                return true;
            }
        } catch(IOException e) {
            // Ignore exception
        } finally {
            Utils.close(trStream);
            Utils.close(enStream);
        }
        return false;
    }

    private static boolean load(InputStream en, InputStream tr, boolean add) {
        Map<String, String> s;
        Map<String, String[]> p;
        if (add) {
            s = strings;
            p = pstrings;
        } else {
            s = new HashMap<String, String>();
            p = new HashMap<String, String[]>();
        }
        /* file format:
           Files are always a group. English file and translated file must provide identical datasets.

           for all single strings:
           {
             unsigned short (2 byte) stringlength
               - length 0 indicates missing translation
               - length 0xFFFE indicates translation equal to original, but otherwise is equal to length 0
             string
           }
           unsigned short (2 byte) 0xFFFF (marks end of single strings)
           for all multi strings:
           {
             unsigned char (1 byte) stringcount
               - count 0 indicates missing translations
               - count 0xFE indicates translations equal to original, but otherwise is equal to length 0
             for stringcount
               unsigned short (2 byte) stringlength
               string
           }
         */
        try
        {
            InputStream ens = new BufferedInputStream(en);
            InputStream trs = new BufferedInputStream(tr);
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
                    if(trnum == 0xFE) /* marks identical string, handle equally to non-translated */
                        trnum = 0;
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
                        enstrings[i] = new String(str, 0, val, Utils.UTF_8);
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
                        trstrings[i] = new String(str, 0, val, Utils.UTF_8);
                    }
                    if(trnum > 0 && !p.containsKey(enstrings[0])) {
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
                    if(trval == 0xFFFE) /* marks identical string, handle equally to non-translated */
                        trval = 0;
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
                        String enstr = new String(str, 0, enval, Utils.UTF_8);
                        if(trval != 0)
                        {
                            val = trs.read(str, 0, trval);
                            if(val != trval) /* file corrupt */
                                return false;
                            String trstr = new String(str, 0, trval, Utils.UTF_8);
                            if(!s.containsKey(enstr))
                                s.put(enstr, trstr);
                        }
                    }
                }
            }
        }
        catch(IOException e)
        {
            return false;
        }
        if(!s.isEmpty())
        {
            strings = s;
            pstrings = p;
            return true;
        }
        return false;
    }

    /**
     * Sets the default locale (see {@link Locale#setDefault(Locale)} to the local
     * given by <code>localName</code>.
     *
     * Ignored if localeName is null. If the locale with name <code>localName</code>
     * isn't found the default local is set to <tt>en</tt> (english).
     *
     * @param localeName the locale name. Ignored if null.
     */
    public static void set(String localeName){
        if (localeName != null) {
            Locale l = LanguageInfo.getLocale(localeName);
            if (load(LanguageInfo.getJOSMLocaleCode(l))) {
                Locale.setDefault(l);
            } else {
                if (!l.getLanguage().equals("en")) {
                    Main.info(tr("Unable to find translation for the locale {0}. Reverting to {1}.",
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
    public static void translateJavaInternalMessages() {
        Locale l = Locale.getDefault();

        JFileChooser.setDefaultLocale(l);
        JColorChooser.setDefaultLocale(l);
        for (String key : javaInternalMessageKeys) {
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
        //case MODE_AR:
        //    return ((n == 0) ? 0 : ((n == 1) ? 1 : ((n == 2) ? 2 : ((((n % 100) >= 3)
        //            && ((n % 100) <= 10)) ? 3 : ((((n % 100) >= 11) && ((n % 100) <= 99)) ? 4 : 5)))));
        case MODE_PL:
            return ((n == 1) ? 0 : (((((n % 10) >= 2) && ((n % 10) <= 4))
                    && (((n % 100) < 10) || ((n % 100) >= 20))) ? 1 : 2));
        //case MODE_RO:
        //    return ((n == 1) ? 0 : ((((n % 100) > 19) || (((n % 100) == 0) && (n != 0))) ? 2 : 1));
        case MODE_RU:
            return ((((n % 10) == 1) && ((n % 100) != 11)) ? 0 : (((((n % 10) >= 2)
                    && ((n % 10) <= 4)) && (((n % 100) < 10) || ((n % 100) >= 20))) ? 1 : 2));
        case MODE_SK:
            return ((n == 1) ? 1 : (((n >= 2) && (n <= 4)) ? 2 : 0));
        //case MODE_SL:
        //    return (((n % 100) == 1) ? 1 : (((n % 100) == 2) ? 2 : ((((n % 100) == 3)
        //            || ((n % 100) == 4)) ? 3 : 0)));
        }
        return 0;
    }

    public static TranslationAdapter getTranslationAdapter() {
        return new TranslationAdapter() {
            @Override
            public String tr(String text, Object... objects) {
                return I18n.tr(text, objects);
            }
        };
    }
}
