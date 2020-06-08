// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is the parent of all classes that handle a specific remote control command
 *
 * @author Bodo Meissner
 */
public abstract class RequestHandler {

    /** preference to determine if all Remote Control actions must be confirmed manually */
    public static final BooleanProperty GLOBAL_CONFIRMATION = new BooleanProperty("remotecontrol.always-confirm", false);
    /** preference to determine if remote control loads data in a new layer */
    public static final BooleanProperty LOAD_IN_NEW_LAYER = new BooleanProperty("remotecontrol.new-layer", false);

    protected static final Pattern SPLITTER_COMMA = Pattern.compile(",\\s*");
    protected static final Pattern SPLITTER_SEMIC = Pattern.compile(";\\s*");

    /** past confirmations */
    protected static final PermissionCache PERMISSIONS = new PermissionCache();

    /** The GET request arguments */
    protected Map<String, String> args;

    /** The request URL without "GET". */
    protected String request;

    /** default response */
    protected String content = "OK\r\n";
    /** default content type */
    protected String contentType = "text/plain";

    /** will be filled with the command assigned to the subclass */
    protected String myCommand;

    /**
     * who sent the request?
     * the host from referer header or IP of request sender
     */
    protected String sender;

    /**
     * Check permission and parameters and handle request.
     *
     * @throws RequestHandlerForbiddenException if request is forbidden by preferences
     * @throws RequestHandlerBadRequestException if request is invalid
     * @throws RequestHandlerErrorException if an error occurs while processing request
     */
    public final void handle() throws RequestHandlerForbiddenException, RequestHandlerBadRequestException, RequestHandlerErrorException {
        checkMandatoryParams();
        validateRequest();
        checkPermission();
        handleRequest();
    }

    /**
     * Validates the request before attempting to perform it.
     * @throws RequestHandlerBadRequestException if request is invalid
     * @since 5678
     */
    protected abstract void validateRequest() throws RequestHandlerBadRequestException;

    /**
     * Handle a specific command sent as remote control.
     *
     * This method of the subclass will do the real work.
     *
     * @throws RequestHandlerErrorException if an error occurs while processing request
     * @throws RequestHandlerBadRequestException if request is invalid
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
    public abstract String getPermissionMessage();

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
    public abstract PermissionPrefWithDefault getPermissionPref();

    /**
     * Returns the mandatory parameters. Both used to enfore their presence at runtime and for documentation.
     * @return the mandatory parameters
     */
    public abstract String[] getMandatoryParams();

    /**
     * Returns the optional parameters. Both used to enfore their presence at runtime and for documentation.
     * @return the optional parameters
     */
    public String[] getOptionalParams() {
        return new String[0];
    }

    /**
     * Returns usage description, for bad requests and documentation.
     * @return usage description
     */
    public String getUsage() {
        return null;
    }

    /**
     * Returns usage examples, for bad requests and documentation.
     * @return Usage examples
     */
    public String[] getUsageExamples() {
        return new String[0];
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
     * Check permissions in preferences and display error message or ask for permission.
     *
     * @throws RequestHandlerForbiddenException if request is forbidden by preferences
     */
    public final void checkPermission() throws RequestHandlerForbiddenException {
        /*
         * If the subclass defines a specific preference and if this is set
         * to false, abort with an error message.
         *
         * Note: we use the deprecated class here for compatibility with
         * older versions of WMSPlugin.
         */
        PermissionPrefWithDefault permissionPref = getPermissionPref();
        if (permissionPref != null && permissionPref.pref != null &&
                !Config.getPref().getBoolean(permissionPref.pref, permissionPref.defaultVal)) {
            String err = MessageFormat.format("RemoteControl: ''{0}'' forbidden by preferences", myCommand);
            Logging.info(err);
            throw new RequestHandlerForbiddenException(err);
        }

        /*
         * Did the user confirm this action previously?
         * If yes, skip the global confirmation dialog.
         */
        if (PERMISSIONS.isAllowed(myCommand, sender)) {
            return;
        }

        /* Does the user want to confirm everything?
         * If yes, display specific confirmation message.
         */
        if (GLOBAL_CONFIRMATION.get()) {
            // Ensure dialog box does not exceed main window size
            Integer maxWidth = (int) Math.max(200, MainApplication.getMainFrame().getWidth()*0.6);
            String message = "<html><div>" + getPermissionMessage() +
                    "<br/>" + tr("Do you want to allow this?") + "</div></html>";
            JLabel label = new JLabel(message);
            if (label.getPreferredSize().width > maxWidth) {
                label.setText(message.replaceFirst("<div>", "<div style=\"width:" + maxWidth + "px;\">"));
            }
            Object[] choices = {tr("Yes, always"), tr("Yes, once"), tr("No")};
            int choice = JOptionPane.showOptionDialog(MainApplication.getMainFrame(), label, tr("Confirm Remote Control action"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, choices[1]);
            if (choice != JOptionPane.YES_OPTION && choice != JOptionPane.NO_OPTION) { // Yes/no refer to always/once
                String err = MessageFormat.format("RemoteControl: ''{0}'' forbidden by user''s choice", myCommand);
                throw new RequestHandlerForbiddenException(err);
            } else if (choice == JOptionPane.YES_OPTION) {
                PERMISSIONS.allow(myCommand, sender);
            }
        }
    }

    /**
     * Set request URL and parse args.
     *
     * @param url The request URL.
     * @throws RequestHandlerBadRequestException if request URL is invalid
     */
    public void setUrl(String url) throws RequestHandlerBadRequestException {
        this.request = url;
        try {
            parseArgs();
        } catch (URISyntaxException e) {
            throw new RequestHandlerBadRequestException(e);
        }
    }

    /**
     * Parse the request parameters as key=value pairs.
     * The result will be stored in {@code this.args}.
     *
     * Can be overridden by subclass.
     * @throws URISyntaxException if request URL is invalid
     */
    protected void parseArgs() throws URISyntaxException {
        this.args = getRequestParameter(new URI(this.request));
    }

    protected final String[] splitArg(String arg, Pattern splitter) {
        return splitter.split(args != null ? args.get(arg) : "");
    }

    /**
     * Returns the request parameters.
     * @param uri URI as string
     * @return map of request parameters
     * @see <a href="http://blog.lunatech.com/2009/02/03/what-every-web-developer-must-know-about-url-encoding">
     *      What every web developer must know about URL encoding</a>
     */
    static Map<String, String> getRequestParameter(URI uri) {
        Map<String, String> r = new HashMap<>();
        if (uri.getRawQuery() == null) {
            return r;
        }
        for (String kv : uri.getRawQuery().split("&")) {
            final String[] kvs = Utils.decodeUrl(kv).split("=", 2);
            r.put(kvs[0], kvs.length > 1 ? kvs[1] : null);
        }
        return r;
    }

    void checkMandatoryParams() throws RequestHandlerBadRequestException {
        String[] mandatory = getMandatoryParams();
        String[] optional = getOptionalParams();
        List<String> missingKeys = new LinkedList<>();
        boolean error = false;
        if (mandatory != null && args != null) {
            for (String key : mandatory) {
                String value = args.get(key);
                if (value == null || value.isEmpty()) {
                    error = true;
                    Logging.warn('\'' + myCommand + "' remote control request must have '" + key + "' parameter");
                    missingKeys.add(key);
                }
            }
        }
        Set<String> knownParams = new HashSet<>();
        if (mandatory != null)
            Collections.addAll(knownParams, mandatory);
        if (optional != null)
            Collections.addAll(knownParams, optional);
        if (args != null) {
            for (String par: args.keySet()) {
                if (!knownParams.contains(par)) {
                    Logging.warn("Unknown remote control parameter {0}, skipping it", par);
                }
            }
        }
        if (error) {
            throw new RequestHandlerBadRequestException(
                    tr("The following keys are mandatory, but have not been provided: {0}",
                            String.join(", ", missingKeys)));
        }
    }

    /**
     * Save command associated with this handler.
     *
     * @param command The command.
     */
    public void setCommand(String command) {
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        myCommand = command;
    }

    /**
     * Returns the response content.
     * @return the response content
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the response content type.
     * @return the response content type
     */
    public String getContentType() {
        return contentType;
    }

    private <T> T get(String key, Function<String, T> parser, Supplier<T> defaultSupplier) {
        String val = args.get(key);
        return val != null && !val.isEmpty() ? parser.apply(val) : defaultSupplier.get();
    }

    private boolean get(String key) {
        return get(key, Boolean::parseBoolean, () -> Boolean.FALSE);
    }

    private boolean isLoadInNewLayer() {
        return get("new_layer", Boolean::parseBoolean, LOAD_IN_NEW_LAYER::get);
    }

    protected DownloadParams getDownloadParams() {
        DownloadParams result = new DownloadParams();
        if (args != null) {
            result = result
                .withNewLayer(isLoadInNewLayer())
                .withLayerName(args.get("layer_name"))
                .withLocked(get("layer_locked"))
                .withDownloadPolicy(get("download_policy", DownloadPolicy::of, () -> DownloadPolicy.NORMAL))
                .withUploadPolicy(get("upload_policy", UploadPolicy::of, () -> UploadPolicy.NORMAL));
        }
        return result;
    }

    protected void validateDownloadParams() throws RequestHandlerBadRequestException {
        try {
            getDownloadParams();
        } catch (IllegalArgumentException e) {
            throw new RequestHandlerBadRequestException(e);
        }
    }

    /**
     * Sets who sent the request (the host from referer header or IP of request sender)
     * @param sender the host from referer header or IP of request sender
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * Base exception of remote control handler errors.
     */
    public static class RequestHandlerException extends Exception {

        /**
         * Constructs a new {@code RequestHandlerException}.
         * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         */
        public RequestHandlerException(String message) {
            super(message);
        }

        /**
         * Constructs a new {@code RequestHandlerException}.
         * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
         */
        public RequestHandlerException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new {@code RequestHandlerException}.
         * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
         */
        public RequestHandlerException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Error raised when a runtime error occurred.
     */
    public static class RequestHandlerErrorException extends RequestHandlerException {

        /**
         * Constructs a new {@code RequestHandlerErrorException}.
         * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
         */
        public RequestHandlerErrorException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Error raised for bad requests.
     */
    public static class RequestHandlerBadRequestException extends RequestHandlerException {

        /**
         * Constructs a new {@code RequestHandlerBadRequestException}.
         * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         */
        public RequestHandlerBadRequestException(String message) {
            super(message);
        }

        /**
         * Constructs a new {@code RequestHandlerBadRequestException}.
         * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
         */
        public RequestHandlerBadRequestException(Throwable cause) {
            super(cause);
        }

        /**
         * Constructs a new {@code RequestHandlerBadRequestException}.
         * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
         */
        public RequestHandlerBadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Error raised for forbidden usage.
     */
    public static class RequestHandlerForbiddenException extends RequestHandlerException {

        /**
         * Constructs a new {@code RequestHandlerForbiddenException}.
         * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         */
        public RequestHandlerForbiddenException(String message) {
            super(message);
        }
    }

    /**
     * Handler that takes an URL as parameter.
     */
    public abstract static class RawURLParseRequestHandler extends RequestHandler {
        @Override
        protected void parseArgs() throws URISyntaxException {
            Map<String, String> args = new HashMap<>();
            if (request.indexOf('?') != -1) {
                String query = request.substring(request.indexOf('?') + 1);
                if (query.indexOf("url=") == 0) {
                    args.put("url", Utils.decodeUrl(query.substring(4)));
                } else {
                    int urlIdx = query.indexOf("&url=");
                    if (urlIdx != -1) {
                        args.put("url", Utils.decodeUrl(query.substring(urlIdx + 5)));
                        query = query.substring(0, urlIdx);
                    } else if (query.indexOf('#') != -1) {
                        query = query.substring(0, query.indexOf('#'));
                    }
                    String[] params = query.split("&", -1);
                    for (String param : params) {
                        int eq = param.indexOf('=');
                        if (eq != -1) {
                            args.put(param.substring(0, eq), Utils.decodeUrl(param.substring(eq + 1)));
                        }
                    }
                }
            }
            this.args = args;
        }
    }

    static class PermissionCache {
        private final Set<Pair<String, String>> allowed = new HashSet<>();

        public void allow(String command, String sender) {
            allowed.add(Pair.create(command, sender));
        }

        public boolean isAllowed(String command, String sender) {
            return allowed.contains(Pair.create(command, sender));
        }

        public void clear() {
            allowed.clear();
        }
    }
}
