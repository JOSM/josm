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
        this(Main.pref.get("help.baseurl", Main.getJOSMWebsite()));
    }

    /**
     * Returns the base URL of wiki.
     * @return the base URL of wiki
     * @since 7434
     */
    public final String getBaseUrlWiki() {
        return baseurl + "/wiki/";
    }

    /**
     * Read the page specified by the url and return the content.
     *
     * If the url is within the baseurl path, parse it as an trac wikipage and replace relative paths etc..
     * @param url the URL to read
     * @return The page as string
     *
     * @throws IOException Throws, if the page could not be loaded.
     */
    public String read(String url) throws IOException {
        URL u = new URL(url);
        try (BufferedReader in = Utils.openURLReader(u)) {
            boolean txt = url.endsWith("?format=txt");
            if (url.startsWith(getBaseUrlWiki()) && !txt)
                return readFromTrac(in, u);
            return readNormal(in, !txt);
        }
    }

    /**
     * Reads the localized version of the given wiki page.
     * @param text The page title, without locale prefix
     * @return the localized version of the given wiki page
     * @throws IOException if any I/O error occurs
     */
    public String readLang(String text) throws IOException {
        String languageCode;
        String res = "";

        languageCode = LanguageInfo.getWikiLanguagePrefix(LocaleType.DEFAULTNOTENGLISH);
        if(languageCode != null) {
            res = readLang(new URL(getBaseUrlWiki() + languageCode + text));
        }

        if(res.isEmpty()) {
            languageCode = LanguageInfo.getWikiLanguagePrefix(LocaleType.BASELANGUAGE);
            if(languageCode != null) {
                res = readLang(new URL(getBaseUrlWiki() + languageCode + text));
            }
        }

        if(res.isEmpty()) {
            languageCode = LanguageInfo.getWikiLanguagePrefix(LocaleType.ENGLISH);
            if(languageCode != null) {
                res = readLang(new URL(getBaseUrlWiki() + languageCode + text));
            }
        }

        if(res.isEmpty()) {
            throw new IOException(text + " does not exist");
        } else {
            return res;
        }
    }

    private String readLang(URL url) throws IOException {
        try (BufferedReader in = Utils.openURLReader(url)) {
            return readFromTrac(in, url);
        } catch (IOException e) {
            Main.addNetworkError(url, Utils.getRootCause(e));
            throw e;
        }
    }

    private String readNormal(BufferedReader in, boolean html) throws IOException {
        StringBuilder b = new StringBuilder();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (!line.contains("[[TranslatedPages]]")) {
                b.append(line.replaceAll(" />", ">")).append('\n');
            }
        }
        return html ? "<html>" + b + "</html>" : b.toString();
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
