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
import org.openstreetmap.josm.Main;

/**
 * Class that contains attirubtes for JCS cache entries. Parameters are used to properly handle HTTP caching
 *
 * @author Wiktor Niesiobędzki
 * @since 8168
 */
public class CacheEntryAttributes extends ElementAttributes {
    private static final long serialVersionUID = 1L; //version
    private final Map<String, String> attrs = new ConcurrentHashMap<String, String>(RESERVED_KEYS.size());
    private static final String NO_TILE_AT_ZOOM = "noTileAtZoom";
    private static final String ETAG = "Etag";
    private static final String LAST_MODIFICATION = "lastModification";
    private static final String EXPIRATION_TIME = "expirationTime";
    private static final String HTTP_RESPONSE_CODE = "httpResponceCode";
    // this contains all of the above
    private static final Set<String> RESERVED_KEYS = new HashSet<>(Arrays.asList(new String[]{
        NO_TILE_AT_ZOOM,
        ETAG,
        LAST_MODIFICATION,
        EXPIRATION_TIME,
        HTTP_RESPONSE_CODE
    }));

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

    public boolean isNoTileAtZoom() {
        return Boolean.toString(true).equals(attrs.get(NO_TILE_AT_ZOOM));
    }
    public void setNoTileAtZoom(boolean noTileAtZoom) {
        attrs.put(NO_TILE_AT_ZOOM, Boolean.toString(noTileAtZoom));
    }
    public String getEtag() {
        return attrs.get(ETAG);
    }
    public void setEtag(String etag) {
        if(etag != null) {
            attrs.put(ETAG, etag);
        }
    }

    private long getLongAttr(String key) {
        String val = attrs.get(key);
        if (val == null) {
            attrs.put(key,  "0");
            return 0;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            attrs.put(key, "0");
            return 0;
        }
    }

    public long getLastModification() {
        return getLongAttr(LAST_MODIFICATION);
    }
    public void setLastModification(long lastModification) {
        attrs.put(LAST_MODIFICATION, Long.toString(lastModification));
    }
    public long getExpirationTime() {
        return getLongAttr(EXPIRATION_TIME);
    }
    public void setExpirationTime(long expirationTime) {
        attrs.put(EXPIRATION_TIME, Long.toString(expirationTime));
    }

    public void setResponseCode(int responseCode) {
        attrs.put(HTTP_RESPONSE_CODE, Integer.toString(responseCode));
    }

    public int getResponseCode() {
        return (int) getLongAttr(HTTP_RESPONSE_CODE);
    }

    /**
     * Sets the metadata about cache entry. As it stores all data together, with other attributes
     * in common map, some keys might not be stored.
     *
     * @param map metadata to save
     */
    public void setMetadata(Map<String, String> map) {
        for (Entry<String, String> e: map.entrySet()) {
            if (RESERVED_KEYS.contains(e.getKey())) {
                Main.info("Metadata key configuration contains key {0} which is reserved for internal use");
            } else {
                attrs.put(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * Returns an unmodifiable Map containing all metadata. Unmodifiable prevents access to metadata within attributes.
     * @return unmodifiable Map with cache element metadata
     */
    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(attrs);
    }
}
