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

    /** The server socket for IPv4 */
    private ServerSocket server4 = null;
    /** The server socket for IPv6 */
    private ServerSocket server6 = null;

    private static volatile RemoteControlHttpServer instance;

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
        try {
            this.server4 = new ServerSocket(port, 1, RemoteControl.getInet4Address());
        } catch (IOException e) {
        }
        try {
            this.server6 = new ServerSocket(port, 1, RemoteControl.getInet6Address());
        } catch (IOException e) {
            if(this.server4 == null) /* both failed */
                throw e;
        }
    }

    /**
     * The main loop, spawns a {@link RequestProcessor} for each connection
     */
    @Override
    public void run() {
        if(server4 != null) {
            Main.info(marktr("RemoteControl::Accepting IPv4 connections on {0}:{1}"),
                server4.getInetAddress(), Integer.toString(server4.getLocalPort()));
        }
        if(server6 != null) {
            Main.info(marktr("RemoteControl::Accepting IPv6 connections on {0}:{1}"),
                server6.getInetAddress(), Integer.toString(server6.getLocalPort()));
        }
        while (true) {
            if(server4 != null) {
                try {
                    @SuppressWarnings("resource")
                    Socket request = server4.accept();
                    RequestProcessor.processRequest(request);
                } catch (SocketException se) {
                    if (!server4.isClosed())
                        Main.error(se);
                } catch (IOException ioe) {
                    Main.error(ioe);
                }
            }
            if(server6 != null) {
                try {
                    @SuppressWarnings("resource")
                    Socket request = server6.accept();
                    RequestProcessor.processRequest(request);
                } catch (SocketException se) {
                    if (!server6.isClosed())
                        Main.error(se);
                } catch (IOException ioe) {
                    Main.error(ioe);
                }
            }
        }
    }

    /**
     * Stops the HTTP server
     *
     * @throws IOException
     */
    public void stopServer() throws IOException {
        if(server4 != null)
            server4.close();
        if(server6 != null)
            server6.close();
        Main.info(marktr("RemoteControl::Server stopped."));
    }
}
