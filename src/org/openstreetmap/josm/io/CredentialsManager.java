// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.io.OsmConnection.OsmAuth;

/**
 * Manages how username and password are stored. In addition all 
 * username/password-related user interaction is encapsulated here.
 */
public interface CredentialsManager {
    enum Key {
        OSM_SERVER_URL("url"), 
        USERNAME("username"), 
        PASSWORD("password");
        final private String pname;
        private Key(String name) {
            pname = name;
        }
        @Override public String toString() {
            return pname;
        }
    };
    
    /**
     * Should throw or return non-null, possibly empty String.
     */
    public String lookup(Key key) throws CMException;

    /**
     * May silently fail to store.
     */
    public void store(Key key, String secret) throws CMException;

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
