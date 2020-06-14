// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Data class to store the outline for background imagery coverage.
 *
 * Configuration data for imagery to describe the coverage area ({@link ImageryInfo.ImageryBounds}).
 * @author Vincent
 */
public class Shape {

    private final List<Coordinate> coords = new ArrayList<>();

    public Shape(String asString, String separator) {
        CheckParameterUtil.ensureParameterNotNull(asString, "asString");
        String[] components = asString.split(separator, -1);
        if (components.length % 2 != 0)
            throw new IllegalArgumentException(MessageFormat.format("Even number of doubles expected in string, got {0}: {1}",
                    components.length, asString));
        for (int i = 0; i < components.length; i += 2) {
            addPoint(components[i], components[i+1]);
        }
    }

    /**
     * Constructs a new empty {@code Shape}.
     */
    public Shape() {
        // shape contents can be set later with addPoint()
    }

    /**
     * Encodes this as a string so that it may be parsed using {@link #Shape(String, String)}
     * @param separator The separator
     * @return The string encoded shape
     */
    public String encodeAsString(String separator) {
        return coords.stream()
                .flatMap(c -> Stream.of(c.getLat(), c.getLon()))
                .map(String::valueOf)
                .collect(Collectors.joining(separator));
    }

    /**
     * Encodes the shapes as a string using {@code ,} and {@code ;} as separators
     * @return The string encoded shapes
     */
    public static String encodeAsString(List<Shape> shapes) {
        return shapes.stream()
                .map(s -> s.encodeAsString(","))
                .collect(Collectors.joining(";"));
    }

    public List<Coordinate> getPoints() {
        return coords;
    }

    public boolean contains(LatLon latlon) {
        if (latlon == null)
            return false;
        List<Node> nodes = coords.stream()
                .map(c -> new Node(new LatLon(c.getLat(), c.getLon())))
                .collect(Collectors.toList());
        return Geometry.nodeInsidePolygon(new Node(latlon), nodes);
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

        coords.add(new Coordinate(LatLon.roundToOsmPrecision(lat), LatLon.roundToOsmPrecision(lon)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(coords);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shape shape = (Shape) obj;
        return Objects.equals(coords, shape.coords);
    }
}
