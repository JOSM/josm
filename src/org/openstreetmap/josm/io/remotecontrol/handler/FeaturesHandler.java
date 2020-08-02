// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.RequestProcessor;

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
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        String q = args.get("q");
        Collection<String> handlers = q == null ? null : Arrays.asList(q.split("[,\\s]+", -1));
        content = getHandlersInfoAsJSON(handlers).toString();
        contentType = "application/json";
        if (args.containsKey("jsonp")) {
            content = args.get("jsonp") + " && " + args.get("jsonp") + '(' + content + ')';
        }
    }

    private static JsonArray getHandlersInfoAsJSON(Collection<String> handlers) {
        JsonArrayBuilder json = Json.createArrayBuilder();
        RequestProcessor.getHandlersInfo(handlers)
                .map(FeaturesHandler::getHandlerInfoAsJSON)
                .forEach(json::add);
        return json.build();
    }

    private static JsonObject getHandlerInfoAsJSON(RequestHandler handler) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("request", handler.getCommand());
        if (handler.getUsage() != null) {
            json.add("usage", handler.getUsage());
        }
        json.add("parameters", toJsonArray(handler.getMandatoryParams()));
        json.add("optional", toJsonArray(handler.getOptionalParams()));
        json.add("examples", toJsonArray(handler.getUsageExamples(handler.getCommand())));
        return json.build();
    }

    private static JsonArray toJsonArray(String[] strings) {
        return Arrays.stream(strings)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Json::createArrayBuilder))
                .build();
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
