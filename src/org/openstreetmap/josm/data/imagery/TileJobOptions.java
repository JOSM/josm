// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.Collections;
import java.util.Map;

/**
 * Class containing all options that are passed from Layer to TileJob
 *
 * @author Wiktor Niesiobedzki
 *
 */
public class TileJobOptions {

    final int connectTimeout;
    final int readTimeout;
    final Map<String, String> headers;
    final long minimumExpiryTime;

    /**
     * Options constructor
     *
     * @param connectTimeout in milliseconds
     * @param readTimeout in milliseconds
     * @param headers http headers
     * @param minimumExpiryTime in seconds
     */
    public TileJobOptions(int connectTimeout, int readTimeout, Map<String, String> headers, long minimumExpiryTime) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.headers = Collections.unmodifiableMap(headers == null ? Collections.emptyMap() : headers);
        this.minimumExpiryTime = minimumExpiryTime;
    }

    /**
     *
     * @return socket connection timeout in milliseconds
     */
    public int getConnectionTimeout() {
        return connectTimeout;
    }

    /**
     *
     * @return socket read timeout in milliseconds
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     *
     * @return unmodifiable map with headers to be sent to tile server
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     *
     * @return minimum cache expire time in seconds for downloaded tiles
     */
    public long getMinimumExpiryTime() {
        return minimumExpiryTime;
    }
}
