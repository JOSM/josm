// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openstreetmap.josm.actions.DownloadPrimitiveAction;
import org.openstreetmap.josm.actions.HistoryInfoAction;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.animation.AnimationExtensionManager;
import org.openstreetmap.josm.gui.datatransfer.OpenTransferHandler;
import org.openstreetmap.josm.gui.dialogs.MenuItemSearchDialog;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreference;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferenceListener;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WikiReader;

/**
 * Panel that fills the main part of the program window when JOSM has just started.
 *
 * It downloads and displays the so called <em>message of the day</em>, which
 * contains news about recent major changes, warning in case of outdated versions, etc.
 */
public final class GettingStarted extends JPanel implements ProxyPreferenceListener {

    private final LinkGeneral lg;
    private String content = "";
    private boolean contentInitialized;
    private final Timer timer = new Timer(50, e -> repaint());

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
            adaptForNimbus(this);
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
    static class MotdContent extends CacheCustomContent<IOException> {
        MotdContent() {
            super("motd.html", CacheCustomContent.INTERVAL_DAILY);
        }

        private final int myVersion = Version.getInstance().getVersion();
        private final String myJava = Utils.getSystemProperty("java.version");
        private final String myLang = LanguageInfo.getWikiLanguagePrefix();

        /**
         * This function gets executed whenever the cached files need updating
         * @see org.openstreetmap.josm.io.CacheCustomContent#updateData()
         */
        @Override
        protected byte[] updateData() throws IOException {
            String motd = new WikiReader().readLang("StartupPage");
            // Save this to prefs in case JOSM is updated so MOTD can be refreshed
            Config.getPref().putInt("cache.motd.html.version", myVersion);
            Config.getPref().put("cache.motd.html.java", myJava);
            Config.getPref().put("cache.motd.html.lang", myLang);
            return motd.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected boolean isOffline() {
            return NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE);
        }

        /**
         * Additionally check if JOSM has been updated and refresh MOTD
         */
        @Override
        protected boolean isCacheValid() {
            // We assume a default of myVersion because it only kicks in in two cases:
            // 1. Not yet written - but so isn't the interval variable, so it gets updated anyway
            // 2. Cannot be written (e.g. while developing). Obviously we don't want to update
            // every time because of something we can't read.
            return (Config.getPref().getInt("cache.motd.html.version", -999) == myVersion)
            && Config.getPref().get("cache.motd.html.java").equals(myJava)
            && Config.getPref().get("cache.motd.html.lang").equals(myLang);
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
        // clear the build-in command ctrl+shift+O, ctrl+space, ctrl+H because it is used as shortcut in JOSM
        lg.getInputMap(JComponent.WHEN_FOCUSED).put(DownloadPrimitiveAction.SHORTCUT.getKeyStroke(), "none");
        lg.getInputMap(JComponent.WHEN_FOCUSED).put(MenuItemSearchDialog.Action.SHORTCUT.getKeyStroke(), "none");
        lg.getInputMap(JComponent.WHEN_FOCUSED).put(HistoryInfoAction.SHORTCUT.getKeyStroke(), "none");
        lg.setTransferHandler(null);

        JScrollPane scroller = new JScrollPane(lg);
        scroller.setViewportBorder(new EmptyBorder(10, 100, 10, 100));
        add(scroller, BorderLayout.CENTER);

        getMOTD();

        setTransferHandler(new OpenTransferHandler());
    }

    @Override
    public void addNotify() {
        timer.start();
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (isShowing()) {
            AnimationExtensionManager.getExtension().adjustForSize(getWidth(), getHeight());
            AnimationExtensionManager.getExtension().animate();
            AnimationExtensionManager.getExtension().paint(g);
        }
    }

    private void getMOTD() {
        // Asynchronously get MOTD to speed-up JOSM startup
        Thread t = new Thread((Runnable) () -> {
            if (!contentInitialized && Config.getPref().getBoolean("help.displaymotd", true)) {
                try {
                    content = new MotdContent().updateIfRequiredString();
                    contentInitialized = true;
                    ProxyPreference.removeProxyPreferenceListener(this);
                } catch (IOException ex) {
                    Logging.log(Logging.LEVEL_WARN, tr("Failed to read MOTD. Exception was: {0}", ex.toString()), ex);
                    content = "<html>" + STYLE + "<h1>" + "JOSM - " + tr("Java OpenStreetMap Editor")
                            + "</h1>\n<h2 align=\"center\">(" + tr("Message of the day not available") + ")</h2></html>";
                    // In case of MOTD not loaded because of proxy error, listen to preference changes to retry after update
                    ProxyPreference.addProxyPreferenceListener(this);
                }
            }

            if (content != null) {
                EventQueue.invokeLater(() -> lg.setText(fixImageLinks(content)));
            }
        }, "MOTD-Loader");
        t.setDaemon(true);
        t.start();
    }

    static String fixImageLinks(String s) {
        Matcher m = Pattern.compile("src=\"/browser/trunk/resources/images/(.*?)\\.(png|svg)\\?format=raw\"").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String im = m.group(1);
            String u = new ImageProvider(im).setMaxSize(ImageSizes.HTMLINLINE).getDataURL();
            if (u != null) {
                m.appendReplacement(sb, Matcher.quoteReplacement("src=\"" + u + '\"'));
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
