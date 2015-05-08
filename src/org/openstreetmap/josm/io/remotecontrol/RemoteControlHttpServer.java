// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.openstreetmap.josm.Main;

/**
 * Simple HTTP server that spawns a {@link RequestProcessor} for every
 * connection.
 *
 * Taken from YWMS plugin by frsantos.
 */
public class RemoteControlHttpServer extends Thread {

    /** The server socket */
    private ServerSocket server = null;

    /** The server instance for IPv4 */
    private static volatile RemoteControlHttpServer instance4 = null;
    /** The server instance for IPv6 */
    private static volatile RemoteControlHttpServer instance6 = null;

    /**
     * Starts or restarts the HTTP server
     */
    public static void restartRemoteControlHttpServer() {
        stopRemoteControlHttpServer();
        int port = Main.pref.getInteger("remote.control.port", 8111);
        try {
            instance4 = new RemoteControlHttpServer(port, false);
            instance4.start();
        } catch (Exception ex) {
            Main.warn(marktr("Cannot start IPv4 remotecontrol server on port {0}: {1}"),
                    Integer.toString(port), ex.getLocalizedMessage());
        }
        try {
            instance6 = new RemoteControlHttpServer(port, true);
            instance6.start();
        } catch (Exception ex) {
            /* only show error when we also have no IPv4 */
            if(instance4 == null) {
                Main.warn(marktr("Cannot start IPv6 remotecontrol server on port {0}: {1}"),
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
                Main.error(ioe);
            }
            instance4 = null;
        }
        if (instance6 != null) {
            try {
                instance6.stopServer();
            } catch (IOException ioe) {
                Main.error(ioe);
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
        Main.info(marktr("RemoteControl::Accepting remote connections on {0}:{1}"),
                server.getInetAddress(), Integer.toString(server.getLocalPort()));
        while (true) {
            try {
                @SuppressWarnings("resource")
                Socket request = server.accept();
                RequestProcessor.processRequest(request);
            } catch (SocketException se) {
                if (!server.isClosed())
                    Main.error(se);
            } catch (IOException ioe) {
                Main.error(ioe);
            }
        }
    }

    /**
     * Stops the HTTP server
     *
     * @throws IOException
     */
    public void stopServer() throws IOException {
        Main.info(marktr("RemoteControl::Server {0}:{1} stopped."),
        server.getInetAddress(), Integer.toString(server.getLocalPort()));
        server.close();
    }
}
