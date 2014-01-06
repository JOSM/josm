// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
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
    private ServerSocket server;

    private static RemoteControlHttpServer instance;

    /**
     * Starts or restarts the HTTP server
     */
    public static void restartRemoteControlHttpServer() {
        int port = Main.pref.getInteger("remote.control.port", 8111);
        try {
            stopRemoteControlHttpServer();

            instance = new RemoteControlHttpServer(port);
            instance.start();
        } catch (BindException ex) {
            Main.warn(marktr("Cannot start remotecontrol server on port {0}: {1}"),
                    Integer.toString(port), ex.getLocalizedMessage());
        } catch (IOException ioe) {
            Main.error(ioe);
        }
    }

    /**
     * Stops the HTTP server
     * @since 5861
     */
    public static void stopRemoteControlHttpServer() {
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
     * Constructor
     * @param port The port this server will listen on
     * @throws IOException when connection errors
     */
    public RemoteControlHttpServer(int port) throws IOException {
        super("RemoteControl HTTP Server");
        this.setDaemon(true);
        // Start the server socket with only 1 connection.
        // Also make sure we only listen
        // on the local interface so nobody from the outside can connect!
        // NOTE: On a dual stack machine with old Windows OS this may not listen on both interfaces!
        this.server = new ServerSocket(port, 1,
            InetAddress.getByName(Main.pref.get("remote.control.host", "localhost")));
    }

    /**
     * The main loop, spawns a {@link RequestProcessor} for each connection
     */
    @Override
    public void run() {
        Main.info(marktr("RemoteControl::Accepting connections on port {0}"),
             Integer.toString(server.getLocalPort()));
        while (true) {
            try {
                Socket request = server.accept();
                RequestProcessor.processRequest(request);
            } catch( SocketException se) {
                if( !server.isClosed() )
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
        server.close();
        Main.info(marktr("RemoteControl::Server stopped."));
    }
}
