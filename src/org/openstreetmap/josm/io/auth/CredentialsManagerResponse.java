// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

/**
 * CredentialsManagerResponse represents the response from {@see CredentialsManager#getCredentials(java.net.Authenticator.RequestorType, boolean)}.
 *
 * The response consists of the username and the password the requested credentials consists of.
 * In addition, it provides information whether authentication was canceled by the user, i.e.
 * because he or she canceled a username/password dialog (see {@see #isCanceled()}.
 *
 */
public class CredentialsManagerResponse {
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
