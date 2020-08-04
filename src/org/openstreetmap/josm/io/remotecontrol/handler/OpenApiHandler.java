// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.io.remotecontrol.RequestProcessor;
import org.openstreetmap.josm.tools.Utils;

/**
 * Reports available commands as <a href="http://spec.openapis.org/oas/v3.0.3">OpenAPI</a>.
 */
public class OpenApiHandler extends RequestHandler {

    /**
     * The remote control command name.
     */
    public static final String command = "openapi.json";

    @Override
    protected void handleRequest() {
        JsonObjectBuilder openapi = getOpenApi();
        StringWriter stringWriter = new StringWriter();
        Json.createWriter(stringWriter).write(openapi.build());
        content = stringWriter.toString();
        contentType = "application/json";
    }

    private JsonObjectBuilder getOpenApi() {
        return Json.createObjectBuilder()
                .add("openapi", "3.0.0")
                .add("info", Json.createObjectBuilder()
                        .add("title", RequestProcessor.JOSM_REMOTE_CONTROL)
                        .add("version", RemoteControl.getVersion())
                        .add("contact", Json.createObjectBuilder()
                                .add("name", "JOSM")
                                .add("url", JosmUrls.getInstance().getJOSMWebsite())))
                .add("servers", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("url", "http://localhost:8111/")))
                .add("paths", getHandlers());
    }

    private JsonObjectBuilder getHandlers() {
        JsonObjectBuilder paths = Json.createObjectBuilder();
        RequestProcessor.getHandlersInfo(null)
                .forEach(handler -> paths.add("/" + handler.getCommand(), getHandler(handler)));
        return paths;
    }

    private JsonObjectBuilder getHandler(RequestHandler handler) {
        JsonArrayBuilder parameters = Json.createArrayBuilder();
        Stream.concat(
                Arrays.stream(handler.getMandatoryParams()),
                Arrays.stream(handler.getOptionalParams())
        ).distinct().map(param -> Json.createObjectBuilder()
                .add("name", param)
                .add("in", "query")
                .add("required", Arrays.asList(handler.getMandatoryParams()).contains(param))
                .add("schema", Json.createObjectBuilder().add("type", "string")) // TODO fix type
        ).forEach(parameters::add);
        return Json.createObjectBuilder().add("get", Json.createObjectBuilder()
                .add("description", getDescription(handler))
                .add("operationId", handler.getCommand())
                .add("parameters", parameters)
                .add("responses", Json.createObjectBuilder()
                        .add("200", Json.createObjectBuilder().add("description", "successful operation")))
        );
    }

    private String getDescription(RequestHandler handler) {
        return Utils.firstNonNull(handler.getUsage(), "")
                + "\n\n* " + String.join("\n* ", handler.getUsageExamples());
    }

    @Override
    public String getUsage() {
        return "JOSM RemoteControl as OpenAPI Specification";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{"https://petstore.swagger.io/?url=http://localhost:8111/openapi.json", "https://swagger.io/specification/"};
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
    protected void validateRequest() {
        // Nothing to do
    }
}
