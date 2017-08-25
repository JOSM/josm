// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.bugreport.DebugTextDisplay;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;
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

            // then open a browser to display the pasted text.
            String openBrowserError = OpenBrowser.displayUrl(getJOSMTicketURL() + "?pdata_stored=" + debugTextPasteId);
            if (openBrowserError != null) {
                Logging.warn(openBrowserError);
                failed(openBrowserError);
            }
        } catch (BugReportSenderException e) {
            Logging.warn(e);
            failed(e.getMessage());
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
            String postQuery = "pdata=" + URLEncoder.encode(pdata, "UTF-8");
            HttpClient client = HttpClient.create(new URL(getJOSMTicketURL()), "POST")
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setRequestBody(postQuery.getBytes(StandardCharsets.UTF_8));

            Response connection = client.connect();

            if (connection.getResponseCode() >= 500) {
                throw new BugReportSenderException("Internal server error.");
            }

            try (InputStream in = connection.getContent()) {
                return retrieveDebugToken(Utils.parseSafeDOM(in));
            }
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException t) {
            throw new BugReportSenderException(t);
        }
    }

    private static String getJOSMTicketURL() {
        return Main.getJOSMWebsite() + "/josmticket";
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

    private void failed(String string) {
        errorMessage = string;
        SwingUtilities.invokeLater(() -> {
            JPanel errorPanel = new JPanel(new GridBagLayout());
            errorPanel.add(new JMultilineLabel(
                    tr("Opening the bug report failed. Please report manually using this website:")),
                    GBC.eol().fill(GridBagConstraints.HORIZONTAL));
            errorPanel.add(new UrlLabel(Main.getJOSMWebsite() + "/newticket", 2), GBC.eop().insets(8, 0, 0, 0));
            errorPanel.add(new DebugTextDisplay(statusText));

            JOptionPane.showMessageDialog(Main.parent, errorPanel, tr("You have encountered a bug in JOSM"),
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Returns the error message that could have occured during bug sending.
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
}
