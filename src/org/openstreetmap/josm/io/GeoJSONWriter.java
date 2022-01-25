// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Writes OSM data as a GeoJSON string, using JSR 353: Java API for JSON Processing (JSON-P).
 * <p>
 * See <a href="https://tools.ietf.org/html/rfc7946">RFC7946: The GeoJSON Format</a>
 */
public class GeoJSONWriter {

    enum Options {
        /** If using the right hand rule, we have to ensure that the "right" side is the interior of the object. */
        RIGHT_HAND_RULE,
        /** Write OSM information to the feature properties field. This tries to follow the Overpass turbo format. */
        WRITE_OSM_INFORMATION,
        /** Skip empty nodes */
        SKIP_EMPTY_NODES
    }

    private final DataSet data;
    private static final Projection projection = Projections.getProjectionByCode("EPSG:4326"); // WGS 84
    private static final BooleanProperty SKIP_EMPTY_NODES = new BooleanProperty("geojson.export.skip-empty-nodes", true);
    private static final BooleanProperty UNTAGGED_CLOSED_IS_POLYGON = new BooleanProperty("geojson.export.untagged-closed-is-polygon", false);
    private static final Set<Way> processedMultipolygonWays = new HashSet<>();
    private EnumSet<Options> options = EnumSet.noneOf(Options.class);

    /**
     * This is used to determine that a tag should be interpreted as a json
     * object or array. The tag should have both {@link #JSON_VALUE_START_MARKER}
     * and {@link #JSON_VALUE_END_MARKER}.
     */
    static final String JSON_VALUE_START_MARKER = "{";
    /**
     * This is used to determine that a tag should be interpreted as a json
     * object or array. The tag should have both {@link #JSON_VALUE_START_MARKER}
     * and {@link #JSON_VALUE_END_MARKER}.
     */
    static final String JSON_VALUE_END_MARKER = "}";

    /**
     * Constructs a new {@code GeoJSONWriter}.
     * @param ds The OSM data set to save
     * @since 12806
     */
    public GeoJSONWriter(DataSet ds) {
        this.data = ds;
        if (Boolean.TRUE.equals(SKIP_EMPTY_NODES.get())) {
            this.options.add(Options.SKIP_EMPTY_NODES);
        }
    }

    /**
     * Set the options for this writer. See {@link Options}.
     * @param options The options to set.
     */
    void setOptions(final Options... options) {
        this.options.clear();
        this.options.addAll(Arrays.asList(options));
    }

    /**
     * Writes OSM data as a GeoJSON string (prettified).
     * @return The GeoJSON data
     */
    public String write() {
        return write(true);
    }

    /**
     * Writes OSM data as a GeoJSON string (prettified or not).
     * @param pretty {@code true} to have pretty output, {@code false} otherwise
     * @return The GeoJSON data
     * @since 6756
     */
    public String write(boolean pretty) {
        StringWriter stringWriter = new StringWriter();
        write(pretty, stringWriter);
        return stringWriter.toString();
    }

    /**
     * Writes OSM data as a GeoJSON string (prettified or not).
     * @param pretty {@code true} to have pretty output, {@code false} otherwise
     * @param writer The writer used to write results
     */
    public void write(boolean pretty, Writer writer) {
        Map<String, Object> config = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, pretty);
        try (JsonWriter jsonWriter = Json.createWriterFactory(config).createWriter(writer)) {
            JsonObjectBuilder object = Json.createObjectBuilder()
                    .add("type", "FeatureCollection")
                    .add("generator", "JOSM");
            appendLayerBounds(data, object);
            appendLayerFeatures(data, object);
            jsonWriter.writeObject(object.build());
        }
    }

    /**
     * Convert a primitive to a json object
     */
    private class GeometryPrimitiveVisitor implements OsmPrimitiveVisitor {

        private final JsonObjectBuilder geomObj;

        GeometryPrimitiveVisitor(JsonObjectBuilder geomObj) {
            this.geomObj = geomObj;
        }

        @Override
        public void visit(Node n) {
            geomObj.add("type", "Point");
            LatLon ll = n.getCoor();
            if (ll != null) {
                geomObj.add("coordinates", getCoorArray(null, ll));
            }
        }

        @Override
        public void visit(Way w) {
            if (w != null) {
                if (!w.isTagged() && processedMultipolygonWays.contains(w)) {
                    // no need to write this object again
                    return;
                }
                boolean writeAsPolygon = w.isClosed() && ((!w.isTagged() && UNTAGGED_CLOSED_IS_POLYGON.get())
                        || ElemStyles.hasAreaElemStyle(w, false));
                final List<Node> nodes = w.getNodes();
                if (writeAsPolygon && options.contains(Options.RIGHT_HAND_RULE) && Geometry.isClockwise(nodes)) {
                    Collections.reverse(nodes);
                }
                final JsonArrayBuilder array = getCoorsArray(nodes);
                if (writeAsPolygon) {
                    geomObj.add("type", "Polygon");
                    geomObj.add("coordinates", Json.createArrayBuilder().add(array));
                } else {
                    geomObj.add("type", "LineString");
                    geomObj.add("coordinates", array);
                }
            }
        }

        @Override
        public void visit(Relation r) {
            if (r == null || !r.isMultipolygon() || r.hasIncompleteMembers()) {
                return;
            }
            if (r.isMultipolygon()) {
                try {
                    this.visitMultipolygon(r);
                    return;
                } catch (MultipolygonBuilder.JoinedPolygonCreationException ex) {
                    Logging.warn("GeoJSON: Failed to export multipolygon {0}, falling back to other multi geometry types", r.getUniqueId());
                    Logging.warn(ex);
                }
            }
            // These are run if (a) r is not a multipolygon or (b) r is not a well-formed multipolygon.
            if (r.getMemberPrimitives().stream().allMatch(IWay.class::isInstance)) {
                this.visitMultiLineString(r);
            } else if (r.getMemberPrimitives().stream().allMatch(INode.class::isInstance)) {
                this.visitMultiPoints(r);
            } else {
                this.visitMultiGeometry(r);
            }
        }

        /**
         * Visit a multi-part geometry.
         * Note: Does not currently recurse down relations. RFC 7946 indicates that we
         * should avoid nested geometry collections. This behavior may change any time in the future!
         * @param r The relation to visit.
         */
        private void visitMultiGeometry(final Relation r) {
            final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            r.getMemberPrimitives().stream().filter(p -> !(p instanceof Relation))
                    .map(p -> {
                        final JsonObjectBuilder tempGeomObj = Json.createObjectBuilder();
                        p.accept(new GeometryPrimitiveVisitor(tempGeomObj));
                        return tempGeomObj.build();
                    }).forEach(jsonArrayBuilder::add);
            geomObj.add("type", "GeometryCollection");
            geomObj.add("geometries", jsonArrayBuilder);
        }

        /**
         * Visit a relation that only contains points
         * @param r The relation to visit
         */
        private void visitMultiPoints(final Relation r) {
            final JsonArrayBuilder multiPoint = Json.createArrayBuilder();
            r.getMembers().stream().map(RelationMember::getMember).filter(Node.class::isInstance).map(Node.class::cast)
                    .map(Node::getCoor).map(latLon -> getCoorArray(null, latLon))
                    .forEach(multiPoint::add);
            geomObj.add("type", "MultiPoint");
            geomObj.add("coordinates", multiPoint);
        }

        /**
         * Visit a relation that is a multi line string
         * @param r The relation to convert
         */
        private void visitMultiLineString(final Relation r) {
            final JsonArrayBuilder multiLine = Json.createArrayBuilder();
            r.getMembers().stream().map(RelationMember::getMember).filter(Way.class::isInstance).map(Way.class::cast)
                    .map(Way::getNodes).map(p -> {
                JsonArrayBuilder array = getCoorsArray(p);
                LatLon ll = p.get(0).getCoor();
                // since first node is not duplicated as last node
                return ll != null ? array.add(getCoorArray(null, ll)) : array;
            }).forEach(multiLine::add);
            geomObj.add("type", "MultiLineString");
            geomObj.add("coordinates", multiLine);
            processedMultipolygonWays.addAll(r.getMemberPrimitives(Way.class));
        }

        /**
         * Convert a multipolygon to geojson
         * @param r The relation to convert
         * @throws MultipolygonBuilder.JoinedPolygonCreationException See {@link MultipolygonBuilder#joinWays(Relation)}.
         * Note that if the exception is thrown, {@link #geomObj} will not have been modified.
         */
        private void visitMultipolygon(final Relation r) throws MultipolygonBuilder.JoinedPolygonCreationException {
                final Pair<List<MultipolygonBuilder.JoinedPolygon>, List<MultipolygonBuilder.JoinedPolygon>> mp =
                        MultipolygonBuilder.joinWays(r);
                final JsonArrayBuilder polygon = Json.createArrayBuilder();
                // Peek would theoretically be better for these two streams, but SonarLint doesn't like it.
                // java:S3864: "Stream.peek" should be used with caution
                final Stream<List<Node>> outer = mp.a.stream().map(MultipolygonBuilder.JoinedPolygon::getNodes).map(nodes -> {
                    final ArrayList<Node> tempNodes = new ArrayList<>(nodes);
                    tempNodes.add(tempNodes.get(0));
                    if (options.contains(Options.RIGHT_HAND_RULE) && Geometry.isClockwise(tempNodes)) {
                        Collections.reverse(nodes);
                    }
                    return nodes;
                });
                final Stream<List<Node>> inner = mp.b.stream().map(MultipolygonBuilder.JoinedPolygon::getNodes).map(nodes -> {
                    final ArrayList<Node> tempNodes = new ArrayList<>(nodes);
                    tempNodes.add(tempNodes.get(0));
                    // Note that we are checking !Geometry.isClockwise, which is different from the outer
                    // ring check.
                    if (options.contains(Options.RIGHT_HAND_RULE) && !Geometry.isClockwise(tempNodes)) {
                        Collections.reverse(nodes);
                    }
                    return nodes;
                });
                Stream.concat(outer, inner)
                        .map(p -> {
                            JsonArrayBuilder array = getCoorsArray(p);
                            LatLon ll = p.get(0).getCoor();
                            // since first node is not duplicated as last node
                            return ll != null ? array.add(getCoorArray(null, ll)) : array;
                        })
                        .forEach(polygon::add);
                final JsonArrayBuilder multiPolygon = Json.createArrayBuilder().add(polygon);
                geomObj.add("type", "MultiPolygon");
                geomObj.add("coordinates", multiPolygon);
                processedMultipolygonWays.addAll(r.getMemberPrimitives(Way.class));
        }

        private JsonArrayBuilder getCoorsArray(Iterable<Node> nodes) {
            final JsonArrayBuilder builder = Json.createArrayBuilder();
            for (Node n : nodes) {
                LatLon ll = n.getCoor();
                if (ll != null) {
                    builder.add(getCoorArray(null, ll));
                }
            }
            return builder;
        }
    }

    private JsonArrayBuilder getCoorArray(JsonArrayBuilder builder, LatLon c) {
        return getCoorArray(builder, projection.latlon2eastNorth(c));
    }

    private static JsonArrayBuilder getCoorArray(JsonArrayBuilder builder, EastNorth c) {
        return (builder != null ? builder : Json.createArrayBuilder())
                .add(BigDecimal.valueOf(c.getX()).setScale(11, RoundingMode.HALF_UP))
                .add(BigDecimal.valueOf(c.getY()).setScale(11, RoundingMode.HALF_UP));
    }

    protected void appendPrimitive(OsmPrimitive p, JsonArrayBuilder array) {
        if (p.isIncomplete() ||
            (this.options.contains(Options.SKIP_EMPTY_NODES) && p instanceof Node && p.getKeys().isEmpty())) {
            return;
        }

        // Properties
        final JsonObjectBuilder propObj = Json.createObjectBuilder();
        for (Map.Entry<String, String> t : p.getKeys().entrySet()) {
            // If writing OSM information, follow Overpass syntax (escape `@` with another `@`)
            final String key = options.contains(Options.WRITE_OSM_INFORMATION) && t.getKey().startsWith("@")
                    ? '@' + t.getKey() : t.getKey();
            propObj.add(key, convertValueToJson(t.getValue()));
        }
        if (options.contains(Options.WRITE_OSM_INFORMATION)) {
            // Use the same format as Overpass
            propObj.add("@id", p.getPrimitiveId().getType().getAPIName() + '/' + p.getUniqueId()); // type/id
            if (!p.isNew()) {
                propObj.add("@timestamp", Instant.ofEpochSecond(p.getRawTimestamp()).toString());
                propObj.add("@version", Integer.toString(p.getVersion()));
                propObj.add("@changeset", Long.toString(p.getChangesetId()));
            }
            if (p.getUser() != null) {
                propObj.add("@user", p.getUser().getName());
                propObj.add("@uid", p.getUser().getId());
            }
            if (options.contains(Options.WRITE_OSM_INFORMATION) && p.getReferrers(true).stream().anyMatch(Relation.class::isInstance)) {
                final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
                for (Relation relation : Utils.filteredCollection(p.getReferrers(), Relation.class)) {
                    final JsonObjectBuilder relationObject = Json.createObjectBuilder();
                    relationObject.add("rel", relation.getId());
                    Collection<RelationMember> members = relation.getMembersFor(Collections.singleton(p));
                    // Each role is a separate object in overpass-turbo geojson export. For now, just concat them.
                    relationObject.add("role",
                            members.stream().map(RelationMember::getRole).collect(Collectors.joining(";")));
                    final JsonObjectBuilder relationKeys = Json.createObjectBuilder();
                    // Uncertain if the @relation reltags need to be @ escaped. I don't think so, as example output
                    // didn't have any metadata in it.
                    for (Map.Entry<String, String> tag : relation.getKeys().entrySet()) {
                        relationKeys.add(tag.getKey(), convertValueToJson(tag.getValue()));
                    }
                    relationObject.add("reltags", relationKeys);
                }
                propObj.add("@relations", jsonArrayBuilder);
            }
        }
        final JsonObject prop = propObj.build();

        // Geometry
        final JsonObjectBuilder geomObj = Json.createObjectBuilder();
        p.accept(new GeometryPrimitiveVisitor(geomObj));
        final JsonObject geom = geomObj.build();

        if (!geom.isEmpty()) {
            // Build primitive JSON object
            array.add(Json.createObjectBuilder()
                    .add("type", "Feature")
                    .add("properties", prop.isEmpty() ? JsonValue.NULL : prop)
                    .add("geometry", geom.isEmpty() ? JsonValue.NULL : geom));
        }
    }

    private static JsonValue convertValueToJson(String value) {
        if (value.startsWith(JSON_VALUE_START_MARKER) && value.endsWith(JSON_VALUE_END_MARKER)) {
            try (JsonParser parser = Json.createParser(new StringReader(value))) {
                if (parser.hasNext() && parser.next() != null) {
                    return parser.getValue();
                }
            } catch (JsonParsingException e) {
                Logging.warn(e);
            }
        }
        return Json.createValue(value);
    }

    protected void appendLayerBounds(DataSet ds, JsonObjectBuilder object) {
        if (ds != null) {
            Iterator<Bounds> it = ds.getDataSourceBounds().iterator();
            if (it.hasNext()) {
                Bounds b = new Bounds(it.next());
                while (it.hasNext()) {
                    b.extend(it.next());
                }
                appendBounds(b, object);
            }
        }
    }

    protected void appendBounds(Bounds b, JsonObjectBuilder object) {
        if (b != null) {
            JsonArrayBuilder builder = Json.createArrayBuilder();
            getCoorArray(builder, b.getMin());
            getCoorArray(builder, b.getMax());
            object.add("bbox", builder);
        }
    }

    protected void appendLayerFeatures(DataSet ds, JsonObjectBuilder object) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        if (ds != null) {
            processedMultipolygonWays.clear();
            Collection<OsmPrimitive> primitives = ds.allNonDeletedPrimitives();
            // Relations first
            for (OsmPrimitive p : primitives) {
                if (p instanceof Relation)
                    appendPrimitive(p, array);
            }
            for (OsmPrimitive p : primitives) {
                if (!(p instanceof Relation))
                    appendPrimitive(p, array);
            }
            processedMultipolygonWays.clear();
        }
        object.add("features", array);
    }
}
