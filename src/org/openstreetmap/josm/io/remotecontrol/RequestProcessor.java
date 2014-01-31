// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.io.remotecontrol.handler.AddNodeHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.AddWayHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.FeaturesHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImageryHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImportHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.LoadAndZoomHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.LoadObjectHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.OpenFileHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerErrorException;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerForbiddenException;
import org.openstreetmap.josm.io.remotecontrol.handler.VersionHandler;
import org.openstreetmap.josm.tools.Utils;

/**
 * Processes HTTP "remote control" requests.
 */
public class RequestProcessor extends Thread {
    /**
     * RemoteControl protocol version. Change minor number for compatible
     * interface extensions. Change major number in case of incompatible
     * changes.
     */
    public static final String PROTOCOLVERSION = "{\"protocolversion\": {\"major\": " +
        RemoteControl.protocolMajorVersion + ", \"minor\": " +
        RemoteControl.protocolMinorVersion +
        "}, \"application\": \"JOSM RemoteControl\"}";

    /** The socket this processor listens on */
    private Socket request;

    /**
     * Collection of request handlers.
     * Will be initialized with default handlers here. Other plug-ins
     * can extend this list by using @see addRequestHandler
     */
    private static Map<String, Class<? extends RequestHandler>> handlers = new TreeMap<String, Class<? extends RequestHandler>>();

    /**
     * Constructor
     *
     * @param request A socket to read the request.
     */
    public RequestProcessor(Socket request) {
        super("RemoteControl request processor");
        this.setDaemon(true);
        this.request = request;
    }

    /**
     * Spawns a new thread for the request
     * @param request The request to process
     */
    public static void processRequest(Socket request) {
        RequestProcessor processor = new RequestProcessor(request);
        processor.start();
    }

    /**
     * Add external request handler. Can be used by other plug-ins that
     * want to use remote control.
     *
     * @param command The command to handle.
     * @param handler The additional request handler.
     */
    static void addRequestHandlerClass(String command,
            Class<? extends RequestHandler> handler) {
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
        if(command.charAt(0) == '/') {
            command = command.substring(1);
        }
        String commandWithSlash = "/" + command;
        if (handlers.get(commandWithSlash) != null) {
            Main.info("RemoteControl: ignoring duplicate command " + command
                    + " with handler " + handler.getName());
        } else {
            if (!silent) {
                Main.info("RemoteControl: adding command \"" +
                    command + "\" (handled by " + handler.getSimpleName() + ")");
            }
            handlers.put(commandWithSlash, handler);
        }
    }

    /** Add default request handlers */
    static {
        addRequestHandlerClass(LoadAndZoomHandler.command, LoadAndZoomHandler.class, true);
        addRequestHandlerClass(LoadAndZoomHandler.command2, LoadAndZoomHandler.class, true);
        addRequestHandlerClass(ImageryHandler.command, ImageryHandler.class, true);
        addRequestHandlerClass(AddNodeHandler.command, AddNodeHandler.class, true);
        addRequestHandlerClass(AddWayHandler.command, AddWayHandler.class, true);
        addRequestHandlerClass(ImportHandler.command, ImportHandler.class, true);
        addRequestHandlerClass(VersionHandler.command, VersionHandler.class, true);
        addRequestHandlerClass(LoadObjectHandler.command, LoadObjectHandler.class, true);
        addRequestHandlerClass(OpenFileHandler.command, OpenFileHandler.class, true);
        addRequestHandlerClass(FeaturesHandler.command, FeaturesHandler.class, true);
    }

    /**
     * The work is done here.
     */
    @Override
    public void run() {
        Writer out = null;
        try {
            OutputStream raw = new BufferedOutputStream(request.getOutputStream());
            out = new OutputStreamWriter(raw);
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream(), "ASCII"));

            String get = in.readLine();
            if (get == null) {
                sendError(out);
                return;
            }
            Main.info("RemoteControl received: " + get);

            StringTokenizer st = new StringTokenizer(get);
            if (!st.hasMoreTokens()) {
                sendError(out);
                return;
            }
            String method = st.nextToken();
            if (!st.hasMoreTokens()) {
                sendError(out);
                return;
            }
            String url = st.nextToken();

            if (!method.equals("GET")) {
                sendNotImplemented(out);
                return;
            }

            int questionPos = url.indexOf('?');

            String command = questionPos < 0 ? url : url.substring(0, questionPos);

            Map <String,String> headers = new HashMap<String, String>();
            int k=0, MAX_HEADERS=20;
            while (k<MAX_HEADERS) {
                get=in.readLine();
                if (get==null) break;
                k++;
                String[] h = get.split(": ", 2);
                if (h.length==2) {
                    headers.put(h[0], h[1]);
                } else break;
            }

            // Who sent the request: trying our best to detect
            // not from localhost => sender = IP
            // from localhost: sender = referer header, if exists
            String sender = null;

            if (!request.getInetAddress().isLoopbackAddress()) {
                sender = request.getInetAddress().getHostAddress();
            } else {
                String ref = headers.get("Referer");
                Pattern r = Pattern.compile("(https?://)?([^/]*)");
                if (ref!=null) {
                    Matcher m = r.matcher(ref);
                    if (m.find()) {
                        sender = m.group(2);
                    }
                }
                if (sender == null) {
                    sender = "localhost";
                }
            }

            // find a handler for this command
            Class<? extends RequestHandler> handlerClass = handlers.get(command);
            if (handlerClass == null) {
                String usage = getUsageAsHtml();
                String websiteDoc = HelpUtil.getWikiBaseHelpUrl() +"/Help/Preferences/RemoteControl";
                String help = "No command specified! The following commands are available:<ul>" + usage
                        + "</ul>" + "See <a href=\""+websiteDoc+"\">"+websiteDoc+"</a> for complete documentation.";
                sendBadRequest(out, help);
            } else {
                // create handler object
                RequestHandler handler = handlerClass.newInstance();
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
                } catch (RequestHandlerErrorException ex) {
                    sendError(out);
                } catch (RequestHandlerBadRequestException ex) {
                    sendBadRequest(out, ex.getMessage());
                } catch (RequestHandlerForbiddenException ex) {
                    sendForbidden(out, ex.getMessage());
                }
            }

        } catch (IOException ioe) {
            Main.debug(Main.getErrorMessage(ioe));
        } catch (Exception e) {
            Main.error(e);
            try {
                sendError(out);
            } catch (IOException e1) {
                Main.warn(e1);
            }
        } finally {
            try {
                request.close();
            } catch (IOException e) {
                Main.debug(Main.getErrorMessage(e));
            }
        }
    }

    /**
     * Sends a 500 error: server error
     *
     * @param out
     *            The writer where the error is written
     * @throws IOException
     *             If the error can not be written
     */
    private void sendError(Writer out) throws IOException {
        sendHeader(out, "500 Internal Server Error", "text/html", true);
        out.write("<HTML>\r\n");
        out.write("<HEAD><TITLE>Internal Error</TITLE>\r\n");
        out.write("</HEAD>\r\n");
        out.write("<BODY>");
        out.write("<H1>HTTP Error 500: Internal Server Error</h2>\r\n");
        out.write("</BODY></HTML>\r\n");
        out.flush();
    }

    /**
     * Sends a 501 error: not implemented
     *
     * @param out
     *            The writer where the error is written
     * @throws IOException
     *             If the error can not be written
     */
    private void sendNotImplemented(Writer out) throws IOException {
        sendHeader(out, "501 Not Implemented", "text/html", true);
        out.write("<HTML>\r\n");
        out.write("<HEAD><TITLE>Not Implemented</TITLE>\r\n");
        out.write("</HEAD>\r\n");
        out.write("<BODY>");
        out.write("<H1>HTTP Error 501: Not Implemented</h2>\r\n");
        out.write("</BODY></HTML>\r\n");
        out.flush();
    }

    /**
     * Sends a 403 error: forbidden
     *
     * @param out
     *            The writer where the error is written
     * @throws IOException
     *             If the error can not be written
     */
    private void sendForbidden(Writer out, String help) throws IOException {
        sendHeader(out, "403 Forbidden", "text/html", true);
        out.write("<HTML>\r\n");
        out.write("<HEAD><TITLE>Forbidden</TITLE>\r\n");
        out.write("</HEAD>\r\n");
        out.write("<BODY>");
        out.write("<H1>HTTP Error 403: Forbidden</h2>\r\n");
        if (help != null) {
            out.write(help);
        }
        out.write("</BODY></HTML>\r\n");
        out.flush();
    }

    /**
     * Sends a 403 error: forbidden
     *
     * @param out
     *            The writer where the error is written
     * @throws IOException
     *             If the error can not be written
     */
    private void sendBadRequest(Writer out, String help) throws IOException {
        sendHeader(out, "400 Bad Request", "text/html", true);
        out.write("<HTML>\r\n");
        out.write("<HEAD><TITLE>Bad Request</TITLE>\r\n");
        out.write("</HEAD>\r\n");
        out.write("<BODY>");
        out.write("<H1>HTTP Error 400: Bad Request</h2>\r\n");
        if (help != null) {
            out.write(help);
        }
        out.write("</BODY></HTML>\r\n");
        out.flush();
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
    private void sendHeader(Writer out, String status, String contentType,
            boolean endHeaders) throws IOException {
        out.write("HTTP/1.1 " + status + "\r\n");
        Date now = new Date();
        out.write("Date: " + now + "\r\n");
        out.write("Server: JOSM RemoteControl\r\n");
        out.write("Content-type: " + contentType + "\r\n");
        out.write("Access-Control-Allow-Origin: *\r\n");
        if (endHeaders)
            out.write("\r\n");
    }

    public static String getHandlersInfoAsJSON() {
        StringBuilder r = new StringBuilder();
        boolean first = true;
        r.append("[");

        for (Entry<String, Class<? extends RequestHandler>> p : handlers.entrySet()) {
            if (first) {
                first = false;
            } else {
                r.append(", ");
            }
            r.append(getHandlerInfoAsJSON(p.getKey()));
        }
        r.append("]");

        return r.toString();
    }

    public static String getHandlerInfoAsJSON(String cmd) {
        StringWriter w = new StringWriter();
        PrintWriter r = new PrintWriter(w);
        RequestHandler handler = null;
        try {
            Class<?> c = handlers.get(cmd);
            if (c==null) return null;
            handler = handlers.get(cmd).newInstance();
        } catch (Exception ex) {
            Main.error(ex);
            return null;
        }

        r.printf("{ \"request\" : \"%s\"", cmd);
        if (handler.getUsage() != null) {
            r.printf(", \"usage\" : \"%s\"", handler.getUsage());
        }
        r.append(", \"parameters\" : [");

        String[] params = handler.getMandatoryParams();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (i == 0) {
                    r.append('\"');
                } else {
                    r.append(", \"");
                }
                r.append(params[i]).append('\"');
            }
        }
        r.append("], \"optional\" : [");
        String[] optional = handler.getOptionalParams();
        if (optional != null) {
            for (int i = 0; i < optional.length; i++) {
                if (i == 0) {
                    r.append('\"');
                } else {
                    r.append(", \"");
                }
                r.append(optional[i]).append('\"');
            }
        }

        r.append("], \"examples\" : [");
        String[] examples = handler.getUsageExamples(cmd.substring(1));
        if (examples != null) {
            for (int i = 0; i < examples.length; i++) {
                if (i == 0) {
                    r.append('\"');
                } else {
                    r.append(", \"");
                }
                r.append(examples[i]).append('\"');
            }
        }
        r.append("]}");
        try {
            return w.toString();
        } finally {
            try {
                w.close();
            } catch (IOException ex) {
                Main.warn(ex);
            }
        }
    }

    /**
     * Reports HTML message with the description of all available commands
     * @return HTML message with the description of all available commands
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static String getUsageAsHtml() throws IllegalAccessException, InstantiationException {
        StringBuilder usage = new StringBuilder(1024);
        for (Entry<String, Class<? extends RequestHandler>> handler : handlers.entrySet()) {
            RequestHandler sample = handler.getValue().newInstance();
            String[] mandatory = sample.getMandatoryParams();
            String[] optional = sample.getOptionalParams();
            String[] examples = sample.getUsageExamples(handler.getKey().substring(1));
            usage.append("<li>");
            usage.append(handler.getKey());
            if (sample.getUsage() != null && !sample.getUsage().isEmpty()) {
                usage.append(" &mdash; <i>").append(sample.getUsage()).append("</i>");
            }
            if (mandatory != null) {
                usage.append("<br/>mandatory parameters: ").append(Utils.join(", ", Arrays.asList(mandatory)));
            }
            if (optional != null) {
                usage.append("<br/>optional parameters: ").append(Utils.join(", ", Arrays.asList(optional)));
            }
            if (examples != null) {
                usage.append("<br/>examples: ");
                for (String ex: examples) {
                    usage.append("<br/> <a href=\"http://localhost:8111"+ex+"\">"+ex+"</a>");
                }
            }
            usage.append("</li>");
        }
        return usage.toString();
    }
}
