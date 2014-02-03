// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Writes OSM data as a GeoJSON string, using JSR 353: Java API for JSON Processing (JSON-P).
 */
public class GeoJSONWriter {

    private OsmDataLayer layer;
    private static final boolean skipEmptyNodes = true;

    /**
     * Constructs a new {@code GeoJSONWriter}.
     * @param layer The OSM data layer to save
     */
    public GeoJSONWriter(OsmDataLayer layer) {
        this.layer = layer;
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
        Map<String, Object> config = new HashMap<String, Object>(1);
        config.put(JsonGenerator.PRETTY_PRINTING, pretty);
        JsonWriter writer = Json.createWriterFactory(config).createWriter(stringWriter);
        JsonObjectBuilder object = Json.createObjectBuilder()
                .add("type", "FeatureCollection")
                .add("generator", "JOSM");
        appendLayerBounds(layer.data, object);
        appendLayerFeatures(layer.data, object);
        writer.writeObject(object.build());
        String result = stringWriter.toString();
        writer.close();
        return result;
    }
    
    private static class GeometryPrimitiveVisitor implements PrimitiveVisitor {
        
        private final JsonObjectBuilder geomObj;
        
        public GeometryPrimitiveVisitor(JsonObjectBuilder geomObj) {
            this.geomObj = geomObj;
        }

        @Override
        public void visit(INode n) {
            geomObj.add("type", "Point");
            LatLon ll = n.getCoor();
            if (ll != null) {
                geomObj.add("coordinates", getCoorArray(n.getCoor()));
            }
        }

        @Override
        public void visit(IWay w) {
            geomObj.add("type", "LineString");
            if (w instanceof Way) {
                JsonArrayBuilder array = Json.createArrayBuilder();
                for (Node n : ((Way)w).getNodes()) {
                    LatLon ll = n.getCoor();
                    if (ll != null) {
                        array.add(getCoorArray(ll));
                    }
                }
                geomObj.add("coordinates", array);
            }
        }

        @Override
        public void visit(IRelation r) {
        }

        private JsonArrayBuilder getCoorArray(LatLon c) {
            return Json.createArrayBuilder().add(c.lon()).add(c.lat());
        }
    }

    protected static void appendPrimitive(OsmPrimitive p, JsonArrayBuilder array) {
        if (p.isIncomplete()) {
            return;
        } else if (skipEmptyNodes && p instanceof Node && p.getKeys().isEmpty()) {
            return;
        }

        // Properties
        final JsonObjectBuilder propObj = Json.createObjectBuilder();
        for (Entry<String, String> t : p.getKeys().entrySet()) {
            propObj.add(t.getKey(), t.getValue());
        }

        // Geometry
        final JsonObjectBuilder geomObj = Json.createObjectBuilder();
        p.accept(new GeometryPrimitiveVisitor(geomObj));

        // Build primitive JSON object
        array.add(Json.createObjectBuilder()
                .add("type", "Feature")
                .add("properties", propObj)
                .add("geometry", geomObj));
    }

    protected static void appendLayerBounds(DataSet ds, JsonObjectBuilder object) {
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

    protected static void appendBounds(Bounds b, JsonObjectBuilder object) {
        if (b != null) {
            object.add("bbox", Json.createArrayBuilder()
                    .add(b.getMinLon()).add(b.getMinLat())
                    .add(b.getMaxLon()).add(b.getMaxLat()));
        }
    }

    protected static void appendLayerFeatures(DataSet ds, JsonObjectBuilder object) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        if (ds != null) {
            for (Node n : ds.getNodes()) {
                appendPrimitive(n, array);
            }
            for (Way w : ds.getWays()) {
                appendPrimitive(w, array);
            }
        }
        object.add("features", array);
    }
}
