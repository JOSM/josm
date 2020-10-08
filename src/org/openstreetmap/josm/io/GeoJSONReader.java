// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.DuplicateWay;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Reader that reads GeoJSON files. See <a href="https://tools.ietf.org/html/rfc7946">RFC7946</a> for more information.
 * @since 15424
 */
public class GeoJSONReader extends AbstractReader {

    private static final String CRS = "crs";
    private static final String NAME = "name";
    private static final String LINK = "link";
    private static final String COORDINATES = "coordinates";
    private static final String FEATURES = "features";
    private static final String PROPERTIES = "properties";
    private static final String GEOMETRY = "geometry";
    private static final String TYPE = "type";
    /** The record separator is 0x1E per RFC 7464 */
    private static final byte RECORD_SEPARATOR_BYTE = 0x1E;
    private Projection projection = Projections.getProjectionByCode("EPSG:4326"); // WGS 84

    GeoJSONReader() {
        // Restricts visibility
    }

    private void parse(final JsonParser parser) throws IllegalDataException {
        while (parser.hasNext()) {
            Event event = parser.next();
            if (event == Event.START_OBJECT) {
                parseRoot(parser.getObject());
            }
        }
        parser.close();
    }

    private void parseRoot(final JsonObject object) throws IllegalDataException {
        parseCrs(object.getJsonObject(CRS));
        switch (Optional.ofNullable(object.getJsonString(TYPE))
                .orElseThrow(() -> new IllegalDataException("No type")).getString()) {
            case "FeatureCollection":
                parseFeatureCollection(object.getJsonArray(FEATURES));
                break;
            case "Feature":
                parseFeature(object);
                break;
            case "GeometryCollection":
                parseGeometryCollection(null, object);
                break;
            default:
                parseGeometry(null, object);
        }
    }

    /**
     * Parse CRS as per https://geojson.org/geojson-spec.html#coordinate-reference-system-objects.
     * CRS are obsolete in RFC7946 but still allowed for interoperability with older applications.
     * Only named CRS are supported.
     *
     * @param crs CRS JSON object
     * @throws IllegalDataException in case of error
     */
    private void parseCrs(final JsonObject crs) throws IllegalDataException {
        if (crs != null) {
            // Inspired by https://github.com/JOSM/geojson/commit/f13ceed4645244612a63581c96e20da802779c56
            JsonObject properties = crs.getJsonObject("properties");
            if (properties != null) {
                switch (crs.getString(TYPE)) {
                    case NAME:
                        String crsName = properties.getString(NAME);
                        if ("urn:ogc:def:crs:OGC:1.3:CRS84".equals(crsName)) {
                            // https://osgeo-org.atlassian.net/browse/GEOT-1710
                            crsName = "EPSG:4326";
                        } else if (crsName.startsWith("urn:ogc:def:crs:EPSG:")) {
                            crsName = crsName.replace("urn:ogc:def:crs:", "");
                        }
                        projection = Optional.ofNullable(Projections.getProjectionByCode(crsName))
                                .orElse(Projections.getProjectionByCode("EPSG:4326")); // WGS84
                        break;
                    case LINK: // Not supported (security risk)
                    default:
                        throw new IllegalDataException(crs.toString());
                }
            }
        }
    }

    private void parseFeatureCollection(final JsonArray features) {
        for (JsonValue feature : features) {
            if (feature instanceof JsonObject) {
                parseFeature((JsonObject) feature);
            }
        }
    }

    private void parseFeature(final JsonObject feature) {
        JsonValue geometry = feature.get(GEOMETRY);
        if (geometry != null && geometry.getValueType() == JsonValue.ValueType.OBJECT) {
            parseGeometry(feature, geometry.asJsonObject());
        } else {
            JsonValue properties = feature.get(PROPERTIES);
            if (properties != null && properties.getValueType() == JsonValue.ValueType.OBJECT) {
                parseNonGeometryFeature(feature, properties.asJsonObject());
            } else {
                Logging.warn(tr("Relation/non-geometry feature without properties found: {0}", feature));
            }
        }
    }

    private void parseNonGeometryFeature(final JsonObject feature, final JsonObject properties) {
        // get relation type
        JsonValue type = properties.get(TYPE);
        if (type == null || properties.getValueType() == JsonValue.ValueType.STRING) {
            Logging.warn(tr("Relation/non-geometry feature without type found: {0}", feature));
            return;
        }

        // create misc. non-geometry feature
        final Relation relation = new Relation();
        relation.put(TYPE, type.toString());
        fillTagsFromFeature(feature, relation);
        getDataSet().addPrimitive(relation);
    }

    private void parseGeometryCollection(final JsonObject feature, final JsonObject geometry) {
        for (JsonValue jsonValue : geometry.getJsonArray("geometries")) {
            parseGeometry(feature, jsonValue.asJsonObject());
        }
    }

    private void parseGeometry(final JsonObject feature, final JsonObject geometry) {
        if (geometry == null) {
            parseNullGeometry(feature);
            return;
        }

        switch (geometry.getString(TYPE)) {
            case "Point":
                parsePoint(feature, geometry.getJsonArray(COORDINATES));
                break;
            case "MultiPoint":
                parseMultiPoint(feature, geometry);
                break;
            case "LineString":
                parseLineString(feature, geometry.getJsonArray(COORDINATES));
                break;
            case "MultiLineString":
                parseMultiLineString(feature, geometry);
                break;
            case "Polygon":
                parsePolygon(feature, geometry.getJsonArray(COORDINATES));
                break;
            case "MultiPolygon":
                parseMultiPolygon(feature, geometry);
                break;
            case "GeometryCollection":
                parseGeometryCollection(feature, geometry);
                break;
            default:
                parseUnknown(geometry);
        }
    }

    private LatLon getLatLon(final JsonArray coordinates) {
        return projection.eastNorth2latlon(new EastNorth(
                parseCoordinate(coordinates.get(0)),
                parseCoordinate(coordinates.get(1))));
    }

    private static double parseCoordinate(JsonValue coordinate) {
        if (coordinate instanceof JsonString) {
            return Double.parseDouble(((JsonString) coordinate).getString());
        } else if (coordinate instanceof JsonNumber) {
            return ((JsonNumber) coordinate).doubleValue();
        } else {
            throw new IllegalArgumentException(Objects.toString(coordinate));
        }
    }

    private void parsePoint(final JsonObject feature, final JsonArray coordinates) {
        fillTagsFromFeature(feature, createNode(getLatLon(coordinates)));
    }

    private void parseMultiPoint(final JsonObject feature, final JsonObject geometry) {
        for (JsonValue coordinate : geometry.getJsonArray(COORDINATES)) {
            parsePoint(feature, coordinate.asJsonArray());
        }
    }

    private void parseLineString(final JsonObject feature, final JsonArray coordinates) {
        if (!coordinates.isEmpty()) {
            createWay(coordinates, false)
                .ifPresent(way -> fillTagsFromFeature(feature, way));
        }
    }

    private void parseMultiLineString(final JsonObject feature, final JsonObject geometry) {
        for (JsonValue coordinate : geometry.getJsonArray(COORDINATES)) {
            parseLineString(feature, coordinate.asJsonArray());
        }
    }

    private void parsePolygon(final JsonObject feature, final JsonArray coordinates) {
        final int size = coordinates.size();
        if (size == 1) {
            createWay(coordinates.getJsonArray(0), true)
                .ifPresent(way -> fillTagsFromFeature(feature, way));
        } else if (size > 1) {
            // create multipolygon
            final Relation multipolygon = new Relation();
            createWay(coordinates.getJsonArray(0), true)
                .ifPresent(way -> multipolygon.addMember(new RelationMember("outer", way)));

            for (JsonValue interiorRing : coordinates.subList(1, size)) {
                createWay(interiorRing.asJsonArray(), true)
                    .ifPresent(way -> multipolygon.addMember(new RelationMember("inner", way)));
            }

            fillTagsFromFeature(feature, multipolygon);
            multipolygon.put(TYPE, "multipolygon");
            getDataSet().addPrimitive(multipolygon);
        }
    }

    private void parseMultiPolygon(final JsonObject feature, final JsonObject geometry) {
        for (JsonValue coordinate : geometry.getJsonArray(COORDINATES)) {
            parsePolygon(feature, coordinate.asJsonArray());
        }
    }

    private Node createNode(final LatLon latlon) {
        final List<Node> existingNodes = getDataSet().searchNodes(new BBox(latlon, latlon));
        if (!existingNodes.isEmpty()) {
            // reuse existing node, avoid multiple nodes on top of each other
            return existingNodes.get(0);
        }
        final Node node = new Node(latlon);
        getDataSet().addPrimitive(node);
        return node;
    }

    private Optional<Way> createWay(final JsonArray coordinates, final boolean autoClose) {
        if (coordinates.isEmpty()) {
            return Optional.empty();
        }

        final List<LatLon> latlons = coordinates.stream()
                .map(coordinate -> getLatLon(coordinate.asJsonArray()))
                .collect(Collectors.toList());

        final int size = latlons.size();
        final boolean doAutoclose;
        if (size > 1) {
            if (latlons.get(0).equals(latlons.get(size - 1))) {
                doAutoclose = false; // already closed
            } else {
                doAutoclose = autoClose;
            }
        } else {
            doAutoclose = false;
        }

        final Way way = new Way();
        getDataSet().addPrimitive(way);
        final List<Node> rawNodes = latlons.stream().map(this::createNode).collect(Collectors.toList());
        if (doAutoclose) {
            rawNodes.add(rawNodes.get(0));
        }
        // see #19833: remove duplicated references to the same node
        final List<Node> wayNodes = new ArrayList<>(rawNodes.size());
        Node last = null;
        for (Node curr : rawNodes) {
            if (last != curr)
                wayNodes.add(curr);
            last = curr;
        }
        way.setNodes(wayNodes);

        return Optional.of(way);
    }

    private static void fillTagsFromFeature(final JsonObject feature, final OsmPrimitive primitive) {
        if (feature != null) {
            primitive.setKeys(getTags(feature));
        }
    }

    private static void parseUnknown(final JsonObject object) {
        Logging.warn(tr("Unknown json object found {0}", object));
    }

    private static void parseNullGeometry(JsonObject feature) {
        Logging.warn(tr("Geometry of feature {0} is null", feature));
    }

    private static Map<String, String> getTags(final JsonObject feature) {
        final Map<String, String> tags = new TreeMap<>();

        if (feature.containsKey(PROPERTIES) && !feature.isNull(PROPERTIES)) {
            JsonValue properties = feature.get(PROPERTIES);
            if (properties != null && properties.getValueType() == JsonValue.ValueType.OBJECT) {
                for (Map.Entry<String, JsonValue> stringJsonValueEntry : properties.asJsonObject().entrySet()) {
                    final JsonValue value = stringJsonValueEntry.getValue();

                    if (value instanceof JsonString) {
                        tags.put(stringJsonValueEntry.getKey(), ((JsonString) value).getString());
                    } else if (value instanceof JsonObject) {
                        Logging.warn(
                            "The GeoJSON contains an object with property '" + stringJsonValueEntry.getKey()
                                + "' whose value has the unsupported type '" + value.getClass().getSimpleName()
                                + "'. That key-value pair is ignored!"
                        );
                    } else if (value.getValueType() != JsonValue.ValueType.NULL) {
                        tags.put(stringJsonValueEntry.getKey(), value.toString());
                    }
                }
            }
        }
        return tags;
    }

    /**
     * Check if the inputstream follows RFC 7464
     * @param source The source to check (should be at the beginning)
     * @return {@code true} if the initial character is {@link GeoJSONReader#RECORD_SEPARATOR_BYTE}.
     */
    private static boolean isLineDelimited(InputStream source) {
        source.mark(2);
        try {
            int start = source.read();
            if (RECORD_SEPARATOR_BYTE == start) {
                return true;
            }
            source.reset();
        } catch (IOException e) {
            Logging.error(e);
        }
        return false;
    }

    @Override
    protected DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        try (InputStream markSupported = source.markSupported() ? source : new BufferedInputStream(source)) {
            ds.setUploadPolicy(UploadPolicy.DISCOURAGED);
            if (isLineDelimited(markSupported)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(markSupported, StandardCharsets.UTF_8))) {
                    String line;
                    String rs = new String(new byte[]{RECORD_SEPARATOR_BYTE}, StandardCharsets.US_ASCII);
                    while ((line = reader.readLine()) != null) {
                        line = Utils.strip(line, rs);
                        try (JsonParser parser = Json.createParser(new StringReader(line))) {
                            parse(parser);
                        }
                    }
                }
            } else {
                try (JsonParser parser = Json.createParser(markSupported)) {
                    parse(parser);
                }
            }
            mergeEqualMultipolygonWays();
        } catch (IOException | JsonParsingException e) {
            throw new IllegalDataException(e);
        }
        return getDataSet();
    }

    /**
     * Import may create duplicate ways were one is member of a multipolygon and untagged and the other is tagged.
     * Try to merge them here.
     */
    private void mergeEqualMultipolygonWays() {
        DuplicateWay test = new DuplicateWay();
        test.startTest(null);
        for (Way w: getDataSet().getWays()) {
            test.visit(w);
        }
        test.endTest();

        if (test.getErrors().isEmpty())
            return;

        for (TestError e : test.getErrors()) {
            if (e.getPrimitives().size() == 2 && !e.isFixable()) {
                Way mpWay = null;
                Way tagged = null;
                for (OsmPrimitive p : e.getPrimitives()) {
                    if (p.isTagged() && p.referrers(Relation.class).count() == 0)
                        tagged = (Way) p;
                    else if (p.referrers(Relation.class).anyMatch(Relation::isMultipolygon))
                        mpWay = (Way) p;
                }
                if (mpWay != null && tagged != null) {
                    for (Relation r : mpWay.referrers(Relation.class).filter(Relation::isMultipolygon)
                            .collect(Collectors.toList())) {
                        for (int i = 0; i < r.getMembersCount(); i++) {
                            if (r.getMember(i).getMember().equals(mpWay)) {
                                r.setMember(i, new RelationMember(r.getRole(i), tagged));
                            }
                        }
                    }
                    mpWay.setDeleted(true);
                }
            }
        }
        ds.cleanupDeletedPrimitives();
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source          the source input stream. Must not be null.
     * @param progressMonitor the progress monitor. If null, {@link NullProgressMonitor#INSTANCE} is assumed
     * @return the dataset with the parsed data
     * @throws IllegalDataException     if an error was found while parsing the data from the source
     * @throws IllegalArgumentException if source is null
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        return new GeoJSONReader().doParseDataSet(source, progressMonitor);
    }
}
