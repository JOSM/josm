// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * Simple HTTP server that spawns a {@link RequestProcessor} for every
 * connection.
 *
 * Taken from YWMS plugin by frsantos.
 */
public class RemoteControlHttpServer extends Thread {

    /** The server socket */
    private final ServerSocket server;

    /** The server instance for IPv4 */
    private static volatile RemoteControlHttpServer instance4;
    /** The server instance for IPv6 */
    private static volatile RemoteControlHttpServer instance6;

    /**
     * Starts or restarts the HTTP server
     */
    public static void restartRemoteControlHttpServer() {
        stopRemoteControlHttpServer();
        int port = Config.getPref().getInt("remote.control.port", 8111);
        try {
            instance4 = new RemoteControlHttpServer(port, false);
            instance4.start();
        } catch (IOException ex) {
            Logging.debug(ex);
            Logging.warn(marktr("Cannot start IPv4 remotecontrol server on port {0}: {1}"),
                    Integer.toString(port), ex.getLocalizedMessage());
        }
        try {
            instance6 = new RemoteControlHttpServer(port, true);
            instance6.start();
        } catch (IOException ex) {
            /* only show error when we also have no IPv4 */
            if (instance4 == null) {
                Logging.debug(ex);
                Logging.warn(marktr("Cannot start IPv6 remotecontrol server on port {0}: {1}"),
                    Integer.toString(port), ex.getLocalizedMessage());
            }
        }
    }

    /**
     * Stops the HTTP server
     * @since 5861
     */
    public static void stopRemoteControlHttpServer() {
        if (instance4 != null) {
            try {
                instance4.stopServer();
            } catch (IOException ioe) {
                Logging.error(ioe);
            }
            instance4 = null;
        }
        if (instance6 != null) {
            try {
                instance6.stopServer();
            } catch (IOException ioe) {
                Logging.error(ioe);
            }
            instance6 = null;
        }
    }

    /**
     * Constructor
     * @param port The port this server will listen on
     * @param ipv6 Whether IPv6 or IPv4 server should be started
     * @throws IOException when connection errors
     * @since 8339
     */
    public RemoteControlHttpServer(int port, boolean ipv6) throws IOException {
        super("RemoteControl HTTP Server");
        this.setDaemon(true);
        this.server = new ServerSocket(port, 1, ipv6 ?
            RemoteControl.getInet6Address() : RemoteControl.getInet4Address());
    }

    /**
     * The main loop, spawns a {@link RequestProcessor} for each connection
     */
    @Override
    public void run() {
        Logging.info(marktr("RemoteControl::Accepting remote connections on {0}:{1}"),
                server.getInetAddress(), Integer.toString(server.getLocalPort()));
        while (true) {
            try {
                @SuppressWarnings("resource")
                Socket request = server.accept();
                RequestProcessor.processRequest(request);
            } catch (SocketException e) {
                if (!server.isClosed()) {
                    Logging.error(e);
                } else {
                    // stop the thread automatically if server is stopped
                    return;
                }
            } catch (IOException ioe) {
                Logging.error(ioe);
            }
        }
    }

    /**
     * Stops the HTTP server
     *
     * @throws IOException if any I/O error occurs
     */
    public void stopServer() throws IOException {
        Logging.info(marktr("RemoteControl::Server {0}:{1} stopped."),
        server.getInetAddress(), Integer.toString(server.getLocalPort()));
        server.close();
    }
}
