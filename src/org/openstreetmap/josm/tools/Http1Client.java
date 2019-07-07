// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.ProgressOutputStream;

/**
 * Provides a uniform access for a HTTP/HTTPS 1.0/1.1 server.
 * @since 15229
 */
public final class Http1Client extends HttpClient {

    private HttpURLConnection connection; // to allow disconnecting before `response` is set

    /**
     * Constructs a new {@code Http1Client}.
     * @param url URL to access
     * @param requestMethod HTTP request method (GET, POST, PUT, DELETE...)
     */
    public Http1Client(URL url, String requestMethod) {
        super(url, requestMethod);
    }

    @Override
    protected void setupConnection(ProgressMonitor progressMonitor) throws IOException {
        connection = (HttpURLConnection) getURL().openConnection();
        connection.setRequestMethod(getRequestMethod());
        connection.setRequestProperty("User-Agent", Version.getInstance().getFullAgentString());
        connection.setConnectTimeout(getConnectTimeout());
        connection.setReadTimeout(getReadTimeout());
        connection.setInstanceFollowRedirects(false); // we do that ourselves
        if (getIfModifiedSince() > 0) {
            connection.setIfModifiedSince(getIfModifiedSince());
        }
        connection.setUseCaches(isUseCache());
        if (!isUseCache()) {
            connection.setRequestProperty("Cache-Control", "no-cache");
        }
        for (Map.Entry<String, String> header : getHeaders().entrySet()) {
            if (header.getValue() != null) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        notifyConnect(progressMonitor);

        if (requiresBody()) {
            logRequestBody();
            byte[] body = getRequestBody();
            connection.setFixedLengthStreamingMode(body.length);
            connection.setDoOutput(true);
            try (OutputStream out = new BufferedOutputStream(
                    new ProgressOutputStream(connection.getOutputStream(), body.length,
                            progressMonitor, getOutputMessage(), isFinishOnCloseOutput()))) {
                out.write(body);
            }
        }
    }

    @Override
    protected ConnectionResponse performConnection() throws IOException {
        connection.connect();
        return new ConnectionResponse() {
            @Override
            public String getResponseVersion() {
                return "HTTP_1";
            }

            @Override
            public int getResponseCode() throws IOException {
                return connection.getResponseCode();
            }

            @Override
            public String getHeaderField(String name) {
                return connection.getHeaderField(name);
            }

            @Override
            public long getContentLengthLong() {
                return connection.getContentLengthLong();
            }

            @Override
            public Map<String, List<String>> getHeaderFields() {
                return connection.getHeaderFields();
            }
        };
    }

    @Override
    protected void performDisconnection() throws IOException {
        connection.disconnect();
    }

    @Override
    protected Response buildResponse(ProgressMonitor progressMonitor) throws IOException {
        return new Http1Response(connection, progressMonitor);
    }

    /**
     * A wrapper for the HTTP 1.x response.
     */
    public static final class Http1Response extends Response {
        private final HttpURLConnection connection;

        private Http1Response(HttpURLConnection connection, ProgressMonitor progressMonitor) throws IOException {
            super(progressMonitor, connection.getResponseCode(), connection.getResponseMessage());
            this.connection = connection;
            debugRedirect();
        }

        @Override
        public URL getURL() {
            return connection.getURL();
        }

        @Override
        public String getRequestMethod() {
            return connection.getRequestMethod();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream in;
            try {
                in = connection.getInputStream();
            } catch (IOException ioe) {
                Logging.debug(ioe);
                in = Optional.ofNullable(connection.getErrorStream()).orElseGet(() -> new ByteArrayInputStream(new byte[]{}));
            }
            return in;
        }

        @Override
        public String getContentEncoding() {
            return connection.getContentEncoding();
        }

        @Override
        public String getContentType() {
            return connection.getHeaderField("Content-Type");
        }

        @Override
        public long getExpiration() {
            return connection.getExpiration();
        }

        @Override
        public long getLastModified() {
            return connection.getLastModified();
        }

        @Override
        public long getContentLength() {
            return connection.getContentLengthLong();
        }

        @Override
        public String getHeaderField(String name) {
            return connection.getHeaderField(name);
        }

        @Override
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

        @Override
        public void disconnect() {
            Http1Client.disconnect(connection);
        }
    }

    /**
     * @see HttpURLConnection#disconnect()
     */
    @Override
    public void disconnect() {
        Http1Client.disconnect(connection);
    }

    private static void disconnect(final HttpURLConnection connection) {
        if (connection != null) {
            // Fix upload aborts - see #263
            connection.setConnectTimeout(100);
            connection.setReadTimeout(100);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logging.warn("InterruptedException in " + Http1Client.class + " during cancel");
                Thread.currentThread().interrupt();
            }
            connection.disconnect();
        }
    }
}
