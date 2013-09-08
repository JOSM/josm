// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import org.openstreetmap.josm.tools.Utils;

/**
 * CredentialsAgentResponse represents the response from {@link CredentialsAgent#getCredentials(java.net.Authenticator.RequestorType, String, boolean)}.
 *
 * The response consists of the username and the password the requested credentials consists of.
 * In addition, it provides information whether authentication was canceled by the user, i.e.
 * because he or she canceled a username/password dialog (see {@link #isCanceled()}.
 * It also provides information whether authentication should be saved.
 *
 */
public class CredentialsAgentResponse {
    private String username;
    private char[] password;
    private boolean canceled;
    private boolean saveCredentials;
    /**
     * Replies the user name
     * @return The user name
     */
    public String getUsername() {
        return username;
    }
    /**
     * Sets the user name
     * @param username The user name
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * Replies the password
     * @return The password in plain text
     */
    public char[] getPassword() {
        return password;
    }
    /**
     * Sets the password
     * @param password The password in plain text
     */
    public void setPassword(char[] password) {
        this.password = Utils.copyArray(password);
    }
    /**
     * Determines if authentication request has been canceled by user
     * @return true if authentication request has been canceled by user, false otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }
    /**
     * Sets the cancelation status (authentication request canceled by user)
     * @param canceled the cancelation status (authentication request canceled by user)
     */
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
    /**
     * Determines if authentication credentials should be saved
     * @return true if authentication credentials should be saved, false otherwise
     */
    public boolean isSaveCredentials() {
        return saveCredentials;
    }
    /**
     * Sets the saving status (authentication credentials to save)
     * @param saveCredentials the saving status (authentication credentials to save)
     */
    public void setSaveCredentials(boolean saveCredentials) {
        this.saveCredentials = saveCredentials;
    }
}
