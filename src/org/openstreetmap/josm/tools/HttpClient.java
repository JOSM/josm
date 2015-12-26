// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.UTFInputStreamReader;

/**
 * Provides a uniform access for a HTTP/HTTPS server. This class should be used in favour of {@link HttpURLConnection}.
 */
public class HttpClient {

    private URL url;
    private final String requestMethod;
    private int connectTimeout = Main.pref.getInteger("socket.timeout.connect", 15) * 1000;
    private int readTimeout = Main.pref.getInteger("socket.timeout.read", 30) * 1000;
    private byte[] requestBody;
    private long ifModifiedSince;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private int maxRedirects = Main.pref.getInteger("socket.maxredirects", 5);
    private boolean useCache;
    private String reasonForRequest;

    private HttpClient(URL url, String requestMethod) {
        this.url = url;
        this.requestMethod = requestMethod;
        this.headers.put("Accept-Encoding", "gzip");
    }

    public Response connect() throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(requestMethod);
        connection.setRequestProperty("User-Agent", Version.getInstance().getFullAgentString());
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setInstanceFollowRedirects(maxRedirects > 0);
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

        if ("PUT".equals(requestMethod) || "POST".equals(requestMethod) || "DELETE".equals(requestMethod)) {
            headers.put("Content-Length", String.valueOf(requestBody.length));
            connection.setDoOutput(true);
            try (OutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
                out.write(requestBody);
            }
        }

        boolean successfulConnection = false;
        try {
            try {
                connection.connect();
                if (reasonForRequest != null && "".equalsIgnoreCase(reasonForRequest)) {
                    Main.info("{0} {1} ({2}) -> {3}", requestMethod, url, reasonForRequest, connection.getResponseCode());
                } else {
                    Main.info("{0} {1} -> {2}", requestMethod, url, connection.getResponseCode());
                }
                if (Main.isDebugEnabled()) {
                    Main.debug("RESPONSE: " + connection.getHeaderFields());
                }
            } catch (IOException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                Main.addNetworkError(url, Utils.getRootCause(e));
                throw e;
            }
            if (isRedirect(connection.getResponseCode())) {
                final String redirectLocation = connection.getHeaderField("Location");
                if (redirectLocation == null) {
                    /* I18n: argument is HTTP response code */
                    String msg = tr("Unexpected response from HTTP server. Got {0} response without ''Location'' header." +
                            " Can''t redirect. Aborting.", connection.getResponseCode());
                    throw new IOException(msg);
                } else if (maxRedirects > 0) {
                    url = new URL(redirectLocation);
                    maxRedirects--;
                    Main.info(tr("Download redirected to ''{0}''", redirectLocation));
                    return connect();
                } else if (maxRedirects == 0) {
                    String msg = tr("Too many redirects to the download URL detected. Aborting.");
                    throw new IOException(msg);
                }
            }
            Response response = new Response(connection);
            successfulConnection = true;
            return response;
        } finally {
            if (!successfulConnection) {
                connection.disconnect();
            }
        }
    }

    /**
     * A wrapper for the HTTP response.
     */
    public static class Response {
        private final HttpURLConnection connection;
        private final int responseCode;
        private final String responseMessage;
        private boolean uncompress;
        private boolean uncompressAccordingToContentDisposition;

        private Response(HttpURLConnection connection) throws IOException {
            CheckParameterUtil.ensureParameterNotNull(connection, "connection");
            this.connection = connection;
            this.responseCode = connection.getResponseCode();
            this.responseMessage = connection.getResponseMessage();
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

        public Response uncompressAccordingToContentDisposition(boolean uncompressAccordingToContentDisposition) {
            this.uncompressAccordingToContentDisposition = uncompressAccordingToContentDisposition;
            return this;
        }

        /**
         * @see HttpURLConnection#getURL()
         */
        public URL getURL() {
            return connection.getURL();
        }

        /**
         * @see HttpURLConnection#getRequestMethod()
         */
        public String getRequestMethod() {
            return connection.getRequestMethod();
        }

        /**
         * Returns an input stream that reads from this HTTP connection, or,
         * error stream if the connection failed but the server sent useful data.
         *
         * Note: the return value can be null, if both the input and the error stream are null.
         * Seems to be the case if the OSM server replies a 401 Unauthorized, see #3887
         *
         * @see HttpURLConnection#getInputStream()
         * @see HttpURLConnection#getErrorStream()
         */
        public InputStream getContent() throws IOException {
            InputStream in;
            try {
                in = connection.getInputStream();
            } catch (IOException ioe) {
                in = connection.getErrorStream();
            }
            in = "gzip".equalsIgnoreCase(getContentEncoding()) ? new GZIPInputStream(in) : in;
            if (uncompress) {
                final String contentType = getContentType();
                Main.debug("Uncompressing input stream according to Content-Type header: {0}", contentType);
                in = Compression.forContentType(contentType).getUncompressedInputStream(in);
            }
            if (uncompressAccordingToContentDisposition) {
                final String contentDisposition = getHeaderField("Content-Disposition");
                final Matcher matcher = Pattern.compile("filename=\"([^\"]+)\"").matcher(contentDisposition);
                if (matcher.find()) {
                    Main.debug("Uncompressing input stream according to Content-Disposition header: {0}", contentDisposition);
                    in = Compression.byExtension(matcher.group(1)).getUncompressedInputStream(in);
                }
            }
            return in;
        }

        /**
         * Returns {@link #getContent()} wrapped in a buffered reader.
         *
         * Detects Unicode charset in use utilizing {@link UTFInputStreamReader}.
         */
        public BufferedReader getContentReader() throws IOException {
            return new BufferedReader(
                    UTFInputStreamReader.create(getContent())
            );
        }

        /**
         * Fetches the HTTP response as String.
         * @return the response
         * @throws IOException
         */
        public String fetchContent() throws IOException {
            try (Scanner scanner = new Scanner(getContentReader()).useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        }

        /**
         * Gets the response code from this HTTP connection.
         *
         * @see HttpURLConnection#getResponseCode()
         */
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * Gets the response message from this HTTP connection.
         *
         * @see HttpURLConnection#getResponseMessage()
         */
        public String getResponseMessage() {
            return responseMessage;
        }

        /**
         * Returns the {@code Content-Encoding} header.
         */
        public String getContentEncoding() {
            return connection.getContentEncoding();
        }

        /**
         * Returns the {@code Content-Type} header.
         */
        public String getContentType() {
            return connection.getHeaderField("Content-Type");
        }

        /**
         * Returns the {@code Content-Length} header.
         */
        public long getContentLength() {
            return connection.getContentLengthLong();
        }

        /**
         * @see HttpURLConnection#getHeaderField(String)
         */
        public String getHeaderField(String name) {
            return connection.getHeaderField(name);
        }

        /**
         * @see HttpURLConnection#getHeaderFields()
         */
        public List<String> getHeaderFields(String name) {
            return connection.getHeaderFields().get(name);
        }

        /**
         * @see HttpURLConnection#disconnect()
         */
        public void disconnect() {
            // TODO is this block necessary for disconnecting?
            // Fix upload aborts - see #263
            connection.setConnectTimeout(100);
            connection.setReadTimeout(100);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Main.warn("InterruptedException in " + getClass().getSimpleName() + " during cancel");
            }

            connection.disconnect();
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
     * @see #create(URL)
     * @see #create(URL, String)
     */
    public URL getURL() {
        return url;
    }

    /**
     * Returns the request method set for this connection.
     * @see #create(URL, String)
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * Returns the set value for the given {@code header}.
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
     * <p/>
     * This might fix #7640, see <a href='https://web.archive.org/web/20140118201501/http://www.tikalk.com/java/forums/httpurlconnection-disable-keep-alive'>here</a>.
     *
     * @param keepAlive whether not to set header {@code Connection=close}
     * @return {@code this}
     */
    public HttpClient keepAlive(boolean keepAlive) {
        return setHeader("Connection", keepAlive ? null : "close");
    }

    /**
     * @return {@code this}
     * @see HttpURLConnection#setConnectTimeout(int)
     */
    public HttpClient setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * @return {@code this}
     * @see HttpURLConnection#setReadTimeout(int) (int)
     */

    public HttpClient setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Sets the {@code Accept} header.
     *
     * @return {@code this}
     */
    public HttpClient setAccept(String accept) {
        return setHeader("Accept", accept);
    }

    /**
     * Sets the request body for {@code PUT}/{@code POST} requests.
     *
     * @return {@code this}
     */
    public HttpClient setRequestBody(byte[] requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    /**
     * Sets the {@code If-Modified-Since} header.
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
     *
     * @return {@code this}
     */
    public HttpClient setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    /**
     * Sets an arbitrary HTTP header.
     *
     * @return {@code this}
     */
    public HttpClient setHeader(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Sets arbitrary HTTP headers.
     *
     * @return {@code this}
     */
    public HttpClient setHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Sets a reason to show on console. Can be {@code null} if no reason is given.
     */
    public HttpClient setReasonForRequest(String reasonForRequest) {
        this.reasonForRequest = reasonForRequest;
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

}
