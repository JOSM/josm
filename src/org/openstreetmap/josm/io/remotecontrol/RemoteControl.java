// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;

/**
 * Manager class for remote control operations.
 *
 * To allow API there is a @see getVersion() method.
 *
 * IMPORTANT! increment the minor version on compatible API extensions
 * and increment the major version and set minor to 0 on incompatible changes.
 */
public class RemoteControl
{
    /**
     * If the remote cotrol feature is enabled or disabled. If disabled,
     * it should not start the server.
     */
    public static final BooleanProperty PROP_REMOTECONTROL_ENABLED = new BooleanProperty("remotecontrol.enabled", false);

    /** API version
     * IMPORTANT! update the version number on API changes.
     */
    static final int apiMajorVersion = 1;
    static final int apiMinorVersion = 0;

    /**
     * RemoteControl HTTP protocol version. Change minor number for compatible
     * interface extensions. Change major number in case of incompatible
     * changes.
     */
    static final int protocolMajorVersion = 1;
    static final int protocolMinorVersion = 2;

    /**
     * Returns an array of int values with major and minor API version
     * and major and minor HTTP protocol version.
     *
     * The function returns an int[4] instead of an object with fields
     * to avoid ClassNotFound errors with old versions of remotecontrol.
     *
     * @return array of integer version numbers:
     *    apiMajorVersion, apiMinorVersion, protocolMajorVersion, protocolMajorVersion
     */
    public int[] getVersion()
    {
        int versions[] = {apiMajorVersion, apiMinorVersion, protocolMajorVersion, protocolMajorVersion};
        return versions;
    }

    /**
     * Start the remote control server
     */
    public static void start() {
        RemoteControlHttpServer.restartRemoteControlHttpServer();
    }

    /**
     * Add external external request handler.
     * Can be used by plugins that want to use remote control.
     *
     * @param handler The additional request handler.
     */
    public void addRequestHandler(String command, Class<? extends RequestHandler> handlerClass)
    {
        RequestProcessor.addRequestHandlerClass(command, handlerClass);
    }
}
