// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.openstreetmap.josm.Main;

/**
 * Simple HTTPS server that spawns a {@link RequestProcessor} for every secure connection.
 * 
 * @since 6941
 */
public class RemoteControlHttpsServer extends Thread {

    /** The server socket */
    private ServerSocket server;

    private static RemoteControlHttpsServer instance;
    private boolean initOK = false;
    private SSLContext sslContext; 

    private static final String KEYSTORE_PATH = "/data/josm.keystore";
    private static final String KEYSTORE_PASSWORD = "josm_ssl";

    private void initialize() {
        if (!initOK) {
            try {
                // Create new keystore
                KeyStore ks = KeyStore.getInstance("JKS");
                char[] password = KEYSTORE_PASSWORD.toCharArray();
                
                // Load keystore
                try (InputStream in = RemoteControlHttpsServer.class.getResourceAsStream(KEYSTORE_PATH)) {
                    if (in == null) {
                        Main.error(tr("Unable to find JOSM keystore at {0}. Remote control will not be available on HTTPS.", KEYSTORE_PATH));
                    } else {
                        ks.load(in, password);
                        
                        if (Main.isDebugEnabled()) {
                            for (Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements();) {
                                Main.debug("Alias in keystore: "+aliases.nextElement());
                            }
                        }
    
                        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                        kmf.init(ks, password);
                        
                        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                        tmf.init(ks);
                        
                        sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                        
                        if (Main.isDebugEnabled()) {
                            Main.debug("SSL Context protocol: " + sslContext.getProtocol());
                            Main.debug("SSL Context provider: " + sslContext.getProvider());
                        }
                        
                        initOK = true;
                    }
                }
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | 
                    IOException | UnrecoverableKeyException | KeyManagementException e) {
                Main.error(e);
            }
        }
    }

    /**
     * Starts or restarts the HTTPS server
     */
    public static void restartRemoteControlHttpsServer() {
        int port = Main.pref.getInteger("remote.control.https.port", 8112);
        try {
            stopRemoteControlHttpsServer();

            instance = new RemoteControlHttpsServer(port);
            if (instance.initOK) {
                instance.start();
            }
        } catch (BindException ex) {
            Main.warn(marktr("Cannot start remotecontrol https server on port {0}: {1}"),
                    Integer.toString(port), ex.getLocalizedMessage());
        } catch (IOException ioe) {
            Main.error(ioe);
        } catch (NoSuchAlgorithmException e) {
            Main.error(e);
        }
    }

    /**
     * Stops the HTTPS server
     */
    public static void stopRemoteControlHttpsServer() {
        if (instance != null) {
            try {
                instance.stopServer();
                instance = null;
            } catch (IOException ioe) {
                Main.error(ioe);
            }
        }
    }

    /**
     * Constructs a new {@code RemoteControlHttpsServer}.
     * @param port The port this server will listen on
     * @throws IOException when connection errors
     * @throws NoSuchAlgorithmException if the JVM does not support TLS (can not happen)
     */
    public RemoteControlHttpsServer(int port) throws IOException, NoSuchAlgorithmException {
        super("RemoteControl HTTPS Server");
        this.setDaemon(true);
        
        initialize();
        
        // Create SSL Server factory
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        if (Main.isDebugEnabled()) {
            Main.debug("SSL factory - Supported Cipher suites: "+Arrays.toString(factory.getSupportedCipherSuites()));
        }
        
        // Start the server socket with only 1 connection.
        // Also make sure we only listen
        // on the local interface so nobody from the outside can connect!
        // NOTE: On a dual stack machine with old Windows OS this may not listen on both interfaces!
        this.server = factory.createServerSocket(port, 1,
            InetAddress.getByName(Main.pref.get("remote.control.host", "localhost")));
        
        if (Main.isDebugEnabled() && server instanceof SSLServerSocket) {
            SSLServerSocket sslServer = (SSLServerSocket) server;
            Main.debug("SSL server - Enabled Cipher suites: "+Arrays.toString(sslServer.getEnabledCipherSuites()));
            Main.debug("SSL server - Enabled Protocols: "+Arrays.toString(sslServer.getEnabledProtocols()));
            Main.debug("SSL server - Enable Session Creation: "+sslServer.getEnableSessionCreation());
            Main.debug("SSL server - Need Client Auth: "+sslServer.getNeedClientAuth());
            Main.debug("SSL server - Want Client Auth: "+sslServer.getWantClientAuth());
            Main.debug("SSL server - Use Client Mode: "+sslServer.getUseClientMode());
        }
    }

    /**
     * The main loop, spawns a {@link RequestProcessor} for each connection.
     */
    @Override
    public void run() {
        Main.info(marktr("RemoteControl::Accepting secure connections on port {0}"),
             Integer.toString(server.getLocalPort()));
        while (true) {
            try {
                @SuppressWarnings("resource")
                Socket request = server.accept();
                if (Main.isDebugEnabled() && request instanceof SSLSocket) {
                    SSLSocket sslSocket = (SSLSocket) request;
                    Main.debug("SSL socket - Enabled Cipher suites: "+Arrays.toString(sslSocket.getEnabledCipherSuites()));
                    Main.debug("SSL socket - Enabled Protocols: "+Arrays.toString(sslSocket.getEnabledProtocols()));
                    Main.debug("SSL socket - Enable Session Creation: "+sslSocket.getEnableSessionCreation());
                    Main.debug("SSL socket - Need Client Auth: "+sslSocket.getNeedClientAuth());
                    Main.debug("SSL socket - Want Client Auth: "+sslSocket.getWantClientAuth());
                    Main.debug("SSL socket - Use Client Mode: "+sslSocket.getUseClientMode());
                    Main.debug("SSL socket - Session: "+sslSocket.getSession());
                }
                RequestProcessor.processRequest(request);
            } catch (SocketException se) {
                if (!server.isClosed()) {
                    Main.error(se);
                }
            } catch (IOException ioe) {
                Main.error(ioe);
            }
        }
    }

    /**
     * Stops the HTTPS server.
     *
     * @throws IOException if any I/O error occurs
     */
    public void stopServer() throws IOException {
        server.close();
        Main.info(marktr("RemoteControl::Server (https) stopped."));
    }
}
