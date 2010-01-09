// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.SAXException;

/**
 * This is an asynchronous task for testing whether an URL points to an OSM API server.
 * It tries to retrieve a list of changesets from the given URL. If it succeeds, the method
 * {@see #isSuccess()} replies true, otherwise false.
 * 
 * Note: it fetches a list of changesets instead of the much smaller capabilities because - strangely enough -
 * an OSM server "http://x.y.y/api/0.6" not only responds to  "http://x.y.y/api/0.6/capabilities" but also
 * to "http://x.y.y/api/0/capabilities" or "http://x.y.y/a/capabilities" with valid capabilities. If we get
 * valid capabilities with an URL we therefore can't be sure that the base URL is valid API URL.
 * 
 */
public class ApiUrlTestTask extends PleaseWaitRunnable{

    private String url;
    private boolean canceled;
    private boolean success;
    private Component parent;
    private HttpURLConnection connection;

    /**
     * Creates the task
     * 
     * @param parent the parent component relative to which the {@see PleaseWaitRunnable}-Dialog is displayed
     * @param url the url. Must not be null.
     * @throws IllegalArgumentException thrown if url is null.
     */
    public ApiUrlTestTask(Component parent, String url) throws IllegalArgumentException {
        super(parent, tr("Testing OSM API URL ''{0}''", url), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(url,"url");
        this.parent = parent;
        this.url = url;
    }

    protected void alertInvalidUrl(String url) {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>"
                        + "''{0}'' isn't a valid OSM API URL.<br>"
                        + "Please check the spelling and validate again."
                        +"</html>",
                        url
                ),
                tr("Invalid API URL"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Preferences/Connection#InvalidAPIUrl")
        );
    }

    protected void alertInvalidChangesetUrl(String url) {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>"
                        + "Failed to build URL ''{0}'' for validating the OSM API server.<br>"
                        + "Please check the spelling of ''{1}'' and validate again."
                        +"</html>",
                        url,
                        getNormalizedApiUrl()
                ),
                tr("Invalid API URL"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Preferences/Connection#InvalidAPIGetChangesetsUrl")
        );
    }

    protected void alertConnectionFailed() {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>"
                        + "Failed to connect to the URL ''{0}''.<br>"
                        + "Please check the spelling of ''{1}'' and your Internet connection and validate again."
                        +"</html>",
                        url,
                        getNormalizedApiUrl()
                ),
                tr("Connection to API failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Preferences/Connection#ConnectionToAPIFailed")
        );
    }

    protected void alertInvalidServerResult(int retCode) {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>"
                        + "Failed to retrieve a list of changesets from the OSM API server at<br>"
                        + "''{1}''. The server responded with the return code {0} instead of 200.<br>"
                        + "Please check the spelling of ''{1}'' and validate again."
                        + "</html>",
                        retCode,
                        getNormalizedApiUrl()
                ),
                tr("Connection to API failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Preferences/Connection#InvalidServerResult")
        );
    }

    protected void alertInvalidChangesetList() {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr("<html>"
                        + "The OSM API server at ''{0}'' didn''t return a valid response.<br>"
                        + "It is likely that ''{0}'' isn''t an OSM API server.<br>"
                        + "Please check the spelling of ''{0}'' and validate again."
                        + "</html>",
                        getNormalizedApiUrl()
                ),
                tr("Connection to API failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Preferences/Connection#InvalidSettings")
        );
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized(this) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void finish() {}

    /**
     * Removes leading and trailing whitespace from the API URL and removes trailing
     * '/'.
     * 
     * @return the normalized API URL
     */
    protected String getNormalizedApiUrl() {
        String apiUrl = url.trim();
        while(apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.lastIndexOf("/"));
        }
        return apiUrl;
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        BufferedReader bin = null;
        try {
            try {
                new URL(getNormalizedApiUrl());
            } catch(MalformedURLException e) {
                alertInvalidUrl(getNormalizedApiUrl());
                return;
            }
            URL capabilitiesUrl;
            String getChangesetsUrl = getNormalizedApiUrl() + "/0.6/changesets";
            try {
                capabilitiesUrl = new URL(getChangesetsUrl);
            } catch(MalformedURLException e) {
                alertInvalidChangesetUrl(getChangesetsUrl);
                return;
            }

            synchronized(this) {
                connection = (HttpURLConnection)capabilitiesUrl.openConnection();
            }
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", Version.getInstance().getAgentString());
            connection.setRequestProperty("Host", connection.getURL().getHost());
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                alertInvalidServerResult(connection.getResponseCode());
                return;
            }
            StringBuilder changesets = new StringBuilder();
            bin = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = bin.readLine()) != null) {
                changesets.append(line).append("\n");
            }
            if (! (changesets.toString().contains("<osm") && changesets.toString().contains("</osm>"))) {
                // heuristic: if there isn't an opening and closing "<osm>" tag in the returned content,
                // then we didn't get a list of changesets in return. Could be replaced by explicitly parsing
                // the result but currently not worth the effort.
                alertInvalidChangesetList();
                return;
            }
            success = true;
        } catch(IOException e) {
            if (canceled)
                // ignore exceptions
                return;
            e.printStackTrace();
            alertConnectionFailed();
            return;
        } finally {
            if (bin != null) {
                try {
                    bin.close();
                } catch(IOException e){/* ignore */}
            }
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    public boolean isSuccess() {
        return success;
    }
}
