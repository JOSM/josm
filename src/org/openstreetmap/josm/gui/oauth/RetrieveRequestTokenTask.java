// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.xml.sax.SAXException;

/**
 * Asynchronous task for retrieving a request token
 */
public class RetrieveRequestTokenTask extends PleaseWaitRunnable {

    private boolean canceled;
    private OAuthToken requestToken;
    private OAuthParameters parameters;
    private OsmOAuthAuthorizationClient client;
    private Component parent;

    /**
     * Creates the task
     *
     * @param parent the parent component relative to which the {@link PleaseWaitRunnable}-Dialog
     * is displayed
     * @param parameters the OAuth parameters. Must not be null.
     * @throws IllegalArgumentException thrown if parameters is null.
     */
    public RetrieveRequestTokenTask(Component parent, OAuthParameters parameters ) {
        super(parent, tr("Retrieving OAuth Request Token..."), false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        this.parameters = parameters;
        this.parent = parent;
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized(this) {
            if (client != null) {
                client.cancel();
            }
        }
    }

    @Override
    protected void finish() { /* not used in this task */}

    protected void alertRetrievingRequestTokenFailed(OsmOAuthAuthorizationException e) {
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
            synchronized(this) {
                client = new OsmOAuthAuthorizationClient(parameters);
            }
            requestToken = client.getRequestToken(getProgressMonitor().createSubTaskMonitor(0, false));
        } catch(OsmTransferCanceledException e) {
            return;
        } catch (OsmOAuthAuthorizationException e) {
            Main.error(e);
            alertRetrievingRequestTokenFailed(e);
            requestToken = null;
        } finally {
            synchronized(this) {
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
