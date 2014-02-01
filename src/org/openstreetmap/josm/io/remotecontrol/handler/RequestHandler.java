// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is the parent of all classes that handle a specific remote control command
 *
 * @author Bodo Meissner
 */
public abstract class RequestHandler {

    public static final String globalConfirmationKey = "remotecontrol.always-confirm";
    public static final boolean globalConfirmationDefault = false;
    public static final String loadInNewLayerKey = "remotecontrol.new-layer";
    public static final boolean loadInNewLayerDefault = false;

    /** The GET request arguments */
    protected Map<String,String> args;

    /** The request URL without "GET". */
    protected String request;

    /** default response */
    protected String content = "OK\r\n";
    /** default content type */
    protected String contentType = "text/plain";

    /** will be filled with the command assigned to the subclass */
    protected String myCommand;

    /**
     * who send th request?
     * the host from refrerer header or IP of request sender
     */
    protected String sender;

    /**
     * Check permission and parameters and handle request.
     *
     * @throws RequestHandlerForbiddenException
     * @throws RequestHandlerBadRequestException
     * @throws RequestHandlerErrorException
     */
    public final void handle() throws RequestHandlerForbiddenException, RequestHandlerBadRequestException, RequestHandlerErrorException {
        checkMandatoryParams();
        validateRequest();
        checkPermission();
        handleRequest();
    }

    /**
     * Validates the request before attempting to perform it.
     * @throws RequestHandlerBadRequestException
     * @since 5678
     */
    protected abstract void validateRequest() throws RequestHandlerBadRequestException;

    /**
     * Handle a specific command sent as remote control.
     *
     * This method of the subclass will do the real work.
     *
     * @throws RequestHandlerErrorException
     * @throws RequestHandlerBadRequestException
     */
    protected abstract void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException;

    /**
     * Get a specific message to ask the user for permission for the operation
     * requested via remote control.
     *
     * This message will be displayed to the user if the preference
     * remotecontrol.always-confirm is true.
     *
     * @return the message
     */
    abstract public String getPermissionMessage();

    /**
     * Get a PermissionPref object containing the name of a special permission
     * preference to individually allow the requested operation and an error
     * message to be displayed when a disabled operation is requested.
     *
     * Default is not to check any special preference. Override this in a
     * subclass to define permission preference and error message.
     *
     * @return the preference name and error message or null
     */
    abstract public PermissionPrefWithDefault getPermissionPref();

    abstract public String[] getMandatoryParams();
    
    public String[] getOptionalParams() {
        return null;
    }

    public String getUsage() {
        return null;
    }

    public String[] getUsageExamples() {
        return null;
    }

    /**
     * Returns usage examples for the given command. To be overriden only my handlers that define several commands.
     * @param cmd The command asked
     * @return Usage examples for the given command
     * @since 6332
     */
    public String[] getUsageExamples(String cmd) {
        return getUsageExamples();
    }

    /**
     * Check permissions in preferences and display error message
     * or ask for permission.
     *
     * @throws RequestHandlerForbiddenException
     */
    final public void checkPermission() throws RequestHandlerForbiddenException
    {
        /*
         * If the subclass defines a specific preference and if this is set
         * to false, abort with an error message.
         *
         * Note: we use the deprecated class here for compatibility with
         * older versions of WMSPlugin.
         */
        PermissionPrefWithDefault permissionPref = getPermissionPref();
        if((permissionPref != null) && (permissionPref.pref != null))
        {
            if (!Main.pref.getBoolean(permissionPref.pref, permissionPref.defaultVal)) {
                String err = MessageFormat.format("RemoteControl: ''{0}'' forbidden by preferences", myCommand);
                Main.info(err);
                throw new RequestHandlerForbiddenException(err);
            }
        }

        /* Does the user want to confirm everything?
         * If yes, display specific confirmation message.
         */
        if (Main.pref.getBoolean(globalConfirmationKey, globalConfirmationDefault)) {
            // Ensure dialog box does not exceed main window size
            Integer maxWidth = (int) Math.max(200, Main.parent.getWidth()*0.6);
            String message = "<html><div>" + getPermissionMessage() +
                    "<br/>" + tr("Do you want to allow this?") + "</div></html>";
            JLabel label = new JLabel(message);
            if (label.getPreferredSize().width > maxWidth) {
                label.setText(message.replaceFirst("<div>", "<div style=\"width:" + maxWidth + "px;\">"));
            }
            if (JOptionPane.showConfirmDialog(Main.parent, label,
                tr("Confirm Remote Control action"),
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    String err = MessageFormat.format("RemoteControl: ''{0}'' forbidden by user''s choice", myCommand);
                    throw new RequestHandlerForbiddenException(err);
            }
        }
    }

    /**
     * Set request URL and parse args.
     *
     * @param url The request URL.
     */
    public void setUrl(String url) {
        this.request = url;
        parseArgs();
    }

    /**
     * Parse the request parameters as key=value pairs.
     * The result will be stored in {@code this.args}.
     *
     * Can be overridden by subclass.
     */
    protected void parseArgs() {
        try {
            String req = URLDecoder.decode(this.request, "UTF-8");
            HashMap<String, String> args = new HashMap<String, String>();
            if (req.indexOf('?') != -1) {
                String query = req.substring(req.indexOf('?') + 1);
                if (query.indexOf('#') != -1) {
                            query = query.substring(0, query.indexOf('#'));
                        }
                String[] params = query.split("&", -1);
                for (String param : params) {
                    int eq = param.indexOf('=');
                    if (eq != -1) {
                        args.put(param.substring(0, eq), param.substring(eq + 1));
                    }
                }
            }
            this.args = args;
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    void checkMandatoryParams() throws RequestHandlerBadRequestException {
        String[] mandatory = getMandatoryParams();
        String[] optional = getOptionalParams();
        List<String> missingKeys = new LinkedList<String>();
        boolean error = false;
        if(mandatory != null) for (String key : mandatory) {
            String value = args.get(key);
            if ((value == null) || (value.length() == 0)) {
                error = true;
                Main.warn("'" + myCommand + "' remote control request must have '" + key + "' parameter");
                missingKeys.add(key);
            }
        }
        HashSet<String> knownParams = new HashSet<String>();
        if (mandatory != null) Collections.addAll(knownParams, mandatory);
        if (optional != null) Collections.addAll(knownParams, optional);
        for (String par: args.keySet()) {
            if (!knownParams.contains(par)) {
                Main.warn("Unknown remote control parameter {0}, skipping it", par);
            }
        }
        if (error) {
            throw new RequestHandlerBadRequestException(
                    "The following keys are mandatory, but have not been provided: "
                    + Utils.join(", ", missingKeys));
        }
        
    }

    /**
     * Save command associated with this handler.
     *
     * @param command The command.
     */
    public void setCommand(String command)
    {
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        myCommand = command;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    protected boolean isLoadInNewLayer() {
        return args.get("new_layer") != null && !args.get("new_layer").isEmpty()
                ? Boolean.parseBoolean(args.get("new_layer"))
                : Main.pref.getBoolean(loadInNewLayerKey, loadInNewLayerDefault);
    }

    protected final String decodeParam(String param) {
        try {
            return URLDecoder.decode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public static class RequestHandlerException extends Exception {

        public RequestHandlerException(String message) {
            super(message);
        }
        public RequestHandlerException(String message, Throwable cause) {
            super(message, cause);
        }
        public RequestHandlerException(Throwable cause) {
            super(cause);
        }
        public RequestHandlerException() {
        }
    }

    public static class RequestHandlerErrorException extends RequestHandlerException {
        public RequestHandlerErrorException(Throwable cause) {
            super(cause);
        }
    }

    public static class RequestHandlerBadRequestException extends RequestHandlerException {

        public RequestHandlerBadRequestException(String message) {
            super(message);
        }
        public RequestHandlerBadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class RequestHandlerForbiddenException extends RequestHandlerException {
        private static final long serialVersionUID = 2263904699747115423L;

        public RequestHandlerForbiddenException(String message) {
            super(message);
        }
    }
}
