// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WikiReader;
import org.openstreetmap.josm.actions.AboutAction;

public class GettingStarted extends JPanel {
    private String content = "";
    static private String styles = "<style type=\"text/css\">\n"+
            "body { font-family: sans-serif; font-weight: bold; }\n"+
            "h1 {text-align: center;}\n"+
            "</style>\n";

    public class LinkGeneral extends JEditorPane implements HyperlinkListener {
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
     * This class encapsulates the "reading URL" task and can be executed in background and in
     * parallel. Since the MOTD is many separate pages this speeds things up quite a lot. If no
     * localized version is available, it automatically falls back to the international one.
     */
    private class readMOTD implements Callable<String> {
        private boolean isLocalized;
        private boolean isHelp;
        private String urlLoc;
        private String urlIntl;
        private String urlBase;

        /**
         * Read a MOTD page
         * @param isLocalized If true, tries to get localized version as defined in urlLoc
         * @param urlBase Base URL (i.e. http://www.openstreetmap.de/wiki/)
         * @param urlLoc Part to append to base URL to receive localized version
         * @param urlIntl Part to append to base URL to receive international version
         * @param makeList If true, the URL's contents will be wrapped in a list (<ul><li>)
         */
        readMOTD(boolean isLocalized, String urlBase, String urlLoc, String urlIntl, boolean makeList) {
          this.isLocalized = isLocalized;
          this.urlBase = urlBase;
          this.urlLoc = urlLoc;
          this.urlIntl = urlIntl;
          this.isHelp = makeList;
        }

        /*
         * Does the actual work
         * @see java.util.concurrent.Callable#call()
         */
        public String call() {
            WikiReader wr = new WikiReader(urlBase);
            String content = "";
            try {
                // If we hit a non-localized link here, we already know there's no translated
                // version available
                String message = isLocalized ? wr.read(urlLoc) : "";
                // Look for non-localized version
                if (message.equals(""))
                    message = wr.read(urlIntl);

                if (!message.equals(""))
                    if(isHelp)
                        content += message;
                    else
                        content += "<ul><li>"+ message.substring(8)
                                            .replaceAll("\n *\\* +","</li><li>")+"</li></ul>";
            } catch (IOException ioe) {
                try {
                    if(isHelp)
                        content += wr.read(urlIntl);
                    else
                        content += "<ul><li>"+wr.read(urlIntl).substring(8)
                                            .replaceAll("\n *\\* +","</li><li>")+"</li></ul>";
                } catch (IOException ioe2) {
                }
            }

            return content;
        }
    }

    /**
     * Grabs current MOTD from cache or webpage and parses it.
     */
    private class assignContent extends CacheCustomContent {
        public assignContent() {
            super("motd.html", CacheCustomContent.INTERVAL_DAILY);
        }

        final private int myVersion = AboutAction.getVersionNumber();

        /**
         * This function gets executed whenever the cached files need updating
         * @see org.openstreetmap.josm.io.CacheCustomContent#updateData()
         */
        protected byte[] updateData() {
            String motd = "";
            String baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
            WikiReader wr = new WikiReader(baseurl);
            String motdcontent = "";
            try {
                motdcontent = wr.read(baseurl + "/wiki/MessageOfTheDay?format=txt");
            } catch (IOException ioe) {
                motdcontent = "<html>" + styles + "<body><h1>" +
                    "JOSM - " + tr("Java OpenStreetMap Editor") +
                    "</h1>\n<h2 align=\"center\">(" +
                    tr ("Message of the day not available") +
                    ")</h2>";
            }

            String languageCode = Main.getLanguageCodeU();

            // Finds wiki links like (underscores inserted for readability):
            // [wiki:LANGCODE:messageoftheday_CONDITON_REVISION LANGCODE]
            // Langcode usually consists of two letters describing the language and may be omitted
            // Condition may be one of the following: >  <  <=  =>
            // Revision is the JOSM version
            Pattern versionPattern = Pattern.compile(
                    "\\[wiki:(?:[A-Z]+:)?MessageOfTheDay(\\>\\=|\\<\\=|\\<|\\>)([0-9]+)\\s*([A-Z]*)\\]",
                    Pattern.CASE_INSENSITIVE);
            // 1=condition, 2=targetVersion, 3=lang
            Matcher matcher = versionPattern.matcher(motdcontent);
            matcher.reset();

            ArrayList<String[]> links = new ArrayList<String[]>();
            String linksList="";
            while (matcher.find()) {
                // Discards all but the selected locale and non-localized links
                if(!(matcher.group(3)+":").equals(languageCode) && !matcher.group(3).equals(""))
                    continue;

                links.add(new String[] {matcher.group(1), matcher.group(2), matcher.group(3)});
                linksList += matcher.group(1)+matcher.group(2)+matcher.group(3)+": ";
            }

            // We cannot use Main.worker here because it's single-threaded and
            // setting it to multi-threading will cause problems elsewhere
            ExecutorService slave = Executors.newCachedThreadPool();

            ArrayList<Future<String>> linkContents = new ArrayList<Future<String>>();
            for(int i=0; i < links.size(); i++) {
                String[] obj = links.get(i);
                int targetVersion = Integer.parseInt(obj[1]);
                String condition = obj[0];
                Boolean isLocalized = !obj[2].equals("");

                // Prefer localized over non-localized links, if they're otherwise the same
                if(!isLocalized && linksList.indexOf(condition + obj[1] + languageCode + " ") >= 0)
                    continue;

                boolean included = false;

                if(myVersion == 0)
                  included = true;
                else if(condition.equals(">="))
                  included=myVersion >= targetVersion;
                else if(condition.equals(">"))
                  included = myVersion > targetVersion;
                else if(condition.equals("<"))
                  included=myVersion < targetVersion;
                else
                  included = myVersion <= targetVersion;

                if(!included) continue;

                boolean isHelp = targetVersion == 1;
                String urlStart = baseurl + "/wiki/";
                String urlEnd = "MessageOfTheDay" + condition + targetVersion
                                    + (isHelp ? "" : "?format=txt");
                String urlLoc = urlStart + languageCode + urlEnd;
                String urlIntl = urlStart + urlEnd;

                // This adds all links to the worker which will download them concurrently
                linkContents.add(slave.submit(new readMOTD(isLocalized, baseurl, urlLoc, urlIntl, isHelp)));
            }
            // Gets newest version numbers
            linkContents.add(slave.submit(new readMOTD(false, baseurl, "",
                    baseurl + "/version?format=txt", true)));

            for(int i=0; i < linkContents.size()-1; i++) {
                try {
                    motd += linkContents.get(i).get();
                } catch (Exception e) {}
            }

            motd = "<html>"
                + styles
                + "<h1>JOSM - "
                + tr("Java OpenStreetMap Editor")
                + "</h1>"
                + motd.replace("</html>", "")
                + getVersionNumber(linkContents.get(linkContents.size()-1))
                + "</html>";

            linkContents.clear();
            try {
                slave.shutdown();
            } catch(SecurityException x) {}

            // Save this to prefs in case JOSM is updated so MOTD can be refreshed
            Main.pref.putInteger("cache.motd.html.version", myVersion);

            return motd.getBytes();
        }

        /**
         * Additionally check if JOSM has been updated and refresh MOTD
         */
        @Override
        protected boolean isCacheValid() {
            // We assume a default of myVersion because it only kicks in in two cases:
            // 1. Not yet written - but so isn't the interval variable, so it gets updated anyway
            // 2. Cannot be written (e.g. while developing). Obviously we don't want to update
            //    everytime because of something we can't read.
            return Main.pref.getInteger("cache.motd.html.version", myVersion) == myVersion;
        }

        /**
         * Tries to read the version number from a given Future<String>
         * @param Future<String> that contains the version page
         * @return String with HTML Code
         */
        private String getVersionNumber(Future<String> linkContent) {
            try {
                String str = linkContent.get();
                Matcher m = Pattern.compile(".*josm-tested\\.jar: *(\\d+).*", Pattern.DOTALL).matcher(str);
                m.matches();
                int curVersion = Integer.parseInt(m.group(1));
                m = Pattern.compile(".*josm-latest\\.jar: *(\\d+).*", Pattern.DOTALL).matcher(str);
                m.matches();
                int latest = Integer.parseInt(m.group(1));
                return "<div style=\"text-align:right;font-size:small;font-weight:normal;\">"
                + "<b>"
                + (curVersion > myVersion ? tr("Update available") + " &#151; ": "")
                + tr("Version Details:") + "</b> "
                + tr("Yours: {2}; Current: {0}; <font style=\"font-size:x-small\">"
                + "(latest untested: {1} &#150; not recommended)</font>",
                curVersion, latest, myVersion)
                + "</div>";
            } catch(Exception e) {
              // e.printStackTrace();
            }

            return "";
        }
    }

    /**
     * Initializes getting the MOTD as well as enabling the FileDrop Listener.
     * Displays a message while the MOTD is downloading.
     */
    public GettingStarted() {
        super(new BorderLayout());
        final LinkGeneral lg = new LinkGeneral(
            "<html>" +
            styles +
            "<h1>" +
            "JOSM - " +
            tr("Java OpenStreetMap Editor") +
            "</h1><h2 align=\"center\">" +
            tr("Downloading \"Message of the day\"") +
            "</h2>");
        JScrollPane scroller = new JScrollPane(lg);
        scroller.setViewportBorder(new EmptyBorder(10,100,10,100));
        add(scroller, BorderLayout.CENTER);

        // Asynchronously get MOTD to speed-up JOSM startup
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (content.length() == 0 && Main.pref.getBoolean("help.displaymotd", true))
                    content = new assignContent().updateIfRequiredString();

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                       lg.setText(content);
                       //lg.moveCaretPosition(0);
                    }
                });
            }
        }, "MOTD-Loader");
        t.setDaemon(true);
        t.start();

        new FileDrop(scroller);
    }
}
