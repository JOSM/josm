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
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WikiReader;
import org.openstreetmap.josm.actions.AboutAction;

public class GettingStarted extends JPanel {

    static private String content = "";
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

    public class readMOTD implements Callable<String> {
        private boolean isLocalized;
        private boolean isHelp;
        private String urlLoc;
        private String urlIntl;
        private String urlBase;

        readMOTD(boolean isLocalized, String urlBase, String urlLoc, String urlIntl, boolean isHelp) {
          this.isLocalized = isLocalized;
          this.urlBase = urlBase;
          this.urlLoc = urlLoc;
          this.urlIntl = urlIntl;
          this.isHelp = isHelp;
        }

        public String call() {
            WikiReader wr = new WikiReader(urlBase);
            String content = "";
            try {
                // If we hit a non-localized link here, we already know there's no translated version available
                String message = isLocalized ? wr.read(urlLoc) : "";
                // Look for non-localized version
                if (message.equals(""))
                    message = wr.read(urlIntl);

                if (!message.equals(""))
                    if(isHelp)
                        content += message;
                    else
                        content += "<ul><li>"+ message.substring(8).replaceAll("\n *\\* +","</li><li>")+"</li></ul>";
            } catch (IOException ioe) {
                try {
                    if(isHelp)
                        content += wr.read(urlIntl);
                    else
                        content += "<ul><li>"+wr.read(urlIntl).substring(8).replaceAll("\n *\\* +","</li><li>")+"</li></ul>";
                } catch (IOException ioe2) {
                }
            }

            return content;
        }
    }

    private void assignContent() {
        if (content.length() > 0 && Main.pref.getBoolean("help.displaymotd", true)) return;

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

        int myVersion = AboutAction.getVersionNumber();
        String languageCode = Main.getLanguageCodeU();

        // Finds wiki links like (underscores inserted for readability): [wiki:LANGCODE:messageoftheday_CONDITON_REVISION LANGCODE]
        // Langcode usually consists of two letters describing the language and may be omitted
        // Condition may be one of the following: >  <  <=  =>
        // Revision is the JOSM version
        Pattern versionPattern = Pattern.compile("\\[wiki:(?:[A-Z]+:)?MessageOfTheDay(\\>\\=|\\<\\=|\\<|\\>)([0-9]+)\\s*([A-Z]*)\\]", Pattern.CASE_INSENSITIVE);
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

        ArrayList<Future<String>> linkContent = new ArrayList<Future<String>>();
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
            String urlEnd = "MessageOfTheDay" + condition + targetVersion + (isHelp ? "" : "?format=txt");
            String urlLoc = urlStart + languageCode + urlEnd;
            String urlIntl = urlStart + urlEnd;

            // This adds all links to the worker which will download them concurrently
            linkContent.add(slave.submit(new readMOTD(isLocalized, baseurl, urlLoc, urlIntl, isHelp)));
        }

        for(int i=0; i < linkContent.size(); i++) {
            try {
                content += linkContent.get(i).get();
            } catch (Exception e) {}
        }

        content = "<html>\n"+
            styles +
            "<h1>JOSM - " + tr("Java OpenStreetMap Editor") + "</h1>\n"+
            content+"\n"+
            "</html>";
    }

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
                assignContent();
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
