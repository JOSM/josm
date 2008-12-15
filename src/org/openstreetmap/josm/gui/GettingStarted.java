// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.BorderLayout;

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
        if (content.length() == 0) {
            String baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
            WikiReader wr = new WikiReader(baseurl);
            String motdcontent = "";
            try {
                motdcontent = wr.read(baseurl + "/wiki/MessageOfTheDay");
            } catch (IOException ioe) {
                motdcontent = "<html><body>\n<h1>" +
                    tr("JOSM, the Java OpenStreetMap editor") +
                    "</h1>\n<h2 align=\"center\">(" +
                    tr ("Message of the day not available") +
                    ")</h2>";
            }

            int myVersion = AboutAction.getVersionNumber();

            Pattern commentPattern = Pattern.compile("\\<p\\>\\s*\\/\\*[^\\*]*\\*\\/\\s*\\<\\/p\\>", Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
            Matcher matcherComment = commentPattern.matcher(motdcontent);
            motdcontent = matcherComment.replaceAll("");

            /* look for hrefs of the form wiki/MessageOfTheDay>123 where > can also be <,<=,>= and the number is the revision number */
            int start = 0;
            Pattern versionPattern = Pattern.compile("(?:\\<p\\>\\s*)?\\<a[^\\>]*href\\=\\\"([^\\\"]*\\/wiki\\/)(MessageOfTheDay(\\%3E%3D|%3C%3D|\\%3E|\\%3C)([0-9]+))\\\"[^\\>]*\\>[^\\<]*\\<\\/a\\>(?:\\s*\\</p\\>)?", Pattern.CASE_INSENSITIVE|Pattern.DOTALL|Pattern.MULTILINE);
            Matcher matcher = versionPattern.matcher(motdcontent);
            matcher.reset();
            while (matcher.find()) {
                int targetVersion = Integer.parseInt(matcher.group(4));
                String condition = matcher.group(3);
                boolean included = false;
                if (condition.equals("%3E")) {
                    if ((myVersion == 0 || myVersion > targetVersion) 
                        /* && ! Main.pref.getBoolean("motd.gt."+targetVersion) */) {
                        /* Main.pref.put("motd.gt."+targetVersion, true); */
                        included = true;
                    }
                } else if (condition.equals("%3E%3D")) {
                    if ((myVersion == 0 || myVersion >= targetVersion) 
                        /* && ! Main.pref.getBoolean("motd.ge."+targetVersion) */) {
                        /* Main.pref.put("motd.ge."+targetVersion, true); */
                        included = true;
                    }
                } else if (condition.equals("%3C")) {
                    included = myVersion < targetVersion;
                } else {
                     included = myVersion <= targetVersion;
                }
                if (matcher.start() > start) {
                    content += motdcontent.substring(start, matcher.start() - 1);
                }
                start = matcher.end();
                if (included) {
                    // translators: set this to a suitable language code to
                    // be able to provide translations in the Wiki.
                    String languageCode = tr("En:");
                    String url = matcher.group(1) + languageCode + matcher.group(2);
                    try {
                        String message = wr.read(url);
                        // a return containing the article name indicates that the page didn't
                        // exist in the Wiki.
                        String emptyIndicator = "Describe \"" + languageCode + "MessageOfTheDay";
                        if (message.indexOf(emptyIndicator) >= 0) {
                            url = matcher.group(1) + matcher.group(2);
                            message = wr.read(url);
                            emptyIndicator = "Describe \"MessageOfTheDay";
                        }
                        if (message.indexOf(emptyIndicator) == -1) {
                            content += message.replace("<html>", "").replace("</html>", "").replace("<div id=\"searchable\">", "").replace("</div>", "");
                        }
                    } catch (IOException ioe) {
                        url = matcher.group(1) + matcher.group(2);
                        try {
                            content += wr.read(url).replace("<html>", "").replace("</html>", "").replace("<div id=\"searchable\">", "").replace("</div>", "");
                        } catch (IOException ioe2) {
                        }            
                    }            
                }
            }
            content += motdcontent.substring(start);
            content = content.replace("<html>", "<html><style type=\"text/css\">\nbody { font-family: sans-serif; font-weight: bold; }\nh1 {text-align: center;}</style>");
            /* replace the wiki title */ 
            content = content.replace("JOSM, the Java OpenStreetMap editor", tr("JOSM, the Java OpenStreetMap editor")); 
        }

    }
    
    public GettingStarted() {
        super(new BorderLayout());
        assignContent();
                                
        // panel.add(GBC.glue(0,1), GBC.eol());
        //panel.setMinimumSize(new Dimension(400, 600));
        JScrollPane scroller = new JScrollPane(new LinkGeneral(content));
        scroller.setViewportBorder(new EmptyBorder(10,100,10,100));
        add(scroller, BorderLayout.CENTER);
    }
}
