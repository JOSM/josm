// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Asynchronous task for retrieving an Access Token.
 *
 */
public class RetrieveAccessTokenTask extends PleaseWaitRunnable {

    private boolean canceled;
    private OAuthToken accessToken;
    private final OAuthParameters parameters;
    private OsmOAuthAuthorizationClient client;
    private final OAuthToken requestToken;
    private final Component parent;

    /**
     * Creates the task
     *
     * @param parent the parent component relative to which the {@link PleaseWaitRunnable}-Dialog
     * is displayed
     * @param parameters the OAuth parameters. Must not be null.
     * @param requestToken the request token for which an Access Token is retrieved. Must not be null.
     * @throws IllegalArgumentException if parameters is null.
     * @throws IllegalArgumentException if requestToken is null.
     */
    public RetrieveAccessTokenTask(Component parent, OAuthParameters parameters, OAuthToken requestToken) {
        super(parent, tr("Retrieving OAuth Access Token..."), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        CheckParameterUtil.ensureParameterNotNull(requestToken, "requestToken");
        this.parameters = parameters;
        this.requestToken = requestToken;
        this.parent = parent;
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
            if (client != null) {
                client.cancel();
            }
        }
    }

    @Override
    protected void finish() { /* not used in this task */}

    protected void alertRetrievingAccessTokenFailed() {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr(
                        "<html>Retrieving an OAuth Access Token from ''{0}'' failed.</html>",
                        parameters.getAccessTokenUrl()
                ),
                tr("Request Failed"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/OAuth#NotAuthorizedException")
        );
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        try {
            synchronized (this) {
                client = new OsmOAuthAuthorizationClient(parameters, requestToken);
            }
            accessToken = client.getAccessToken(getProgressMonitor().createSubTaskMonitor(0, false));
        } catch (OsmTransferCanceledException e) {
            Logging.trace(e);
            return;
        } catch (final OsmOAuthAuthorizationException e) {
            Logging.error(e);
            GuiHelper.runInEDT(this::alertRetrievingAccessTokenFailed);
            accessToken = null;
        } finally {
            synchronized (this) {
                client = null;
            }
        }
    }

    /**
     * Replies true if the task was canceled.
     *
     * @return {@code true} if user aborted operation
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Replies the retrieved Access Token. null, if something went wrong.
     *
     * @return the retrieved Access Token
     */
    public OAuthToken getAccessToken() {
        return accessToken;
    }
}
