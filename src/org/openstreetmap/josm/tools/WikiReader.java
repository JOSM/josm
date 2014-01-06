// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.LanguageInfo.LocaleType;

/**
 * Read a trac-wiki page.
 *
 * @author imi
 */
public class WikiReader {

    private final String baseurl;

    /**
     * Constructs a new {@code WikiReader} for the given base URL.
     * @param baseurl The wiki base URL
     */
    public WikiReader(String baseurl) {
        this.baseurl = baseurl;
    }

    /**
     * Constructs a new {@code WikiReader}.
     */
    public WikiReader() {
        this.baseurl = Main.pref.get("help.baseurl", Main.JOSM_WEBSITE);
    }

    /**
     * Read the page specified by the url and return the content.
     *
     * If the url is within the baseurl path, parse it as an trac wikipage and replace relative
     * pathes etc..
     *
     * @throws IOException Throws, if the page could not be loaded.
     */
    public String read(String url) throws IOException {
        URL u = new URL(url);
        BufferedReader in = Utils.openURLReader(u);
        try {
            if (url.startsWith(baseurl) && !url.endsWith("?format=txt"))
                return readFromTrac(in, u);
            return readNormal(in);
        } finally {
            Utils.close(in);
        }
    }

    public String readLang(String text) throws IOException {
        String languageCode;
        String res = "";

        languageCode = LanguageInfo.getWikiLanguagePrefix(LocaleType.DEFAULTNOTENGLISH);
        if(languageCode != null) {
            res = readLang(new URL(baseurl + "/wiki/" + languageCode + text));
        }

        if(res.isEmpty()) {
            languageCode = LanguageInfo.getWikiLanguagePrefix(LocaleType.BASELANGUAGE);
            if(languageCode != null) {
                res = readLang(new URL(baseurl + "/wiki/" + languageCode + text));
            }
        }

        if(res.isEmpty()) {
            languageCode = LanguageInfo.getWikiLanguagePrefix(LocaleType.ENGLISH);
            if(languageCode != null) {
                res = readLang(new URL(baseurl + "/wiki/" + languageCode + text));
            }
        }

        if(res.isEmpty()) {
            throw new IOException(text + " does not exist");
        } else {
            return res;
        }
    }

    private String readLang(URL url) throws IOException {
        BufferedReader in;
        try {
            in = Utils.openURLReader(url);
        } catch (IOException e) {
            Main.addNetworkError(url, Utils.getRootCause(e));
            throw e;
        }
        try {
            return readFromTrac(in, url);
        } finally {
            Utils.close(in);
        }
    }

    private String readNormal(BufferedReader in) throws IOException {
        StringBuilder b = new StringBuilder();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (!line.contains("[[TranslatedPages]]")) {
                b.append(line.replaceAll(" />", ">")).append("\n");
            }
        }
        return "<html>" + b + "</html>";
    }

    protected String readFromTrac(BufferedReader in, URL url) throws IOException {
        boolean inside = false;
        boolean transl = false;
        boolean skip = false;
        String b = "";
        String full = "";
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            full += line;
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
                skip = true;
            }
            if (inside && !transl && !skip) {
                // add a border="0" attribute to images, otherwise the internal help browser
                // will render a thick  border around images inside an <a> element
                b += line.replaceAll("<img ", "<img border=\"0\" ")
                         .replaceAll("<span class=\"icon\">.</span>", "")
                         .replaceAll("href=\"/", "href=\"" + baseurl + "/")
                         .replaceAll(" />", ">")
                         + "\n";
            } else if (transl && line.contains("</div>")) {
                transl = false;
            }
            if (line.contains("</div>")) {
                skip = false;
            }
        }
        if (b.indexOf("      Describe ") >= 0
        || b.indexOf(" does not exist. You can create it here.</p>") >= 0)
            return "";
        if(b.isEmpty())
            b = full;
        return "<html><base href=\""+url.toExternalForm() +"\"> " + b + "</html>";
    }
}
