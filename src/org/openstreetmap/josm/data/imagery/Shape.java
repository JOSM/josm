// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.awt.Polygon;
import java.text.MessageFormat;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.CheckParameterUtil;

import static org.openstreetmap.josm.tools.I18n.tr;

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
                .flatMap(c -> Stream.of(c.getLat(), c.getLon()))
                .map(String::valueOf)
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
        return new AbstractList<Coordinate>() {
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
        };
    }

    /**
     * Check if the coordinates are inside this shape.
     * @see Polygon#contains(int, int)
     * @param latlon The latlon to look for
     * @return {@code true} if the LatLon is inside the shape.
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
        return Objects.hash(getPoints());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shape shape = (Shape) obj;
        return Objects.equals(getPoints(), shape.getPoints());
    }

    @Override
    public String toString() {
        return "Shape{coords=" + getPoints() + '}';
    }
}
