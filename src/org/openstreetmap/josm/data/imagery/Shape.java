// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Polygon;
import java.text.MessageFormat;
import java.util.AbstractList;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Data class to store the outline for background imagery coverage.
 *
 * Configuration data for imagery to describe the coverage area ({@link ImageryInfo.ImageryBounds}).
 * @author Vincent
 */
public class Shape {

    private final Polygon coords;

    public Shape(String asString, String separator) {
        CheckParameterUtil.ensureParameterNotNull(asString, "asString");
        String[] components = asString.split(separator, -1);
        if (components.length % 2 != 0)
            throw new IllegalArgumentException(MessageFormat.format("Even number of doubles expected in string, got {0}: {1}",
                    components.length, asString));
        int size = components.length / 2;
        this.coords = new Polygon(new int[size], new int[size], 0);
        for (int i = 0; i < components.length; i += 2) {
            addPoint(components[i], components[i+1]);
        }
    }

    /**
     * Constructs a new empty {@code Shape}.
     */
    public Shape() {
        coords = new Polygon();
        // shape contents can be set later with addPoint()
    }

    /**
     * Encodes this as a string so that it may be parsed using {@link #Shape(String, String)}
     * @param separator The separator
     * @return The string encoded shape
     */
    public String encodeAsString(String separator) {
        return getPoints().stream()
                .map(c -> c.getLat() + separator + c.getLon())
                .collect(Collectors.joining(separator));
    }

    /**
     * Encodes the shapes as a string using {@code ,} and {@code ;} as separators
     * @param shapes The shapes to encode
     * @return The string encoded shapes
     */
    public static String encodeAsString(List<Shape> shapes) {
        return shapes.stream()
                .map(s -> s.encodeAsString(","))
                .collect(Collectors.joining(";"));
    }

    public List<Coordinate> getPoints() {
        return new CoordinateList(this.coords);
    }

    /**
     * Check if the coordinates are inside this shape.
     * @param latlon The latlon to look for
     * @return {@code true} if the LatLon is inside the shape.
     * @see Polygon#contains(int, int)
     */
    public boolean contains(LatLon latlon) {
        return coords.contains(
                latlon.getX() * LatLon.MAX_SERVER_INV_PRECISION,
                latlon.getY() * LatLon.MAX_SERVER_INV_PRECISION);
    }

    public void addPoint(String sLat, String sLon) {
        CheckParameterUtil.ensureParameterNotNull(sLat, "sLat");
        CheckParameterUtil.ensureParameterNotNull(sLon, "sLon");

        double lat, lon;

        try {
            lat = Double.parseDouble(sLat);
            if (!LatLon.isValidLat(lat))
                throw new IllegalArgumentException(tr("Illegal latitude value ''{0}''", lat));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MessageFormat.format("Illegal double value ''{0}''", sLat), e);
        }

        try {
            lon = Double.parseDouble(sLon);
            if (!LatLon.isValidLon(lon))
                throw new IllegalArgumentException(tr("Illegal longitude value ''{0}''", lon));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MessageFormat.format("Illegal double value ''{0}''", sLon), e);
        }

        coords.addPoint(
                (int) Math.round((lon * LatLon.MAX_SERVER_INV_PRECISION)),
                (int) Math.round((lat * LatLon.MAX_SERVER_INV_PRECISION)));
    }

    @Override
    public int hashCode() {
        // This was Objects.hash(getPoints()), but that made 8-24MB of allocations on application startup
        return 31 + hashPolygon(this.coords); // Arrays#hash, new array instantiation
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shape shape = (Shape) obj;
        return equalsPolygon(this.coords, shape.coords);
    }

    @Override
    public String toString() {
        return "Shape{coords=" + getPoints() + '}';
    }

    /**
     * Hash a polygon
     * @param coords The polygon to hash
     * @return The hashcode to use; equivalent to {@link Polygon#hashCode()}, but zero allocations.
     */
    private static int hashPolygon(Polygon coords) {
        // This is faster than coords.hashCode() by ~90% and performs effectively no memory allocations.
        // This was originally written to replace Objects.hash(getPoints()). The only difference is +31 on the return.
        // Objects.hash(getPoints()) -> Arrays.hash(getPoints()) -> sum of hashes of Coordinates
        // First, AbstractList#hashCode equivalent
        int hashCode = 1;
        for (int index = 0; index < coords.npoints; index++) {
            final double lat = coords.ypoints[index] / LatLon.MAX_SERVER_INV_PRECISION;
            final double lon = coords.xpoints[index] / LatLon.MAX_SERVER_INV_PRECISION;
            // Coordinate uses Object.hash(x, y) - new array instantiation *and* two Double instantiations
            // Double conversion is 3.22MB
            // The array creation for Object.hash(x, y) is 2.11 MB
            final int coordinateHash = 31 * (31 + Double.hashCode(lon)) + Double.hashCode(lat);
            hashCode = 31 * hashCode + coordinateHash; // hashCode * 31 + coordinate.hashCode()
        }
        return hashCode;
    }

    /**
     * Check that two {@link Polygon}s are equal
     * @param first The first polygon to check
     * @param second The second polygon to check
     * @return {@code true} if the polygons are equal
     */
    private static boolean equalsPolygon(Polygon first, Polygon second) {
        // If the coordinate lists aren't the same size, short circuit.
        // We aren't doing fuzzy comparisons here.
        if (first.npoints != second.npoints) {
            return false;
        }
        for (int i = 0; i < first.npoints; i++) {
            if (first.xpoints[i] != second.xpoints[i] ||
                    first.ypoints[i] != second.ypoints[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * A list of {@link Coordinate}s that attempts to be very efficient in terms of CPU time and memory allocations.
     */
    private static final class CoordinateList extends AbstractList<Coordinate> {
        private final Polygon coords;

        CoordinateList(Polygon coords) {
            this.coords = coords;
        }

        @Override
        public Coordinate get(int index) {
            double lat = coords.ypoints[index] / LatLon.MAX_SERVER_INV_PRECISION;
            double lon = coords.xpoints[index] / LatLon.MAX_SERVER_INV_PRECISION;
            return new Coordinate(lat, lon);
        }

        @Override
        public int size() {
            return coords.npoints;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CoordinateList) {
                CoordinateList other = (CoordinateList) o;
                return equalsPolygon(this.coords, other.coords);
            }
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return hashPolygon(this.coords);
        }
    }
}
