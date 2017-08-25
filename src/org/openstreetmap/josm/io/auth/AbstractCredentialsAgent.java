// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.awt.GraphicsEnvironment;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.util.EnumMap;
import java.util.Map;

import org.openstreetmap.josm.gui.io.CredentialDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * Partial implementation of the {@link CredentialsAgent} interface.
 * <p>
 * Provides a memory cache for the credentials and means to query the information 
 * from the user.
 */
public abstract class AbstractCredentialsAgent implements CredentialsAgent {

    protected Map<RequestorType, PasswordAuthentication> memoryCredentialsCache = new EnumMap<>(RequestorType.class);

    @Override
    public CredentialsAgentResponse getCredentials(final RequestorType requestorType, final String host, boolean noSuccessWithLastResponse)
            throws CredentialsAgentException {
        if (requestorType == null)
            return null;
        PasswordAuthentication credentials = lookup(requestorType, host);
        final String username = (credentials == null || credentials.getUserName() == null) ? "" : credentials.getUserName();
        final String password = (credentials == null || credentials.getPassword() == null) ? "" : String.valueOf(credentials.getPassword());

        final CredentialsAgentResponse response = new CredentialsAgentResponse();

        /*
         * Last request was successful and there was no credentials stored
         * in file (or only the username is stored).
         * -> Try to recall credentials that have been entered
         * manually in this session.
         */
        if (!noSuccessWithLastResponse && memoryCredentialsCache.containsKey(requestorType) &&
                (credentials == null || credentials.getPassword() == null || credentials.getPassword().length == 0)) {
            PasswordAuthentication pa = memoryCredentialsCache.get(requestorType);
            response.setUsername(pa.getUserName());
            response.setPassword(pa.getPassword());
            response.setCanceled(false);
        /*
         * Prompt the user for credentials. This happens the first time each
         * josm start if the user does not save the credentials to preference
         * file (username=="") and each time after authentication failed
         * (noSuccessWithLastResponse == true).
         */
        } else if (noSuccessWithLastResponse || username.isEmpty() || password.isEmpty()) {
            if (!GraphicsEnvironment.isHeadless()) {
                GuiHelper.runInEDTAndWait(() -> {
                    CredentialDialog dialog;
                    if (requestorType.equals(RequestorType.PROXY))
                        dialog = CredentialDialog.getHttpProxyCredentialDialog(
                                username, password, host, getSaveUsernameAndPasswordCheckboxText());
                    else
                        dialog = CredentialDialog.getOsmApiCredentialDialog(
                                username, password, host, getSaveUsernameAndPasswordCheckboxText());
                    dialog.setVisible(true);
                    response.setCanceled(dialog.isCanceled());
                    if (dialog.isCanceled())
                        return;
                    response.setUsername(dialog.getUsername());
                    response.setPassword(dialog.getPassword());
                    response.setSaveCredentials(dialog.isSaveCredentials());
                });
            }
            if (response.isCanceled() || response.getUsername() == null || response.getPassword() == null) {
                return response;
            }
            if (response.isSaveCredentials()) {
                store(requestorType, host, new PasswordAuthentication(
                        response.getUsername(),
                        response.getPassword()
                ));
            /*
             * User decides not to save credentials to file. Keep it
             * in memory so we don't have to ask over and over again.
             */
            } else {
                PasswordAuthentication pa = new PasswordAuthentication(response.getUsername(), response.getPassword());
                memoryCredentialsCache.put(requestorType, pa);
            }
        /*
         * We got it from file.
         */
        } else {
            response.setUsername(username);
            response.setPassword(password.toCharArray());
            response.setCanceled(false);
        }
        return response;
    }

    /**
     * Provide the text for a checkbox that offers to save the
     * username and password that has been entered by the user.
     * @return checkbox text
     */
    public abstract String getSaveUsernameAndPasswordCheckboxText();
}
