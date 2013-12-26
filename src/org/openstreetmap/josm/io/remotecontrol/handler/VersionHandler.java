// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.RequestProcessor;

/**
 * Handler for version request.
 */
public class VersionHandler extends RequestHandler {

    /**
     * The remote control command name used to reply version.
     */
    public static final String command = "version";

    @Override
    protected void handleRequest() throws RequestHandlerErrorException,
            RequestHandlerBadRequestException {
        content = RequestProcessor.PROTOCOLVERSION;
        contentType = "application/json";
        if (args.containsKey("jsonp")) {
            content = args.get("jsonp") + " && " + args.get("jsonp") + "(" + content + ")";
        }
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to report its protocol version. This enables web sites to detect a running JOSM.");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.READ_PROTOCOL_VERSION;
    }

    @Override
    public String[] getMandatoryParams() {
        return null;
    }

    @Override
    public String[] getOptionalParams() {
        return new String[]{"jsonp"};
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        // Nothing to do
    }

    @Override
    public String getUsage() {
        return "returns the current protocol version of the installed JOSM RemoteControl";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] { "/version", "/version?jsonp=test"};
    }
}