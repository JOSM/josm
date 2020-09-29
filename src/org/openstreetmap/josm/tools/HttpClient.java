// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.openstreetmap.josm.data.validation.routines.DomainValidator;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.ProgressInputStream;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Provides a uniform access for a HTTP/HTTPS server. This class should be used in favour of {@link HttpURLConnection}.
 * @since 9168
 */
public abstract class HttpClient {

    /**
     * HTTP client factory.
     * @since 15229
     */
    @FunctionalInterface
    public interface HttpClientFactory {
        /**
         * Creates a new instance for the given URL and a {@code GET} request
         *
         * @param url the URL
         * @param requestMethod the HTTP request method to perform when calling
         * @return a new instance
         */
        HttpClient create(URL url, String requestMethod);
    }

    private URL url;
    private final String requestMethod;
    private int connectTimeout = (int) TimeUnit.SECONDS.toMillis(Config.getPref().getInt("socket.timeout.connect", 15));
    private int readTimeout = (int) TimeUnit.SECONDS.toMillis(Config.getPref().getInt("socket.timeout.read", 30));
    private byte[] requestBody;
    private long ifModifiedSince;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private int maxRedirects = Config.getPref().getInt("socket.maxredirects", 5);
    private boolean useCache = true;
    private String reasonForRequest;
    private String outputMessage = tr("Uploading data ...");
    private Response response;
    private boolean finishOnCloseOutput = true;
    private boolean debug;

    // Pattern to detect Tomcat error message. Be careful with change of format:
    // CHECKSTYLE.OFF: LineLength
    // https://svn.apache.org/viewvc/tomcat/trunk/java/org/apache/catalina/valves/ErrorReportValve.java?r1=1740707&r2=1779641&pathrev=1779641&diff_format=h
    // CHECKSTYLE.ON: LineLength
    private static final Pattern TOMCAT_ERR_MESSAGE = Pattern.compile(
        ".*<p><b>[^<]+</b>[^<]+</p><p><b>[^<]+</b> (?:<u>)?([^<]*)(?:</u>)?</p><p><b>[^<]+</b> (?:<u>)?[^<]*(?:</u>)?</p>.*",
        Pattern.CASE_INSENSITIVE);

    private static HttpClientFactory factory;

    static {
        try {
            CookieHandler.setDefault(new CookieManager());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to set default cookie handler", e);
        }
    }

    /**
     * Registers a new HTTP client factory.
     * @param newFactory new HTTP client factory
     * @since 15229
     */
    public static void setFactory(HttpClientFactory newFactory) {
        factory = Objects.requireNonNull(newFactory);
    }

    /**
     * Constructs a new {@code HttpClient}.
     * @param url URL to access
     * @param requestMethod HTTP request method (GET, POST, PUT, DELETE...)
     */
    protected HttpClient(URL url, String requestMethod) {
        try {
            String host = url.getHost();
            String asciiHost = DomainValidator.unicodeToASCII(host);
            this.url = asciiHost.equals(host) ? url : new URL(url.getProtocol(), asciiHost, url.getPort(), url.getFile());
        } catch (MalformedURLException e) {
            throw new JosmRuntimeException(e);
        }
        this.requestMethod = requestMethod;
        this.headers.put("Accept-Encoding", "gzip, deflate");
    }

    /**
     * Opens the HTTP connection.
     * @return HTTP response
     * @throws IOException if any I/O error occurs
     */
    public final Response connect() throws IOException {
        return connect(null);
    }

    /**
     * Opens the HTTP connection.
     * @param progressMonitor progress monitor
     * @return HTTP response
     * @throws IOException if any I/O error occurs
     * @since 9179
     */
    public final Response connect(ProgressMonitor progressMonitor) throws IOException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        setupConnection(progressMonitor);

        boolean successfulConnection = false;
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            ConnectionResponse cr;
            try {
                if (Logging.isDebugEnabled()) {
                    Logging.debug("REQUEST HEADERS: {0}", headers);
                }
                cr = performConnection();
                final boolean hasReason = reasonForRequest != null && !reasonForRequest.isEmpty();
                logRequest("{0} {1}{2} -> {3} {4} ({5}{6})",
                        getRequestMethod(), getURL(), hasReason ? (" (" + reasonForRequest + ')') : "",
                        cr.getResponseVersion(), cr.getResponseCode(),
                        stopwatch,
                        cr.getContentLengthLong() > 0
                                ? ("; " + Utils.getSizeString(cr.getContentLengthLong(), Locale.getDefault()))
                                : ""
                );
                if (Logging.isDebugEnabled()) {
                    try {
                        Logging.debug("RESPONSE HEADERS: {0}", cr.getHeaderFields());
                    } catch (IllegalArgumentException e) {
                        Logging.warn(e);
                    }
                }
                if (DefaultAuthenticator.getInstance().isEnabled() && cr.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    DefaultAuthenticator.getInstance().addFailedCredentialHost(url.getHost());
                }
            } catch (IOException | RuntimeException e) {
                logRequest("{0} {1} -> !!! ({2})", requestMethod, url, stopwatch);
                Logging.warn(e);
                //noinspection ThrowableResultOfMethodCallIgnored
                NetworkManager.addNetworkError(url, Utils.getRootCause(e));
                throw e;
            }
            if (isRedirect(cr.getResponseCode())) {
                final String redirectLocation = cr.getHeaderField("Location");
                if (redirectLocation == null) {
                    /* I18n: argument is HTTP response code */
                    throw new IOException(tr("Unexpected response from HTTP server. Got {0} response without ''Location'' header." +
                            " Can''t redirect. Aborting.", cr.getResponseCode()));
                } else if (maxRedirects > 0) {
                    url = new URL(url, redirectLocation);
                    maxRedirects--;
                    logRequest(tr("Download redirected to ''{0}''", redirectLocation));
                    response = connect();
                    successfulConnection = true;
                    return response;
                } else if (maxRedirects == 0) {
                    String msg = tr("Too many redirects to the download URL detected. Aborting.");
                    throw new IOException(msg);
                }
            }
            response = buildResponse(progressMonitor);
            successfulConnection = true;
            return response;
        } finally {
            if (!successfulConnection) {
                performDisconnection();
                progressMonitor.finishTask();
            }
        }
    }

    protected abstract void setupConnection(ProgressMonitor progressMonitor) throws IOException;

    protected abstract ConnectionResponse performConnection() throws IOException;

    protected abstract void performDisconnection() throws IOException;

    protected abstract Response buildResponse(ProgressMonitor progressMonitor) throws IOException;

    protected final void notifyConnect(ProgressMonitor progressMonitor) {
        progressMonitor.beginTask(tr("Contacting Server..."), 1);
        progressMonitor.indeterminateSubTask(null);
    }

    protected final void logRequest(String pattern, Object... args) {
        if (debug) {
            Logging.debug(pattern, args);
        } else {
            Logging.info(pattern, args);
        }
    }

    protected final void logRequestBody() {
        logRequest("{0} {1} ({2}) ...", requestMethod, url, Utils.getSizeString(requestBody.length, Locale.getDefault()));
        if (Logging.isTraceEnabled() && hasRequestBody()) {
            Logging.trace("BODY: {0}", new String(requestBody, StandardCharsets.UTF_8));
        }
    }

    /**
     * Returns the HTTP response which is set only after calling {@link #connect()}.
     * Calling this method again, returns the identical object (unless another {@link #connect()} is performed).
     *
     * @return the HTTP response
     * @since 9309
     */
    public final Response getResponse() {
        return response;
    }

    /**
     * A wrapper for the HTTP connection response.
     * @since 15229
     */
    public interface ConnectionResponse {
        /**
         * Gets the HTTP version from the HTTP response.
         * @return the HTTP version from the HTTP response
         */
        String getResponseVersion();

        /**
         * Gets the status code from an HTTP response message.
         * For example, in the case of the following status lines:
         * <PRE>
         * HTTP/1.0 200 OK
         * HTTP/1.0 401 Unauthorized
         * </PRE>
         * It will return 200 and 401 respectively.
         * Returns -1 if no code can be discerned
         * from the response (i.e., the response is not valid HTTP).
         * @return the HTTP Status-Code, or -1
         * @throws IOException if an error occurred connecting to the server.
         */
        int getResponseCode() throws IOException;

        /**
         * Returns the value of the {@code content-length} header field as a long.
         *
         * @return  the content length of the resource that this connection's URL
         *          references, or {@code -1} if the content length is not known.
         */
        long getContentLengthLong();

        /**
         * Returns an unmodifiable Map of the header fields.
         * The Map keys are Strings that represent the response-header field names.
         * Each Map value is an unmodifiable List of Strings that represents
         * the corresponding field values.
         *
         * @return a Map of header fields
         */
        Map<String, List<String>> getHeaderFields();

        /**
         * Returns the value of the named header field.
         * @param name the name of a header field.
         * @return the value of the named header field, or {@code null}
         *          if there is no such field in the header.
         */
        String getHeaderField(String name);
    }

    /**
     * A wrapper for the HTTP response.
     */
    public abstract static class Response {
        private final ProgressMonitor monitor;
        private final int responseCode;
        private final String responseMessage;
        private boolean uncompress;
        private boolean uncompressAccordingToContentDisposition;
        private String responseData;

        protected Response(ProgressMonitor monitor, int responseCode, String responseMessage) {
            this.monitor = Objects.requireNonNull(monitor, "monitor");
            this.responseCode = responseCode;
            this.responseMessage = responseMessage;
        }

        protected final void debugRedirect() throws IOException {
            if (responseCode >= 300) {
                String contentType = getContentType();
                if (contentType == null ||
                    contentType.contains("text") ||
                    contentType.contains("html") ||
                    contentType.contains("xml")
                ) {
                    String content = fetchContent();
                    Logging.debug(content.isEmpty() ? "Server did not return any body" : "Response body: \n" + content);
                } else {
                    Logging.debug("Server returned content: {0} of length: {1}. Not printing.", contentType, getContentLength());
                }
            }
        }

        /**
         * Sets whether {@link #getContent()} should uncompress the input stream if necessary.
         *
         * @param uncompress whether the input stream should be uncompressed if necessary
         * @return {@code this}
         */
        public final Response uncompress(boolean uncompress) {
            this.uncompress = uncompress;
            return this;
        }

        /**
         * Sets whether {@link #getContent()} should uncompress the input stream according to {@code Content-Disposition}
         * HTTP header.
         * @param uncompressAccordingToContentDisposition whether the input stream should be uncompressed according to
         * {@code Content-Disposition}
         * @return {@code this}
         * @since 9172
         */
        public final Response uncompressAccordingToContentDisposition(boolean uncompressAccordingToContentDisposition) {
            this.uncompressAccordingToContentDisposition = uncompressAccordingToContentDisposition;
            return this;
        }

        /**
         * Returns the URL.
         * @return the URL
         * @see HttpURLConnection#getURL()
         * @since 9172
         */
        public abstract URL getURL();

        /**
         * Returns the request method.
         * @return the HTTP request method
         * @see HttpURLConnection#getRequestMethod()
         * @since 9172
         */
        public abstract String getRequestMethod();

        /**
         * Returns an input stream that reads from this HTTP connection, or,
         * error stream if the connection failed but the server sent useful data.
         * <p>
         * Note: the return value can be null, if both the input and the error stream are null.
         * Seems to be the case if the OSM server replies a 401 Unauthorized, see #3887
         * @return input or error stream
         * @throws IOException if any I/O error occurs
         *
         * @see HttpURLConnection#getInputStream()
         * @see HttpURLConnection#getErrorStream()
         */
        @SuppressWarnings("resource")
        public final InputStream getContent() throws IOException {
            InputStream in = getInputStream();
            in = new ProgressInputStream(in, getContentLength(), monitor);
            in = "gzip".equalsIgnoreCase(getContentEncoding())
                    ? new GZIPInputStream(in)
                    : "deflate".equalsIgnoreCase(getContentEncoding())
                    ? new InflaterInputStream(in)
                    : in;
            Compression compression = Compression.NONE;
            if (uncompress) {
                final String contentType = getContentType();
                Logging.debug("Uncompressing input stream according to Content-Type header: {0}", contentType);
                compression = Compression.forContentType(contentType);
            }
            if (uncompressAccordingToContentDisposition && Compression.NONE == compression) {
                final String contentDisposition = getHeaderField("Content-Disposition");
                final Matcher matcher = Pattern.compile("filename=\"([^\"]+)\"").matcher(
                        contentDisposition != null ? contentDisposition : "");
                if (matcher.find()) {
                    Logging.debug("Uncompressing input stream according to Content-Disposition header: {0}", contentDisposition);
                    compression = Compression.byExtension(matcher.group(1));
                }
            }
            in = compression.getUncompressedInputStream(in);
            return in;
        }

        protected abstract InputStream getInputStream() throws IOException;

        /**
         * Returns {@link #getContent()} wrapped in a buffered reader.
         *
         * Detects Unicode charset in use utilizing {@link UTFInputStreamReader}.
         * @return buffered reader
         * @throws IOException if any I/O error occurs
         */
        public final BufferedReader getContentReader() throws IOException {
            return new BufferedReader(
                    UTFInputStreamReader.create(getContent())
            );
        }

        /**
         * Fetches the HTTP response as String.
         * @return the response
         * @throws IOException if any I/O error occurs
         */
        public final synchronized String fetchContent() throws IOException {
            if (responseData == null) {
                try (Scanner scanner = new Scanner(getContentReader()).useDelimiter("\\A")) { // \A - beginning of input
                    responseData = scanner.hasNext() ? scanner.next() : "";
                }
            }
            return responseData;
        }

        /**
         * Gets the response code from this HTTP connection.
         * @return HTTP response code
         *
         * @see HttpURLConnection#getResponseCode()
         */
        public final int getResponseCode() {
            return responseCode;
        }

        /**
         * Gets the response message from this HTTP connection.
         * @return HTTP response message
         *
         * @see HttpURLConnection#getResponseMessage()
         * @since 9172
         */
        public final String getResponseMessage() {
            return responseMessage;
        }

        /**
         * Returns the {@code Content-Encoding} header.
         * @return {@code Content-Encoding} HTTP header
         * @see HttpURLConnection#getContentEncoding()
         */
        public abstract String getContentEncoding();

        /**
         * Returns the {@code Content-Type} header.
         * @return {@code Content-Type} HTTP header
         * @see HttpURLConnection#getContentType()
         */
        public abstract String getContentType();

        /**
         * Returns the {@code Expire} header.
         * @return {@code Expire} HTTP header
         * @see HttpURLConnection#getExpiration()
         * @since 9232
         */
        public abstract long getExpiration();

        /**
         * Returns the {@code Last-Modified} header.
         * @return {@code Last-Modified} HTTP header
         * @see HttpURLConnection#getLastModified()
         * @since 9232
         */
        public abstract long getLastModified();

        /**
         * Returns the {@code Content-Length} header.
         * @return {@code Content-Length} HTTP header
         * @see HttpURLConnection#getContentLengthLong()
         */
        public abstract long getContentLength();

        /**
         * Returns the value of the named header field.
         * @param name the name of a header field
         * @return the value of the named header field, or {@code null} if there is no such field in the header
         * @see HttpURLConnection#getHeaderField(String)
         * @since 9172
         */
        public abstract String getHeaderField(String name);

        /**
         * Returns an unmodifiable Map mapping header keys to a List of header values.
         * As per RFC 2616, section 4.2 header names are case insensitive, so returned map is also case insensitive
         * @return unmodifiable Map mapping header keys to a List of header values
         * @see HttpURLConnection#getHeaderFields()
         * @since 9232
         */
        public abstract Map<String, List<String>> getHeaderFields();

        /**
         * Indicates that other requests to the server are unlikely in the near future.
         * @see HttpURLConnection#disconnect()
         */
        public abstract void disconnect();
    }

    /**
     * Creates a new instance for the given URL and a {@code GET} request
     *
     * @param url the URL
     * @return a new instance
     */
    public static HttpClient create(URL url) {
        return create(url, "GET");
    }

    /**
     * Creates a new instance for the given URL and a {@code GET} request
     *
     * @param url the URL
     * @param requestMethod the HTTP request method to perform when calling
     * @return a new instance
     */
    public static HttpClient create(URL url, String requestMethod) {
        if (factory == null) {
            throw new IllegalStateException("HTTP factory has not been set");
        }
        return factory.create(url, requestMethod)
                // #18812: specify `Accept=*/*` to prevent Java from adding `Accept=text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2`
                .setAccept("*/*");
    }

    /**
     * Returns the URL set for this connection.
     * @return the URL
     * @see #create(URL)
     * @see #create(URL, String)
     * @since 9172
     */
    public final URL getURL() {
        return url;
    }

    /**
     * Returns the request body set for this connection.
     * @return the HTTP request body, or null
     * @since 15229
     */
    public final byte[] getRequestBody() {
        return Utils.copyArray(requestBody);
    }

    /**
     * Determines if a non-empty request body has been set for this connection.
     * @return {@code true} if the request body is set and non-empty
     * @since 15229
     */
    public final boolean hasRequestBody() {
        return requestBody != null && requestBody.length > 0;
    }

    /**
     * Determines if the underlying HTTP method requires a body.
     * @return {@code true} if the underlying HTTP method requires a body
     * @since 15229
     */
    public final boolean requiresBody() {
        return "PUT".equals(requestMethod) || "POST".equals(requestMethod) || "DELETE".equals(requestMethod);
    }

    /**
     * Returns the request method set for this connection.
     * @return the HTTP request method
     * @see #create(URL, String)
     * @since 9172
     */
    public final String getRequestMethod() {
        return requestMethod;
    }

    /**
     * Returns the set value for the given {@code header}.
     * @param header HTTP header name
     * @return HTTP header value
     * @since 9172
     */
    public final String getRequestHeader(String header) {
        return headers.get(header);
    }

    /**
     * Returns the connect timeout.
     * @return the connect timeout, in milliseconds
     * @since 15229
     */
    public final int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Returns the read timeout.
     * @return the read timeout, in milliseconds
     * @since 15229
     */
    public final int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Returns the {@code If-Modified-Since} header value.
     * @return the {@code If-Modified-Since} header value
     * @since 15229
     */
    public final long getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * Determines whether not to set header {@code Cache-Control=no-cache}.
     * By default, {@code useCache} is true, i.e., the header {@code Cache-Control=no-cache} is not sent.
     *
     * @return whether not to set header {@code Cache-Control=no-cache}
     * @since 15229
     */
    public final boolean isUseCache() {
        return useCache;
    }

    /**
     * Returns the headers.
     * @return the headers
     * @since 15229
     */
    public final Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the reason for request.
     * @return the reason for request
     * @since 15229
     */
    public final String getReasonForRequest() {
        return reasonForRequest;
    }

    /**
     * Returns the output message.
     * @return the output message
     */
    protected final String getOutputMessage() {
        return outputMessage;
    }

    /**
     * Determines whether the progress monitor task will be finished when the output stream is closed. {@code true} by default.
     * @return the finishOnCloseOutput
     */
    protected final boolean isFinishOnCloseOutput() {
        return finishOnCloseOutput;
    }

    /**
     * Sets whether not to set header {@code Cache-Control=no-cache}.
     * By default, {@code useCache} is true, i.e., the header {@code Cache-Control=no-cache} is not sent.
     *
     * @param useCache whether not to set header {@code Cache-Control=no-cache}
     * @return {@code this}
     * @see HttpURLConnection#setUseCaches(boolean)
     */
    public final HttpClient useCache(boolean useCache) {
        this.useCache = useCache;
        return this;
    }

    /**
     * Sets whether not to set header {@code Connection=close}
     * <p>
     * This might fix #7640, see
     * <a href='https://web.archive.org/web/20140118201501/http://www.tikalk.com/java/forums/httpurlconnection-disable-keep-alive'>here</a>.
     *
     * @param keepAlive whether not to set header {@code Connection=close}
     * @return {@code this}
     */
    public final HttpClient keepAlive(boolean keepAlive) {
        return setHeader("Connection", keepAlive ? null : "close");
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource referenced
     * by this URLConnection. If the timeout expires before the connection can be established, a
     * {@link java.net.SocketTimeoutException} is raised. A timeout of zero is interpreted as an infinite timeout.
     * @param connectTimeout an {@code int} that specifies the connect timeout value in milliseconds
     * @return {@code this}
     * @see HttpURLConnection#setConnectTimeout(int)
     */
    public final HttpClient setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Sets the read timeout to a specified timeout, in milliseconds. A non-zero value specifies the timeout when reading from
     * input stream when a connection is established to a resource. If the timeout expires before there is data available for
     * read, a {@link java.net.SocketTimeoutException} is raised. A timeout of zero is interpreted as an infinite timeout.
     * @param readTimeout an {@code int} that specifies the read timeout value in milliseconds
     * @return {@code this}
     * @see HttpURLConnection#setReadTimeout(int)
     */
    public final HttpClient setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Sets the {@code Accept} header.
     * @param accept header value
     *
     * @return {@code this}
     */
    public final HttpClient setAccept(String accept) {
        return setHeader("Accept", accept);
    }

    /**
     * Sets the request body for {@code PUT}/{@code POST} requests.
     * @param requestBody request body
     *
     * @return {@code this}
     */
    public final HttpClient setRequestBody(byte[] requestBody) {
        this.requestBody = Utils.copyArray(requestBody);
        return this;
    }

    /**
     * Sets the {@code If-Modified-Since} header.
     * @param ifModifiedSince header value
     *
     * @return {@code this}
     */
    public final HttpClient setIfModifiedSince(long ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
        return this;
    }

    /**
     * Sets the maximum number of redirections to follow.
     *
     * Set {@code maxRedirects} to {@code -1} in order to ignore redirects, i.e.,
     * to not throw an {@link IOException} in {@link #connect()}.
     * @param maxRedirects header value
     *
     * @return {@code this}
     */
    public final HttpClient setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    /**
     * Sets an arbitrary HTTP header.
     * @param key header name
     * @param value header value
     *
     * @return {@code this}
     */
    public final HttpClient setHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Sets arbitrary HTTP headers.
     * @param headers HTTP headers
     *
     * @return {@code this}
     */
    public final HttpClient setHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Sets a reason to show on console. Can be {@code null} if no reason is given.
     * @param reasonForRequest Reason to show
     * @return {@code this}
     * @since 9172
     */
    public final HttpClient setReasonForRequest(String reasonForRequest) {
        this.reasonForRequest = reasonForRequest;
        return this;
    }

    /**
     * Sets the output message to be displayed in progress monitor for {@code PUT}, {@code POST} and {@code DELETE} methods.
     * Defaults to "Uploading data ..." (translated). Has no effect for {@code GET} or any other method.
     * @param outputMessage message to be displayed in progress monitor
     * @return {@code this}
     * @since 12711
     */
    public final HttpClient setOutputMessage(String outputMessage) {
        this.outputMessage = outputMessage;
        return this;
    }

    /**
     * Sets whether the progress monitor task will be finished when the output stream is closed. This is {@code true} by default.
     * @param finishOnCloseOutput whether the progress monitor task will be finished when the output stream is closed
     * @return {@code this}
     * @since 10302
     */
    public final HttpClient setFinishOnCloseOutput(boolean finishOnCloseOutput) {
        this.finishOnCloseOutput = finishOnCloseOutput;
        return this;
    }

    /**
     * Sets the connect log at DEBUG level instead of the default INFO level.
     * @param debug {@code true} to set the connect log at DEBUG level
     * @return {@code this}
     * @since 15389
     */
    public final HttpClient setLogAtDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * Determines if the given status code is an HTTP redirection.
     * @param statusCode HTTP status code
     * @return {@code true} if the given status code is an HTTP redirection
     * @since 15423
     */
    public static boolean isRedirect(final int statusCode) {
        switch (statusCode) {
            case HttpURLConnection.HTTP_MOVED_PERM: // 301
            case HttpURLConnection.HTTP_MOVED_TEMP: // 302
            case HttpURLConnection.HTTP_SEE_OTHER: // 303
            case 307: // TEMPORARY_REDIRECT:
            case 308: // PERMANENT_REDIRECT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Disconnect client.
     * @see HttpURLConnection#disconnect()
     * @since 9309
     */
    public abstract void disconnect();

    /**
     * Returns a {@link Matcher} against predefined Tomcat error messages.
     * If it matches, error message can be extracted from {@code group(1)}.
     * @param data HTML contents to check
     * @return a {@link Matcher} against predefined Tomcat error messages
     * @since 13358
     */
    public static Matcher getTomcatErrorMatcher(String data) {
        return data != null ? TOMCAT_ERR_MESSAGE.matcher(data) : null;
    }
}
