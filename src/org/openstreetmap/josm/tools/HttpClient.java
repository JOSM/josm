// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.io.Compression;

/**
 * Provides a uniform access for a HTTP/HTTPS server. This class should be used in favour of {@link HttpURLConnection}.
 */
public class HttpClient {

    private URL url;
    private final String requestMethod;
    private int connectTimeout = Main.pref.getInteger("socket.timeout.connect", 15) * 1000;
    private int readTimeout = Main.pref.getInteger("socket.timeout.read", 30) * 1000;
    private String accept;
    private String contentType;
    private String acceptEncoding = "gzip";
    private long contentLength;
    private byte[] requestBody;
    private long ifModifiedSince;
    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private int maxRedirects = Main.pref.getInteger("socket.maxredirects", 5);

    private HttpClient(URL url, String requestMethod) {
        this.url = url;
        this.requestMethod = requestMethod;
    }

    public Response connect() throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", Version.getInstance().getFullAgentString());
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        if (accept != null) {
            connection.setRequestProperty("Accept", accept);
        }
        if (contentType != null) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        if (acceptEncoding != null) {
            connection.setRequestProperty("Accept-Encoding", acceptEncoding);
        }
        if (contentLength > 0) {
            connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
        }
        if ("PUT".equals(requestMethod) || "POST".equals(requestMethod) || "DELETE".equals(requestMethod)) {
            connection.setDoOutput(true);
            try (OutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
                out.write(requestBody);
            }
        }
        if (ifModifiedSince > 0) {
            connection.setIfModifiedSince(ifModifiedSince);
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }

        boolean successfulConnection = false;
        try {
            try {
                connection.connect();
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
                } else {
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
        private boolean uncompress;

        private Response(HttpURLConnection connection) throws IOException {
            this.connection = connection;
            this.responseCode = connection.getResponseCode();
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
         * Returns an input stream that reads from this HTTP connection, or,
         * error stream if the connection failed but the server sent useful data.
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
                return Compression.forContentType(getContentType()).getUncompressedInputStream(in);
            } else {
                return in;
            }
        }

        /**
         * Returns {@link #getContent()} wrapped in a buffered reader
         */
        public BufferedReader getContentReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getContent(), StandardCharsets.UTF_8));
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
         * @see HttpURLConnection#disconnect()
         */
        public void disconnect() {
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
     * @param url           the URL
     * @param requestMethod the HTTP request method to perform when calling
     * @return a new instance
     */
    public static HttpClient create(URL url, String requestMethod) {
        return new HttpClient(url, requestMethod);
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
        this.accept = accept;
        return this;
    }

    /**
     * Sets the {@code Content-Type} header.
     *
     * @return {@code this}
     */
    public HttpClient setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Sets the {@code Accept-Encoding} header.
     *
     * @return {@code this}
     */
    public HttpClient setAcceptEncoding(String acceptEncoding) {
        this.acceptEncoding = acceptEncoding;
        return this;
    }

    /**
     * Sets the {@code Content-Length} header for {@code PUT}/{@code POST} requests.
     *
     * @return {@code this}
     */
    public HttpClient setContentLength(long contentLength) {
        this.contentLength = contentLength;
        return this;
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
