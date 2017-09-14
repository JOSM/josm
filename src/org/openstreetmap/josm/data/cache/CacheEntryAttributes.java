// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.engine.ElementAttributes;
import org.openstreetmap.josm.tools.Logging;

/**
 * Class that contains attributes for JCS cache entries. Parameters are used to properly handle HTTP caching,
 * and metadata structures, that should be stored together with the cache entry
 *
 * @author Wiktor Niesiobędzki
 * @since 8168
 */
public class CacheEntryAttributes extends ElementAttributes {
    private static final long serialVersionUID = 1L; //version
    private final Map<String, String> attrs = new ConcurrentHashMap<>(RESERVED_KEYS.size());
    private static final String NO_TILE_AT_ZOOM = "noTileAtZoom";
    private static final String ETAG = "Etag";
    private static final String LAST_MODIFICATION = "lastModification";
    private static final String EXPIRATION_TIME = "expirationTime";
    private static final String HTTP_RESPONSE_CODE = "httpResponceCode";
    private static final String ERROR_MESSAGE = "errorMessage";
    // this contains all of the above
    private static final Set<String> RESERVED_KEYS = new HashSet<>(Arrays.asList(
        NO_TILE_AT_ZOOM,
        ETAG,
        LAST_MODIFICATION,
        EXPIRATION_TIME,
        HTTP_RESPONSE_CODE,
        ERROR_MESSAGE
    ));

    /**
     * Constructs a new {@code CacheEntryAttributes}.
     */
    public CacheEntryAttributes() {
        super();
        attrs.put(NO_TILE_AT_ZOOM, "false");
        attrs.put(LAST_MODIFICATION, "0");
        attrs.put(EXPIRATION_TIME, "0");
        attrs.put(HTTP_RESPONSE_CODE, "200");
    }

    /**
     * @return if the entry is marked as "no tile at this zoom level"
     */
    public boolean isNoTileAtZoom() {
        return Boolean.toString(true).equals(attrs.get(NO_TILE_AT_ZOOM));
    }

    /**
     * Sets the marker for "no tile at this zoom level"
     * @param noTileAtZoom true if this entry is "no tile at this zoom level"
     */
    public void setNoTileAtZoom(boolean noTileAtZoom) {
        attrs.put(NO_TILE_AT_ZOOM, Boolean.toString(noTileAtZoom));
    }

    /**
     * @return ETag header value, that was returned for this entry.
     */
    public String getEtag() {
        return attrs.get(ETAG);
    }

    /**
     * Sets the ETag header that was set with this entry
     * @param etag Etag header
     */
    public void setEtag(String etag) {
        if (etag != null) {
            attrs.put(ETAG, etag);
        }
    }

    /**
     * Utility for conversion from String to int, with default to 0, in case of any errors
     *
     * @param key - integer as string
     * @return int value of the string
     */
    private long getLongAttr(String key) {
        try {
            return Long.parseLong(attrs.computeIfAbsent(key, k -> "0"));
        } catch (NumberFormatException e) {
            attrs.put(key, "0");
            return 0;
        }
    }

    /**
     * @return last modification of the object in cache in milliseconds from Epoch
     */
    public long getLastModification() {
        return getLongAttr(LAST_MODIFICATION);
    }

    /**
     * sets last modification of the object in cache
     *
     * @param lastModification time in format of milliseconds from Epoch
     */
    public void setLastModification(long lastModification) {
        attrs.put(LAST_MODIFICATION, Long.toString(lastModification));
    }

    /**
     * @return when the object expires in milliseconds from Epoch
     */
    public long getExpirationTime() {
        return getLongAttr(EXPIRATION_TIME);
    }

    /**
     * sets expiration time for the object in cache
     *
     * @param expirationTime in format of milliseconds from epoch
     */
    public void setExpirationTime(long expirationTime) {
        attrs.put(EXPIRATION_TIME, Long.toString(expirationTime));
    }

    /**
     * Sets the HTTP response code that was sent with the cache entry
     *
     * @param responseCode http status code
     * @since 8389
     */
    public void setResponseCode(int responseCode) {
        attrs.put(HTTP_RESPONSE_CODE, Integer.toString(responseCode));
    }

    /**
     * @return http status code
     * @since 8389
     */
    public int getResponseCode() {
        return (int) getLongAttr(HTTP_RESPONSE_CODE);
    }

    /**
     * Sets the metadata about cache entry. As it stores all data together, with other attributes
     * in common map, some keys might not be stored.
     *
     * @param map metadata to save
     * @since 8418
     */
    public void setMetadata(Map<String, String> map) {
        for (Entry<String, String> e: map.entrySet()) {
            if (RESERVED_KEYS.contains(e.getKey())) {
                Logging.info("Metadata key configuration contains key {0} which is reserved for internal use");
            } else {
                attrs.put(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * Returns an unmodifiable Map containing all metadata. Unmodifiable prevents access to metadata within attributes.
     *
     * @return unmodifiable Map with cache element metadata
     * @since 8418
     */
    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(attrs);
    }

    /**
     * @return error message returned while retrieving this object
     */
    public String getErrorMessage() {
        return attrs.get(ERROR_MESSAGE);
    }

    /**
     * @param error error related to this object
     * @since 10469
     */
    public void setError(Exception error) {
        setErrorMessage(Logging.getErrorMessage(error));
    }

    /**
     * @param message error message related to this object
     */
    public void setErrorMessage(String message) {
        attrs.put(ERROR_MESSAGE, message);
    }
}
