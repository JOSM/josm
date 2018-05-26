// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This is an asynchronous task for testing whether an URL points to an OSM API server.
 * It tries to retrieve capabilities from the given URL. If it succeeds, the method
 * {@link #isSuccess()} replies true, otherwise false.
 * @since 2745
 */
public class ApiUrlTestTask extends PleaseWaitRunnable {

    private final String url;
    private boolean canceled;
    private boolean success;
    private final Component parent;
    private HttpClient connection;

    /**
     * Constructs a new {@code ApiUrlTestTask}.
     *
     * @param parent the parent component relative to which the {@link PleaseWaitRunnable}-Dialog is displayed
     * @param url the url. Must not be null.
     * @throws IllegalArgumentException if url is null.
     */
    public ApiUrlTestTask(Component parent, String url) {
        super(parent, tr("Testing OSM API URL ''{0}''", url), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        this.parent = parent;
        this.url = url;
    }

    protected void alertInvalidUrl(String url) {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "''{0}'' is not a valid OSM API URL.<br>"
                        + "Please check the spelling and validate again."
                        + "</html>",
                        url
                ),
                tr("Invalid API URL"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Preferences/Connection#InvalidAPIUrl")
        );
    }

    protected void alertInvalidCapabilitiesUrl(String url) {
        HelpAwareOptionPane.showMessageDialogInEDT(
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
        HelpAwareOptionPane.showMessageDialogInEDT(
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
        HelpAwareOptionPane.showMessageDialogInEDT(
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

    protected void alertInvalidCapabilities() {
        HelpAwareOptionPane.showMessageDialogInEDT(
                parent,
                tr("<html>"
                        + "The OSM API server at ''{0}'' did not return a valid response.<br>"
                        + "It is likely that ''{0}'' is not an OSM API server.<br>"
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
        synchronized (this) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void finish() {
        // Do nothing
    }

    /**
     * Removes leading and trailing whitespace from the API URL and removes trailing '/'.
     *
     * @return the normalized API URL
     */
    protected String getNormalizedApiUrl() {
        String apiUrl = url.trim();
        while (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.lastIndexOf('/'));
        }
        return apiUrl;
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            try {
                new URL(getNormalizedApiUrl());
            } catch (MalformedURLException e) {
                alertInvalidUrl(getNormalizedApiUrl());
                return;
            }
            URL capabilitiesUrl;
            String getCapabilitiesUrl = getNormalizedApiUrl() + "/0.6/capabilities";
            try {
                capabilitiesUrl = new URL(getCapabilitiesUrl);
            } catch (MalformedURLException e) {
                alertInvalidCapabilitiesUrl(getCapabilitiesUrl);
                return;
            }

            synchronized (this) {
                connection = HttpClient.create(capabilitiesUrl);
                connection.connect();
            }

            if (connection.getResponse().getResponseCode() != HttpURLConnection.HTTP_OK) {
                alertInvalidServerResult(connection.getResponse().getResponseCode());
                return;
            }

            try {
                Capabilities.CapabilitiesParser.parse(new InputSource(connection.getResponse().getContent()));
            } catch (SAXException | ParserConfigurationException e) {
                Logging.warn(e);
                alertInvalidCapabilities();
                return;
            }
            success = true;
        } catch (IOException e) {
            if (canceled)
                // ignore exceptions
                return;
            Logging.error(e);
            alertConnectionFailed();
        }
    }

    /**
     * Determines if the test has been canceled.
     * @return {@code true} if canceled, {@code false} otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Determines if the test has succeeded.
     * @return {@code true} if success, {@code false} otherwise
     */
    public boolean isSuccess() {
        return success;
    }
}
