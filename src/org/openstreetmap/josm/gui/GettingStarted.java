// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreference;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferenceListener;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WikiReader;

public final class GettingStarted extends JPanel implements ProxyPreferenceListener {

    private final LinkGeneral lg;
    private String content = "";
    private boolean contentInitialized = false;

    private static final String STYLE = "<style type=\"text/css\">\n"
            + "body {font-family: sans-serif; font-weight: bold; }\n"
            + "h1 {text-align: center; }\n"
            + ".icon {font-size: 0; }\n"
            + "</style>\n";

    public static class LinkGeneral extends JosmEditorPane implements HyperlinkListener {

        /**
         * Constructs a new {@code LinkGeneral} with the given HTML text
         * @param text The text to display
         */
        public LinkGeneral(String text) {
            setContentType("text/html");
            setText(text);
            setEditable(false);
            setOpaque(false);
            addHyperlinkListener(this);
        }

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                OpenBrowser.displayUrl(e.getDescription());
            }
        }
    }

    /**
     * Grabs current MOTD from cache or webpage and parses it.
     */
    private static class MotdContent extends CacheCustomContent<IOException> {
        public MotdContent() {
            super("motd.html", CacheCustomContent.INTERVAL_DAILY);
        }

        final private int myVersion = Version.getInstance().getVersion();
        final private String myJava = System.getProperty("java.version");
        final private String myLang = LanguageInfo.getWikiLanguagePrefix();

        /**
         * This function gets executed whenever the cached files need updating
         * @see org.openstreetmap.josm.io.CacheCustomContent#updateData()
         */
        @Override
        protected byte[] updateData() throws IOException {
            String motd = new WikiReader().readLang("StartupPage");
            // Save this to prefs in case JOSM is updated so MOTD can be refreshed
            Main.pref.putInteger("cache.motd.html.version", myVersion);
            Main.pref.put("cache.motd.html.java", myJava);
            Main.pref.put("cache.motd.html.lang", myLang);
            return motd.getBytes(Utils.UTF_8);
        }

        /**
         * Additionally check if JOSM has been updated and refresh MOTD
         */
        @Override
        protected boolean isCacheValid() {
            // We assume a default of myVersion because it only kicks in in two cases:
            // 1. Not yet written - but so isn't the interval variable, so it gets updated anyway
            // 2. Cannot be written (e.g. while developing). Obviously we don't want to update
            // everytime because of something we can't read.
            return (Main.pref.getInteger("cache.motd.html.version", -999) == myVersion)
            && Main.pref.get("cache.motd.html.java").equals(myJava)
            && Main.pref.get("cache.motd.html.lang").equals(myLang);
        }
    }

    /**
     * Initializes getting the MOTD as well as enabling the FileDrop Listener. Displays a message
     * while the MOTD is downloading.
     */
    public GettingStarted() {
        super(new BorderLayout());
        lg = new LinkGeneral("<html>" + STYLE + "<h1>" + "JOSM - " + tr("Java OpenStreetMap Editor")
                + "</h1><h2 align=\"center\">" + tr("Downloading \"Message of the day\"") + "</h2></html>");
        // clear the build-in command ctrl+shift+O, because it is used as shortcut in JOSM
        lg.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK), "none");

        JScrollPane scroller = new JScrollPane(lg);
        scroller.setViewportBorder(new EmptyBorder(10, 100, 10, 100));
        add(scroller, BorderLayout.CENTER);

        getMOTD();

        new FileDrop(scroller);
    }

    private void getMOTD() {
        // Asynchronously get MOTD to speed-up JOSM startup
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!contentInitialized && Main.pref.getBoolean("help.displaymotd", true)) {
                    try {
                        content = new MotdContent().updateIfRequiredString();
                        contentInitialized = true;
                        ProxyPreference.removeProxyPreferenceListener(GettingStarted.this);
                    } catch (IOException ex) {
                        Main.warn(tr("Failed to read MOTD. Exception was: {0}", ex.toString()));
                        content = "<html>" + STYLE + "<h1>" + "JOSM - " + tr("Java OpenStreetMap Editor")
                                + "</h1>\n<h2 align=\"center\">(" + tr("Message of the day not available") + ")</h2></html>";
                        // In case of MOTD not loaded because of proxy error, listen to preference changes to retry after update
                        ProxyPreference.addProxyPreferenceListener(GettingStarted.this);
                    }
                }

                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        lg.setText(fixImageLinks(content));
                    }
                });
            }
        }, "MOTD-Loader");
        t.setDaemon(true);
        t.start();
    }

    private String fixImageLinks(String s) {
        Matcher m = Pattern.compile("src=\""+Main.JOSM_WEBSITE+"/browser/trunk(/images/.*?\\.png)\\?format=raw\"").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String im = m.group(1);
            URL u = getClass().getResource(im);
            if (u != null) {
                m.appendReplacement(sb, Matcher.quoteReplacement("src=\"" + u.toString() + "\""));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public void proxyPreferenceChanged() {
        getMOTD();
    }
}
