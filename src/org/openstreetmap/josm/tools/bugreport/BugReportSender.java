// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class handles sending the bug report to JOSM website.
 * <p>
 * Currently, we try to open a browser window for the user that displays the bug report.
 *
 * @author Michael Zangl
 * @since 10055
 */
public class BugReportSender extends Thread {

    /**
     * Called during bug submission to JOSM bugtracker. Completes the bug report submission and handles errors.
     * @since 12790
     */
    public interface BugReportSendingHandler {
        /**
         * Called when a bug is sent to JOSM bugtracker.
         * @param bugUrl URL to visit to effectively submit the bug report to JOSM website
         * @param statusText the status text being sent
         * @return <code>null</code> for success or a string in case of an error
         */
        String sendingBugReport(String bugUrl, String statusText);

        /**
         * Called when a bug failed to be sent to JOSM bugtracker.
         * @param errorMessage the error message
         * @param statusText the status text being sent
         */
        void failed(String errorMessage, String statusText);
    }

    /**
     * The fallback bug report sending handler if none is set.
     * @since 12790
     */
    public static final BugReportSendingHandler FALLBACK_BUGREPORT_SENDING_HANDLER = new BugReportSendingHandler() {
        @Override
        public String sendingBugReport(String bugUrl, String statusText) {
            return OpenBrowser.displayUrl(bugUrl);
        }

        @Override
        public void failed(String errorMessage, String statusText) {
            Logging.error("Unable to send bug report: {0}\n{1}", errorMessage, statusText);
        }
    };

    private static volatile BugReportSendingHandler handler = FALLBACK_BUGREPORT_SENDING_HANDLER;

    private final String statusText;
    private String errorMessage;

    /**
     * Creates a new sender.
     * @param statusText The status text to send.
     */
    protected BugReportSender(String statusText) {
        super("Bug report sender");
        this.statusText = statusText;
    }

    @Override
    public void run() {
        try {
            // first, send the debug text using post.
            String debugTextPasteId = pasteDebugText();
            String bugUrl = getJOSMTicketURL() + "?pdata_stored=" + debugTextPasteId;

            // then notify handler
            errorMessage = handler.sendingBugReport(bugUrl, statusText);
            if (errorMessage != null) {
                Logging.warn(errorMessage);
                handler.failed(errorMessage, statusText);
            }
        } catch (BugReportSenderException e) {
            Logging.warn(e);
            errorMessage = e.getMessage();
            handler.failed(errorMessage, statusText);
        }
    }

    /**
     * Sends the debug text to the server.
     * @return The token which was returned by the server. We need to pass this on to the ticket system.
     * @throws BugReportSenderException if sending the report failed.
     */
    private String pasteDebugText() throws BugReportSenderException {
        try {
            String text = Utils.strip(statusText);
            String pdata = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            String postQuery = "pdata=" + Utils.encodeUrl(pdata);
            HttpClient client = HttpClient.create(new URL(getJOSMTicketURL()), "POST")
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setRequestBody(postQuery.getBytes(StandardCharsets.UTF_8));

            Response connection = client.connect();

            if (connection.getResponseCode() >= 500) {
                throw new BugReportSenderException("Internal server error.");
            }

            try (InputStream in = connection.getContent()) {
                return retrieveDebugToken(XmlUtils.parseSafeDOM(in));
            }
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException t) {
            throw new BugReportSenderException(t);
        }
    }

    private static String getJOSMTicketURL() {
        return Config.getUrls().getJOSMWebsite() + "/josmticket";
    }

    private static String retrieveDebugToken(Document document) throws XPathExpressionException, BugReportSenderException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        String status = (String) xpath.compile("/josmticket/@status").evaluate(document, XPathConstants.STRING);
        if (!"ok".equals(status)) {
            String message = (String) xpath.compile("/josmticket/error/text()").evaluate(document,
                    XPathConstants.STRING);
            if (message.isEmpty()) {
                message = "Error in server response but server did not tell us what happened.";
            }
            throw new BugReportSenderException(message);
        }

        String token = (String) xpath.compile("/josmticket/preparedid/text()")
                .evaluate(document, XPathConstants.STRING);
        if (token.isEmpty()) {
            throw new BugReportSenderException("Server did not respond with a prepared id.");
        }
        return token;
    }

    /**
     * Returns the error message that could have occurred during bug sending.
     * @return the error message, or {@code null} if successful
     */
    public final String getErrorMessage() {
        return errorMessage;
    }

    private static class BugReportSenderException extends Exception {
        BugReportSenderException(String message) {
            super(message);
        }

        BugReportSenderException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Opens the bug report window on the JOSM server.
     * @param statusText The status text to send along to the server.
     * @return bug report sender started thread
     */
    public static BugReportSender reportBug(String statusText) {
        BugReportSender sender = new BugReportSender(statusText);
        sender.start();
        return sender;
    }

    /**
     * Sets the {@link BugReportSendingHandler} for bug report sender.
     * @param bugReportSendingHandler the handler in charge of completing the bug report submission and handle errors. Must not be null
     * @since 12790
     */
    public static void setBugReportSendingHandler(BugReportSendingHandler bugReportSendingHandler) {
        handler = Objects.requireNonNull(bugReportSendingHandler, "bugReportSendingHandler");
    }
}
