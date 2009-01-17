// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.BorderLayout;
import java.awt.Component;

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

    private void assignContent() {
        if (content.length() > 0 && Main.pref.getBoolean("help.displaymotd", true)) return;

        String baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
        WikiReader wr = new WikiReader(baseurl);
        String motdcontent = "";
        try {
            motdcontent = wr.read(baseurl + "/wiki/MessageOfTheDay?format=txt");
        } catch (IOException ioe) {
            motdcontent = "<html><body>\n<h1>" +
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

        ArrayList<Object> links = new ArrayList<Object>();
        String linksList="";
        while (matcher.find()) {
            // Discards all but the selected locale and non-localized links
            if(!(matcher.group(3)+":").equals(languageCode) && !matcher.group(3).equals(""))
                continue;

            links.add(new String[] {matcher.group(1), matcher.group(2), matcher.group(3)});
            linksList += matcher.group(1)+matcher.group(2)+matcher.group(3)+": ";
        }

        for(int i=0; i < links.size(); i++) {
            String[] obj = (String[])links.get(i);
            int targetVersion = Integer.parseInt(obj[1]);
            String condition = obj[0];
            Boolean isLocalized = !obj[2].equals("");

            // Prefer localized over non-localized links, if they're otherwise the same
            if(!isLocalized && linksList.indexOf(condition+obj[1]+languageCode+" ") >= 0)
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

            Boolean isHelp = targetVersion == 1;

            String urlStart = baseurl + "/wiki/";
            String urlEnd = "MessageOfTheDay" + condition + targetVersion + (isHelp ? "" : "?format=txt");

            try {
                // If we hit a non-localized link here, we already know there's no translated version available
                String message = isLocalized ? wr.read(urlStart + languageCode + urlEnd) : "";
                // Look for non-localized version
                if (message.equals(""))
                    message = wr.read(urlStart + urlEnd);

                if (!message.equals(""))
                    if(isHelp)
                        content += message;
                    else
                        content += "<ul><li>"+ message.substring(8)+"</li></ul>";
            } catch (IOException ioe) {
                try {
                    if(isHelp)
                        content += wr.read(urlStart + urlEnd);
                    else
                        content += "<ul><li>"+wr.read(urlStart + urlEnd).substring(8)+"</li></ul>";
                } catch (IOException ioe2) {
                }
            }
        }

        content = "<html>\n"+
            "<style type=\"text/css\">\n"+
            "body { font-family: sans-serif; font-weight: bold; }\n"+
            "h1 {text-align: center;}\n"+
            "</style>\n"+
            "<h1>JOSM - " + tr("Java OpenStreetMap Editor") + "</h1>\n"+
            content+"\n"+
            "</html>";
    }

    public GettingStarted() {
        super(new BorderLayout());
        assignContent();

        // panel.add(GBC.glue(0,1), GBC.eol());
        //panel.setMinimumSize(new Dimension(400, 600));
        Component linkGeneral = new LinkGeneral(content);
        JScrollPane scroller = new JScrollPane(linkGeneral);
        scroller.setViewportBorder(new EmptyBorder(10,100,10,100));
        add(scroller, BorderLayout.CENTER);

        new FileDrop(linkGeneral);
    }
}
