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
 * Asynchronous task for retrieving a request token
 */
public class RetrieveRequestTokenTask extends PleaseWaitRunnable {

    private boolean canceled;
    private OAuthToken requestToken;
    private final OAuthParameters parameters;
    private OsmOAuthAuthorizationClient client;
    private final Component parent;

    /**
     * Creates the task
     *
     * @param parent the parent component relative to which the {@link PleaseWaitRunnable}-Dialog
     * is displayed
     * @param parameters the OAuth parameters. Must not be null.
     * @throws IllegalArgumentException if parameters is null.
     */
    public RetrieveRequestTokenTask(Component parent, OAuthParameters parameters) {
        super(parent, tr("Retrieving OAuth Request Token..."), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        this.parameters = parameters;
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

    protected void alertRetrievingRequestTokenFailed() {
        HelpAwareOptionPane.showOptionDialog(
                parent,
                tr(
                        "<html>Retrieving an OAuth Request Token from ''{0}'' failed.</html>",
                        parameters.getRequestTokenUrl()
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
                client = new OsmOAuthAuthorizationClient(parameters);
            }
            requestToken = client.getRequestToken(getProgressMonitor().createSubTaskMonitor(0, false));
        } catch (OsmTransferCanceledException e) {
            Logging.trace(e);
            return;
        } catch (final OsmOAuthAuthorizationException e) {
            Logging.error(e);
            GuiHelper.runInEDT(this::alertRetrievingRequestTokenFailed);
            requestToken = null;
        } finally {
            synchronized (this) {
                client = null;
            }
        }
    }

    /**
     * Replies true if the task was canceled
     *
     * @return true if the task was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Replies the request token. null, if something went wrong.
     *
     * @return the request token
     */
    public OAuthToken getRequestToken() {
        return requestToken;
    }
}
