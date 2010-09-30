// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.openstreetmap.josm.tools.WikiReader;

/**
 * Reads help content from the JOSM Wiki and prepares it for rendering in the internal
 * help browser.
 *
 * The help content has to be <strong>filtered</strong> because only the main content <tt>&lt;div&gt;</tt>
 * of a Wiki help page is displayed in the internal help browser.
 *
 * It also has to be <strong>transformed</strong> because the internal help browser required slightly
 * different HTML than what is provided by the Wiki.
 *
 * @see WikiReader
 */
public class HelpContentReader {

    /** the base url */
    private String baseUrl;

    /**
     * constructor
     *
     * @param baseUrl the base url of the JOSM help wiki, i.e. http://josm.openstreetmap.org
     */
    public HelpContentReader(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Fetches the content of a help topic from the JOSM wiki.
     *
     * @param helpTopicUrl  the absolute help topic URL
     * @return the content, filtered and transformed for being displayed in the internal help browser
     * @throws HelpContentReaderException thrown if problem occurs
     * @throws MissingHelpContentException thrown if this helpTopicUrl doesn't point to an existing Wiki help page
     */
    public String fetchHelpTopicContent(String helpTopicUrl, boolean dotest) throws HelpContentReaderException {
        URL url = null;
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            url = new URL(helpTopicUrl);
            con = (HttpURLConnection)url.openConnection();
            con.connect();
            in = new BufferedReader(new InputStreamReader(con.getInputStream(),"utf-8"));
            return prepareHelpContent(in, dotest);
        } catch(MalformedURLException e) {
            throw new HelpContentReaderException(e);
        } catch(IOException e) {
            HelpContentReaderException ex = new HelpContentReaderException(e);
            if (con != null) {
                try {
                    ex.setResponseCode(con.getResponseCode());
                } catch(IOException e1) {
                    // ignore
                }
            }
            throw ex;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch(IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Reads help content from the input stream and prepares it to be rendered later
     * in the internal help browser.
     *
     * Throws a {@see MissingHelpContentException} if the content read from the stream
     * most likely represents a stub help page.
     *
     * @param in the input stream
     * @return the content
     * @throws HelpContentReaderException thrown if an exception occurs
     * @throws MissingHelpContentException thrown, if the content read isn't a help page
     */
    protected String prepareHelpContent(BufferedReader in, boolean dotest) throws HelpContentReaderException {
        boolean isInContent = false;
        boolean isInTranslationsSideBar = false;
        boolean isExistingHelpPage = false;
        StringBuffer sball = new StringBuffer();
        StringBuffer sb = new StringBuffer();
        try {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                sball.append(line);
                sball.append("\n");
                if (line.contains("<div id=\"searchable\">")) {
                    isInContent = true;
                } else if (line.contains("<div class=\"wiki-toc trac-nav\"")) {
                    isInTranslationsSideBar = true;
                } else if (line.contains("<div class=\"wikipage searchable\">")) {
                    isInContent = true;
                } else if (line.contains("<div class=\"buttons\">")) {
                    isInContent = false;
                } else if (line.contains("<h3>Attachments</h3>")) {
                    isInContent = false;
                } else if (line.contains("<div id=\"attachments\">")) {
                    isInContent = false;
                } else if (line.contains("<div class=\"trac-modifiedby\">")) {
                    continue;
                } else if (line.contains("<input type=\"submit\" name=\"attachfilebutton\"")) {
                    // heuristic: if we find a button for uploading images we are in an
                    // existing pages. Otherwise this is probably the stub page for a not yet
                    // existing help page
                    isExistingHelpPage = true;
                }
                if (isInContent && !isInTranslationsSideBar) {
                    // add a border="0" attribute to images, otherwise the internal help browser
                    // will render a thick  border around images inside an <a> element
                    //
                    // Also make sure image URLs are absolute
                    //
                    line = line.replaceAll("<img ([^>]*)src=\"/", "<img border=\"0\" \\1src=\"" + baseUrl + "/").replaceAll("href=\"/",
                            "href=\"" + baseUrl + "/").replaceAll(" />", ">");
                    sb.append(line);
                    sb.append("\n");
                } else if (isInTranslationsSideBar && line.contains("</div>")) {
                    isInTranslationsSideBar = false;
                }
            }
        } catch(IOException e) {
            throw new HelpContentReaderException(e);
        }
        if(!dotest && sb.length() == 0)
            sb = sball;
        else if (dotest && !isExistingHelpPage)
            throw new MissingHelpContentException();
        sb.insert(0, "<html>");
        sb.append("<html>");
        return sb.toString();
    }
}
