// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.gui.io.CredentialDialog;

abstract public class AbstractCredentialsAgent implements CredentialsAgent {

    Map<RequestorType, PasswordAuthentication> memoryCredentialsCache = new HashMap<RequestorType, PasswordAuthentication>();

    /**
     * @see CredentialsAgent#getCredentials(RequestorType, boolean)
     */
    @Override
    public CredentialsAgentResponse getCredentials(RequestorType requestorType, boolean noSuccessWithLastResponse) throws CredentialsAgentException{
        if (requestorType == null)
            return null;
        PasswordAuthentication credentials =  lookup(requestorType);
        String username = (credentials == null || credentials.getUserName() == null) ? "" : credentials.getUserName();
        String password = (credentials == null || credentials.getPassword() == null) ? "" : String.valueOf(credentials.getPassword());

        CredentialsAgentResponse response = new CredentialsAgentResponse();

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
        } else if (noSuccessWithLastResponse || username.equals("") || password.equals("")) {
            CredentialDialog dialog = null;
            switch(requestorType) {
            case SERVER: dialog = CredentialDialog.getOsmApiCredentialDialog(username, password, getSaveUsernameAndPasswordCheckboxText()); break;
            case PROXY: dialog = CredentialDialog.getHttpProxyCredentialDialog(username, password, getSaveUsernameAndPasswordCheckboxText()); break;
            }
            dialog.setVisible(true);
            response.setCanceled(dialog.isCanceled());
            if (dialog.isCanceled())
                return response;
            response.setUsername(dialog.getUsername());
            response.setPassword(dialog.getPassword());
            if (dialog.isSaveCredentials()) {
                store(requestorType, new PasswordAuthentication(
                        response.getUsername(),
                        response.getPassword()
                ));
            /*
             * User decides not to save credentials to file. Keep it
             * in memory so we don't have to ask over and over again.
             */
            } else {
                PasswordAuthentication pa = new PasswordAuthentication(dialog.getUsername(), dialog.getPassword());
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
     */
    public abstract String getSaveUsernameAndPasswordCheckboxText();
}
