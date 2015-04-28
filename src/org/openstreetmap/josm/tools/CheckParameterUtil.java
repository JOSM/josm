// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.text.MessageFormat;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;

/**
 * This utility class provides a collection of static helper methods for checking
 * parameters at run-time.
 * @since 2711
 */
public final class CheckParameterUtil {

    private CheckParameterUtil() {
        // Hide default constructor for utils classes
    }

    /**
     * Ensures an OSM primitive ID is valid
     * @param id The id to check
     * @param parameterName The parameter name
     * @throws IllegalArgumentException if the primitive ID is not valid (negative or zero)
     */
    public static void ensureValidPrimitiveId(PrimitiveId id, String parameterName) throws IllegalArgumentException {
        ensureParameterNotNull(id, parameterName);
        if (id.getUniqueId() <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Expected unique id > 0 for primitive ''{1}'', got {0}", id.getUniqueId(), parameterName));
    }

    /**
     * Ensures lat/lon coordinates are valid
     * @param latlon The lat/lon to check
     * @param parameterName The parameter name
     * @throws IllegalArgumentException if the lat/lon are {@code null} or not valid
     * @since 5980
     */
    public static void ensureValidCoordinates(LatLon latlon, String parameterName) throws IllegalArgumentException {
        ensureParameterNotNull(latlon, parameterName);
        if (!latlon.isValid())
            throw new IllegalArgumentException(MessageFormat.format("Expected valid lat/lon for parameter ''{0}'', got {1}", parameterName, latlon));
    }

    /**
     * Ensures east/north coordinates are valid
     * @param eastnorth The east/north to check
     * @param parameterName The parameter name
     * @throws IllegalArgumentException if the east/north are {@code null} or not valid
     * @since 5980
     */
    public static void ensureValidCoordinates(EastNorth eastnorth, String parameterName) throws IllegalArgumentException {
        ensureParameterNotNull(eastnorth, parameterName);
        if (!eastnorth.isValid())
            throw new IllegalArgumentException(MessageFormat.format("Expected valid east/north for parameter ''{0}'', got {1}", parameterName, eastnorth));
    }

    /**
     * Ensures a version number is valid
     * @param version The version to check
     * @param parameterName The parameter name
     * @throws IllegalArgumentException if the version is not valid (negative)
     */
    public static void ensureValidVersion(long version, String parameterName) throws IllegalArgumentException {
        if (version < 0)
            throw new IllegalArgumentException(MessageFormat.format("Expected value of type long > 0 for parameter ''{0}'', got {1}", parameterName, version));
    }

    /**
     * Ensures a parameter is not {@code null}
     * @param value The parameter to check
     * @param parameterName The parameter name
     * @throws IllegalArgumentException if the parameter is {@code null}
     */
    public static void ensureParameterNotNull(Object value, String parameterName) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' must not be null", parameterName));
    }

    /**
     * Ensures a parameter is not {@code null}. Can find line number in the stack trace, so parameter name is optional
     * @param value The parameter to check
     * @throws IllegalArgumentException if the parameter is {@code null}
     * @since 3871
     */
    public static void ensureParameterNotNull(Object value) throws IllegalArgumentException {
        if (value == null)
            throw new IllegalArgumentException("Parameter must not be null");
    }

    /**
     * Ensures that the condition {@code condition} holds.
     * @param condition The condition to check
     * @throws IllegalArgumentException if the condition does not hold
     */
    public static void ensureThat(boolean condition, String message) throws IllegalArgumentException {
        if (!condition)
            throw new IllegalArgumentException(message);
    }

    /**
     * Ensures that <code>id</code> is non-null primitive id of type {@link OsmPrimitiveType#NODE}
     *
     * @param id  the primitive  id
     * @param parameterName the name of the parameter to be checked
     * @throws IllegalArgumentException if id is null
     * @throws IllegalArgumentException if id.getType() != NODE
     */
    public static void ensureValidNodeId(PrimitiveId id, String parameterName) throws IllegalArgumentException {
        ensureParameterNotNull(id, parameterName);
        if (! id.getType().equals(OsmPrimitiveType.NODE))
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' of type node expected, got ''{1}''", parameterName, id.getType().getAPIName()));
    }
}
