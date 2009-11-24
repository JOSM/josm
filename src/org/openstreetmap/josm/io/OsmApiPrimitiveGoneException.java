// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * Represents an exception thrown by the OSM API if JOSM tries to update or delete a primitive
 * which is already deleted on the server.
 *
 */
public class OsmApiPrimitiveGoneException extends OsmApiException{
    /**
     * The regexp pattern for the error header replied by the OSM API
     */
    static public final String ERROR_HEADER_PATTERN = "The (\\S+) with the id (\\d+) has already been deleted";
    /** the type of the primitive which is gone on the server */
    private OsmPrimitiveType type;
    /** the id of the primitive */
    private long id;

    public OsmApiPrimitiveGoneException(String errorHeader, String errorBody) {
        super(HttpURLConnection.HTTP_GONE, errorHeader, errorBody);
        if (errorHeader == null) return;
        Pattern p = Pattern.compile(ERROR_HEADER_PATTERN);
        Matcher m = p.matcher(errorHeader);
        if (m.matches()) {
            type = OsmPrimitiveType.from(m.group(1));
            id = Long.parseLong(m.group(2));
        }
    }

    /**
     * Replies true if we know what primitive this exception was thrown for
     *
     * @return true if we know what primitive this exception was thrown for
     */
    public boolean isKnownPrimitive() {
        return id > 0 && type != null;
    }

    /**
     * Replies the type of the primitive this exception was thrown for. null,
     * if the type is not known.
     *
     * @return the type of the primitive this exception was thrown for
     */
    public OsmPrimitiveType getPrimitiveType() {
        return type;
    }

    /**
     * Replies the id of the primitive this exception was thrown for. 0, if
     * the id is not known.
     *
     * @return the id of the primitive this exception was thrown for
     */
    public long getPrimitiveId() {
        return id;
    }
}
