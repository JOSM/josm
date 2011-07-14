// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

/**
 * CredentialsAgentResponse represents the response from {@see CredentialsAgent#getCredentials(java.net.Authenticator.RequestorType, boolean)}.
 *
 * The response consists of the username and the password the requested credentials consists of.
 * In addition, it provides information whether authentication was canceled by the user, i.e.
 * because he or she canceled a username/password dialog (see {@see #isCanceled()}.
 *
 */
public class CredentialsAgentResponse {
    private String username;
    private char[] password;
    private boolean canceled;
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public char[] getPassword() {
        return password;
    }
    public void setPassword(char[] password) {
        this.password = password;
    }
    public boolean isCanceled() {
        return canceled;
    }
    public void setCanceled(boolean cancelled) {
        this.canceled = cancelled;
    }
}
