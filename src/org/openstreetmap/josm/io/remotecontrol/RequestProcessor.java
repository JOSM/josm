// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.openstreetmap.josm.io.remotecontrol.handler.AddNodeHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImageryHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImportHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.LoadAndZoomHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerErrorException;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerForbiddenException;
import org.openstreetmap.josm.io.remotecontrol.handler.VersionHandler;

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
    private static HashMap<String, Class<? extends RequestHandler>> handlers = new HashMap<String, Class<? extends RequestHandler>>();

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
        if(command.charAt(0) == '/')
        {
            command = command.substring(1);
        }
        String commandWithSlash = "/" + command;
        if (handlers.get(commandWithSlash) != null) {
            System.out.println("RemoteControl: ignoring duplicate command " + command
                    + " with handler " + handler.getName());
        } else {
            if (!silent) {
                System.out.println("RemoteControl: adding command \"" +
                    command + "\" (handled by " + handler.getSimpleName() + ")");
            }
            handlers.put(commandWithSlash, handler);
        }
    }

    /** Add default request handlers */
    static {
        addRequestHandlerClass(LoadAndZoomHandler.command,
                LoadAndZoomHandler.class, true);
        addRequestHandlerClass(LoadAndZoomHandler.command2,
                LoadAndZoomHandler.class, true);
        addRequestHandlerClass(ImageryHandler.command, ImageryHandler.class, true);
        addRequestHandlerClass(AddNodeHandler.command, AddNodeHandler.class, true);
        addRequestHandlerClass(ImportHandler.command, ImportHandler.class, true);
        addRequestHandlerClass(VersionHandler.command, VersionHandler.class, true);
    }

    /**
     * The work is done here.
     */
    public void run() {
        Writer out = null;
        try {
            OutputStream raw = new BufferedOutputStream(
                    request.getOutputStream());
            out = new OutputStreamWriter(raw);
            Reader in = new InputStreamReader(new BufferedInputStream(
                    request.getInputStream()), "ASCII");

            StringBuffer requestLine = new StringBuffer();
            while (requestLine.length() < 1024) {
                int c = in.read();
                if (c == '\r' || c == '\n')
                    break;
                requestLine.append((char) c);
            }

            System.out.println("RemoteControl received: " + requestLine);
            String get = requestLine.toString();
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

            String command = null;
            int questionPos = url.indexOf('?');
            if(questionPos < 0)
            {
                command = url;
            }
            else
            {
                command = url.substring(0, questionPos);
            }

            // find a handler for this command
            Class<? extends RequestHandler> handlerClass = handlers
                    .get(command);
            if (handlerClass == null) {
                // no handler found
                sendBadRequest(out);
            } else {
                // create handler object
                RequestHandler handler = handlerClass.newInstance();
                try {
                    handler.setCommand(command);
                    handler.setUrl(url);
                    handler.checkPermission();
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
                    sendBadRequest(out);
                } catch (RequestHandlerForbiddenException ex) {
                    sendForbidden(out);
                }
            }

        } catch (IOException ioe) {
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendError(out);
            } catch (IOException e1) {
            }
        } finally {
            try {
                request.close();
            } catch (IOException e) {
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
    private void sendForbidden(Writer out) throws IOException {
        sendHeader(out, "403 Forbidden", "text/html", true);
        out.write("<HTML>\r\n");
        out.write("<HEAD><TITLE>Forbidden</TITLE>\r\n");
        out.write("</HEAD>\r\n");
        out.write("<BODY>");
        out.write("<H1>HTTP Error 403: Forbidden</h2>\r\n");
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
    private void sendBadRequest(Writer out) throws IOException {
        sendHeader(out, "400 Bad Request", "text/html", true);
        out.write("<HTML>\r\n");
        out.write("<HEAD><TITLE>Bad Request</TITLE>\r\n");
        out.write("</HEAD>\r\n");
        out.write("<BODY>");
        out.write("<H1>HTTP Error 400: Bad Request</h2>\r\n");
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
}