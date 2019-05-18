// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Exception thrown when a communication error occurs when accessing the <a href="http://wiki.openstreetmap.org/wiki/API_v0.6">OSM API</a>.
 * @see OsmApi
 */
public class OsmApiException extends OsmTransferException {

    private int responseCode;
    private String contentType;
    private String errorHeader;
    private String errorBody;
    private String accessedUrl;
    private String login;

    /**
     * Constructs an {@code OsmApiException} with the specified response code, error header and error body
     * @param responseCode The HTTP response code replied by the OSM server.
     * See {@link java.net.HttpURLConnection HttpURLConnection} for predefined HTTP response code values
     * @param errorHeader The error header, as transmitted in the {@code Error} field of the HTTP response header
     * @param errorBody The error body, as transmitted in the HTTP response body
     * @param accessedUrl The complete URL accessed when this error occurred
     * @param login the login used to connect to OSM API (can be null)
     * @param contentType the response content-type
     * @since 13499
     */
    public OsmApiException(int responseCode, String errorHeader, String errorBody, String accessedUrl, String login, String contentType) {
        this.responseCode = responseCode;
        this.errorHeader = errorHeader;
        this.errorBody = Utils.strip(errorBody);
        this.accessedUrl = accessedUrl;
        this.login = login;
        this.contentType = contentType;
        checkHtmlBody();
    }

    /**
     * Constructs an {@code OsmApiException} with the specified response code, error header and error body
     * @param responseCode The HTTP response code replied by the OSM server.
     * See {@link java.net.HttpURLConnection HttpURLConnection} for predefined HTTP response code values
     * @param errorHeader The error header, as transmitted in the {@code Error} field of the HTTP response header
     * @param errorBody The error body, as transmitted in the HTTP response body
     * @param accessedUrl The complete URL accessed when this error occurred
     * @param login the login used to connect to OSM API (can be null)
     * @since 12992
     */
    public OsmApiException(int responseCode, String errorHeader, String errorBody, String accessedUrl, String login) {
        this(responseCode, errorHeader, errorBody, accessedUrl, login, null);
    }

    /**
     * Constructs an {@code OsmApiException} with the specified response code, error header and error body
     * @param responseCode The HTTP response code replied by the OSM server.
     * See {@link java.net.HttpURLConnection HttpURLConnection} for predefined HTTP response code values
     * @param errorHeader The error header, as transmitted in the {@code Error} field of the HTTP response header
     * @param errorBody The error body, as transmitted in the HTTP response body
     * @param accessedUrl The complete URL accessed when this error occurred
     * @since 5584
     */
    public OsmApiException(int responseCode, String errorHeader, String errorBody, String accessedUrl) {
        this(responseCode, errorHeader, errorBody, accessedUrl, null);
    }

    /**
     * Constructs an {@code OsmApiException} with the specified response code, error header and error body
     * @param responseCode The HTTP response code replied by the OSM server.
     * See {@link java.net.HttpURLConnection HttpURLConnection} for predefined HTTP response code values
     * @param errorHeader The error header, as transmitted in the {@code Error} field of the HTTP response header
     * @param errorBody The error body, as transmitted in the HTTP response body
     */
    public OsmApiException(int responseCode, String errorHeader, String errorBody) {
        this(responseCode, errorHeader, errorBody, null);
    }

    /**
     * Constructs an {@code OsmApiException} with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     */
    public OsmApiException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code OsmApiException} with the specified cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code>
     * (which typically contains the class and detail message of <code>cause</code>).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause} method).
     *              A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public OsmApiException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an {@code OsmApiException} with the specified detail message and cause.
     *
     * <p> Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated
     * into this exception's detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     *
     */
    public OsmApiException(String message, Throwable cause) {
        super(message, cause);
    }

    private void checkHtmlBody() {
        if (errorBody != null && errorBody.matches("^<.*>.*<.*>$")) {
            setContentType("text/html");
            if (!errorBody.contains("<html>")) {
                errorBody = "<html>" + errorBody + "</html>";
            }
        }
    }

    /**
     * Replies the HTTP response code.
     * @return The HTTP response code replied by the OSM server. Refer to
     * <a href="http://wiki.openstreetmap.org/wiki/API_v0.6">OSM API</a> to see the list of response codes returned by the API for each call.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Sets the HTTP response code.
     * @param responseCode The HTTP response code replied by the OSM server.
     * See {@link java.net.HttpURLConnection HttpURLConnection} for predefined HTTP response code values
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * Replies the error header.
     * @return the error header, as transmitted in the {@code Error} field of the HTTP response header
     */
    public String getErrorHeader() {
        return errorHeader;
    }

    /**
     * Sets the error header.
     * @param errorHeader the error header, as transmitted in the {@code Error} field of the HTTP response header
     */
    public void setErrorHeader(String errorHeader) {
        this.errorHeader = errorHeader;
    }

    /**
     * Replies the error body.
     * @return The error body, as transmitted in the HTTP response body
     */
    public String getErrorBody() {
        return errorBody;
    }

    /**
     * Sets the error body.
     * @param errorBody The error body, as transmitted in the HTTP response body
     */
    public void setErrorBody(String errorBody) {
        this.errorBody = errorBody;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResponseCode=")
        .append(responseCode);
        String eh = "";
        try {
            if (errorHeader != null)
                eh = tr(errorHeader.trim());
            if (!eh.isEmpty()) {
                sb.append(", Error Header=<")
                .append(eh)
                .append('>');
            }
        } catch (IllegalArgumentException e) {
            // Ignored
            Logging.trace(e);
        }
        try {
            String eb = errorBody != null ? tr(errorBody.trim()) : "";
            if (!eb.isEmpty() && !eb.equals(eh)) {
                sb.append(", Error Body=<")
                .append(eb)
                .append('>');
            }
        } catch (IllegalArgumentException e) {
            // Ignored
            Logging.trace(e);
        }
        return sb.toString();
    }

    /**
     * Replies a message suitable to be displayed in a message dialog
     *
     * @return a message which is suitable to be displayed in a message dialog
     */
    public String getDisplayMessage() {
        StringBuilder sb = new StringBuilder();
        String header = Utils.strip(errorHeader);
        String body = Utils.strip(errorBody);
        if ((header == null || header.isEmpty()) && (body == null || body.isEmpty())) {
            sb.append(tr("The server replied an error with code {0}.", responseCode));
        } else {
            if (header != null && !header.isEmpty()) {
                sb.append(tr(header));
            }
            if (body != null && !body.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(". ");
                }
                sb.append(tr(body));
            }
            sb.append(' ').append(tr("(Code={0})", responseCode));
        }
        return sb.toString();
    }

    /**
     * Sets the complete URL accessed when this error occurred.
     * This is distinct from the one set with {@link #setUrl}, which is generally only the base URL of the server.
     * @param url the complete URL accessed when this error occurred.
     */
    public void setAccessedUrl(String url) {
        this.accessedUrl = url;
    }

    /**
     * Replies the complete URL accessed when this error occurred.
     * This is distinct from the one returned by {@link #getUrl}, which is generally only the base URL of the server.
     * @return the complete URL accessed when this error occurred.
     */
    public String getAccessedUrl() {
        return accessedUrl;
    }

    /**
     * Sets the login used to connect to OSM API.
     * @param login the login used to connect to OSM API
     * @since 12992
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Replies the login used to connect to OSM API.
     * @return the login used to connect to OSM API, or {@code null}
     * @since 12992
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the response content-type.
     * @param contentType the response content-type.
     * @since 13499
     */
    public final void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Replies the response content-type.
     * @return the response content-type
     * @since 13499
     */
    public final String getContentType() {
        return contentType;
    }

    /**
     * Determines if the exception has {@code text/html} as content type.
     * @return {@code true} if the exception has {@code text/html} as content type.
     * @since 14810
     */
    public final boolean isHtml() {
        return "text/html".equals(contentType);
    }
}
