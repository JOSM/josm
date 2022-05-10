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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.DuplicateWay;
import org.openstreetmap.josm.gui.conflict.tags.TagConflictResolutionUtil;
import org.openstreetmap.josm.gui.conflict.tags.TagConflictResolverModel;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
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
    /**
     * WGS 84 is the specified CRS for geojson -- alternate coordinate systems are considered to be deprecated from
     * GJ2008.
     */
    private static final String CRS_GEOJSON = "EPSG:4326";
    private Projection projection = Projections.getProjectionByCode(CRS_GEOJSON); // WGS 84

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
                JsonValue.ValueType valueType = object.get(FEATURES).getValueType();
                CheckParameterUtil.ensureThat(valueType == JsonValue.ValueType.ARRAY, "features must be ARRAY, but is " + valueType);
                parseFeatureCollection(object.getJsonArray(FEATURES), false);
                break;
            case "Feature":
                parseFeature(object);
                break;
            case "GeometryCollection":
                parseGeometryCollection(null, object, false);
                break;
            default:
                parseGeometry(null, object);
        }
    }

    /**
     * Parse CRS as per <a href="https://geojson.org/geojson-spec.html#coordinate-reference-system-objects">
     * https://geojson.org/geojson-spec.html#coordinate-reference-system-objects</a>.
     * CRS are obsolete in RFC7946 but still allowed for interoperability with older applications.
     * Only named CRS are supported.
     *
     * @param crs CRS JSON object
     * @throws IllegalDataException in case of error
     */
    private void parseCrs(final JsonObject crs) throws IllegalDataException {
        if (crs != null) {
            // Inspired by https://github.com/JOSM/geojson/commit/f13ceed4645244612a63581c96e20da802779c56
            JsonObject properties = crs.getJsonObject(PROPERTIES);
            if (properties != null) {
                switch (crs.getString(TYPE)) {
                    case NAME:
                        String crsName = properties.getString(NAME);
                        if ("urn:ogc:def:crs:OGC:1.3:CRS84".equals(crsName)) {
                            // https://osgeo-org.atlassian.net/browse/GEOT-1710
                            crsName = CRS_GEOJSON;
                        } else if (crsName.startsWith("urn:ogc:def:crs:EPSG:")) {
                            crsName = crsName.replace("urn:ogc:def:crs:", "");
                        }
                        projection = Optional.ofNullable(Projections.getProjectionByCode(crsName))
                                .orElse(Projections.getProjectionByCode(CRS_GEOJSON)); // WGS84
                        break;
                    case LINK: // Not supported (security risk)
                    default:
                        throw new IllegalDataException(crs.toString());
                }
            }
        }
    }

    private Optional<? extends OsmPrimitive> parseFeatureCollection(final JsonArray features, boolean createRelation) {
        List<OsmPrimitive> primitives = features.stream().filter(JsonObject.class::isInstance).map(JsonObject.class::cast)
                .map(this::parseFeature).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        if (createRelation && primitives.size() > 1) {
            Relation relation = new Relation();
            relation.setMembers(primitives.stream().map(osm -> new RelationMember("", osm)).collect(Collectors.toList()));
            getDataSet().addPrimitive(relation);
            return Optional.of(relation);
        } else if (primitives.size() == 1) {
            return Optional.of(primitives.get(0));
        }
        return Optional.empty();
    }

    private Optional<? extends OsmPrimitive> parseFeature(final JsonObject feature) {
        JsonValue geometry = feature.get(GEOMETRY);
        if (geometry != null && geometry.getValueType() == JsonValue.ValueType.OBJECT) {
            return parseGeometry(feature, geometry.asJsonObject());
        } else {
            JsonValue properties = feature.get(PROPERTIES);
            if (properties != null && properties.getValueType() == JsonValue.ValueType.OBJECT) {
                return parseNonGeometryFeature(feature, properties.asJsonObject());
            } else {
                Logging.warn(tr("Relation/non-geometry feature without properties found: {0}", feature));
            }
        }
        return Optional.empty();
    }

    private Optional<? extends OsmPrimitive> parseNonGeometryFeature(final JsonObject feature, final JsonObject properties) {
        // get relation type
        JsonValue type = properties.get(TYPE);
        if (type == null || properties.getValueType() == JsonValue.ValueType.STRING) {
            Logging.warn(tr("Relation/non-geometry feature without type found: {0}", feature));
            if (!feature.containsKey(FEATURES)) {
                return Optional.empty();
            }
        }

        // create misc. non-geometry feature
        OsmPrimitive primitive = null;
        if (feature.containsKey(FEATURES) && feature.get(FEATURES).getValueType() == JsonValue.ValueType.ARRAY) {
            Optional<? extends OsmPrimitive> osm = parseFeatureCollection(feature.getJsonArray(FEATURES), true);
            if (osm.isPresent()) {
                primitive = osm.get();
                fillTagsFromFeature(feature, primitive);
            }
        }
        return Optional.ofNullable(primitive);
    }

    private Optional<Relation> parseGeometryCollection(final JsonObject feature, final JsonObject geometry, boolean createRelation) {
        List<RelationMember> relationMembers = new ArrayList<>(geometry.getJsonArray("geometries").size());
        for (JsonValue jsonValue : geometry.getJsonArray("geometries")) {
            parseGeometry(feature, jsonValue.asJsonObject()).map(osm -> new RelationMember("", osm)).ifPresent(relationMembers::add);
        }
        if (createRelation) {
            Relation relation = new Relation();
            relation.setMembers(relationMembers);
            getDataSet().addPrimitive(relation);
            return Optional.of(fillTagsFromFeature(feature, relation));
        }
        return Optional.empty();
    }

    private Optional<? extends OsmPrimitive> parseGeometry(final JsonObject feature, final JsonObject geometry) {
        if (geometry == null) {
            parseNullGeometry(feature);
            return Optional.empty();
        }

        switch (geometry.getString(TYPE)) {
            case "Point":
                return parsePoint(feature, geometry.getJsonArray(COORDINATES));
            case "MultiPoint":
                return parseMultiPoint(feature, geometry);
            case "LineString":
                return parseLineString(feature, geometry.getJsonArray(COORDINATES));
            case "MultiLineString":
                return parseMultiLineString(feature, geometry);
            case "Polygon":
                return parsePolygon(feature, geometry.getJsonArray(COORDINATES));
            case "MultiPolygon":
                return parseMultiPolygon(feature, geometry);
            case "GeometryCollection":
                return parseGeometryCollection(feature, geometry, true);
            default:
                parseUnknown(geometry);
                return Optional.empty();
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

    private Optional<Node> parsePoint(final JsonObject feature, final JsonArray coordinates) {
        return Optional.of(fillTagsFromFeature(feature, createNode(getLatLon(coordinates))));
    }

    private Optional<Relation> parseMultiPoint(final JsonObject feature, final JsonObject geometry) {
        List<RelationMember> nodes = new ArrayList<>(geometry.getJsonArray(COORDINATES).size());
        for (JsonValue coordinate : geometry.getJsonArray(COORDINATES)) {
            parsePoint(feature, coordinate.asJsonArray()).map(node -> new RelationMember("", node)).ifPresent(nodes::add);
        }
        Relation returnRelation = new Relation();
        returnRelation.setMembers(nodes);
        getDataSet().addPrimitive(returnRelation);
        return Optional.of(fillTagsFromFeature(feature, returnRelation));
    }

    private Optional<Way> parseLineString(final JsonObject feature, final JsonArray coordinates) {
        if (!coordinates.isEmpty()) {
            Optional<Way> way = createWay(coordinates, false);
            way.ifPresent(tWay -> fillTagsFromFeature(feature, tWay));
            return way;
        }
        return Optional.empty();
    }

    private Optional<Relation> parseMultiLineString(final JsonObject feature, final JsonObject geometry) {
        final List<RelationMember> ways = new ArrayList<>(geometry.getJsonArray(COORDINATES).size());
        for (JsonValue coordinate : geometry.getJsonArray(COORDINATES)) {
            parseLineString(feature, coordinate.asJsonArray()).map(way -> new RelationMember("", way)).ifPresent(ways::add);
        }
        final Relation relation = new Relation();
        relation.setMembers(ways);
        getDataSet().addPrimitive(relation);
        return Optional.of(fillTagsFromFeature(feature, relation));
    }

    private Optional<? extends OsmPrimitive> parsePolygon(final JsonObject feature, final JsonArray coordinates) {
        final int size = coordinates.size();
        if (size == 1) {
            Optional<Way> optionalWay = createWay(coordinates.getJsonArray(0), true);
            optionalWay.ifPresent(way -> fillTagsFromFeature(feature, way));
            return optionalWay;
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
            return Optional.of(multipolygon);
        }
        return Optional.empty();
    }

    private Optional<Relation> parseMultiPolygon(final JsonObject feature, final JsonObject geometry) {
        List<RelationMember> relationMembers = new ArrayList<>(geometry.getJsonArray(COORDINATES).size());
        for (JsonValue coordinate : geometry.getJsonArray(COORDINATES)) {
            parsePolygon(feature, coordinate.asJsonArray()).map(poly -> new RelationMember("", poly)).ifPresent(relationMembers::add);
        }
        Relation relation = new Relation();
        relation.setMembers(relationMembers);
        return Optional.of(fillTagsFromFeature(feature, relation));
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

    /**
     * Merge existing tags in primitive (if any) with the values given in the GeoJSON feature.
     * @param feature the GeoJSON feature
     * @param primitive the OSM primitive
     * @param <O> The primitive type
     * @return The primitive passed in as {@code primitive} for easier chaining
     */
    private static <O extends OsmPrimitive> O fillTagsFromFeature(final JsonObject feature, final O primitive) {
        if (feature != null) {
            TagCollection featureTags = getTags(feature);
            primitive.setKeys(new TagMap(primitive.isTagged() ? mergeAllTagValues(primitive, featureTags) : featureTags));
        }
        return primitive;
    }

    private static TagCollection mergeAllTagValues(final OsmPrimitive primitive, TagCollection featureTags) {
        TagCollection tags = TagCollection.from(primitive).union(featureTags);
        TagConflictResolutionUtil.applyAutomaticTagConflictResolution(tags);
        TagConflictResolutionUtil.normalizeTagCollectionBeforeEditing(tags, Collections.singletonList(primitive));
        TagConflictResolverModel tagModel = new TagConflictResolverModel();
        tagModel.populate(new TagCollection(tags), tags.getKeysWithMultipleValues());
        tagModel.actOnDecisions((k, d) -> d.keepAll());
        return tagModel.getAllResolutions();
    }

    private static void parseUnknown(final JsonObject object) {
        Logging.warn(tr("Unknown json object found {0}", object));
    }

    private static void parseNullGeometry(JsonObject feature) {
        Logging.warn(tr("Geometry of feature {0} is null", feature));
    }

    private static TagCollection getTags(final JsonObject feature) {
        final TagCollection tags = new TagCollection();

        if (feature.containsKey(PROPERTIES) && !feature.isNull(PROPERTIES)) {
            JsonValue properties = feature.get(PROPERTIES);
            if (properties != null && properties.getValueType() == JsonValue.ValueType.OBJECT) {
                for (Map.Entry<String, JsonValue> stringJsonValueEntry : properties.asJsonObject().entrySet()) {
                    final JsonValue value = stringJsonValueEntry.getValue();

                    if (value instanceof JsonString) {
                        tags.add(new Tag(stringJsonValueEntry.getKey(), ((JsonString) value).getString()));
                    } else if (value instanceof JsonObject) {
                        Logging.warn(
                            "The GeoJSON contains an object with property '" + stringJsonValueEntry.getKey()
                                + "' whose value has the unsupported type '" + value.getClass().getSimpleName()
                                + "'. That key-value pair is ignored!"
                        );
                    } else if (value.getValueType() != JsonValue.ValueType.NULL) {
                        tags.add(new Tag(stringJsonValueEntry.getKey(), value.toString()));
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
        } catch (IOException | IllegalArgumentException | JsonParsingException e) {
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
                List<Way> mpWays = new ArrayList<>();
                Way replacement = null;
                for (OsmPrimitive p : e.getPrimitives()) {
                    if (p.isTagged() && !p.referrers(Relation.class).findAny().isPresent())
                        replacement = (Way) p;
                    else if (p.referrers(Relation.class).anyMatch(Relation::isMultipolygon))
                        mpWays.add((Way) p);
                }
                if (replacement == null && mpWays.size() == 2) {
                    replacement = mpWays.remove(1);
                }
                if (replacement != null && mpWays.size() == 1) {
                    Way mpWay = mpWays.get(0);
                    for (Relation r : mpWay.referrers(Relation.class).filter(Relation::isMultipolygon)
                            .collect(Collectors.toList())) {
                        for (int i = 0; i < r.getMembersCount(); i++) {
                            if (r.getMember(i).getMember().equals(mpWay)) {
                                r.setMember(i, new RelationMember(r.getRole(i), replacement));
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
