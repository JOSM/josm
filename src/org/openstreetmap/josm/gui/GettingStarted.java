// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WikiReader;

public class GettingStarted extends JPanel {
    private String content = "";
    static private String styles = "<style type=\"text/css\">\n"
        + "body { font-family: sans-serif; font-weight: bold; }\n" + "h1 {text-align: center;}\n" + "</style>\n";

    public static class LinkGeneral extends JEditorPane implements HyperlinkListener {
        public LinkGeneral(String text) {
            setContentType("text/html");
            setText(text);
            setEditable(false);
            setOpaque(false);
            addHyperlinkListener(this);
        }

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                OpenBrowser.displayUrl(e.getDescription());
            }
        }
    }

    /**
     * Grabs current MOTD from cache or webpage and parses it.
     */
    private static class MotdContent extends CacheCustomContent {
        public MotdContent() {
            super("motd.html", CacheCustomContent.INTERVAL_DAILY);
        }

        final private int myVersion = Version.getInstance().getVersion();
        final private String myLang = LanguageInfo.getWikiLanguagePrefix();

        /**
         * This function gets executed whenever the cached files need updating
         * @see org.openstreetmap.josm.io.CacheCustomContent#updateData()
         */
        @Override
        protected byte[] updateData() {
            String motd = new WikiReader().readLang("StartupPage");
            if (motd.length() == 0) {
                motd = "<html>" + styles + "<body><h1>" + "JOSM - " + tr("Java OpenStreetMap Editor")
                + "</h1>\n<h2 align=\"center\">(" + tr("Message of the day not available") + ")</h2></html>";
            }
            // Save this to prefs in case JOSM is updated so MOTD can be refreshed
            Main.pref.putInteger("cache.motd.html.version", myVersion);
            Main.pref.put("cache.motd.html.lang", myLang);
            try {
                return motd.getBytes("utf-8");
            } catch(UnsupportedEncodingException e){
                e.printStackTrace();
                return new byte[0];
            }
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
            && Main.pref.get("cache.motd.html.lang").equals(myLang);
        }
    }

    /**
     * Initializes getting the MOTD as well as enabling the FileDrop Listener. Displays a message
     * while the MOTD is downloading.
     */
    public GettingStarted() {
        super(new BorderLayout());
        final LinkGeneral lg = new LinkGeneral("<html>" + styles + "<h1>" + "JOSM - " + tr("Java OpenStreetMap Editor")
                + "</h1><h2 align=\"center\">" + tr("Downloading \"Message of the day\"") + "</h2>");
        // clear the build-in command ctrl+shift+O, because it is used as shortcut in JOSM
        lg.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK), "none");

        JScrollPane scroller = new JScrollPane(lg);
        scroller.setViewportBorder(new EmptyBorder(10, 100, 10, 100));
        add(scroller, BorderLayout.CENTER);

        // Asynchronously get MOTD to speed-up JOSM startup
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (content.length() == 0 && Main.pref.getBoolean("help.displaymotd", true)) {
                    content = new MotdContent().updateIfRequiredString();
                }

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        lg.setText(content);
                        // lg.moveCaretPosition(0);
                    }
                });
            }
        }, "MOTD-Loader");
        t.setDaemon(true);
        t.start();

        new FileDrop(scroller);
    }
}
