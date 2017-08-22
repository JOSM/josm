// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.RequestProcessor;
import org.openstreetmap.josm.tools.Logging;

/**
 * Reports available commands, their parameters and examples
 * @since 6091
 */
public class FeaturesHandler extends RequestHandler {

    /**
     * The remote control command name used to reply version.
     */
    public static final String command = "features";

    @Override
    protected void handleRequest() throws RequestHandlerErrorException,
            RequestHandlerBadRequestException {
        StringBuilder buf = new StringBuilder();
        String q = args.get("q");
        if (q != null) {
            buf.append('[');
            boolean first = true;
            for (String s: q.split("[,\\s]+")) {
               if (first) {
                   first = false;
               } else {
                   buf.append(", ");
               }
               String info = RequestProcessor.getHandlerInfoAsJSON('/'+s);
               if (info != null) {
                   buf.append(info);
               } else {
                   Logging.warn("Unknown handler {0} passed to /features request", s);
               }
            }
            buf.append(']');
        } else {
            buf.append(RequestProcessor.getHandlersInfoAsJSON());
        }

        content = buf.toString();
        contentType = "application/json";
        if (args.containsKey("jsonp")) {
            content = args.get("jsonp") + " && " + args.get("jsonp") + '(' + content + ')';
        }
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to report its supported features. This enables web sites to guess a running JOSM version");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.READ_PROTOCOL_VERSION;
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[0];
    }

    @Override
    public String[] getOptionalParams() {
        return new String[]{"jsonp", "q"};
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        // Nothing to do
    }

    @Override
    public String getUsage() {
        return "reports available commands, their parameters and examples";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {"/features", "/features?q=import,add_node"};
    }
}
