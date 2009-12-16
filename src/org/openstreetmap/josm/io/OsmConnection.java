// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.net.HttpURLConnection;
import java.net.Authenticator.RequestorType;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.logging.Logger;

import org.openstreetmap.josm.io.auth.CredentialsManagerException;
import org.openstreetmap.josm.io.auth.CredentialsManagerFactory;
import org.openstreetmap.josm.io.auth.CredentialsManagerResponse;
import org.openstreetmap.josm.tools.Base64;

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 *
 * @author imi
 */
public class OsmConnection {
    private static final Logger logger = Logger.getLogger(OsmConnection.class.getName());

    protected boolean cancel = false;
    protected HttpURLConnection activeConnection;

    /**
     * Initialize the http defaults and the authenticator.
     */
    static {
        try {
            HttpURLConnection.setFollowRedirects(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        cancel = true;
        synchronized (this) {
            if (activeConnection != null) {
                activeConnection.setConnectTimeout(100);
                activeConnection.setReadTimeout(100);
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }

        synchronized (this) {
            if (activeConnection != null) {
                activeConnection.disconnect();
            }
        }
    }

    protected void addAuth(HttpURLConnection con) throws OsmTransferException {
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        CredentialsManagerResponse response;
        String token;
        try {
            synchronized (CredentialsManagerFactory.getCredentialManager()) {
                response = CredentialsManagerFactory.getCredentialManager().getCredentials(RequestorType.SERVER, false /* don't know yet whether the credentials will succeed */);
            }
        } catch (CredentialsManagerException e) {
            throw new OsmTransferException(e);
        }
        if (response == null) {
            token = ":";
        } else if (response.isCanceled()) {
            cancel = true;
            return;
        } else {
            String username= response.getUsername() == null ? "" : response.getUsername();
            String password = response.getPassword() == null ? "" : String.valueOf(response.getPassword());
            token = username + ":" + password;
            try {
                ByteBuffer bytes = encoder.encode(CharBuffer.wrap(token));
                con.addRequestProperty("Authorization", "Basic "+Base64.encode(bytes));
            } catch(CharacterCodingException e) {
                throw new OsmTransferException(e);
            }
        }
    }

    /**
     * Replies true if this connection is canceled
     *
     * @return true if this connection is canceled
     * @return
     */
    public boolean isCanceled() {
        return cancel;
    }
}
