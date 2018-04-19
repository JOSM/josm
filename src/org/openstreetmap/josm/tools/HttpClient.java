// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.ProgressInputStream;
import org.openstreetmap.josm.io.ProgressOutputStream;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Provides a uniform access for a HTTP/HTTPS server. This class should be used in favour of {@link HttpURLConnection}.
 * @since 9168
 */
public final class HttpClient {

    private URL url;
    private final String requestMethod;
    private int connectTimeout = (int) TimeUnit.SECONDS.toMillis(Config.getPref().getInt("socket.timeout.connect", 15));
    private int readTimeout = (int) TimeUnit.SECONDS.toMillis(Config.getPref().getInt("socket.timeout.read", 30));
    private byte[] requestBody;
    private long ifModifiedSince;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private int maxRedirects = Config.getPref().getInt("socket.maxredirects", 5);
    private boolean useCache;
    private String reasonForRequest;
    private String outputMessage = tr("Uploading data ...");
    private HttpURLConnection connection; // to allow disconnecting before `response` is set
    private Response response;
    private boolean finishOnCloseOutput = true;

    // Pattern to detect Tomcat error message. Be careful with change of format:
    // CHECKSTYLE.OFF: LineLength
    // https://svn.apache.org/viewvc/tomcat/trunk/java/org/apache/catalina/valves/ErrorReportValve.java?r1=1740707&r2=1779641&pathrev=1779641&diff_format=h
    // CHECKSTYLE.ON: LineLength
    private static final Pattern TOMCAT_ERR_MESSAGE = Pattern.compile(
        ".*<p><b>[^<]+</b>[^<]+</p><p><b>[^<]+</b> (?:<u>)?([^<]*)(?:</u>)?</p><p><b>[^<]+</b> (?:<u>)?[^<]*(?:</u>)?</p>.*",
        Pattern.CASE_INSENSITIVE);

    static {
        try {
            CookieHandler.setDefault(new CookieManager());
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to set default cookie handler", e);
        }
    }

    private HttpClient(URL url, String requestMethod) {
        this.url = url;
        this.requestMethod = requestMethod;
        this.headers.put("Accept-Encoding", "gzip");
    }

    /**
     * Opens the HTTP connection.
     * @return HTTP response
     * @throws IOException if any I/O error occurs
     */
    public Response connect() throws IOException {
        return connect(null);
    }

    /**
     * Opens the HTTP connection.
     * @param progressMonitor progress monitor
     * @return HTTP response
     * @throws IOException if any I/O error occurs
     * @since 9179
     */
    public Response connect(ProgressMonitor progressMonitor) throws IOException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        this.connection = connection;
        connection.setRequestMethod(requestMethod);
        connection.setRequestProperty("User-Agent", Version.getInstance().getFullAgentString());
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setInstanceFollowRedirects(false); // we do that ourselves
        if (ifModifiedSince > 0) {
            connection.setIfModifiedSince(ifModifiedSince);
        }
        connection.setUseCaches(useCache);
        if (!useCache) {
            connection.setRequestProperty("Cache-Control", "no-cache");
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getValue() != null) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        progressMonitor.beginTask(tr("Contacting Server..."), 1);
        progressMonitor.indeterminateSubTask(null);

        if ("PUT".equals(requestMethod) || "POST".equals(requestMethod) || "DELETE".equals(requestMethod)) {
            Logging.info("{0} {1} ({2}) ...", requestMethod, url, Utils.getSizeString(requestBody.length, Locale.getDefault()));
            if (Logging.isTraceEnabled() && requestBody.length > 0) {
                Logging.trace("BODY: {0}", new String(requestBody, StandardCharsets.UTF_8));
            }
            connection.setFixedLengthStreamingMode(requestBody.length);
            connection.setDoOutput(true);
            try (OutputStream out = new BufferedOutputStream(
                    new ProgressOutputStream(connection.getOutputStream(), requestBody.length,
                            progressMonitor, outputMessage, finishOnCloseOutput))) {
                out.write(requestBody);
            }
        }

        boolean successfulConnection = false;
        try {
            try {
                connection.connect();
                final boolean hasReason = reasonForRequest != null && !reasonForRequest.isEmpty();
                Logging.info("{0} {1}{2} -> {3}{4}",
                        requestMethod, url, hasReason ? (" (" + reasonForRequest + ')') : "",
                        connection.getResponseCode(),
                        connection.getContentLengthLong() > 0
                                ? (" (" + Utils.getSizeString(connection.getContentLengthLong(), Locale.getDefault()) + ')')
                                : ""
                );
                if (Logging.isDebugEnabled()) {
                    Logging.debug("RESPONSE: {0}", connection.getHeaderFields());
                }
                if (DefaultAuthenticator.getInstance().isEnabled() && connection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    DefaultAuthenticator.getInstance().addFailedCredentialHost(url.getHost());
                }
            } catch (IOException | IllegalArgumentException | NoSuchElementException e) {
                Logging.info("{0} {1} -> !!!", requestMethod, url);
                Logging.warn(e);
                //noinspection ThrowableResultOfMethodCallIgnored
                Main.addNetworkError(url, Utils.getRootCause(e));
                throw e;
            }
            if (isRedirect(connection.getResponseCode())) {
                final String redirectLocation = connection.getHeaderField("Location");
                if (redirectLocation == null) {
                    /* I18n: argument is HTTP response code */
                    throw new IOException(tr("Unexpected response from HTTP server. Got {0} response without ''Location'' header." +
                            " Can''t redirect. Aborting.", connection.getResponseCode()));
                } else if (maxRedirects > 0) {
                    url = new URL(url, redirectLocation);
                    maxRedirects--;
                    Logging.info(tr("Download redirected to ''{0}''", redirectLocation));
                    return connect();
                } else if (maxRedirects == 0) {
                    String msg = tr("Too many redirects to the download URL detected. Aborting.");
                    throw new IOException(msg);
                }
            }
            response = new Response(connection, progressMonitor);
            successfulConnection = true;
            return response;
        } finally {
            if (!successfulConnection) {
                connection.disconnect();
            }
        }
    }

    /**
     * Returns the HTTP response which is set only after calling {@link #connect()}.
     * Calling this method again, returns the identical object (unless another {@link #connect()} is performed).
     *
     * @return the HTTP response
     * @since 9309
     */
    public Response getResponse() {
        return response;
    }

    /**
     * A wrapper for the HTTP response.
     */
    public static final class Response {
        private final HttpURLConnection connection;
        private final ProgressMonitor monitor;
        private final int responseCode;
        private final String responseMessage;
        private boolean uncompress;
        private boolean uncompressAccordingToContentDisposition;
        private String responseData;

        private Response(HttpURLConnection connection, ProgressMonitor monitor) throws IOException {
            CheckParameterUtil.ensureParameterNotNull(connection, "connection");
            CheckParameterUtil.ensureParameterNotNull(monitor, "monitor");
            this.connection = connection;
            this.monitor = monitor;
            this.responseCode = connection.getResponseCode();
            this.responseMessage = connection.getResponseMessage();
            if (this.responseCode >= 300) {
                String contentType = getContentType();
                if (contentType == null || (
                        contentType.contains("text") ||
                        contentType.contains("html") ||
                        contentType.contains("xml"))
                        ) {
                    String content = this.fetchContent();
                    if (content.isEmpty()) {
                        Logging.debug("Server did not return any body");
                    } else {
                        Logging.debug("Response body: ");
                        Logging.debug(this.fetchContent());
                    }
                } else {
                    Logging.debug("Server returned content: {0} of length: {1}. Not printing.", contentType, this.getContentLength());
                }
            }
        }

        /**
         * Sets whether {@link #getContent()} should uncompress the input stream if necessary.
         *
         * @param uncompress whether the input stream should be uncompressed if necessary
         * @return {@code this}
         */
        public Response uncompress(boolean uncompress) {
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
        public Response uncompressAccordingToContentDisposition(boolean uncompressAccordingToContentDisposition) {
            this.uncompressAccordingToContentDisposition = uncompressAccordingToContentDisposition;
            return this;
        }

        /**
         * Returns the URL.
         * @return the URL
         * @see HttpURLConnection#getURL()
         * @since 9172
         */
        public URL getURL() {
            return connection.getURL();
        }

        /**
         * Returns the request method.
         * @return the HTTP request method
         * @see HttpURLConnection#getRequestMethod()
         * @since 9172
         */
        public String getRequestMethod() {
            return connection.getRequestMethod();
        }

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
        public InputStream getContent() throws IOException {
            InputStream in;
            try {
                in = connection.getInputStream();
            } catch (IOException ioe) {
                Logging.debug(ioe);
                in = Optional.ofNullable(connection.getErrorStream()).orElseGet(() -> new ByteArrayInputStream(new byte[]{}));
            }
            in = new ProgressInputStream(in, getContentLength(), monitor);
            in = "gzip".equalsIgnoreCase(getContentEncoding()) ? new GZIPInputStream(in) : in;
            Compression compression = Compression.NONE;
            if (uncompress) {
                final String contentType = getContentType();
                Logging.debug("Uncompressing input stream according to Content-Type header: {0}", contentType);
                compression = Compression.forContentType(contentType);
            }
            if (uncompressAccordingToContentDisposition && Compression.NONE.equals(compression)) {
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

        /**
         * Returns {@link #getContent()} wrapped in a buffered reader.
         *
         * Detects Unicode charset in use utilizing {@link UTFInputStreamReader}.
         * @return buffered reader
         * @throws IOException if any I/O error occurs
         */
        public BufferedReader getContentReader() throws IOException {
            return new BufferedReader(
                    UTFInputStreamReader.create(getContent())
            );
        }

        /**
         * Fetches the HTTP response as String.
         * @return the response
         * @throws IOException if any I/O error occurs
         */
        public synchronized String fetchContent() throws IOException {
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
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * Gets the response message from this HTTP connection.
         * @return HTTP response message
         *
         * @see HttpURLConnection#getResponseMessage()
         * @since 9172
         */
        public String getResponseMessage() {
            return responseMessage;
        }

        /**
         * Returns the {@code Content-Encoding} header.
         * @return {@code Content-Encoding} HTTP header
         * @see HttpURLConnection#getContentEncoding()
         */
        public String getContentEncoding() {
            return connection.getContentEncoding();
        }

        /**
         * Returns the {@code Content-Type} header.
         * @return {@code Content-Type} HTTP header
         */
        public String getContentType() {
            return connection.getHeaderField("Content-Type");
        }

        /**
         * Returns the {@code Expire} header.
         * @return {@code Expire} HTTP header
         * @see HttpURLConnection#getExpiration()
         * @since 9232
         */
        public long getExpiration() {
            return connection.getExpiration();
        }

        /**
         * Returns the {@code Last-Modified} header.
         * @return {@code Last-Modified} HTTP header
         * @see HttpURLConnection#getLastModified()
         * @since 9232
         */
        public long getLastModified() {
            return connection.getLastModified();
        }

        /**
         * Returns the {@code Content-Length} header.
         * @return {@code Content-Length} HTTP header
         * @see HttpURLConnection#getContentLengthLong()
         */
        public long getContentLength() {
            return connection.getContentLengthLong();
        }

        /**
         * Returns the value of the named header field.
         * @param name the name of a header field
         * @return the value of the named header field, or {@code null} if there is no such field in the header
         * @see HttpURLConnection#getHeaderField(String)
         * @since 9172
         */
        public String getHeaderField(String name) {
            return connection.getHeaderField(name);
        }

        /**
         * Returns an unmodifiable Map mapping header keys to a List of header values.
         * As per RFC 2616, section 4.2 header names are case insensitive, so returned map is also case insensitive
         * @return unmodifiable Map mapping header keys to a List of header values
         * @see HttpURLConnection#getHeaderFields()
         * @since 9232
         */
        public Map<String, List<String>> getHeaderFields() {
            // returned map from HttpUrlConnection is case sensitive, use case insensitive TreeMap to conform to RFC 2616
            Map<String, List<String>> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Entry<String, List<String>> e: connection.getHeaderFields().entrySet()) {
                if (e.getKey() != null) {
                    ret.put(e.getKey(), e.getValue());
                }
            }
            return Collections.unmodifiableMap(ret);
        }

        /**
         * @see HttpURLConnection#disconnect()
         */
        public void disconnect() {
            HttpClient.disconnect(connection);
        }
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
        return new HttpClient(url, requestMethod);
    }

    /**
     * Returns the URL set for this connection.
     * @return the URL
     * @see #create(URL)
     * @see #create(URL, String)
     * @since 9172
     */
    public URL getURL() {
        return url;
    }

    /**
     * Returns the request method set for this connection.
     * @return the HTTP request method
     * @see #create(URL, String)
     * @since 9172
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * Returns the set value for the given {@code header}.
     * @param header HTTP header name
     * @return HTTP header value
     * @since 9172
     */
    public String getRequestHeader(String header) {
        return headers.get(header);
    }

    /**
     * Sets whether not to set header {@code Cache-Control=no-cache}
     *
     * @param useCache whether not to set header {@code Cache-Control=no-cache}
     * @return {@code this}
     * @see HttpURLConnection#setUseCaches(boolean)
     */
    public HttpClient useCache(boolean useCache) {
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
    public HttpClient keepAlive(boolean keepAlive) {
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
    public HttpClient setConnectTimeout(int connectTimeout) {
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
    public HttpClient setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Sets the {@code Accept} header.
     * @param accept header value
     *
     * @return {@code this}
     */
    public HttpClient setAccept(String accept) {
        return setHeader("Accept", accept);
    }

    /**
     * Sets the request body for {@code PUT}/{@code POST} requests.
     * @param requestBody request body
     *
     * @return {@code this}
     */
    public HttpClient setRequestBody(byte[] requestBody) {
        this.requestBody = Utils.copyArray(requestBody);
        return this;
    }

    /**
     * Sets the {@code If-Modified-Since} header.
     * @param ifModifiedSince header value
     *
     * @return {@code this}
     */
    public HttpClient setIfModifiedSince(long ifModifiedSince) {
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
    public HttpClient setMaxRedirects(int maxRedirects) {
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
    public HttpClient setHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Sets arbitrary HTTP headers.
     * @param headers HTTP headers
     *
     * @return {@code this}
     */
    public HttpClient setHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Sets a reason to show on console. Can be {@code null} if no reason is given.
     * @param reasonForRequest Reason to show
     * @return {@code this}
     * @since 9172
     */
    public HttpClient setReasonForRequest(String reasonForRequest) {
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
    public HttpClient setOutputMessage(String outputMessage) {
        this.outputMessage = outputMessage;
        return this;
    }

    /**
     * Sets whether the progress monitor task will be finished when the output stream is closed. This is {@code true} by default.
     * @param finishOnCloseOutput whether the progress monitor task will be finished when the output stream is closed
     * @return {@code this}
     * @since 10302
     */
    public HttpClient setFinishOnCloseOutput(boolean finishOnCloseOutput) {
        this.finishOnCloseOutput = finishOnCloseOutput;
        return this;
    }

    private static boolean isRedirect(final int statusCode) {
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
     * @see HttpURLConnection#disconnect()
     * @since 9309
     */
    public void disconnect() {
        HttpClient.disconnect(connection);
    }

    private static void disconnect(final HttpURLConnection connection) {
        if (connection != null) {
            // Fix upload aborts - see #263
            connection.setConnectTimeout(100);
            connection.setReadTimeout(100);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logging.warn("InterruptedException in " + HttpClient.class + " during cancel");
                Thread.currentThread().interrupt();
            }
            connection.disconnect();
        }
    }

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
