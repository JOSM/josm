// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Manager class for remote control operations.
 *
 * IMPORTANT! increment the minor version on compatible API extensions
 * and increment the major version and set minor to 0 on incompatible changes.
 */
public class RemoteControl {

    /**
     * If the remote control feature is enabled or disabled. If disabled,
     * it should not start the server.
     */
    public static final BooleanProperty PROP_REMOTECONTROL_ENABLED = new BooleanProperty("remotecontrol.enabled", false);

    /**
     * RemoteControl HTTP protocol version. Change minor number for compatible
     * interface extensions. Change major number in case of incompatible
     * changes.
     */
    static final int protocolMajorVersion = 1;
    static final int protocolMinorVersion = 9;

    /**
     * Starts the remote control server
     */
    public static void start() {
        RemoteControlHttpServer.restartRemoteControlHttpServer();
    }

    /**
     * Stops the remote control server
     * @since 5861
     */
    public static void stop() {
        RemoteControlHttpServer.stopRemoteControlHttpServer();
    }

    /**
     * Adds external request handler.
     * Can be used by plugins that want to use remote control.
     *
     * @param command The command name.
     * @param handlerClass The additional request handler.
     */
    public void addRequestHandler(String command, Class<? extends RequestHandler> handlerClass) {
        RequestProcessor.addRequestHandlerClass(command, handlerClass);
    }

    /**
     * Returns the remote control directory.
     * @return The remote control directory
     * @since 7335
     */
    public static String getRemoteControlDir() {
        return new File(Config.getDirs().getUserDataDirectory(true), "remotecontrol").getAbsolutePath();
    }

    /**
     * Returns the IPv6 address used for remote control.
     * @return the IPv6 address used for remote control
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @since 8337
     */
    public static InetAddress getInet6Address() throws UnknownHostException {
        for (InetAddress a : InetAddress.getAllByName(Config.getPref().get("remote.control.host.ipv6", "::1"))) {
            if (a instanceof Inet6Address) {
                return a;
            }
        }
        throw new UnknownHostException();
    }

    /**
     * Returns the IPv4 address used for remote control.
     * @return the IPv4 address used for remote control
     * @throws UnknownHostException if the local host name could not be resolved into an address.
     * @since 8337
     */
    public static InetAddress getInet4Address() throws UnknownHostException {
        // Return an address to the loopback interface by default
        for (InetAddress a : InetAddress.getAllByName(Config.getPref().get("remote.control.host.ipv4", "127.0.0.1"))) {
            if (a instanceof Inet4Address) {
                return a;
            }
        }
        throw new UnknownHostException();
    }
}
