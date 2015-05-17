// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jcs.engine.ElementAttributes;

/**
 * Class that contains attirubtes for JCS cache entries. Parameters are used to properly handle HTTP caching
 *
 * @author Wiktor Niesiobędzki
 * @since 8168
 */
public class CacheEntryAttributes extends ElementAttributes {
    private static final long serialVersionUID = 1L; //version
    private final Map<String, String> attrs = new HashMap<String, String>();
    private static final String NO_TILE_AT_ZOOM = "noTileAtZoom";
    private static final String ETAG = "Etag";
    private static final String LAST_MODIFICATION = "lastModification";
    private static final String EXPIRATION_TIME = "expirationTime";
    private static final String HTTP_RESPONSE_CODE = "httpResponceCode";

    /**
     * Constructs a new {@code CacheEntryAttributes}.
     */
    public CacheEntryAttributes() {
        super();
        attrs.put(NO_TILE_AT_ZOOM, "false");
        attrs.put(ETAG, null);
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
        attrs.put(ETAG, etag);
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

}
