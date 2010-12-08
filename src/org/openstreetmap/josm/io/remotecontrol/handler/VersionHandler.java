// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.RequestProcessor;

/**
 * Handler for version request.
 */
public class VersionHandler extends RequestHandler {

    public static final String command = "version";
    public static final String permissionKey = "remotecontrol.permission.read-protocolversion";
    public static final boolean permissionDefault = true;

    @Override
    protected void handleRequest() throws RequestHandlerErrorException,
            RequestHandlerBadRequestException {
        content = RequestProcessor.PROTOCOLVERSION;
        contentType = "application/json";
        if (args.containsKey("jsonp")) {
            content = args.get("jsonp")+ " && " + args.get("jsonp") + "(" + content + ")";
        }
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to report its protocol version. This enables web sites to detect a running JOSM.");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref()
    {
        return new PermissionPrefWithDefault(permissionKey, permissionDefault,
                "RemoteControl: /version forbidden by preferences");
    }
}