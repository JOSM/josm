// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.io.OsmConnection.OsmAuth;

/**
 * Manages how username and password are stored. In addition all 
 * username/password-related user interaction is encapsulated here.
 */
public interface CredentialsManager {
    /**
     * lookupUsername, lookupPassword:
     *
     * Should throw or return non-null, possibly empty String.
     */
    public String lookupUsername() throws CMException;
    public String lookupPassword() throws CMException;

    /**
     * storeUsername, storePassword:
     *
     * May silently fail to store.
     */
    public void storeUsername(String username) throws CMException;
    public void storePassword(String password) throws CMException;

    /**
     * If authentication using the stored credentials fails, this method is
     * called to promt for new username/password.
     */
    public java.net.PasswordAuthentication getPasswordAuthentication(OsmAuth caller);

    /**
     * Credentials-related preference gui.
     */
    public interface PreferenceAdditions {
        public void addPreferenceOptions(javax.swing.JPanel panel);
        public void preferencesChanged();
    }
    public PreferenceAdditions newPreferenceAdditions();

    public class CMException extends Exception {
        public CMException() {super();}
        public CMException(String message, Throwable cause) {super(message, cause);}
        public CMException(String message) {super(message);}
        public CMException(Throwable cause) {super(cause);}
    }
    public class NoContentException extends CMException {
        public NoContentException() {super();}
        public NoContentException(String message, Throwable cause) {super(message, cause);}
        public NoContentException(String message) {super(message);}
        public NoContentException(Throwable cause) {super(cause);}
    }
}
