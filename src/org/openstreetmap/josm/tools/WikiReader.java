// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.openstreetmap.josm.Main;

/**
 * Read a trac-wiki page.
 *
 * @author imi
 */
public class WikiReader {

    private final String baseurl;

    public WikiReader(String baseurl) {
        this.baseurl = baseurl;
    }

    public WikiReader() {
        this.baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
    }

    /**
     * Read the page specified by the url and return the content.
     *
     * If the url is within the baseurl path, parse it as an trac wikipage and replace relative
     * pathes etc..
     *
     * @return
     * @throws IOException Throws, if the page could not be loaded.
     */
    public String read(String url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream(), "utf-8"));
        if (url.startsWith(baseurl) && !url.endsWith("?format=txt"))
            return readFromTrac(in);
        return readNormal(in);
    }

    public String readLang(String text) {
        String languageCode = LanguageInfo.getWikiLanguagePrefix();
        String url = baseurl + "/wiki/" + languageCode + text;
        String res = "";
        InputStream in = null;
        try {
            in = new URL(url).openStream();
            res = readFromTrac(new BufferedReader(new InputStreamReader(in, "utf-8")));
        } catch (IOException ioe) {
            System.out.println(tr("Warning: failed to read MOTD from ''{0}''. Exception was: {1}", url, ioe
                    .toString()));
        } catch(SecurityException e) {
            System.out.println(tr(
                    "Warning: failed to read MOTD from ''{0}'' for security reasons. Exception was: {1}", url, e
                    .toString()));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        if (res.length() == 0 && languageCode.length() != 0) {
            url = baseurl + "/wiki/" + text;
            try {
                in = new URL(url).openStream();
            } catch (IOException e) {
                System.out.println(tr("Warning: failed to read MOTD from ''{0}''. Exception was: {1}", url, e
                        .toString()));
                return res;
            } catch (SecurityException e) {
                System.out.println(tr(
                        "Warning: failed to read MOTD from ''{0}'' for security reasons. Exception was: {1}", url, e
                        .toString()));
                return res;
            }
            try {
                res = readFromTrac(new BufferedReader(new InputStreamReader(in, "utf-8")));
            } catch (IOException ioe) {
                System.out.println(tr("Warning: failed to read MOTD from ''{0}''. Exception was: {1}", url, ioe
                        .toString()));
                return res;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return res;
    }

    private String readNormal(BufferedReader in) throws IOException {
        String b = "";
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (!line.contains("[[TranslatedPages]]")) {
                b += line.replaceAll(" />", ">") + "\n";
            }
        }
        return "<html>" + b + "</html>";
    }

    private String readFromTrac(BufferedReader in) throws IOException {
        boolean inside = false;
        boolean transl = false;
        String b = "";
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (line.contains("<div id=\"searchable\">")) {
                inside = true;
            } else if (line.contains("<div class=\"wiki-toc trac-nav\"")) {
                transl = true;
            } else if (line.contains("<div class=\"wikipage searchable\">")) {
                inside = true;
            } else if (line.contains("<div class=\"buttons\">")) {
                inside = false;
            } else if (line.contains("<h3>Attachments</h3>")) {
                inside = false;
            } else if (line.contains("<div id=\"attachments\">")) {
                inside = false;
            } else if (line.contains("<div class=\"trac-modifiedby\">")) {
                continue;
            }
            if (inside && !transl) {
                // add a border="0" attribute to images, otherwise the internal help browser
                // will render a thick  border around images inside an <a> element
                //
                b += line.replaceAll("<img src=\"/", "<img border=\"0\" src=\"" + baseurl + "/").replaceAll("href=\"/",
                        "href=\"" + baseurl + "/").replaceAll(" />", ">")
                        + "\n";
            } else if (transl && line.contains("</div>")) {
                transl = false;
            }
        }
        if (b.indexOf("      Describe ") >= 0)
            return "";
        return "<html>" + b + "</html>";
    }
}
