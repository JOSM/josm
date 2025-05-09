// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.json.Json;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.remotecontrol.handler.AddNodeHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.AddWayHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.AuthorizationHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.FeaturesHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImageryHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImportHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.LoadAndZoomHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.LoadDataHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.LoadObjectHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.OpenApiHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.OpenFileHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerErrorException;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerForbiddenException;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerOsmApiException;
import org.openstreetmap.josm.io.remotecontrol.handler.VersionHandler;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Processes HTTP "remote control" requests.
 */
public class RequestProcessor extends Thread {

    /** This is purely used to ensure that remote control commands are executed in the order in which they are received */
    private static final ReentrantLock ORDER_LOCK = new ReentrantLock(true);
    private static final Charset RESPONSE_CHARSET = StandardCharsets.UTF_8;
    private static final String RESPONSE_TEMPLATE = "<!DOCTYPE html><html><head><meta charset=\""
            + RESPONSE_CHARSET.name()
            + "\">%s</head><body>%s</body></html>";

    /**
     * The string "JOSM RemoteControl"
     */
    public static final String JOSM_REMOTE_CONTROL = "JOSM RemoteControl";

    /**
     * RemoteControl protocol version. Change minor number for compatible
     * interface extensions. Change major number in case of incompatible
     * changes.
     */
    public static final String PROTOCOLVERSION = Json.createObjectBuilder()
            .add("protocolversion", Json.createObjectBuilder()
                    .add("major", RemoteControl.protocolMajorVersion)
                    .add("minor", RemoteControl.protocolMinorVersion))
            .add("application", JOSM_REMOTE_CONTROL)
            .add("version", Version.getInstance().getVersion())
            .build().toString();

    /** The socket this processor listens on */
    private final Socket request;

    /**
     * Collection of request handlers.
     * Will be initialized with default handlers here. Other plug-ins
     * can extend this list by using @see addRequestHandler
     */
    private static final Map<String, Class<? extends RequestHandler>> handlers = new TreeMap<>();

    static {
        initialize();
    }

    /**
     * Constructor
     *
     * @param request A socket to read the request.
     */
    public RequestProcessor(Socket request) {
        super("RemoteControl request processor");
        this.setDaemon(true);
        this.request = Objects.requireNonNull(request);
    }

    /**
     * Spawns a new thread for the request
     * @param request The request to process
     */
    public static void processRequest(Socket request) {
        new RequestProcessor(request).start();
    }

    /**
     * Add external request handler. Can be used by other plug-ins that
     * want to use remote control.
     *
     * @param command The command to handle.
     * @param handler The additional request handler.
     */
    public static void addRequestHandlerClass(String command, Class<? extends RequestHandler> handler) {
        addRequestHandlerClass(command, handler, false);
    }

    /**
     * Add external request handler. Message can be suppressed.
     * (for internal use)
     *
     * @param command The command to handle.
     * @param handler The additional request handler.
     * @param silent Don't show message if true.
     */
    private static void addRequestHandlerClass(String command,
                Class<? extends RequestHandler> handler, boolean silent) {
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        String commandWithSlash = '/' + command;
        if (handlers.get(commandWithSlash) != null) {
            Logging.info("RemoteControl: ignoring duplicate command " + command
                    + " with handler " + handler.getName());
        } else {
            if (!silent) {
                Logging.info("RemoteControl: adding command \"" +
                    command + "\" (handled by " + handler.getSimpleName() + ')');
            }
            handlers.put(commandWithSlash, handler);
            try {
                Optional.ofNullable(handler.getConstructor().newInstance().getPermissionPref())
                        .ifPresent(PermissionPrefWithDefault::addPermissionPref);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Logging.debug(e);
            }
        }
    }

    /**
     * Force the class to initialize and load the handlers
     */
    public static void initialize() {
        if (handlers.isEmpty()) {
            addRequestHandlerClass(LoadAndZoomHandler.command, LoadAndZoomHandler.class, true);
            addRequestHandlerClass(LoadAndZoomHandler.command2, LoadAndZoomHandler.class, true);
            addRequestHandlerClass(LoadObjectHandler.command, LoadObjectHandler.class, true);
            addRequestHandlerClass(LoadDataHandler.command, LoadDataHandler.class, true);
            addRequestHandlerClass(ImportHandler.command, ImportHandler.class, true);
            addRequestHandlerClass(OpenFileHandler.command, OpenFileHandler.class, true);
            PermissionPrefWithDefault.addPermissionPref(PermissionPrefWithDefault.ALLOW_WEB_RESOURCES);
            addRequestHandlerClass(ImageryHandler.command, ImageryHandler.class, true);
            PermissionPrefWithDefault.addPermissionPref(PermissionPrefWithDefault.CHANGE_SELECTION);
            PermissionPrefWithDefault.addPermissionPref(PermissionPrefWithDefault.CHANGE_VIEWPORT);
            addRequestHandlerClass(AddNodeHandler.command, AddNodeHandler.class, true);
            addRequestHandlerClass(AddWayHandler.command, AddWayHandler.class, true);
            addRequestHandlerClass(VersionHandler.command, VersionHandler.class, true);
            addRequestHandlerClass(FeaturesHandler.command, FeaturesHandler.class, true);
            addRequestHandlerClass(OpenApiHandler.command, OpenApiHandler.class, true);
            addRequestHandlerClass(AuthorizationHandler.command, AuthorizationHandler.class, true);
        }
    }

    /**
     * The work is done here.
     */
    @Override
    public void run() {
        // The locks ensure that we process the instructions in the order in which they came.
        // This is mostly important when the caller is attempting to create a new layer and add multiple download
        // instructions for that layer. See #23821 for additional details.
        ORDER_LOCK.lock();
        try (request;
             Writer out = new OutputStreamWriter(new BufferedOutputStream(request.getOutputStream()), RESPONSE_CHARSET);
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.US_ASCII))) {
            realRun(in, out, request);
        } catch (IOException ioe) {
            Logging.debug(Logging.getErrorMessage(ioe));
        } finally {
            ORDER_LOCK.unlock();
        }
    }

    /**
     * Perform the actual commands
     * @param in The reader for incoming data
     * @param out The writer for outgoing data
     * @param request The actual request
     * @throws IOException Usually occurs if one of the {@link Writer} methods has problems.
     */
    private static void realRun(BufferedReader in, Writer out, Socket request) throws IOException {
        try {
            String get = in.readLine();
            if (get == null) {
                sendInternalError(out, null);
                return;
            }
            Logging.info("RemoteControl received: " + get);

            StringTokenizer st = new StringTokenizer(get);
            if (!st.hasMoreTokens()) {
                sendInternalError(out, null);
                return;
            }
            String method = st.nextToken();
            if (!st.hasMoreTokens()) {
                sendInternalError(out, null);
                return;
            }
            String url = st.nextToken();

            if (!"GET".equals(method)) {
                sendNotImplemented(out);
                return;
            }

            final int questionPos = url.indexOf('?');

            final String command = questionPos < 0 ? url : url.substring(0, questionPos);

            final Map<String, String> headers = parseHeaders(in);
            final String sender = parseSender(headers, request);
            callHandler(url, command, out, sender);
        } catch (ReflectiveOperationException e) {
            Logging.error(e);
            try {
                sendInternalError(out, e.getMessage());
            } catch (IOException e1) {
                Logging.warn(e1);
            }
        }
    }

    /**
     * Parse the headers from the request
     * @param in The request reader
     * @return The map of headers
     * @throws IOException See {@link BufferedReader#readLine()}
     */
    private static Map<String, String> parseHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        int k = 0;
        int maxHeaders = 20;
        int lastSize = -1;
        while (k < maxHeaders && lastSize != headers.size()) {
            lastSize = headers.size();
            String get = in.readLine();
            if (get != null) {
                k++;
                String[] h = get.split(": ", 2);
                if (h.length == 2) {
                    headers.put(h[0], h[1]);
                }
            }
        }
        return headers;
    }

    /**
     * Attempt to figure out who sent the request
     * @param headers The headers (we currently look for {@code Referer})
     * @param request The request to look at
     * @return The sender (or {@code "localhost"} if none could be found)
     */
    private static String parseSender(Map<String, String> headers, Socket request) {
        // Who sent the request: trying our best to detect
        // not from localhost => sender = IP
        // from localhost: sender = referer header, if exists
        if (!request.getInetAddress().isLoopbackAddress()) {
            return request.getInetAddress().getHostAddress();
        }
        String ref = headers.get("Referer");
        Pattern r = Pattern.compile("(https?://)?([^/]*)");
        if (ref != null) {
            Matcher m = r.matcher(ref);
            if (m.find()) {
                return m.group(2);
            }
        }
        return "localhost";
    }

    /**
     * Call the handler for the command
     * @param url The request URL
     * @param command The command we are using
     * @param out The writer to use for indicating success or failure
     * @param sender The sender of the request
     * @throws ReflectiveOperationException If the handler class has an issue
     * @throws IOException If one of the {@link Writer} methods has issues
     */
    private static void callHandler(String url, String command, Writer out, String sender) throws ReflectiveOperationException, IOException {
        // find a handler for this command
        Class<? extends RequestHandler> handlerClass = handlers.get(command);
        if (handlerClass == null) {
            String usage = getUsageAsHtml();
            String websiteDoc = HelpUtil.getWikiBaseHelpUrl() +"/Help/Preferences/RemoteControl";
            String help = "No command specified! The following commands are available:<ul>" + usage
                    + "</ul>" + "See <a href=\""+websiteDoc+"\">"+websiteDoc+"</a> for complete documentation.";
            sendErrorHtml(out, 400, "Bad Request", help);
        } else {
            // create handler object
            RequestHandler handler = handlerClass.getConstructor().newInstance();
            try {
                handler.setCommand(command);
                handler.setUrl(url);
                handler.setSender(sender);
                handler.handle();
                sendHeader(out, "200 OK", handler.getContentType(), false);
                out.write("Content-length: " + handler.getContent().length()
                        + "\r\n");
                out.write("\r\n");
                out.write(handler.getContent());
                out.flush();
            } catch (RequestHandlerOsmApiException ex) {
                Logging.debug(ex);
                sendBadGateway(out, ex.getMessage());
            } catch (RequestHandlerErrorException ex) {
                Logging.debug(ex);
                sendInternalError(out, ex.getMessage());
            } catch (RequestHandlerBadRequestException ex) {
                Logging.debug(ex);
                sendBadRequest(out, ex.getMessage());
            } catch (RequestHandlerForbiddenException ex) {
                Logging.debug(ex);
                sendForbidden(out, ex.getMessage());
            }
        }
    }

    private static void sendError(Writer out, int errorCode, String errorName, String help) throws IOException {
        sendErrorHtml(out, errorCode, errorName, help == null ? "" : "<p>"+Utils.escapeReservedCharactersHTML(help) + "</p>");
    }

    private static void sendErrorHtml(Writer out, int errorCode, String errorName, String helpHtml) throws IOException {
        sendHeader(out, errorCode + " " + errorName, "text/html", true);
        out.write(String.format(
                RESPONSE_TEMPLATE,
                "<title>" + errorName + "</title>",
                "<h1>HTTP Error " + errorCode + ": " + errorName + "</h1>" +
                helpHtml
        ));
        out.flush();
    }

    /**
     * Sends a 500 error: internal server error
     *
     * @param out
     *            The writer where the error is written
     * @param help
     *            Optional HTML help content to display, can be null
     * @throws IOException
     *             If the error can not be written
     */
    private static void sendInternalError(Writer out, String help) throws IOException {
        sendError(out, 500, "Internal Server Error", help);
    }

    /**
     * Sends a 501 error: not implemented
     *
     * @param out
     *            The writer where the error is written
     * @throws IOException
     *             If the error can not be written
     */
    private static void sendNotImplemented(Writer out) throws IOException {
        sendError(out, 501, "Not Implemented", null);
    }

    /**
     * Sends a 502 error: bad gateway
     *
     * @param out
     *            The writer where the error is written
     * @param help
     *            Optional HTML help content to display, can be null
     * @throws IOException
     *             If the error can not be written
     */
    private static void sendBadGateway(Writer out, String help) throws IOException {
        sendError(out, 502, "Bad Gateway", help);
    }

    /**
     * Sends a 403 error: forbidden
     *
     * @param out
     *            The writer where the error is written
     * @param help
     *            Optional HTML help content to display, can be null
     * @throws IOException
     *             If the error can not be written
     */
    private static void sendForbidden(Writer out, String help) throws IOException {
        sendError(out, 403, "Forbidden", help);
    }

    /**
     * Sends a 400 error: bad request
     *
     * @param out The writer where the error is written
     * @param help Optional help content to display, can be null
     * @throws IOException If the error can not be written
     */
    private static void sendBadRequest(Writer out, String help) throws IOException {
        sendError(out, 400, "Bad Request", help);
    }

    /**
     * Send common HTTP headers to the client.
     *
     * @param out
     *            The Writer
     * @param status
     *            The status string ("200 OK", "500", etc)
     * @param contentType
     *            The content type of the data sent
     * @param endHeaders
     *            If true, adds a new line, ending the headers.
     * @throws IOException
     *             When error
     */
    private static void sendHeader(Writer out, String status, String contentType,
            boolean endHeaders) throws IOException {
        out.write("HTTP/1.1 " + status + "\r\n");
        out.write("Date: " + new Date() + "\r\n");
        out.write("Server: " + JOSM_REMOTE_CONTROL + "\r\n");
        out.write("Content-type: " + contentType + "; charset=" + RESPONSE_CHARSET.name().toLowerCase(Locale.ENGLISH) + "\r\n");
        out.write("Access-Control-Allow-Origin: *\r\n");
        if (endHeaders)
            out.write("\r\n");
    }

    /**
     * Returns the information for the given (if null: all) handlers.
     * @param handlers the handlers
     * @return the information for the given (if null: all) handlers
     */
    public static Stream<RequestHandler> getHandlersInfo(Collection<String> handlers) {
        return Utils.firstNonNull(handlers, RequestProcessor.handlers.keySet()).stream()
                .map(RequestProcessor::getHandlerInfo)
                .filter(Objects::nonNull);
    }

    /**
     * Returns the information for a given handler.
     * @param cmd handler key
     * @return the information for the given handler
     */
    public static RequestHandler getHandlerInfo(String cmd) {
        if (cmd == null) {
            return null;
        }
        if (!cmd.startsWith("/")) {
            cmd = "/" + cmd;
        }
        try {
            Class<?> c = handlers.get(cmd);
            if (c == null) return null;
            RequestHandler handler = handlers.get(cmd).getConstructor().newInstance();
            handler.setCommand(cmd);
            return handler;
        } catch (ReflectiveOperationException ex) {
            Logging.warn("Unknown handler " + cmd);
            Logging.error(ex);
            return null;
        }
    }

    /**
     * Reports HTML message with the description of all available commands
     * @return HTML message with the description of all available commands
     * @throws ReflectiveOperationException if a reflective operation fails for one handler class
     */
    public static String getUsageAsHtml() throws ReflectiveOperationException {
        StringBuilder usage = new StringBuilder(1024);
        for (Entry<String, Class<? extends RequestHandler>> handler : handlers.entrySet()) {
            RequestHandler sample = handler.getValue().getConstructor().newInstance();
            String[] mandatory = sample.getMandatoryParams();
            String[] optional = sample.getOptionalParams();
            String[] examples = sample.getUsageExamples(handler.getKey().substring(1));
            usage.append("<li>")
                 .append(handler.getKey());
            if (!Utils.isEmpty(sample.getUsage())) {
                usage.append(" &mdash; <i>").append(sample.getUsage()).append("</i>");
            }
            if (mandatory != null && mandatory.length > 0) {
                usage.append("<br/>mandatory parameters: ").append(String.join(", ", mandatory));
            }
            if (optional != null && optional.length > 0) {
                usage.append("<br/>optional parameters: ").append(String.join(", ", optional));
            }
            if (examples != null && examples.length > 0) {
                usage.append("<br/>examples: ");
                for (String ex: examples) {
                    usage.append("<br/> <a href=\"http://localhost:8111").append(ex).append("\">").append(ex).append("</a>");
                }
            }
            usage.append("</li>");
        }
        return usage.toString();
    }
}
