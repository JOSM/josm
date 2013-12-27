// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.openstreetmap.josm.tools.Utils;
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
 */
public class HelpContentReader extends WikiReader {

    /**
     * constructor
     *
     * @param baseUrl the base url of the JOSM help wiki, i.e. http://josm.openstreetmap.org
     */
    public HelpContentReader(String baseUrl) {
        super(baseUrl);
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
        if(helpTopicUrl == null)
            throw new MissingHelpContentException();
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            URL u = new URL(helpTopicUrl);
            con = Utils.openHttpConnection(u);
            con.connect();
            in = new BufferedReader(new InputStreamReader(con.getInputStream(), Utils.UTF_8));
            return prepareHelpContent(in, dotest, u);
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
            Utils.close(in);
        }
    }

    /**
     * Reads help content from the input stream and prepares it to be rendered later
     * in the internal help browser.
     *
     * Throws a {@link MissingHelpContentException} if the content read from the stream
     * most likely represents a stub help page.
     *
     * @param in the input stream
     * @return the content
     * @throws HelpContentReaderException thrown if an exception occurs
     * @throws MissingHelpContentException thrown, if the content read isn't a help page
     * @since 5936
     */
    protected String prepareHelpContent(BufferedReader in, boolean dotest, URL url) throws HelpContentReaderException {
        String s = "";
        try {
            s = readFromTrac(in, url);
        } catch(IOException e) {
            throw new HelpContentReaderException(e);
        }
        if(dotest && s.isEmpty())
            throw new MissingHelpContentException();
        return s;
    }
}
