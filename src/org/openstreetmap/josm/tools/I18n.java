// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Vector;
import java.util.logging.Logger;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Internationalisation support.
 *
 * @author Immanuel.Scholz
 */
public class I18n {
    static private final Logger logger = Logger.getLogger(I18n.class.getName());

    /* Base name for translation data. Used for detecting available translations */
    private static final String TR_BASE = "org.openstreetmap.josm.i18n.Translation_";

    /**
     * Set by MainApplication. Changes here later will probably mess up everything, because
     * many strings are already loaded.
     */
    public static org.xnap.commons.i18n.I18n i18n;

    public static final String tr(String text, Object... objects) {
        if (i18n == null)
            return MessageFormat.format(filter(text), objects);
        return filter(i18n.tr(text, objects));
    }

    public static final String tr(String text) {
        if (i18n == null)
            return filter(text);
        return filter(i18n.tr(text));
    }

    public static final String trc(String ctx, String text) {
        if (i18n == null)
            return text;
        return i18n.trc(ctx, text);
    }

    /* NOTE: marktr does NOT support context strings - use marktrc instead */
    public static final String marktr(String text) {
        return text;
    }

    public static final String marktrc(String context, String text) {
        return text;
    }

    public static final String trn(String text, String pluralText, long n, Object... objects) {
        if (i18n == null)
            return n == 1 ? tr(text, objects) : tr(pluralText, objects);
            return filter(i18n.trn(text, pluralText, n, objects));
    }

    public static final String trn(String text, String pluralText, long n) {
        if (i18n == null)
            return n == 1 ? tr(text) : tr(pluralText);
            return filter(i18n.trn(text, pluralText, n));
    }

    public static final String trnc(String ctx, String text, String pluralText, long n, Object... objects) {
        if (i18n == null)
            return n == 1 ? tr(text, objects) : tr(pluralText, objects);
            return i18n.trnc(ctx, text, pluralText, n, objects);
    }

    public static final String trnc(String ctx, String text, String pluralText, long n) {
        if (i18n == null)
            return n == 1 ? tr(text) : tr(pluralText);
            return i18n.trnc(ctx, text, pluralText, n);
    }

    public static final String filter(String text)
    {
        int i;
        if(text.startsWith("_:") && (i = text.indexOf("\n")) >= 0)
            return text.substring(i+1);
        return text;
    }

    /**
     * Get a list of all available JOSM Translations.
     * @return an array of locale objects.
     */
    public static final Locale[] getAvailableTranslations(ProgressMonitor monitor) {
        monitor.indeterminateSubTask(tr("Loading available locales..."));
        Vector<Locale> v = new Vector<Locale>();
        LinkedList<String>str = new LinkedList<String>();
        Locale[] l = Locale.getAvailableLocales();
        monitor.subTask(tr("Checking locales..."));
        monitor.setTicksCount(l.length);
        for (int i = 0; i < l.length; i++) {
            monitor.setCustomText(tr("Checking translation for locale ''{0}''", l[i].getDisplayName()));
            String loc = l[i].toString();
            String cn = TR_BASE + loc;
            try {
                Class.forName(cn);
                v.add(l[i]);
                str.add(loc);
            } catch (ClassNotFoundException e) {
            }
            monitor.worked(1);
        }
        /* hmm, don't know why this is necessary */
        try {
            if(!str.contains("nb")) {
                v.add(new Locale("nb"));
            }
        } catch (Exception e) {}
        try {
            if(!str.contains("gl")) {
                v.add(new Locale("gl"));
            }
        } catch (Exception e) {}
        l = new Locale[v.size()];
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
        /* try initial language settings, may be changed later again */
        try { i18n = I18nFactory.getI18n(MainApplication.class); }
        catch (MissingResourceException ex) { Locale.setDefault(Locale.ENGLISH);}
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
            Locale d = Locale.getDefault();
            if (localeName.equals("he")) {
                localeName = "iw_IL";
            }
            int i = localeName.indexOf('_');
            if (i > 0) {
                l = new Locale(localeName.substring(0, i), localeName.substring(i + 1));
            } else {
                l = new Locale(localeName);
            }
            try {
                Locale.setDefault(l);
                i18n = I18nFactory.getI18n(MainApplication.class);
            } catch (MissingResourceException ex) {
                if (!l.getLanguage().equals("en")) {
                    System.out.println(tr("Unable to find translation for the locale {0}. Reverting to {1}.",
                            l.getDisplayName(), d.getDisplayName()));
                    Locale.setDefault(d);
                } else {
                    i18n = null;
                }
            }
        }
    }
}
